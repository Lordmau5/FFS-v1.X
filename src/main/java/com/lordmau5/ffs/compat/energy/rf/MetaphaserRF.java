package com.lordmau5.ffs.compat.energy.rf;

import cofh.api.energy.IEnergyReceiver;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Created by Dustin on 02.06.2016.
 */

/**
 * Handles Redstone Flux
 */
public enum MetaphaserRF {

    INSTANCE;

    public void outputToTile(TileEntityMetaphaser metaphaser) {
        if(metaphaser.getFluidAmount() <= 0)
            return;

        BlockPos outsidePos = metaphaser.getPos().offset(metaphaser.getTileFacing().getOpposite());
        if(metaphaser.getWorld().isAirBlock(outsidePos))
            return;

        TileEntity outsideTile = metaphaser.getWorld().getTileEntity(outsidePos);
        if(outsideTile == null || !(outsideTile instanceof IEnergyReceiver))
            return;

        IEnergyReceiver receiver = (IEnergyReceiver) outsideTile;
        int maxReceive = receiver.receiveEnergy(metaphaser.getTileFacing(), extractEnergy(metaphaser, EnumFacing.DOWN, metaphaser.getFluidAmount(), true, true), true);
        if(maxReceive > 0)
            receiver.receiveEnergy(metaphaser.getTileFacing(), extractEnergy(metaphaser, EnumFacing.DOWN, maxReceive, false, true), false);
    }

    public int getMaxEnergyBuffer(TileEntityMetaphaser metaphaser) {
        return (int) Math.ceil((float) metaphaser.getCapacity() / 200f);
    }

    public int convertForOutput(int amount) {
        return (int) Math.ceil((double) amount * 0.90d);
    }

    public int getMaxEnergyStored(TileEntityMetaphaser metaphaser, EnumFacing facing) {
        return metaphaser.getCapacity();
    }

    public boolean canConnectEnergy(TileEntityMetaphaser metaphaser, EnumFacing facing) {
        return metaphaser.isValid();
    }

    public int extractEnergy(TileEntityMetaphaser metaphaser, EnumFacing facing, int maxExtract, boolean simulate, boolean ignoreGetExtract) {
        if(!metaphaser.isValid())
            return 0;

        if(!metaphaser.getExtract() && !ignoreGetExtract)
            return 0;

        if(metaphaser.getFluidAmount() <= 0)
            return 0;

        maxExtract = Math.min(maxExtract, metaphaser.getFluidAmount());

        int energy = convertForOutput(metaphaser.drain(maxExtract, false).amount);

        if(simulate)
            return energy;

        return convertForOutput(metaphaser.drain(maxExtract, true).amount);
    }

    public int receiveEnergy(TileEntityMetaphaser metaphaser, EnumFacing facing, int maxReceive, boolean simulate) {
        if(!metaphaser.isValid())
            return 0;

        if(metaphaser.getExtract())
            return 0;

        if(metaphaser.getCapacity() - metaphaser.getFluidAmount() <= 0)
            return 0;

        maxReceive = Math.min(maxReceive, getMaxEnergyBuffer(metaphaser));

        maxReceive = metaphaser.fill(FluidRegistry.getFluidStack(FancyFluidStorage.fluidMetaphasedFlux.getName(), maxReceive), false);

        if(simulate)
            return maxReceive;

        return metaphaser.fill(FluidRegistry.getFluidStack(FancyFluidStorage.fluidMetaphasedFlux.getName(), maxReceive), true);
    }

}
