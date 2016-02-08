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
import net.minecraftforge.fml.common.Optional;

/**
 * - Use as master -> AbstractTankValve
 * - Slave-valve to extract the fluid
 * - Bring to other tank (?)
 * - 2 energy valves possible.
 */

public class TileEntityEnergyValve extends AbstractTankValve implements IPipeConnection, IEnergyReceiver, IEnergyProvider {



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
        return 0;
    }

    @Override
    public int receiveEnergy(EnumFacing facing, int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored(EnumFacing facing) {
        return 0;
    }

    @Override
    public int getMaxEnergyStored(EnumFacing facing) {
        return 0;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing facing) {
        return isValid();
    }
}
