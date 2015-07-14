package com.lordmau5.ffs.client;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.util.ExtendedBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

/**
 * Created by Dustin on 02.07.2015.
 */
public class FrameBlockAccessWrapper extends IBlockAccessHandler {

    public FrameBlockAccessWrapper(IBlockAccess iBlockAccess) {
        super(iBlockAccess);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Block block = super.getBlock(x, y, z);
        TileEntity tile = getTileEntity(x, y, z);
        if(tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getBlock() == null)
                return block;

            Block fac = frame.getBlock().getBlock();
            if(fac != null) {
                block = fac;
            }
        }
        return block;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int var1, int var2, int var3, int var4) {
        return ba.getLightBrightnessForSkyBlocks(var1, var2, var3, var4);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        TileEntity tile = getTileEntity(x, y, z);
        if(tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            ExtendedBlock exBlock = frame.getBlock();
            if(exBlock.getBlock() != null) {
                return exBlock.getMetadata();
            }
        }
        return super.getBlockMetadata(x, y, z);
    }

}
