package com.lordmau5.ffs.blocks;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityValve;
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
 * Created by Dustin on 28.06.2015.
 */
public class BlockValve extends Block {

    public BlockValve() {
        super(Material.iron);
        setUnlocalizedName("blockValve");
        setRegistryName("blockValve");
        setCreativeTab(CreativeTabs.tabRedstone);
        setHardness(5.0F); // Same hardness as an iron block
        setResistance(10.0F); // Same as hardness

        setDefaultState(blockState.getBaseState()
                .withProperty(FFSStateProps.VALVE_VALID, false)
                .withProperty(FFSStateProps.VALVE_MASTER, false)
                .withProperty(FFSStateProps.VALVE_INSIDE, EnumFacing.Axis.X));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityValve();
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve) world.getTileEntity(pos);
            valve.breakTank(null);
        }
        super.onBlockDestroyedByExplosion(world, pos, explosion);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if(!world.isRemote) {
            TileEntityValve valve = (TileEntityValve) world.getTileEntity(pos);
            valve.breakTank(null);
        }

        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;

        TileEntityValve valve = (TileEntityValve) world.getTileEntity(pos);

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
        return Item.getItemFromBlock(FancyFluidStorage.blockValve);
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.SOLID || layer == EnumWorldBlockLayer.TRANSLUCENT;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FFSStateProps.VALVE_VALID, FFSStateProps.VALVE_MASTER, FFSStateProps.VALVE_INSIDE);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve) tile;

            state = state.withProperty(FFSStateProps.VALVE_VALID, valve.isValid())
                    .withProperty(FFSStateProps.VALVE_MASTER, valve.isMaster())
                    .withProperty(FFSStateProps.VALVE_INSIDE, (valve.getInside() == null) ? EnumFacing.Axis.X : valve.getInside().getAxis());
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
        if(te instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve)te;
            return valve.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public boolean canCreatureSpawn(IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }
}
