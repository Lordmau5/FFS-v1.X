package com.lordmau5.ffs.tile.util;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Created by Dustin on 20.01.2016.
 */
public class TankConfig {

    private int tankHeight;
    private FluidStack lockedFluid;

    private FluidStack fluidStack;
    private int fluidTemperature = 0;
    private int fluidCapacity = 0;

    private void resetVariables() {
        tankHeight = 0;
        lockedFluid = null;
        fluidStack = null;
        fluidTemperature = 0;
        fluidCapacity = 0;
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

    public FluidStack getFluidStack() {
        return fluidStack;
    }

    public void setFluidStack(FluidStack fluidStack) {
        this.fluidStack = fluidStack;
    }

    public int getFluidTemperature() {
        return fluidTemperature;
    }

    public void setFluidTemperature(int fluidTemperature) {
        this.fluidTemperature = fluidTemperature;
    }

    public int getFluidCapacity() {
        return fluidCapacity;
    }

    public void setFluidCapacity(int fluidCapacity) {
        this.fluidCapacity = fluidCapacity;
    }

    public void readFromNBT(NBTTagCompound mainTag) {
        resetVariables();

        if(!mainTag.hasKey("tankConfig"))
            return;

        if(mainTag.getBoolean("hasFluid")) {
            if (mainTag.hasKey("fluidName")) {
                try {
                    setFluidStack(new FluidStack(FluidRegistry.getFluid(mainTag.getString("fluidName")), mainTag.getInteger("fluidAmount")));
                    setFluidCapacity(mainTag.getInteger("fluidCapacity"));
                } catch (IllegalArgumentException e) {
                    System.out.println("Unable to load fluid: " + mainTag.getString("fluidName"));
                }
            }
        }

        NBTTagCompound tag = mainTag.getCompoundTag("tankConfig");

        setTankHeight(tag.getInteger("tankHeight"));
        if(tag.hasKey("lockedFluid")) {
            NBTBase base = tag.getTag("lockedFluid");
            if(base instanceof NBTTagString) { // Old store method, convert it.
                lockFluid(FluidRegistry.getFluidStack(String.valueOf(base), 1000));
            }
            if(base instanceof NBTTagCompound) {
                lockFluid(FluidStack.loadFluidStackFromNBT((NBTTagCompound) base));
            }
        }
        setFluidCapacity(tag.getInteger("capacity"));

        if(tag.hasKey("fluid")) {
            NBTTagCompound fluidTag = tag.getCompoundTag("fluid");

            setFluidStack(FluidStack.loadFluidStackFromNBT(fluidTag));
        }

    }

    public void writeToNBT(NBTTagCompound mainTag) {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("tankHeight", getTankHeight());
        if(getLockedFluid() != null) {
            NBTTagCompound fluidTag = new NBTTagCompound();
            getLockedFluid().writeToNBT(fluidTag);

            tag.setTag("lockedFluid", fluidTag);
        }
        tag.setInteger("capacity", getFluidCapacity());

        if(getFluidStack() != null) {
            NBTTagCompound fluidTag = new NBTTagCompound();
            getFluidStack().writeToNBT(fluidTag);

            tag.setTag("fluid", fluidTag);
        }

        mainTag.setTag("tankConfig", tag);
    }

}
