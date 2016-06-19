package com.lordmau5.ffs.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Created by Lordmau5 on 19.06.2016.
 */
public interface IProxy {

    void preInit(FMLPreInitializationEvent event);

    void init(FMLInitializationEvent event);
}
