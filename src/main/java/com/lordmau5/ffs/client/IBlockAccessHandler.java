package com.lordmau5.ffs.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Created by Dustin on 02.07.2015.
 */
public class IBlockAccessHandler implements IBlockAccess {

    protected final IBlockAccess ba;

    public IBlockAccessHandler(IBlockAccess iBlockAccess) {
       this.ba = iBlockAccess;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return ba.isSideSolid(x, y, z, side, _default);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int dir) {
        return ba.isBlockProvidingPowerTo(x, y, z, dir);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return ba.isAirBlock(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            return ba.getTileEntity(x, y, z);
        } else {
            return null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int var_) {
        return 15 << 20 | 15 << 4;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getHeight() {
        return ba.getHeight();
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return ba.getBlockMetadata(x, y, z);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return ba.getBlock(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BiomeGenBase getBiomeGenForCoords(int cX, int cZ) {
        return ba.getBiomeGenForCoords(cX, cZ);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean extendedLevelsInChunkCache() {
        return ba.extendedLevelsInChunkCache();
    }

}
