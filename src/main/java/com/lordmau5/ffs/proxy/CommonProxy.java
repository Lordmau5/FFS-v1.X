package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.compat.oc.OCCompatibility;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Created by Dustin on 29.06.2015.
 */
public class CommonProxy implements IProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new FancyFluidStorage());

        if(Loader.isModLoaded("ComputerCraft")) {
            //new CCPeripheralProvider().register();
        }

        if(Loader.isModLoaded("OpenComputers")) {
            new OCCompatibility().init();
        }
    }
}
