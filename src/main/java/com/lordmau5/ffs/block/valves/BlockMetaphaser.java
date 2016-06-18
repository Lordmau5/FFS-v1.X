package com.lordmau5.ffs.block.valves;

import com.lordmau5.ffs.block.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import com.lordmau5.ffs.util.FFSStateProps;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Created by Dustin on 08.02.2016.
 */
public class BlockMetaphaser extends AbstractBlockValve {

    public BlockMetaphaser() {
        super("blockMetaphaser");
    }

    @Override
    public void setDefaultState() {
        setDefaultState(blockState.getBaseState()
                .withProperty(FFSStateProps.TILE_VALID, false)
                .withProperty(FFSStateProps.TILE_METAPHASER_IS_OUTPUT, false)
                .withProperty(FFSStateProps.TILE_INSIDE_DUAL, EnumFacing.Axis.X));
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FFSStateProps.TILE_VALID, FFSStateProps.TILE_METAPHASER_IS_OUTPUT, FFSStateProps.TILE_INSIDE_DUAL);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityMetaphaser) {
            TileEntityMetaphaser metaphaser = (TileEntityMetaphaser) tile;

            state = state.withProperty(FFSStateProps.TILE_VALID, metaphaser.isValid())
                    .withProperty(FFSStateProps.TILE_METAPHASER_IS_OUTPUT, metaphaser.getExtract())
                    .withProperty(FFSStateProps.TILE_INSIDE_DUAL, (metaphaser.getTileFacing() == null) ? EnumFacing.Axis.X : metaphaser.getTileFacing().getAxis());
        }
        return state;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityMetaphaser();
    }
}
