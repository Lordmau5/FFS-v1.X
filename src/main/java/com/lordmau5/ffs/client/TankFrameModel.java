package com.lordmau5.ffs.client;

import com.google.common.collect.Lists;
import com.lordmau5.ffs.util.FFSStateProps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.util.List;

/**
 * Created by Dustin on 11.01.2016.
 */
public class TankFrameModel implements IBakedModel {

    private IBakedModel model;
    private TextureAtlasSprite cachedParticleTexture;

    @Override
    public List<BakedQuad> getQuads(IBlockState iBlockState, EnumFacing enumFacing, long l) {
        model = ((IExtendedBlockState) iBlockState).getValue(FFSStateProps.FRAME_MODEL);
        IBlockState fake_state = ((IExtendedBlockState) iBlockState).getValue(FFSStateProps.FRAME_STATE);

        if(model == null)
            return Lists.newArrayList();

        if(fake_state != null) {
            if(fake_state.getBlock().canRenderInLayer(fake_state, MinecraftForgeClient.getRenderLayer())) {
                return model.getQuads(fake_state, enumFacing, l);
            }
        }
        return Lists.newArrayList();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return model != null && model.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return model != null && model.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return model != null && model.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        if(model != null && model != this) {
            cachedParticleTexture = model.getParticleTexture();
        }
        return cachedParticleTexture != null ? cachedParticleTexture : Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stone");
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return model != null ? model.getOverrides() : null;
    }
}
