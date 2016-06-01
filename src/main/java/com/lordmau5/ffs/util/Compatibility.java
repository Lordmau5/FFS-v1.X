package com.lordmau5.ffs.util;

import net.minecraftforge.fml.common.ModAPIManager;

/**
 * Created by Dustin on 01.06.2016.
 */
public class Compatibility {

    private static boolean CoFHAPI_Loaded = false;

    public static void init() {
        CoFHAPI_Loaded = ModAPIManager.INSTANCE.hasAPI("CoFHAPI|energy");
    }

    public static boolean isEnergyModSupplied() {
        return CoFHAPI_Loaded;
    }

}
