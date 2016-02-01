package com.lordmau5.ffs.client;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Created by Dustin on 23.01.2016.
 */
public class FrameBlockAccessWrapper extends IBlockAccessHandler {

    public FrameBlockAccessWrapper(IBlockAccess iBlockAccess) {
        super(iBlockAccess);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = super.getBlockState(pos);
        TileEntity tile = getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            state = frame.getBlockState();
        }
        return state;
    }

}
