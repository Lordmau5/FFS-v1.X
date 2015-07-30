package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.FancyFluidStorage;
import cpw.mods.fml.common.Loader;
import de.npe.gameanalytics.minecraft.MCSimpleAnalytics;
import net.minecraft.launchwrapper.Launch;

/**
 * Created by Dustin on 30.07.2015.
 */
public class FFSAnalytics extends MCSimpleAnalytics {

    static String GAME_KEY = "2b36a1907820e76a137e5922205123f5";
    static String SECRET_KEY = "2562f324cc5257e84df00399f2132cdf017379c1";

    public enum Category {
        TANK
    }

    public enum Event {
        TANK_BUILD,
        TANK_BREAK,
        FLUID_INTAKE,
        FLUID_OUTTAKE,
        RAIN_INTAKE
    }

    public FFSAnalytics() {
        super((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment") ? "1.7.10-1.3.3" : Loader.instance().activeModContainer().getVersion(), GAME_KEY, SECRET_KEY);
    }

    @Override
    public boolean isActive() {
        return FancyFluidStorage.instance.ANONYMOUS_STATISTICS;
    }

    public void event(Category cat, Event event) {
        if(isActive())
            eventDesign(cat.name() + ":" + event.name());
    }

    public void event(Category cat, Event event, Number number) {
        if(isActive())
            eventDesign(cat.name() + ":" + event.name(), number);
    }

}
