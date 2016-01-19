package com.lordmau5.ffs.blocks;

import net.minecraftforge.fml.common.Optional;

/**
 * Created by Dustin on 02.07.2015.
 */
@Optional.Interface(iface = "com.cricketcraft.chisel.api.IFacade", modid = "chisel")
public class BlockTankFrameOpaque extends BlockTankFrame
        //implements IFacade
        {

    public BlockTankFrameOpaque() {
        super("blockTankFrameOpaque");
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }
}
