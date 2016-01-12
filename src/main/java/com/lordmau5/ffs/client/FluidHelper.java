package com.lordmau5.ffs.client;

/**
 * Created by Dustin on 10.07.2015.
 */

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;

public final class FluidHelper {

    public enum FluidType {
        FLOWING,
        STILL,
        FROZEN
    }

    private static Map<FluidType, Map<Fluid, TextureAtlasSprite>> textureMap = Maps.newHashMap();
    private static TextureAtlasSprite missingIcon = null;

    public FluidHelper() {}

    public static void initTextures(TextureMap map) {
        missingIcon = map.getMissingSprite();

        textureMap.clear();

        for (FluidType type : FluidType.values()) {
            textureMap.put(type, new HashMap<Fluid, TextureAtlasSprite>());
        }

        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
            if (fluid.getFlowing() != null) {
                String flow = fluid.getFlowing().toString();
                TextureAtlasSprite sprite;
                if (map.getTextureExtry(flow) != null) {
                    sprite = map.getTextureExtry(flow);
                } else {
                    sprite = map.registerSprite(fluid.getStill());
                }
                textureMap.get(FluidType.FLOWING).put(fluid, sprite);
            }

            if (fluid.getStill() != null) {
                String still = fluid.getStill().toString();
                TextureAtlasSprite sprite;
                if (map.getTextureExtry(still) != null) {
                    sprite = map.getTextureExtry(still);
                } else {
                    sprite = map.registerSprite(fluid.getStill());
                }
                textureMap.get(FluidType.STILL).put(fluid, sprite);
            }
        }
    }

    public static final ResourceLocation BLOCK_TEXTURE = TextureMap.locationBlocksTexture;

    public static TextureAtlasSprite getFluidTexture(FluidStack stack, FluidType type) {
        if (stack == null) {
            return missingIcon;
        }
        return getFluidTexture(stack.getFluid(), type);
    }

    public static TextureAtlasSprite getFluidTexture(Fluid fluid, FluidType type) {
        if (fluid == null || type == null) {
            return missingIcon;
        }
        Map<Fluid, TextureAtlasSprite> map = textureMap.get(type);
        return map.containsKey(fluid) ? map.get(fluid) : missingIcon;
    }
}