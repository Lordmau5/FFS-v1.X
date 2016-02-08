package com.lordmau5.ffs.blocks;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 28.06.2015.
 */
public class BlockFluidValve extends AbstractBlockValve {

    public BlockFluidValve() {
        super("blockFluidValve", FancyFluidStorage.blockFluidValve);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityFluidValve();
    }

}
