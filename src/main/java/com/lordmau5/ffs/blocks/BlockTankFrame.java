package com.lordmau5.ffs.blocks;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.ITankTile;
import com.lordmau5.ffs.tile.ITankValve;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.util.FFSStateProps;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.chisel.api.IFacade;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Dustin on 02.07.2015.
 */
@Optional.InterfaceList(
        @Optional.Interface(iface = "team.chisel.api.IFacade", modid = "chisel")
)
public class BlockTankFrame extends Block implements IFacade {

    public BlockTankFrame() {
        super(Material.rock);
    }

    public BlockTankFrame(String name) {
        this();
        setUnlocalizedName(name);
        setRegistryName(name);
        setDefaultState(((IExtendedBlockState) blockState.getBaseState())
                .withProperty(FFSStateProps.FRAME_MODEL, null)
                .withProperty(FFSStateProps.FRAME_MODEL, null));
    }

    @Override
    public BlockState createBlockState() {
        return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[] { FFSStateProps.FRAME_MODEL, FFSStateProps.FRAME_STATE });
    }

    @Override
    public BlockState getBlockState() {
        return super.getBlockState();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTankFrame();
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(pos);
            frame.setBlockState(null);
            frame.breakFrame();
            frame.onBreak();
        }
        super.onBlockDestroyedByExplosion(world, pos, explosion);
    }

    @Override
    public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(pos);
            if(!player.capabilities.isCreativeMode) {
                ArrayList<ItemStack> items = new ArrayList<>();

                IBlockState state = frame.getBlockState();
                Block block = state.getBlock();

                if(block.canSilkHarvest(world, pos, state, player) && EnchantmentHelper.getSilkTouchModifier(player)) {
                    ForgeEventFactory.fireBlockHarvesting(items, world, pos, state, 0, 1.0f, true, player);

                    ItemStack itemstack = new ItemStack(Item.getItemFromBlock(block), 1, block.getMetaFromState(state));
                    items.add(itemstack);

                    for (ItemStack is : items)
                    {
                        spawnAsEntity(world, pos, is);
                    }
                }
                else {
                    ForgeEventFactory.fireBlockHarvesting(items, world, pos, state, 0, 1.0f, false, player);

                    items.addAll(block.getDrops(world, pos, state, 0));
                    for (ItemStack is : items)
                    {
                        spawnAsEntity(world, pos, is);
                    }
                }
            }
        }
        return super.removedByPlayer(world, pos, player, willHarvest);
    }

    @Override
    public Item getItemDropped(IBlockState block, Random rand, int fortune)
    {
        return null;
    }

    @Override
    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
         return true;
     }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getBlockState() == null)
                return state;

            return ((IExtendedBlockState) state).withProperty(FFSStateProps.FRAME_MODEL, frame.getFakeModel()).withProperty(FFSStateProps.FRAME_STATE, frame.getExtendedBlockState());
        } else {
            return state;
        }
    }

    @Override
    public int getLightValue(IBlockAccess world, BlockPos pos) {
        int lightValue = 0;

        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && (tile instanceof TileEntityTankFrame)) {
            lightValue = ((TileEntityTankFrame)tile).getLightValue();
        }

        return lightValue;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;

        ITankTile tile = (ITankTile) world.getTileEntity(pos);
        if (tile != null && tile.getMasterValve() != null) {
            ITankValve valve = tile.getMasterValve();
            if (valve.isValid()) {
                if (GenericUtil.isFluidContainer(player.getHeldItem()))
                    return GenericUtil.fluidContainerHandler(world, pos, valve, player, side);

                player.openGui(FancyFluidStorage.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if(tile == null || !(tile instanceof TileEntityTankFrame))
            return true;

        TileEntityTankFrame myFrame = (TileEntityTankFrame) tile;

        tile = worldIn.getTileEntity(pos.offset(side.getOpposite()));
        if(tile == null || !(tile instanceof TileEntityTankFrame))
            return true;

        TileEntityTankFrame otherFrame = (TileEntityTankFrame) tile;

        return myFrame.getBlockState() != otherFrame.getBlockState();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            frame.onBreak();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean canCreatureSpawn(IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }

    @Override
    public float getAmbientOcclusionLightValue() {
        return super.getAmbientOcclusionLightValue();
    }

    /**
     * Fake World Overrides!
     */

    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, EffectRenderer effectRenderer) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().addDestroyEffects(fakeWorld, pos, effectRenderer);
        }
        return super.addDestroyEffects(world, pos, effectRenderer);
    }


    @Override
    public boolean addHitEffects(World world, MovingObjectPosition target, EffectRenderer effectRenderer) {
        TileEntity tile = world.getTileEntity(target.getBlockPos());
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().addHitEffects(fakeWorld, target, effectRenderer);
        }
        return super.addHitEffects(world, target, effectRenderer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, BlockPos pos, int pass) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().colorMultiplier(fakeWorld, pos, pass);
        }
        return 16777215;
    }

    @Override
    public float getBlockHardness(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().getBlockHardness(fakeWorld, pos);
        }
        return super.getBlockHardness(world, pos);
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().getExplosionResistance(fakeWorld, pos, exploder, explosion);
        }
        return super.getExplosionResistance(world, pos, exploder, explosion);
    }

    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().getFlammability(fakeWorld, pos, face);
        }
        return super.getFlammability(world, pos, face);
    }

    @Override
    public int getLightOpacity(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().getLightOpacity(fakeWorld, pos);
        }
        return super.getLightOpacity(world, pos);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().getPickBlock(target, fakeWorld, pos, player);
        }
        return super.getPickBlock(target, world, pos, player);
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            World fakeWorld = ((TileEntityTankFrame) tile).getFakeWorld();
            IBlockState state = ((TileEntityTankFrame) tile).getBlockState();
            if(fakeWorld != null && state != null)
                return state.getBlock().getPlayerRelativeBlockHardness(player, fakeWorld, pos);
        }
        return super.getPlayerRelativeBlockHardness(player, world, pos);
    }

    /**
     * Chisel!
     */
    @Override
    public IBlockState getFacade(IBlockAccess world, BlockPos blockPos, EnumFacing enumFacing) {
        TileEntity tile = world.getTileEntity(blockPos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getMasterValve() == null)
                return null;

            return frame.getBlockState();
        }
        return world.getBlockState(blockPos);
    }
}
