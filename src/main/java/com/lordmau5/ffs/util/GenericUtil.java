package com.lordmau5.ffs.util;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dustin on 28.06.2015.
 */
public class GenericUtil {

    private static List<Block> blacklistedBlocks;
    private static List<ItemStack> glassList;

    public static void init() {
        glassList = OreDictionary.getOres("blockGlass");

        blacklistedBlocks = new ArrayList<>();

        blacklistedBlocks.add(Blocks.grass);
        blacklistedBlocks.add(Blocks.dirt);
        blacklistedBlocks.add(Blocks.bedrock);
        blacklistedBlocks.add(Blocks.redstone_lamp);
        blacklistedBlocks.add(Blocks.sponge);
    }

    public static boolean isBlockGlass(Block block, int metadata) {
        if(block == null || block.getMaterial() == Material.air)
            return false;

        if(block instanceof BlockGlass)
            return true;

        ItemStack is = new ItemStack(block, 1, metadata);

        if(block.getMaterial() == Material.glass && !is.getUnlocalizedName().contains("pane"))
            return true;

        for(ItemStack is2 : glassList) {
            if(is2.getUnlocalizedName().equals(is.getUnlocalizedName()))
                return true;
        }
        return false;
    }

    public static boolean isValidTankBlock(World world, Position3D pos, ExtendedBlock extendedBlock) {
        Block block = extendedBlock.getBlock();

        if (block.hasTileEntity(extendedBlock.getMetadata())) {
            TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            return tile != null && tile instanceof TileEntityTankFrame;
        }

        if(blacklistedBlocks.contains(block))
            return false;

        if(!block.isOpaqueCube())
            return false;

        if(!block.func_149730_j() || !block.isNormalCube())
            return false;

        if(block.canProvidePower())
            return false;

        if(block.getMaterial() == Material.sand)
            return false;

        return true;

    }

    public static boolean isFluidContainer(ItemStack playerItem) {
        if (playerItem == null)
            return false;

        return FluidContainerRegistry.isContainer(playerItem) || playerItem.getItem() instanceof IFluidContainerItem;

    }

