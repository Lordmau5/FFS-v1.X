package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.animation.FastTESR;
import net.minecraftforge.fluids.FluidStack;

/**
 * Created by Dustin on 29.06.2015.
 */
public class ValveRenderer extends FastTESR {

    private int red, green, blue, alpha;
    private int lightmap_X, lightmap_Y;

    private void preGL() {
        GlStateManager.pushMatrix();

        GlStateManager.disableCull();
    }

    private void postGL() {
        GlStateManager.enableCull();

        GlStateManager.popMatrix();
    }

    @Override
    public void renderTileEntityFast(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage, VertexBuffer vb)
    {
        AbstractTankValve valve = (AbstractTankValve) tile;

        if (valve == null || !valve.isValid() || !valve.isMaster())
            return;

        BlockPos bottomDiag = valve.bottomDiagFrame;
        BlockPos topDiag = valve.topDiagFrame;
        if (bottomDiag == null || topDiag == null)
            return;

        BlockPos valvePos = valve.getPos();

        x += bottomDiag.getX() - valvePos.getX();
        y += bottomDiag.getY() - valvePos.getY() + 1;
        z += bottomDiag.getZ() - valvePos.getZ();

        vb.setTranslation(x, y, z);

        int height = topDiag.getY() - bottomDiag.getY();
        int xSize = topDiag.getX() - bottomDiag.getX() + (FancyFluidStorage.INSTANCE.TANK_RENDER_INSIDE ? 0 : 1);
        int zSize = topDiag.getZ() - bottomDiag.getZ() + (FancyFluidStorage.INSTANCE.TANK_RENDER_INSIDE ? 0 : 1);

        if (valve.getCapacity() == 0 || valve.getFluidAmount() == 0)
            return;

        float fillPercentage = (float) valve.getFluidAmount() / (float) valve.getCapacity();

        if (fillPercentage > 0 && valve.getFluid() != null) {
            FluidStack fluid = valve.getFluid();

            boolean isNegativeDensity = fluid.getFluid().getDensity(fluid) < 0;

            int i = getWorld().getCombinedLight(valvePos.offset(valve.getTileFacing()), fluid.getFluid().getLuminosity());
            lightmap_X = i >> 0x10 & 0xFFFF;
            lightmap_Y = i & 0xFFFF;

            int c = fluid.getFluid().getColor(fluid);
            blue = c & 0xFF;
            green = (c >> 8) & 0xFF;
            red = (c >> 16) & 0xFF;
            alpha = isNegativeDensity ? (int) Math.ceil((0.125f + fillPercentage - (0.125f * fillPercentage)) * 255) : (c >> 24) & 0xFF;

            preGL();

            TextureAtlasSprite flowing = FluidHelper.getFluidTexture(fluid.getFluid(), FluidHelper.FluidType.FLOWING);
            TextureAtlasSprite still = FluidHelper.getFluidTexture(fluid.getFluid(), FluidHelper.FluidType.STILL);

            float stillMinU = still.getMinU(), stillMaxU = still.getMaxU(), stillMinV = still.getMinV(), stillMaxV = still.getMaxV();
            float flowMinU = flowing.getMinU(), flowMaxU = flowMinU + (stillMaxU - stillMinU), flowMinV_ = flowing.getMinV(), flowMaxV_ = flowMinV_ + (stillMaxV - stillMinV);

            float pureRenderHeight = (height - 1) * fillPercentage;

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

                for (int rX = (FancyFluidStorage.INSTANCE.TANK_RENDER_INSIDE ? 1 : 0); rX < xSize; rX++) {
                    for (int rZ = (FancyFluidStorage.INSTANCE.TANK_RENDER_INSIDE ? 1 : 0); rZ < zSize; rZ++) {

                        float zMinOffset = 0;
                        float zMaxOffset = 0;
                        float xMinOffset = 0;
                        float xMaxOffset = 0;
                        //North
                        if (rZ == (FancyFluidStorage.INSTANCE.TANK_RENDER_INSIDE ? 1 : 0)) {
                            zMinOffset = 0.001f;

                            addVertexWithUV(vb, rX, rY, rZ + zMinOffset, flowMaxU, flowMaxV_);
                            addVertexWithUV(vb, rX, renderHeight, rZ + zMinOffset, flowMaxU, flowMinV);
                            addVertexWithUV(vb, rX + 1, renderHeight, rZ + zMinOffset, flowMinU, flowMinV);
                            addVertexWithUV(vb, rX + 1, rY, rZ + zMinOffset, flowMinU, flowMaxV_);
                        }

                        //South
                        if (rZ == zSize - 1) {
                            zMaxOffset = 0.001f;

                            addVertexWithUV(vb, rX, rY, rZ + 1 - zMaxOffset, flowMaxU, flowMaxV_);
                            addVertexWithUV(vb, rX + 1, rY, rZ + 1 - zMaxOffset, flowMinU, flowMaxV_);
                            addVertexWithUV(vb, rX + 1, renderHeight, rZ + 1 - zMaxOffset, flowMinU, flowMinV);
                            addVertexWithUV(vb, rX, renderHeight, rZ + 1 - zMaxOffset, flowMaxU, flowMinV);
                        }

                        //West
                        if (rX == (FancyFluidStorage.INSTANCE.TANK_RENDER_INSIDE ? 1 : 0)) {
                            xMinOffset = 0.001f;

                            addVertexWithUV(vb, rX + xMinOffset, rY, rZ, flowMaxU, flowMaxV_);
                            addVertexWithUV(vb, rX + xMinOffset, rY, rZ + 1, flowMinU, flowMaxV_);
                            addVertexWithUV(vb, rX + xMinOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                            addVertexWithUV(vb, rX + xMinOffset, renderHeight, rZ, flowMaxU, flowMinV);
                        }

                        //East
                        if (rX == xSize - 1) {
                            xMaxOffset = 0.001f;

                            addVertexWithUV(vb, rX + 1 - xMaxOffset, rY, rZ, flowMaxU, flowMaxV_);
                            addVertexWithUV(vb, rX + 1 - xMaxOffset, renderHeight, rZ, flowMaxU, flowMinV);
                            addVertexWithUV(vb, rX + 1 - xMaxOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                            addVertexWithUV(vb, rX + 1 - xMaxOffset, rY, rZ + 1, flowMinU, flowMaxV_);
                        }

                        //Top
                        if(isNegativeDensity) {
                            if(rY == height - 2) {
                                addVertexWithUV(vb, rX + xMinOffset, renderHeight, rZ, stillMinU, stillMinV);
                                addVertexWithUV(vb, rX + xMinOffset, renderHeight, rZ + 1, stillMinU, stillMaxV);
                                addVertexWithUV(vb, rX + 1 - xMaxOffset, renderHeight, rZ + 1, stillMaxU, stillMaxV);
                                addVertexWithUV(vb, rX + 1 - xMaxOffset, renderHeight, rZ, stillMaxU, stillMinV);
                            }
                        }
                        else {
                            if (rY == Math.floor(pureRenderHeight) || rY + 1 == Math.ceil(pureRenderHeight)) {
                                addVertexWithUV(vb, rX + xMinOffset, renderHeight, rZ, stillMinU, stillMinV);
                                addVertexWithUV(vb, rX + xMinOffset, renderHeight, rZ + 1, stillMinU, stillMaxV);
                                addVertexWithUV(vb, rX + 1 - xMaxOffset, renderHeight, rZ + 1, stillMaxU, stillMaxV);
                                addVertexWithUV(vb, rX + 1 - xMaxOffset, renderHeight, rZ, stillMaxU, stillMinV);
                            }
                        }

                        //Bottom
                        addVertexWithUV(vb, rX + 1, 0.01f, rZ + zMinOffset, stillMinU, stillMinV);
                        addVertexWithUV(vb, rX + 1, 0.01f, rZ + 1 - zMaxOffset, stillMinU, stillMaxV);
                        addVertexWithUV(vb, rX, 0.01f, rZ + 1 - zMaxOffset, stillMaxU, stillMaxV);
                        addVertexWithUV(vb, rX, 0.01f, rZ + zMinOffset, stillMaxU, stillMinV);
                    }
                }
            }

            // Disabled for now. Gotta figure out rotation on this bloody thing for the negative density fluids
//            if(isNegativeDensity) {
//                GlStateManager.rotate(180, 0, 0, 1);
//            }

            postGL();
        }
    }

    private void addVertexWithUV(VertexBuffer vb, double x, double y, double z, double u, double v) {
        vb.pos(x, y, z).color(red, green, blue, alpha).tex(u, v).lightmap(lightmap_X, lightmap_Y).endVertex();
    }
}