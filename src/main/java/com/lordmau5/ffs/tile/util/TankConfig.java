package com.lordmau5.ffs.tile.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Created by Dustin on 20.01.2016.
 */
public class TankConfig {

    private int tankHeight;
    private FluidStack lockedFluid;

    private void resetVariables() {
        tankHeight = 0;
        lockedFluid = null;
    }

    public int getTankHeight() {
        return tankHeight;
    }

    public void setTankHeight(int tankHeight) {
        this.tankHeight = tankHeight;
    }

    public void lockFluid(FluidStack lockedFluid) {
        this.lockedFluid = lockedFluid;
    }

    public void unlockFluid() {
        this.lockedFluid = null;
    }

    public boolean isFluidLocked() {
        return lockedFluid != null;
    }

    public FluidStack getLockedFluid() {
        return lockedFluid;
    }

    public void readFromNBT(NBTTagCompound mainTag) {
        resetVariables();

        if(!mainTag.hasKey("tankConfig"))
            return;

        NBTTagCompound tag = mainTag.getCompoundTag("tankConfig");

        setTankHeight(tag.getInteger("tankHeight"));
        if(tag.hasKey("lockedFluid")) {
            lockFluid(FluidRegistry.getFluidStack(tag.getString("lockedFluid"), 1000));
        }
    }

    public void writeToNBT(NBTTagCompound mainTag) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("tankHeight", getTankHeight());
        if(getLockedFluid() != null) {
            tag.setString("lockedFluid", FluidRegistry.getFluidName(getLockedFluid()));
        }

        mainTag.setTag("tankConfig", tag);
    }

}
