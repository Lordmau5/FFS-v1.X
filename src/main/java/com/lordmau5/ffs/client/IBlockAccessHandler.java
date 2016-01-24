package com.lordmau5.ffs.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by Dustin on 23.01.2016.
 */
public class IBlockAccessHandler implements IBlockAccess {

    protected final IBlockAccess ba;

    public IBlockAccessHandler(IBlockAccess iBlockAccess) {
        this.ba = iBlockAccess;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return ba.isSideSolid(pos, side, _default);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return ba.getStrongPower(pos, direction);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return ba.isAirBlock(pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        if (pos.getY() >= 0 && pos.getY() < 256) {
            return ba.getTileEntity(pos);
        } else {
            return null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return 15 << 20 | 15 << 4;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public WorldType getWorldType() {
        return ba.getWorldType();
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return ba.getBlockState(pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
        return ba.getBiomeGenForCoords(pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean extendedLevelsInChunkCache() {
        return ba.extendedLevelsInChunkCache();
    }

}