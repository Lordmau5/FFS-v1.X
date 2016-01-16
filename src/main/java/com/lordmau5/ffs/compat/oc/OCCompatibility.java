package com.lordmau5.ffs.compat.oc;

import com.lordmau5.ffs.tile.TileEntityValve;
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
 * Created by Dustin on 16.01.2016.
 */
public class OCCompatibility {

    public void init() {
        Driver.add(new OpenComputersDriver());
    }

    public static class OpenComputersDriver extends DriverTileEntity {

        public static class InternalManagedEnvironment extends ManagedEnvironmentOCTile<TileEntityValve> {
            public InternalManagedEnvironment(TileEntityValve tile) {
                super(tile, "ffs_valve");
            }

            @Override
            public int priority() {
                return 1;
            }

            @Callback(doc = "function():string;  Returns the fluid name, if the tank contains a fluid.")
            public Object[] getFluidName(Context c, Arguments a) {
                if(tile.getFluid() == null)
                    return null;
                return new Object[]{tile.getFluid().getLocalizedName()};
            }

            @Callback(doc = "function():number;  Returns the fluid amount. If there is no fluid, returns 0.")
            public Object[] getFluidAmount(Context c, Arguments a) {
                return new Object[]{tile.getFluidAmount()};
            }

            @Callback(doc = "function():number;  Returns the tank capacity.")
            public Object[] getFluidCapacity(Context c, Arguments a) {
                return new Object[]{tile.getCapacity()};
            }

            @Callback(doc = "function():number;  Returns the tank capacity.")
            public Object[] getAutoOutput(Context context, Arguments args) throws Exception {
                if(args.count() == 1) {
                    if(!(args.isBoolean(0))) {
                        throw new Exception("expected argument 1 to be of type \"boolean\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    for(TileEntityValve valve : tile.getAllValves())
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

                    List<TileEntityValve> valves = tile.getValvesByName(args.checkString(0));
                    if(valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    List<String> valveNames = new ArrayList<>();
                    for(TileEntityValve valve : valves) {
                        valve.setAutoOutput(args.checkBoolean(1));
                        valveNames.add(valve.getValveName());
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
                    for(TileEntityValve valve : tile.getAllValves()) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else if(args.count() == 1) {
                    if(!(args.isString(0))) {

                        //throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityValve> valves = tile.getValvesByName(args.checkString(0));
                    if(valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityValve valve : valves) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else {
                    throw new Exception("insufficient number of arguments found - expected 1, got " + args.count());
                }
            }
        }

        @Override
        public Class<?> getTileEntityClass() {
            return TileEntityValve.class;
        }

        @Override
        public ManagedEnvironment createEnvironment(World world, BlockPos pos) {
            return new InternalManagedEnvironment(((TileEntityValve) world.getTileEntity(pos)));
        }
    }

}
