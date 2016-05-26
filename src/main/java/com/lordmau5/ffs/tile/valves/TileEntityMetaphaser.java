package com.lordmau5.ffs.tile.valves;

/**
 * Created by Dustin on 07.02.2016.
 */

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * - Slave-valve to extract the fluid
 * - Bring to other tank (?)
 * - 2 energy valves possible.
 *
 * - 1:1 ratio
 */
public class TileEntityMetaphaser extends AbstractTankValve implements IEnergyReceiver, IEnergyProvider {

    private int maxEnergyBuffer = -1;
    private boolean isExtract = false;

    @Override
    public void buildTank(EnumFacing inside) {
        super.buildTank(inside);
    }

    public TileEntityMetaphaser() {
        super();

        maxEnergyBuffer = -1;
    }

    public boolean getExtract() {
        return isExtract;
    }

    public void setExtract(boolean extract) {
        isExtract = extract;
        setNeedsUpdate();
    }

    @Override
    public void update() {
        super.update();

        if(isExtract)
            outputToTile();
    }

    private void outputToTile() {
        if(getFluidAmount() <= 0)
            return;

        BlockPos outsidePos = getPos().offset(getTileFacing().getOpposite());
        if(getWorld().isAirBlock(outsidePos))
            return;

        TileEntity outsideTile = getWorld().getTileEntity(outsidePos);
        if(outsideTile == null || !(outsideTile instanceof IEnergyReceiver))
            return;

        IEnergyReceiver receiver = (IEnergyReceiver) outsideTile;
        int maxReceive = receiver.receiveEnergy(getTileFacing(), getFluidAmount(), true);
        if(maxReceive > 0)
            receiver.receiveEnergy(getTileFacing(), internal_extractEnergy(maxReceive, false, false), false);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        tag.setBoolean("isExtract", isExtract);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        isExtract = tag.getBoolean("isExtract");
    }

    // CoFH / RF-API

    private int getMaxEnergyBuffer() {
        if(maxEnergyBuffer == -1)
            maxEnergyBuffer = (int) Math.ceil((float) getCapacity() / 200f);

        return maxEnergyBuffer;
    }

    @Override
    public void setValvePos(BlockPos masterValvePos) {
        super.setValvePos(masterValvePos);

        maxEnergyBuffer = -1;
    }

    private int convertForOutput(int amount) {
        return (int) Math.ceil((double) amount * 0.90d);
    }

    private int internal_extractEnergy(int extractEnergy, boolean simulate, boolean ignoreGetExtract) {
        if(!isValid())
            return 0;

        if(!getExtract() && !ignoreGetExtract)
            return 0;

        if(getFluidAmount() <= 0)
            return 0;

        int energy = convertForOutput(drain(extractEnergy, false).amount);

        if(simulate)
            return energy;

        return convertForOutput(drain(extractEnergy, true).amount);
    }

    @Override
    public int extractEnergy(EnumFacing facing, int maxExtract, boolean simulate) {
        maxExtract = Math.min(maxExtract, getFluidAmount());

        return internal_extractEnergy(maxExtract, simulate, true);
    }

    @Override
    public int receiveEnergy(EnumFacing facing, int maxReceive, boolean simulate) {
        if(!isValid())
            return 0;

        if(getExtract())
            return 0;

        if(getCapacity() - getFluidAmount() <= 0)
            return 0;

        maxReceive = Math.min(maxReceive, getMaxEnergyBuffer());

        maxReceive = fill(FluidRegistry.getFluidStack(FancyFluidStorage.fluidMetaphasedFlux.getName(), maxReceive), false);

        if(simulate)
            return maxReceive;

        return fill(FluidRegistry.getFluidStack(FancyFluidStorage.fluidMetaphasedFlux.getName(), maxReceive), true);
    }

    @Override
    public int getEnergyStored(EnumFacing facing) {
        return (int) Math.ceil((double) getFluidAmount() * 0.75d);
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
