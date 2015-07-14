package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.Position3D;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

/**
 * Created by Dustin on 29.06.2015.
 */
public class ValveRenderer extends TileEntitySpecialRenderer implements ISimpleBlockRenderingHandler {

    public static final int id = RenderingRegistry.getNextAvailableRenderId();

    private void preGL() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(1f, 1f, 1f, 1f);

        bindTexture(TextureMap.locationBlocksTexture);
    }

    private void postGL() {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float f)
    {
        TileEntityValve valve = (TileEntityValve) tile;

        if (valve == null || !valve.isValid())
            return;

        Tessellator t = Tessellator.instance;

        if (valve.isMaster()) {
            Position3D bottomDiag = valve.bottomDiagFrame;
            Position3D topDiag = valve.topDiagFrame;
            if (bottomDiag == null || topDiag == null)
                return;

            int height = topDiag.getY() - bottomDiag.getY();
            int xSize = topDiag.getX() - bottomDiag.getX() + 1;
            int zSize = topDiag.getZ() - bottomDiag.getZ() + 1;

            if (valve.getCapacity() == 0 || valve.getFluidAmount() == 0)
                return;

            double fillPercentage = (double) valve.getFluidAmount() / (double) valve.getCapacity();

            if (fillPercentage > 0 && valve.getFluid() != null) {
                FluidStack fluid = valve.getFluid();

                preGL();

                IIcon flowing = FluidHelper.getFluidTexture(fluid.getFluid(), true);
                IIcon still = FluidHelper.getFluidTexture(fluid.getFluid(), false);

                float flowMinU = flowing.getMinU(), flowMaxU = flowing.getMaxU(), flowMinV_ = flowing.getMinV(), flowMaxV = flowing.getMaxV();
                float stillMinU = still.getMinU(), stillMaxU = still.getMaxU(), stillMinV = still.getMinV(), stillMaxV = still.getMaxV();

                GL11.glTranslatef((float) x, (float) y, (float) z);
                GL11.glTranslatef((float) (bottomDiag.getX() - tile.xCoord), (float) bottomDiag.getY() - tile.yCoord + 1, (float) (bottomDiag.getZ() - tile.zCoord));

                t.startDrawingQuads();

                float pureRenderHeight = (height - 1) * (float) fillPercentage;

                for (int rX = 0; rX < xSize; rX++) {
                    for (int rZ = 0; rZ < zSize; rZ++) {
                        for (int rY = 0; rY < Math.ceil(pureRenderHeight); rY++) {
                            float renderHeight = pureRenderHeight - rY;
                            renderHeight = Math.min(renderHeight, 1) + rY;

                            float flowMinV = flowMinV_;
                            if (renderHeight - rY < 1.0f) flowMinV += (flowMaxV - flowMinV_) * (1.0f - (renderHeight - rY));

                            if(rY == 0)
                                renderHeight = Math.max(0.01f, renderHeight);

                            float zMinOffset = 0;
                            float zMaxOffset = 0;
                            float xMinOffset = 0;
                            float xMaxOffset = 0;
                            //North
                            if (rZ == 0) {
                                zMinOffset = 0.005f;
                                t.addVertexWithUV(rX, rY, rZ + zMinOffset, flowMaxU, flowMaxV);
                                t.addVertexWithUV(rX, renderHeight, rZ + zMinOffset, flowMaxU, flowMinV);
                                t.addVertexWithUV(rX + 1, renderHeight, rZ + zMinOffset, flowMinU, flowMinV);
                                t.addVertexWithUV(rX + 1, rY, rZ + zMinOffset, flowMinU, flowMaxV);
                            }

                            //South
                            if (rZ == zSize - 1) {
                                zMaxOffset = 0.005f;
                                t.addVertexWithUV(rX, rY, rZ + 1 - zMaxOffset, flowMaxU, flowMaxV);
                                t.addVertexWithUV(rX + 1, rY, rZ + 1 - zMaxOffset, flowMinU, flowMaxV);
                                t.addVertexWithUV(rX + 1, renderHeight, rZ + 1 - zMaxOffset, flowMinU, flowMinV);
                                t.addVertexWithUV(rX, renderHeight, rZ + 1 - zMaxOffset, flowMaxU, flowMinV);
                            }

                            //West
                            if (rX == 0) {
                                xMinOffset = 0.005f;
                                t.addVertexWithUV(rX + xMinOffset, rY, rZ, flowMaxU, flowMaxV);
                                t.addVertexWithUV(rX + xMinOffset, rY, rZ + 1, flowMinU, flowMaxV);
                                t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                                t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ, flowMaxU, flowMinV);
                            }

                            //East
                            if (rX == xSize - 1) {
                                xMaxOffset = 0.005f;
                                t.addVertexWithUV(rX + 1 - xMaxOffset, rY, rZ, flowMaxU, flowMaxV);
                                t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ, flowMaxU, flowMinV);
                                t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                                t.addVertexWithUV(rX + 1 - xMaxOffset, rY, rZ + 1, flowMinU, flowMaxV);
                            }

                            //Top
                            if (rY == Math.floor(pureRenderHeight) || rY + 1 == Math.ceil(pureRenderHeight)) {
                                t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ, stillMinU, stillMinV);
                                t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ + 1, stillMinU, stillMaxV);
                                t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ + 1, stillMaxU, stillMaxV);
                                t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ, stillMaxU, stillMinV);
                            }

                            //Bottom
                            t.addVertexWithUV(rX + 1, 0.01f, rZ + zMinOffset, stillMinU, stillMinV);
                            t.addVertexWithUV(rX + 1, 0.01f, rZ + 1 - zMaxOffset, stillMinU, stillMaxV);
                            t.addVertexWithUV(rX, 0.01f, rZ + 1 - zMaxOffset, stillMaxU, stillMaxV);
                            t.addVertexWithUV(rX, 0.01f, rZ + zMinOffset, stillMaxU, stillMinV);
                        }
                    }
                }

                t.draw();

                postGL();
            }
        }
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;

        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem)));
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, -1.0F);
        renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
        tessellator.draw();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

        // Here to prevent Minecraft from crashing when nothing renders on a render pass
        // (rarely in pass 0, often in pass 1)
        // This is a 1.7 bug.
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);

        if(MinecraftForgeClient.getRenderPass() == 1)
            return false;

        if(world == null)
            return false;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityValve)) {
            return false;
        }

        TileEntityValve valve = (TileEntityValve) tile;
        boolean isMaster = valve.isMaster();

        if(!valve.isValid()) {
            renderer.renderStandardBlock(block, x, y, z);
            renderer.setOverrideBlockTexture(isMaster ? FancyFluidStorage.proxy.tex_MasterValve[0] : FancyFluidStorage.proxy.tex_SlaveValve[0]);
            renderer.renderStandardBlock(block, x, y, z);
            renderer.clearOverrideBlockTexture();

            return false;
        }

        renderer.renderStandardBlock(block, x, y, z);
        ForgeDirection dr = valve.getInside();
        if (dr.offsetX != 0) {
            renderer.renderFaceXNeg(block, x, y, z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
            renderer.renderFaceXPos(block, x, y, z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
        } else if (dr.offsetY != 0) {
            renderer.renderFaceYNeg(block, x, y, z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
            renderer.renderFaceYPos(block, x, y, z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
        } else if (dr.offsetZ != 0) {
            renderer.renderFaceZNeg(block, x, y, z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
            renderer.renderFaceZPos(block, x, y, z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
        }

        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return id;
    }

}
