package com.lordmau5.ffs.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.ISmartBlockModel;

import java.util.List;

/**
 * Created by Dustin on 11.01.2016.
 */
public class TankValveModel implements ISmartBlockModel {

    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        EnumWorldBlockLayer layer = MinecraftForgeClient.getRenderLayer();
        /*IBlockState frameState = ((IExtendedBlockState) state).getValue(FFSStateProps.FRAME_STATE);

        Minecraft mc = Minecraft.getMinecraft();
        if(frameState != null) {
            if(frameState.getBlock().canRenderInLayer(layer)) {
                IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(frameState);

                if (model instanceof ISmartBlockModel) {
                    model = ((ISmartBlockModel) model).handleBlockState(frameState);
                }

                return model;
            }
        }*/

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
        return null;
    }
}
