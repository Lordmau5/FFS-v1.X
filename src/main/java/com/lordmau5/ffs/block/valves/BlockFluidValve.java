package com.lordmau5.ffs.block.valves;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.block.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.util.FFSStateProps;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Created by Dustin on 28.06.2015.
 */
public class BlockFluidValve extends AbstractBlockValve {

    public BlockFluidValve() {
        super("blockFluidValve", FancyFluidStorage.blockFluidValve);
    }

    @Override
    public void setDefaultState() {
        setDefaultState(blockState.getBaseState()
                .withProperty(FFSStateProps.TILE_VALID, false)
                .withProperty(FFSStateProps.TILE_MASTER, false)
                .withProperty(FFSStateProps.TILE_INSIDE_DUAL, EnumFacing.Axis.X));
    }

    @Override
    public BlockState createBlockState() {
        return new BlockState(this, FFSStateProps.TILE_VALID, FFSStateProps.TILE_MASTER, FFSStateProps.TILE_INSIDE_DUAL);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityFluidValve) {
            TileEntityFluidValve valve = (TileEntityFluidValve) tile;

            state = state.withProperty(FFSStateProps.TILE_VALID, valve.isValid())
                    .withProperty(FFSStateProps.TILE_MASTER, valve.isMaster())
                    .withProperty(FFSStateProps.TILE_INSIDE_DUAL, (valve.getTileFacing() == null) ? EnumFacing.Axis.X : valve.getTileFacing().getAxis());
        }
        return state;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityFluidValve();
    }
}
