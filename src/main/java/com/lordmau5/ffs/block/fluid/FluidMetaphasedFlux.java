package com.lordmau5.ffs.block.fluid;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

/**
 * Created by Dustin on 19.02.2016.
 */
public class FluidMetaphasedFlux extends Fluid {

    private static ResourceLocation still = new ResourceLocation("ffs:textures/blocks/power/fluid/metaphased_RF");
    private static ResourceLocation flowing = new ResourceLocation("ffs:textures/blocks/power/fluid/metaphased_RF_Flow");

    public FluidMetaphasedFlux() {
        super("metaphasedFlux", still, flowing);
    }

    @Override
    public int getColor() {
        return 0xFFFFFFFF;
    }
}
