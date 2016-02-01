package com.lordmau5.ffs.client;

import com.google.common.collect.ImmutableList;
import com.lordmau5.ffs.util.FFSStateProps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
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
        EnumWorldBlockLayer layer = MinecraftForgeClient.getRenderLayer();
        IBlockState fake_state = ((IExtendedBlockState) state).getValue(FFSStateProps.FRAME_STATE);
        BlockPos pos = ((IExtendedBlockState) state).getValue(FFSStateProps.BLOCK_POS);

        Minecraft mc = Minecraft.getMinecraft();
        if(fake_state != null) {
            if(fake_state.getBlock().canRenderInLayer(layer)) {
                IBakedModel model = mc.getBlockRendererDispatcher().getModelFromBlockState(fake_state, mc.theWorld, pos);
                if(model instanceof ISmartBlockModel) {
                    model = ((ISmartBlockModel) model).handleBlockState(fake_state);
                }
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
