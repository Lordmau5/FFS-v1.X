package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.compat.RiMEventHandler;
import me.planetguy.remaininmotion.api.event.EventManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

/**
 * Created by Dustin on 29.06.2015.
 */
public class CommonProxy {

    public boolean BUILDCRAFT_LOADED;

//    public IIcon tex_Valve, tex_ValveItem;
//    public IIcon[] tex_SlaveValve, tex_MasterValve;

    public void preInit() { }

    public void init() {
        MinecraftForge.EVENT_BUS.register(new FancyFluidStorage());

        BUILDCRAFT_LOADED = Loader.isModLoaded("BuildCraftAPI|Transport");
        //if(Loader.isModLoaded("ComputerCraft")) {
            //new CCPeripheralProvider().register();
        //}

        if(Loader.isModLoaded("JAKJ_RedstoneInMotion")) {
            EventManager.registerEventHandler(new RiMEventHandler());
        }
    }
}
