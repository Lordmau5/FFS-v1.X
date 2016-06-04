package com.lordmau5.ffs.compat;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModAPIManager;

/**
 * Created by Dustin on 02.06.2016.
 */
public enum Compatibility {

    INSTANCE;

    public boolean isCoFHLoaded;
    public boolean isIC2Loaded;
    public boolean isTeslaLoaded;

    public boolean isWAILALoaded;
    public boolean isOpenComputersLoaded;

    public void init() {
        isCoFHLoaded = ModAPIManager.INSTANCE.hasAPI("CoFHAPI|energy");
        isIC2Loaded = ModAPIManager.INSTANCE.hasAPI("IC2API");
        isTeslaLoaded = ModAPIManager.INSTANCE.hasAPI("Tesla|API");

        isWAILALoaded = Loader.isModLoaded("Waila");
        isOpenComputersLoaded = Loader.isModLoaded("OpenComputers");
    }

    public boolean isEnergyModSupplied() {
        return isCoFHLoaded || isIC2Loaded || isTeslaLoaded;
    }

}
