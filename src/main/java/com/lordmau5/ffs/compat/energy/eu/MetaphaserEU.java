package com.lordmau5.ffs.compat.energy.eu;

/**
 * Created by Dustin on 02.06.2016.
 */

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import com.lordmau5.ffs.util.GenericUtil;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Handles IC2 Energy Units
 */
public enum MetaphaserEU {

    INSTANCE;

    public void load(TileEntityMetaphaser metaphaser) {
        if(!metaphaser.getWorld().isRemote)
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(metaphaser));
    }

    public void unload(TileEntityMetaphaser metaphaser) {
        if(!metaphaser.getWorld().isRemote)
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(metaphaser));
    }

    public double getDemandedEnergy(TileEntityMetaphaser metaphaser) {
        return Math.ceil((double) (metaphaser.getCapacity() - metaphaser.getFluidAmount()) / 4d);
    }

    public int getSinkTier(TileEntityMetaphaser metaphaser) {
        return Integer.MAX_VALUE;
    }

    public double injectEnergy(TileEntityMetaphaser metaphaser, EnumFacing directionFrom, double amount, double voltage) {
        if(!metaphaser.isValid())
            return amount;

        if(metaphaser.getExtract())
            return amount;

        if(metaphaser.getCapacity() - metaphaser.getFluidAmount() <= 0)
            return amount;

        int i_power = (int) Math.floor(amount / 4);
        double rest = amount % 4;

        int accepted = metaphaser.fill(FluidRegistry.getFluidStack(FancyFluidStorage.fluidMetaphasedFlux.getName(), i_power), true);

        return ((i_power - accepted) * 4) + rest;
    }

    public boolean acceptsEnergyFrom(TileEntityMetaphaser metaphaser, IEnergyEmitter emitter, EnumFacing side) {
        return metaphaser.isValid() && !metaphaser.getExtract();
    }

    public double getOfferedEnergy(TileEntityMetaphaser metaphaser) {
        return Math.min(128d, metaphaser.getFluidAmount() * 4 * GenericUtil.calculateEnergyLoss());
    }

    public void drawEnergy(TileEntityMetaphaser metaphaser, double amount) {
        if(!metaphaser.isValid())
            return;

        if(!metaphaser.getExtract())
            return;

        if(metaphaser.getFluidAmount() <= 0)
            return;

        double actualDrain = amount / 4d * (1d + (1d - GenericUtil.calculateEnergyLoss()));
        double overflow = actualDrain % 1;
        if(overflow == 0.0d || amount < 128d) {
            metaphaser.ic2Overflow = 0.0d;
        }
        else {
            metaphaser.ic2Overflow += overflow;
            if (metaphaser.ic2Overflow > 1.0d) {
                actualDrain += 1;
                metaphaser.ic2Overflow = 0.0d;
            }
        }
        int maxDrain = (int) Math.round(actualDrain);
        metaphaser.drain(maxDrain, true);
    }

    public int getSourceTier(TileEntityMetaphaser metaphaser) {
        return 2; // MV
    }

    public boolean emitsEnergyTo(TileEntityMetaphaser metaphaser, IEnergyAcceptor receiver, EnumFacing side) {
        return metaphaser.isValid() && metaphaser.getExtract();
    }

}
