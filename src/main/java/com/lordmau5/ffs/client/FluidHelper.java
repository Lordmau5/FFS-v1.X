package com.lordmau5.ffs.client;

/**
 * Created by Dustin on 10.07.2015.
 */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;

public final class FluidHelper {

    public static IIcon getFluidTexture(Fluid fluid, boolean flowing) {
        if (fluid == null) {
            return null;
        }
        IIcon icon = flowing ? fluid.getFlowingIcon() : fluid.getStillIcon();
        if (icon == null) {
            icon = ((TextureMap) Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture)).getAtlasSprite("missingno");
        }
        return icon;
    }
}