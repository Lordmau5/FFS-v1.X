package com.lordmau5.ffs.compat;

import net.darkhax.tesla.api.ITeslaConsumer;
import net.darkhax.tesla.api.ITeslaHolder;
import net.darkhax.tesla.api.ITeslaProducer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/**
 * Created by Dustin on 01.06.2016.
 */
public class Capabilities {
    public static class Tesla {
        @CapabilityInject(ITeslaConsumer.class)
        public static Capability<ITeslaConsumer> RECEIVER = null;

        @CapabilityInject(ITeslaProducer.class)
        public static Capability<ITeslaProducer> PROVIDER = null;

        @CapabilityInject(ITeslaHolder.class)
        public static Capability<ITeslaHolder> HOLDER = null;
    }
}
