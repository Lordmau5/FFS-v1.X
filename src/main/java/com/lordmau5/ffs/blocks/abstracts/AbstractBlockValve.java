package com.lordmau5.ffs.blocks.abstracts;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import com.lordmau5.ffs.util.FFSStateProps;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Created by Dustin on 08.02.2016.
 */
public abstract class AbstractBlockValve extends Block {

    private Block blockDrop;

    public AbstractBlockValve(String name, Block blockDrop) {
        super(Material.iron);
        this.blockDrop = blockDrop;

        setUnlocalizedName(name);
        setRegistryName(name);
        setCreativeTab(CreativeTabs.tabRedstone);
        setHardness(5.0F);
        setResistance(10.0F);

        setDefaultState(blockState.getBaseState()
                .withProperty(FFSStateProps.TILE_VALID, false)
                .withProperty(FFSStateProps.TILE_MASTER, false)
                .withProperty(FFSStateProps.TILE_INSIDE_DUAL, EnumFacing.Axis.X));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof AbstractTankValve) {
            AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);
            valve.breakTank(null);
        }
        super.onBlockDestroyedByExplosion(world, pos, explosion);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if(!world.isRemote) {
            AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);
            if(valve.isValid())
                valve.breakTank(null);
        }

        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;

        AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);

        if(valve.isValid()) {
            if(GenericUtil.isFluidContainer(player.getHeldItem()))
                return GenericUtil.fluidContainerHandler(world, pos, valve, player, side);

            player.openGui(FancyFluidStorage.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        else {
            valve.buildTank(side.getOpposite());
        }
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.blockDrop);
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.SOLID || layer == EnumWorldBlockLayer.TRANSLUCENT;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FFSStateProps.TILE_VALID, FFSStateProps.TILE_MASTER, FFSStateProps.TILE_INSIDE_DUAL);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof AbstractTankValve) {
            AbstractTankValve valve = (AbstractTankValve) tile;

            state = state.withProperty(FFSStateProps.TILE_VALID, valve.isValid())
                    .withProperty(FFSStateProps.TILE_MASTER, valve.isMaster())
                    .withProperty(FFSStateProps.TILE_INSIDE_DUAL, (valve.getTileFacing() == null) ? EnumFacing.Axis.X : valve.getTileFacing().getAxis());
        }
        return state;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockState otherState = worldIn.getBlockState(pos.offset(side));
        return otherState != getBlockState();
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if(te instanceof AbstractTankValve) {
            AbstractTankValve valve = (AbstractTankValve)te;
            return valve.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public boolean canCreatureSpawn(IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }

}
