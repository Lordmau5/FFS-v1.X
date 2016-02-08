package com.lordmau5.ffs.compat.oc;

import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankComputer;
import dan200.computercraft.api.lua.LuaException;
import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.api.prefab.ManagedEnvironment;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Vexatos
 * Adjusted to suit FFS.
 */
public class OCCompatibility {

    public void init() {
        Driver.add(new OpenComputersDriver());
    }

    public static class OpenComputersDriver extends DriverTileEntity {

        public static class InternalManagedEnvironment extends ManagedEnvironmentOCTile<TileEntityTankComputer> {
            public InternalManagedEnvironment(TileEntityTankComputer tile) {
                super(tile, "ffs_valve");
            }

            @Override
            public int priority() {
                return 1;
            }

            @Callback(doc = "function():string;  Returns the fluid name, if the tank contains a fluid.")
            public Object[] getFluidName(Context c, Arguments a) {
                if(tile.getMasterValve().getFluid() == null)
                    return null;
                return new Object[]{tile.getMasterValve().getFluid().getLocalizedName()};
            }

            @Callback(doc = "function():number;  Returns the fluid amount. If there is no fluid, returns 0.")
            public Object[] getFluidAmount(Context c, Arguments a) {
                return new Object[]{tile.getMasterValve().getFluidAmount()};
            }

            @Callback(doc = "function():number;  Returns the tank capacity.")
            public Object[] getFluidCapacity(Context c, Arguments a) {
                return new Object[]{tile.getMasterValve().getCapacity()};
            }

            @Callback(doc = "function([valveName:string]):object;  Returns the valves with their auto-output state in a list. If a valveName is supplied, it'll return a list of those with that name.")
            public Object[] setAutoOutput(Context context, Arguments args) throws Exception {
                if(args.count() == 1) {
                    if(!(args.isBoolean(0))) {
                        throw new Exception("expected argument 1 to be of type \"boolean\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    for(TileEntityFluidValve valve : tile.getValves())
                        valve.setAutoOutput(args.checkBoolean(0));

                    return new Object[]{args.checkBoolean(0)};
                }
                else if(args.count() == 2) {
                    if(!(args.isString(0))) {
                        throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    if(!(args.isBoolean(1))) {
                        throw new Exception("expected argument 2 to be of type \"boolean\", found \"" + args.checkAny(1).getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityFluidValve> valves = tile.getValvesByName(args.checkString(0));
                    if(valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    List<String> valveNames = new ArrayList<>();
                    for(TileEntityFluidValve valve : valves) {
                        valve.setAutoOutput(args.checkBoolean(1));
                        valveNames.add(valve.getTileName());
                    }
                    return new Object[]{valveNames};
                }
                else {
                    throw new Exception("insufficient number of arguments found - expected 1 or 2, got " + args.count());
                }
            }

            @Callback(doc = "function([valveName:string]):object;  Returns the valves with their auto-output state in a list. If a valveName is supplied, it'll return a list of those with that name.")
            public Object[] doesAutoOutput(Context context, Arguments args) throws Exception {
                if(args.count() == 0) {
                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityFluidValve valve : tile.getValves()) {
                        valveOutputs.put(valve.getTileName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else if(args.count() == 1) {
                    if(!(args.isString(0))) {
                        throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityFluidValve> valves = tile.getValvesByName(args.checkString(0));
                    if(valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityFluidValve valve : valves) {
                        valveOutputs.put(valve.getTileName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else {
                    throw new Exception("insufficient number of arguments found - expected 1, got " + args.count());
                }
            }

            @Callback(doc = "function():boolean;  Returns if the tank is locked to a certain fluid.")
            public Object[] isFluidLocked(Context c, Arguments a)
            {
                return new Object[]{tile.getMasterValve().getTankConfig().isFluidLocked()};
            }

            @Callback(doc = "function():string;  Returns the locked fluid name, if the tank is locked, otherwise null.")
            public Object[] getLockedFluid(Context c, Arguments a)
            {
                return new Object[]{tile.getMasterValve().getTankConfig().isFluidLocked() ? tile.getMasterValve().getTankConfig().getLockedFluid().getLocalizedName() : null};
            }

            @Callback(doc = "function([boolean:state]):boolean;  (Un-)locks the fluid in the tank. Returns the new state.")
            public Object[] toggleFluidLock(Context c, Arguments a) throws Exception
            {
                if(a.count() == 0) {
                    if(tile.getMasterValve().getFluid() == null) {
                        throw new Exception("can't lock tank to fluid, no fluid in tank");
                    }

                    tile.getMasterValve().toggleFluidLock(!tile.getMasterValve().getTankConfig().isFluidLocked());

                    return new Object[]{tile.getMasterValve().getTankConfig().isFluidLocked()};
                }
                else if(a.count() == 1) {
                    if(!a.isBoolean(1)) {
                        throw new LuaException("expected argument 1 to be of type \"Boolean\", found \"" + a.checkAny(1).getClass().getSimpleName() + "\"");
                    }

                    boolean state = a.checkBoolean(1);
                    if(state && tile.getMasterValve().getFluid() == null) {
                        throw new LuaException("can't lock tank to fluid, no fluid in tank");
                    }

                    tile.getMasterValve().toggleFluidLock(state);

                    return new Object[]{tile.getMasterValve().getTankConfig().isFluidLocked()};
                }
                else {
                    throw new Exception("insufficient number of arguments found - expected 1, got " + a.count());
                }
            }
        }

        @Override
        public Class<?> getTileEntityClass() {
            return TileEntityTankComputer.class;
        }

        @Override
        public ManagedEnvironment createEnvironment(World world, BlockPos pos) {
            return new InternalManagedEnvironment(((TileEntityTankComputer) world.getTileEntity(pos)));
        }
    }

}
