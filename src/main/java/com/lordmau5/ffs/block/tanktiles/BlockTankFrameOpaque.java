package com.lordmau5.ffs.block.tanktiles;

import net.minecraft.block.state.IBlockState;

/**
 * Created by Dustin on 02.07.2015.
 */
public class BlockTankFrameOpaque extends BlockTankFrame {

    public BlockTankFrameOpaque() {
        super("blockTankFrameOpaque");
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
