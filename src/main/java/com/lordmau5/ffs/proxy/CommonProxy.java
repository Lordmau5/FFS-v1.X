package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.compat.CCPeripheralProvider;
import com.lordmau5.ffs.compat.oc.OCCompatibility;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModAPIManager;

/**
 * Created by Dustin on 29.06.2015.
 */
public class CommonProxy {

    public boolean BUILDCRAFT_LOADED;

    public void preInit() { }

    public void init() {
        MinecraftForge.EVENT_BUS.register(new FancyFluidStorage());

        BUILDCRAFT_LOADED = ModAPIManager.INSTANCE.hasAPI("BuildCraftAPI|transport");
        if(Loader.isModLoaded("ComputerCraft")) {
            new CCPeripheralProvider().register();
        }

        if(Loader.isModLoaded("OpenComputers")) {
            new OCCompatibility().init();
        }

        /*if(Loader.isModLoaded("JAKJ_RedstoneInMotion")) {
            EventManager.registerEventHandler(new RiMEventHandler());
        }*/
    }
}
