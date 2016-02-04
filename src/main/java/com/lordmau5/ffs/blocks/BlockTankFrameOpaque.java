package com.lordmau5.ffs.blocks;

/**
 * Created by Dustin on 02.07.2015.
 */
public class BlockTankFrameOpaque extends BlockTankFrame {

    public BlockTankFrameOpaque() {
        super("blockTankFrameOpaque");
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean isFullCube() {
        return true;
    }
}
