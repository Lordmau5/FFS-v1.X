package com.lordmau5.ffs.client;

import com.google.common.collect.ImmutableList;
import com.lordmau5.ffs.util.FFSStateProps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.util.List;

/**
 * Created by Dustin on 11.01.2016.
 */
public class TankFrameModel implements ISmartBlockModel {

    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        IBakedModel model = ((IExtendedBlockState) state).getValue(FFSStateProps.FRAME_MODEL);
        IBlockState fake_state = ((IExtendedBlockState) state).getValue(FFSStateProps.FRAME_STATE);

        if(fake_state != null) {
            if(fake_state.getBlock().canRenderInLayer(MinecraftForgeClient.getRenderLayer())) {
                return model;
            }
        }

        return this;
    }

    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing p_177551_1_) {
        return ImmutableList.of();
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return ImmutableList.of();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return null;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }
}
