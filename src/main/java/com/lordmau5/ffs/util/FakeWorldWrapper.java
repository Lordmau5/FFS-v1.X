package com.lordmau5.ffs.util;

import com.lordmau5.ffs.block.tanktiles.BlockTankFrame;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankFrame;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Created by Dustin on 03.02.2016.
 */
public class FakeWorldWrapper extends World {

    public final World wrappedWorld;

    public FakeWorldWrapper(World wrappedWorld) {
        super(wrappedWorld.getSaveHandler(), wrappedWorld.getWorldInfo(), wrappedWorld.provider, wrappedWorld.theProfiler, wrappedWorld.isRemote);

        this.wrappedWorld = wrappedWorld;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = wrappedWorld.getBlockState(pos);
        if(state.getBlock() instanceof BlockTankFrame) {
            IBlockState fake_state = ((TileEntityTankFrame)wrappedWorld.getTileEntity(pos)).getBlockState();
            if(fake_state != null)
                state = fake_state;
        }
        return state;
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        IBlockState state = getBlockState(pos);
        if(state == null || !state.getBlock().hasTileEntity(state))
            return null;

        TileEntity tile = state.getBlock().createTileEntity(this, state);
        if(tile == null)
            return null;

        tile.setWorldObj(this);
        tile.setPos(pos);

        return tile;
    }

    @Override
    public boolean isBlockLoaded(BlockPos pos) {
        return wrappedWorld.isBlockLoaded(pos);
    }

    @Override
    public boolean isBlockLoaded(BlockPos pos, boolean allowEmpty) {
        return wrappedWorld.isBlockLoaded(pos, allowEmpty);
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return wrappedWorld.getChunkProvider();
    }

    @Override
    protected int getRenderDistanceChunks() {
        return 0;
    }
}
