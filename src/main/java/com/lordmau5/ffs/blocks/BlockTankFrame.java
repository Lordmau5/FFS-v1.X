package com.lordmau5.ffs.blocks;

import com.cricketcraft.chisel.api.IFacade;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.TankFrameRenderer;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import cpw.mods.fml.common.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Dustin on 02.07.2015.
 */
@Optional.Interface(iface = "com.cricketcraft.chisel.api.IFacade", modid = "chisel")
public class BlockTankFrame extends Block implements IFacade {

    public BlockTankFrame() {
        super(Material.rock);
        setBlockTextureName(FancyFluidStorage.modId + ":" + "blockValve");
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityTankFrame();
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            frame.setBlock(null);
            frame.breakFrame();
            frame.onBreak();
        }
        super.onBlockDestroyedByExplosion(world, x, y, z, explosion);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            if(!player.capabilities.isCreativeMode) {
                ArrayList<ItemStack> items = new ArrayList<>();

                Block block = frame.getBlock().getBlock();
                int meta = frame.getBlock().getMetadata();

                if(block.canSilkHarvest(world, player, x, y, z, meta) && EnchantmentHelper.getSilkTouchModifier(player)) {
                    ForgeEventFactory.fireBlockHarvesting(items, world, block, x, y, z, meta, 0, 1.0f, true, player);

                    ItemStack itemstack = new ItemStack(Item.getItemFromBlock(block), 1, meta);
                    items.add(itemstack);

                    for (ItemStack is : items)
                    {
                        this.dropBlockAsItem(world, x, y, z, is);
                    }
                }
                else {
                    ForgeEventFactory.fireBlockHarvesting(items, world, block, x, y, z, meta, 0, 1.0f, false, player);

                    items.addAll(block.getDrops(world, x, y, z, meta, 0));
                    for (ItemStack is : items)
                    {
                        this.dropBlockAsItem(world, x, y, z, is);
                    }
                }
            }
            frame.onBreak();
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
    {
        return null;
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            return frame.getBlock().getBlock().getPlayerRelativeBlockHardness(player, world, x, y, z);
        }
        return super.getPlayerRelativeBlockHardness(player, world, x, y, z);
    }

    @Override
    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        return true;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        ForgeHooksClient.setRenderPass(pass);
        return true;
    }

    @Override
    public int getRenderType() {
        return TankFrameRenderer.id;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            TileEntityValve valve = frame.getValve();

            ExtendedBlock block = frame.getBlock();
            if(block != null) {
                if(valve != null && valve.getFluid() != null) {
                    return valve.getFluidLuminosity();
                }
            }
        }
        return super.getLightValue(world, x, y, z);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)) {
            return true;
        }
        if (player.isSneaking()) return false;

        TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
        if(frame != null && frame.getValve() != null) {
            TileEntityValve valve = frame.getValve();
            if (valve.isValid()) {
                if(GenericUtil.isFluidContainer(player.getHeldItem()))
                    return GenericUtil.fluidContainerHandler(world, x, y, z, valve, player);

                player.openGui(FancyFluidStorage.instance, 0, world, valve.xCoord, valve.yCoord, valve.zCoord);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            ExtendedBlock block = ((TileEntityTankFrame)tile).getBlock();
            if(block != null)
                return block.getBlock().getPickBlock(target, world, x, y, z, player);
        }
        return null;
    }

    public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getBlock() != null) {
                return frame.getBlock().getBlock().getFlammability(world, x, y, z, face);
            }
        }
        return 0;
    }

    @Override
    public boolean isBurning(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            return frame.burning;
        }
        return false;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            frame.onBreak();
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    @Optional.Method(modid = "chisel")
    public Block getFacade(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getValve() == null)
                return null;

            ExtendedBlock block = frame.getBlock();
            if(block != null)
                return block.getBlock();
        }
        return null;
    }

    @Override
    @Optional.Method(modid = "chisel")
    public int getFacadeMetadata(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getValve() == null)
                return 0;

            ExtendedBlock block = frame.getBlock();
            if(block != null)
                return block.getMetadata();
        }
        return 0;
    }

}