    public static boolean fluidContainerHandler(World world, int x, int y, int z, TileEntityValve valve, EntityPlayer player) {
        ItemStack current = player.inventory.getCurrentItem();

        if (current != null) {
            // Handle FluidContainerRegistry
            if (FluidContainerRegistry.isContainer(current)) {
                FluidStack liquid = FluidContainerRegistry.getFluidForFilledItem(current);
                // Handle filled containers
                if (liquid != null) {
                    int qty = valve.fill(ForgeDirection.UNKNOWN, liquid, true);

                    if (qty != 0 && !player.capabilities.isCreativeMode) {
                        if (current.stackSize > 1) {
                            if (!player.inventory.addItemStackToInventory(FluidContainerRegistry.drainFluidContainer(current))) {
                                player.dropPlayerItemWithRandomChoice(FluidContainerRegistry.drainFluidContainer(current), false);
                            }

                            player.inventory.setInventorySlotContents(player.inventory.currentItem, GenericUtil.consumeItem(current));
                        } else {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, FluidContainerRegistry.drainFluidContainer(current));
                        }
                    }

                    return true;

                    // Handle empty containers
                } else {
                    FluidStack available = valve.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;

                    if (available != null) {
                        ItemStack filled = FluidContainerRegistry.fillFluidContainer(available, current);

                        liquid = FluidContainerRegistry.getFluidForFilledItem(filled);

                        if (liquid != null) {
                            if (!player.capabilities.isCreativeMode) {
                                if (current.stackSize > 1) {
                                    if (!player.inventory.addItemStackToInventory(filled)) {
                                        return false;
                                    } else {
                                        player.inventory.setInventorySlotContents(player.inventory.currentItem, GenericUtil.consumeItem(current));
                                    }
                                } else {
                                    player.inventory.setInventorySlotContents(player.inventory.currentItem, GenericUtil.consumeItem(current));
                                    player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
                                }
                            }

                            valve.drain(ForgeDirection.UNKNOWN, liquid.amount, true);

                            return true;
                        }
                    }
                }
            } else if (current.getItem() instanceof IFluidContainerItem) {
                if (current.stackSize != 1) {
                    return false;
                }

                if (!world.isRemote) {
                    IFluidContainerItem container = (IFluidContainerItem) current.getItem();
                    FluidStack liquid = container.getFluid(current);
                    FluidStack tankLiquid = valve.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
                    boolean mustDrain = liquid == null || liquid.amount == 0;
                    boolean mustFill = tankLiquid == null || tankLiquid.amount == 0;
                    if (mustDrain && mustFill) {
                        // Both are empty, do nothing
                    } else if (mustDrain || !player.isSneaking()) {
                        liquid = valve.drain(ForgeDirection.UNKNOWN, 1000, false);
                        int qtyToFill = container.fill(current, liquid, true);
                        valve.drain(ForgeDirection.UNKNOWN, qtyToFill, true);
                    } else {
                        if (liquid.amount > 0) {
                            int qty = valve.fill(ForgeDirection.UNKNOWN, liquid, false);
                            valve.fill(ForgeDirection.UNKNOWN, container.drain(current, qty, true), true);
                        }
                    }
                }

                return true;
            }
            return false;
        }
        return false;
    }

    public static ItemStack consumeItem(ItemStack stack) {
        if (stack.stackSize == 1) {
            if (stack.getItem().hasContainerItem(stack)) {
                return stack.getItem().getContainerItem(stack);
            } else {
                return null;
            }
        } else {
            stack.splitStack(1);

            return stack;
        }
    }

    private static Map<Position3D, ExtendedBlock> getBlocksBetweenPoints(World world, Position3D pos1, Position3D pos2) {
        Map<Position3D, ExtendedBlock> blocks = new HashMap<>();

        Position3D distance = pos2.getDistance(pos1);
        int dX, dY, dZ;
        dX = distance.getX();
        dY = distance.getY();
        dZ = distance.getZ();

        for(int x=0; x<=dX; x++)
            for(int y=0; y<=dY; y++)
                for(int z=0; z<=dZ; z++) {
                    Position3D pos = new Position3D(pos1.getX() + x, pos1.getY() + y, pos1.getZ() + z);
                    blocks.put(pos, new ExtendedBlock(world.getBlock(pos.getX(), pos.getY(), pos.getZ()), world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ())));
                }

        return blocks;
    }

    public static Map<Position3D, ExtendedBlock>[] getTankFrame(World world, Position3D bottomDiag, Position3D topDiag) {
        Map<Position3D, ExtendedBlock>[] maps = new HashMap[3];
        maps[0] = new HashMap<>(); // Frame Blocks
        maps[1] = new HashMap<>(); // Inside wall blocks
        maps[2] = new HashMap<>(); // Inside air

        int x1 = bottomDiag.getX();
        int y1 = bottomDiag.getY();
        int z1 = bottomDiag.getZ();
        int x2 = topDiag.getX();
        int y2 = topDiag.getY();
        int z2 = topDiag.getZ();

        // Calculate frames only
        for(Map.Entry<Position3D, ExtendedBlock> e : getBlocksBetweenPoints(world, new Position3D(x1, y1, z1), new Position3D(x2, y2, z2)).entrySet()) {
            Position3D p = e.getKey();
            if(((p.getX() == x1 || p.getX() == x2) && (p.getY() == y1 || p.getY() == y2)) ||
                ((p.getX() == x1 || p.getX() == x2) && (p.getZ() == z1 || p.getZ() == z2)) ||
                ((p.getY() == y1 || p.getY() == y2) && (p.getZ() == z1 || p.getZ() == z2))) {

                    maps[0].put(p, e.getValue());
            }
            else if(((p.getX() == x1 || p.getX() == x2) || (p.getY() == y1 || p.getY() == y2) || (p.getZ() == z1 || p.getZ() == z2))) {
                maps[1].put(p, e.getValue());
            }
            else {
                maps[2].put(p, e.getValue());
            }
        }

        return maps;
    }

    public static String intToFancyNumber(int number) {
        return NumberFormat.getIntegerInstance().format(number);
    }

}
