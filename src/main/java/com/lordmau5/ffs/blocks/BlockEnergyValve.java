package com.lordmau5.ffs.blocks;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.valves.TileEntityEnergyValve;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 08.02.2016.
 */
public class BlockEnergyValve extends AbstractBlockValve {

    public BlockEnergyValve() {
        super("blockEnergyValve", FancyFluidStorage.blockEnergyValve);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityEnergyValve();
    }

}
