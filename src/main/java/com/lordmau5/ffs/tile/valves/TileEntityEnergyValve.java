package com.lordmau5.ffs.tile.valves;

/**
 * Created by Dustin on 07.02.2016.
 */

import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Optional;

/**
 * - Slave-valve to extract the fluid
 * - Bring to other tank (?)
 * - 2 energy valves possible.
 *
 * - 1:1 ratio
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "buildcraft.api.transport.IPipeConnection", modid = "BuildCraftAPI|transport")
})
public class TileEntityEnergyValve extends AbstractTankValve implements IPipeConnection, IEnergyReceiver, IEnergyProvider {

    private int maxEnergyBuffer = 0;

    @Override
    public void buildTank(EnumFacing inside) {
        super.buildTank(inside);

        maxEnergyBuffer = (int) Math.ceil(getCapacity() / 200);
    }

    // BC

    @Optional.Method(modid = "BuildCraftAPI|transport")
    @Override
    public ConnectOverride overridePipeConnection(IPipeTile.PipeType pipeType, EnumFacing from) {
        if(pipeType != IPipeTile.PipeType.POWER)
            return ConnectOverride.DISCONNECT;

        return isValid() ? ConnectOverride.CONNECT : ConnectOverride.DISCONNECT;
    }

    // CoFH / RF-API

    @Override
    public int extractEnergy(EnumFacing facing, int maxExtract, boolean simulate) {
        if(!isValid())
            return 0;

        if(getFluidAmount() <= 0)
            return 0;

        return 0;
    }

    @Override
    public int receiveEnergy(EnumFacing facing, int maxReceive, boolean simulate) {
        if(!isValid())
            return 0;

        if(getCapacity() - getFluidAmount() <= 0)
            return 0;

        maxReceive = Math.min(maxReceive, maxEnergyBuffer);

        maxReceive = fill(FluidRegistry.getFluidStack(FluidRegistry.WATER.getName(), maxReceive), false);

        if(simulate)
            return maxReceive;

        return fill(FluidRegistry.getFluidStack(FluidRegistry.WATER.getName(), maxReceive), true);
    }

    @Override
    public int getEnergyStored(EnumFacing facing) {
        return (int) Math.ceil((float) getFluidAmount() * 0.75f);
    }

    @Override
    public int getMaxEnergyStored(EnumFacing facing) {
        return getCapacity();
    }

    @Override
    public boolean canConnectEnergy(EnumFacing facing) {
        return isValid();
    }
}
