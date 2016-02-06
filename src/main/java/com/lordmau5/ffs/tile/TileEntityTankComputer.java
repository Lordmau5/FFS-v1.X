package com.lordmau5.ffs.tile;

/**
 * Created by Dustin on 21.01.2016.
 */

import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.interfaces.IFacingTile;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
public class TileEntityTankComputer extends AbstractTankTile implements IFacingTile, IPeripheral {

    public List<TileEntityTankValve> getValves() {
        return getMasterValve().getAllValves().stream().filter(p -> p instanceof TileEntityTankValve).map(p -> (TileEntityTankValve) p).collect(Collectors.toList());
    }

    // Used by CC and OC
    public List<TileEntityTankValve> getValvesByName(String name) {
        List<TileEntityTankValve> valves = new ArrayList<>();
        if(getValves().isEmpty())
            return valves;

        for(TileEntityTankValve valve : getValves()) {
            if(valve.getTileName().toLowerCase().equals(name.toLowerCase()))
                valves.add(valve);
        }
        return valves;
    }

    @Override
    public void setTileFacing(EnumFacing facing) {
        this.tile_facing = facing;
    }

    @Override
    public EnumFacing getTileFacing() {
        if(getMasterValve() == null || !getMasterValve().isValid())
            return null;

        return this.tile_facing;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readTileFacingFromNBT(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        saveTileFacingToNBT(tag);
    }

    // ComputerCraft
    public String[] methodNames() {
        return new String[]{"getFluidName", "getFluidAmount", "getFluidCapacity", "setAutoOutput", "doesAutoOutput", "isFluidLocked", "getLockedFluid", "toggleFluidLock"};
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public String getType() {
        return "ffs_valve";
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public String[] getMethodNames() {
        return methodNames();
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
        switch(method) {
            case 0: { // getFluidName
                if(getMasterValve().getFluid() == null)
                    return null;
                return new Object[]{getMasterValve().getFluid().getLocalizedName()};
            }
            case 1: { // getFluidAmount
                return new Object[]{getMasterValve().getFluidAmount()};
            }
            case 2: { // getFluidCapacity
                return new Object[]{getMasterValve().getCapacity()};
            }
            case 3: { // setAutoOutput
                if(arguments.length == 1) {
                    if(!(arguments[0] instanceof Boolean)) {
                        throw new LuaException("expected argument 1 to be of type \"boolean\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    for(TileEntityTankValve valve : getValves())
                        valve.setAutoOutput((boolean) arguments[0]);

                    return new Object[]{(boolean) arguments[0]};
                }
                else if(arguments.length == 2) {
                    if(!(arguments[0] instanceof String)) {
                        throw new LuaException("expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    if(!(arguments[1] instanceof Boolean)) {
                        throw new LuaException("expected argument 2 to be of type \"boolean\", found \"" + arguments[1].getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityTankValve> valves = getValvesByName((String) arguments[0]);
                    if(valves.isEmpty()) {
                        throw new LuaException("no valves found");
                    }

                    List<String> valveNames = new ArrayList<>();
                    for(TileEntityTankValve valve : valves) {
                        valve.setAutoOutput((boolean) arguments[1]);
                        valveNames.add(valve.getTileName());
                    }
                    return new Object[]{valveNames};
                }
                else {
                    throw new LuaException("insufficient number of arguments found - expected 1 or 2, got " + arguments.length);
                }
            }
            case 4: { // doesAutoOutput
                if(arguments.length == 0) {
                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityTankValve valve : getValves()) {
                        valveOutputs.put(valve.getTileName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else if(arguments.length == 1) {
                    if(!(arguments[0] instanceof String)) {
                        throw new LuaException("expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityTankValve> valves = getValvesByName((String) arguments[0]);
                    if(valves.isEmpty()) {
                        throw new LuaException("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityTankValve valve : valves) {
                        valveOutputs.put(valve.getTileName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else {
                    throw new LuaException("insufficient number of arguments found - expected 1, got " + arguments.length);
                }
            }
            case 5: { // isFluidLocked
                return new Object[]{getMasterValve().getTankConfig().isFluidLocked()};
            }
            case 6: { // getLockedFluid
                return new Object[]{getMasterValve().getTankConfig().isFluidLocked() ? getMasterValve().getTankConfig().getLockedFluid().getLocalizedName() : null};
            }
            case 7: { // toggleFluidLock
                if(arguments.length == 0) {
                    if(getMasterValve().getFluid() == null) {
                        throw new LuaException("can't lock tank to fluid, no fluid in tank");
                    }

                    getMasterValve().toggleFluidLock(!getMasterValve().getTankConfig().isFluidLocked());

                    return new Object[]{getMasterValve().getTankConfig().isFluidLocked()};
                }
                else if(arguments.length == 1) {
                    if(!(arguments[0] instanceof Boolean)) {
                        throw new LuaException("expected argument 1 to be of type \"Boolean\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    boolean state = (boolean) arguments[0];
                    if(state && getMasterValve().getFluid() == null) {
                        throw new LuaException("can't lock tank to fluid, no fluid in tank");
                    }

                    getMasterValve().toggleFluidLock(state);

                    return new Object[]{getMasterValve().getTankConfig().isFluidLocked()};
                }
                else {
                    throw new LuaException("insufficient number of arguments found - expected 1, got " + arguments.length);
                }
            }
            default:
        }
        return null;
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public void attach(IComputerAccess computer) {

    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public void detach(IComputerAccess computer) {

    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public boolean equals(IPeripheral other) {
        return false;
    }
}
