package com.lordmau5.ffs.block.fluid;

import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;

/**
 * Created by Dustin on 19.02.2016.
 */
public class BlockMetaphasedFlux extends BlockFluidClassic {
    public BlockMetaphasedFlux(Fluid fluid) {
        super(fluid, Material.water);
    }
}
