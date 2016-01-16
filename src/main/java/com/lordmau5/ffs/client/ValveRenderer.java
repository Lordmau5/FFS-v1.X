package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityValve;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

/**
 * Created by Dustin on 29.06.2015.
 */
public class ValveRenderer extends TileEntitySpecialRenderer {

    private int red, green, blue, alpha;

    private void preGL() {
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.enableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        bindTexture(TextureMap.locationBlocksTexture);
    }

    private void postGL() {
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.disableCull();

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage)
    {
        TileEntityValve valve = (TileEntityValve) tile;
        BlockPos valvePos = valve.getPos();

        if (valve == null || !valve.isValid() || !valve.isMaster())
            return;

        Tessellator t = Tessellator.getInstance();

        //if (valve.isMaster()) {
            BlockPos bottomDiag = valve.bottomDiagFrame;
            BlockPos topDiag = valve.topDiagFrame;
            if (bottomDiag == null || topDiag == null)
                return;

            int height = topDiag.getY() - bottomDiag.getY();
            int xSize = topDiag.getX() - bottomDiag.getX() + (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 0 : 1);
            int zSize = topDiag.getZ() - bottomDiag.getZ() + (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 0 : 1);

            if (valve.getCapacity() == 0 || valve.getFluidAmount() == 0)
                return;

            float fillPercentage = (float) valve.getFluidAmount() / (float) valve.getCapacity();

            if (fillPercentage > 0 && valve.getFluid() != null) {
                FluidStack fluid = valve.getFluid();

                int c = fluid.getFluid().getColor();
                red = c & 0xFF;
                green = (c >> 8) & 0xFF;
                blue = (c >> 16) & 0xFF;
                alpha = (c >> 24) & 0xFF;

                preGL();

                TextureAtlasSprite flowing = FluidHelper.getFluidTexture(fluid.getFluid(), FluidHelper.FluidType.FLOWING);
                TextureAtlasSprite still = FluidHelper.getFluidTexture(fluid.getFluid(), FluidHelper.FluidType.STILL);

                float stillMinU = still.getMinU(), stillMaxU = still.getMaxU(), stillMinV = still.getMinV(), stillMaxV = still.getMaxV();
                float flowMinU = flowing.getMinU(), flowMaxU = flowMinU + (stillMaxU - stillMinU), flowMinV_ = flowing.getMinV(), flowMaxV_ = flowMinV_ + (stillMaxV - stillMinV);

                GlStateManager.translate((float) x, (float) y, (float) z);
                GlStateManager.translate((float) (bottomDiag.getX() - valvePos.getX()), (float) bottomDiag.getY() - valvePos.getY() + 1, (float) (bottomDiag.getZ() - valvePos.getZ()));

                t.getWorldRenderer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                float pureRenderHeight = (height - 1) * fillPercentage;
                boolean isNegativeDensity = fluid.getFluid().getDensity(fluid) < 0;

                for (int rY = 0; rY < (isNegativeDensity ? (height - 1) : Math.ceil(pureRenderHeight)); rY++) {
                    float renderHeight = pureRenderHeight - rY;
                    renderHeight = Math.min(renderHeight, 1) + rY;

                    if (rY == 0)
                        renderHeight = Math.max(0.01f, renderHeight);

                    if (isNegativeDensity) {
                        renderHeight = 1f + rY;
                    }

                    float flowMinV = flowMinV_;
                    if (renderHeight - rY < 1.0f)
                        flowMinV += (flowMaxV_ - flowMinV_) * (1.0f - (renderHeight - rY));

                    for (int rX = (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0); rX < xSize; rX++) {
                        for (int rZ = (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0); rZ < zSize; rZ++) {

                            float zMinOffset = 0;
                            float zMaxOffset = 0;
                            float xMinOffset = 0;
                            float xMaxOffset = 0;
                            //North
                            if (rZ == (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0)) {
                                zMinOffset = 0.005f;

                                addVertexWithUV(t, rX, rY, rZ + zMinOffset, flowMaxU, flowMaxV_);
                                addVertexWithUV(t, rX, renderHeight, rZ + zMinOffset, flowMaxU, flowMinV);
                                addVertexWithUV(t, rX + 1, renderHeight, rZ + zMinOffset, flowMinU, flowMinV);
                                addVertexWithUV(t, rX + 1, rY, rZ + zMinOffset, flowMinU, flowMaxV_);
                            }

                            //South
                            if (rZ == zSize - 1) {
                                zMaxOffset = 0.005f;

                                addVertexWithUV(t, rX, rY, rZ + 1 - zMaxOffset, flowMaxU, flowMaxV_);
                                addVertexWithUV(t, rX + 1, rY, rZ + 1 - zMaxOffset, flowMinU, flowMaxV_);
                                addVertexWithUV(t, rX + 1, renderHeight, rZ + 1 - zMaxOffset, flowMinU, flowMinV);
                                addVertexWithUV(t, rX, renderHeight, rZ + 1 - zMaxOffset, flowMaxU, flowMinV);
                            }

                            //West
                            if (rX == (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0)) {
                                xMinOffset = 0.005f;

                                addVertexWithUV(t, rX + xMinOffset, rY, rZ, flowMaxU, flowMaxV_);
                                addVertexWithUV(t, rX + xMinOffset, rY, rZ + 1, flowMinU, flowMaxV_);
                                addVertexWithUV(t, rX + xMinOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                                addVertexWithUV(t, rX + xMinOffset, renderHeight, rZ, flowMaxU, flowMinV);
                            }

                            //East
                            if (rX == xSize - 1) {
                                xMaxOffset = 0.005f;

                                addVertexWithUV(t, rX + 1 - xMaxOffset, rY, rZ, flowMaxU, flowMaxV_);
                                addVertexWithUV(t, rX + 1 - xMaxOffset, renderHeight, rZ, flowMaxU, flowMinV);
                                addVertexWithUV(t, rX + 1 - xMaxOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                                addVertexWithUV(t, rX + 1 - xMaxOffset, rY, rZ + 1, flowMinU, flowMaxV_);
                            }

                            //Top
                            if(isNegativeDensity) {
                                if(rY == height - 2) {
                                    addVertexWithUV(t, rX + xMinOffset, renderHeight, rZ, stillMinU, stillMinV);
                                    addVertexWithUV(t, rX + xMinOffset, renderHeight, rZ + 1, stillMinU, stillMaxV);
                                    addVertexWithUV(t, rX + 1 - xMaxOffset, renderHeight, rZ + 1, stillMaxU, stillMaxV);
                                    addVertexWithUV(t, rX + 1 - xMaxOffset, renderHeight, rZ, stillMaxU, stillMinV);
                                }
                            }
                            else {
                                if (rY == Math.floor(pureRenderHeight) || rY + 1 == Math.ceil(pureRenderHeight)) {
                                    addVertexWithUV(t, rX + xMinOffset, renderHeight, rZ, stillMinU, stillMinV);
                                    addVertexWithUV(t, rX + xMinOffset, renderHeight, rZ + 1, stillMinU, stillMaxV);
                                    addVertexWithUV(t, rX + 1 - xMaxOffset, renderHeight, rZ + 1, stillMaxU, stillMaxV);
                                    addVertexWithUV(t, rX + 1 - xMaxOffset, renderHeight, rZ, stillMaxU, stillMinV);
                                }
                            }

                            //Bottom
                            addVertexWithUV(t, rX + 1, 0.01f, rZ + zMinOffset, stillMinU, stillMinV);
                            addVertexWithUV(t, rX + 1, 0.01f, rZ + 1 - zMaxOffset, stillMinU, stillMaxV);
                            addVertexWithUV(t, rX, 0.01f, rZ + 1 - zMaxOffset, stillMaxU, stillMaxV);
                            addVertexWithUV(t, rX, 0.01f, rZ + zMinOffset, stillMaxU, stillMinV);
                        }
                    }
                }

                if(isNegativeDensity) {
                    GlStateManager.color(1f, 1f, 1f, 0.125f + fillPercentage - (0.125f * fillPercentage));
                    GlStateManager.rotate(180, 0, 0, 1);
                    GlStateManager.translate(bottomDiag.getX() - topDiag.getX() - 1, -height + 1, 0);
                }

                t.draw();

                postGL();
            }
        //}
    }

    private void addVertexWithUV(Tessellator t, double x, double y, double z, double u, double v) {
        t.getWorldRenderer().pos(x, y, z).tex(u, v).color(red, green, blue, alpha).endVertex();
    }
}