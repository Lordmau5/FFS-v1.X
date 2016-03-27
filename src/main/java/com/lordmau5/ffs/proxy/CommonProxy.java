package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

/**
 * Created by Dustin on 29.06.2015.
 */
public class CommonProxy {

    public void preInit() { }

    public void init() {
        MinecraftForge.EVENT_BUS.register(new FancyFluidStorage());

        if(Loader.isModLoaded("ComputerCraft")) {
            //new CCPeripheralProvider().register();
        }

        if(Loader.isModLoaded("OpenComputers")) {
            //new OCCompatibility().init();
        }
    }
}
