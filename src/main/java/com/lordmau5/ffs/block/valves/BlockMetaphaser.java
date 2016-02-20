package com.lordmau5.ffs.block.valves;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.block.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import com.lordmau5.ffs.util.FFSStateProps;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Created by Dustin on 08.02.2016.
 */
public class BlockMetaphaser extends AbstractBlockValve {

    public BlockMetaphaser() {
        super("blockMetaphaser", FancyFluidStorage.blockMetaphaser);
    }

    @Override
    public void setDefaultState() {
        setDefaultState(blockState.getBaseState()
                .withProperty(FFSStateProps.TILE_VALID, false)
                .withProperty(FFSStateProps.TILE_METAPHASER_IS_OUTPUT, false)
                .withProperty(FFSStateProps.TILE_INSIDE_DUAL, EnumFacing.Axis.X));
    }

    @Override
    public BlockState createBlockState() {
        return new BlockState(this, FFSStateProps.TILE_VALID, FFSStateProps.TILE_METAPHASER_IS_OUTPUT, FFSStateProps.TILE_INSIDE_DUAL);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityMetaphaser) {
            TileEntityMetaphaser valve = (TileEntityMetaphaser) tile;

            state = state.withProperty(FFSStateProps.TILE_VALID, valve.isValid())
                    .withProperty(FFSStateProps.TILE_METAPHASER_IS_OUTPUT, valve.isExtract)
                    .withProperty(FFSStateProps.TILE_INSIDE_DUAL, (valve.getTileFacing() == null) ? EnumFacing.Axis.X : valve.getTileFacing().getAxis());
        }
        return state;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityMetaphaser();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntityMetaphaser valve = (TileEntityMetaphaser) world.getTileEntity(pos);

        if(valve.isValid()) {
            if(GenericUtil.isFluidContainer(player.getHeldItem()))
                return GenericUtil.fluidContainerHandler(world, pos, valve, player, side);

            if (player.isSneaking()) {
                if(!world.isRemote)
                    valve.setExtract(!valve.isExtract);

                return true;
            }
            player.openGui(FancyFluidStorage.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        else {
            if (player.isSneaking()) return false;

            valve.buildTank(side.getOpposite());
        }
        return true;
    }
}
