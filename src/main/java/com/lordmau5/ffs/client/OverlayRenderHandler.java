package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/**
 * Created by Dustin on 14.02.2016.
 */
public class OverlayRenderHandler {

    private Minecraft mc = Minecraft.getMinecraft();
    private double playerX;
    private double playerY;
    private double playerZ;

    private BlockPos lastPos;

    private int maxTicks = 20 * 5;
    private int ticksRemaining;

    public static TextureAtlasSprite overlayTexture;

    @SubscribeEvent
    public void clientEndTick(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.END)
            return;

        if(ticksRemaining > 0)
            ticksRemaining--;

        if(mc.objectMouseOver != null) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            if (pos != null && lastPos != null && !pos.equals(lastPos))
                ticksRemaining = maxTicks;
            lastPos = pos;
        }
    }

    @SubscribeEvent
    public void overlayRender(DrawBlockHighlightEvent event) {
        if(!FancyFluidStorage.INSTANCE.TANK_OVERLAY_RENDER)
            return;

        EntityPlayer player = event.getPlayer();
        BlockPos pos = event.getTarget().getBlockPos();
        if(pos == null)
            return;

        World world = player.getEntityWorld();
        if(world == null)
            return;

        TileEntity tile = world.getTileEntity(pos);
        if(tile == null || !(tile instanceof AbstractTankTile))
            return;

        AbstractTankTile tankTile = (AbstractTankTile) tile;
        if(!tankTile.isValid())
            return;

        AbstractTankValve valve = tankTile.getMasterValve();
        if(valve == null)
            return;

        playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) event.getPartialTicks();
        playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) event.getPartialTicks();
        playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) event.getPartialTicks();

        drawAll(modifyBoundingBox(valve.getRenderBoundingBox()));
    }

    private AxisAlignedBB modifyBoundingBox(AxisAlignedBB box)
    {
        box = new AxisAlignedBB(box.minX - 0.002f, box.minY - 0.002f, box.minZ - 0.002f, box.maxX + 1.002f, box.maxY + 1.002f, box.maxZ + 1.002f);
        return box.offset(-playerX, -playerY, -playerZ);
    }

    private void drawAll(AxisAlignedBB b)
    {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tess = Tessellator.getInstance();
        VertexBuffer vb = tess.getBuffer();

        int x_axis = (int) (b.maxX - b.minX);
        int y_axis = (int) (b.maxY - b.minY);
        int z_axis = (int) (b.maxZ - b.minZ);

        float texMinU = overlayTexture.getMinU(), 
                texMaxU = overlayTexture.getMaxU(),
                texMinV = overlayTexture.getMinV(),
                texMaxV = overlayTexture.getMaxV();

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        for(int x = 0; x < x_axis; x++) {
            for(int y = 0; y < y_axis; y++) {
                for(int z = 0; z < z_axis; z++) {

                    //BOTTOM / DOWN
                    if(y == 0) {
                        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                        addVertex(vb, b.minX + x, b.minY, b.minZ + z, texMinU, texMaxV);
                        addVertex(vb, b.minX + x + 1, b.minY, b.minZ + z, texMaxU, texMaxV);
                        addVertex(vb, b.minX + x + 1, b.minY, b.minZ + z + 1, texMaxU, texMinV);
                        addVertex(vb, b.minX + x, b.minY, b.minZ + z + 1, texMinU, texMinV);

                        tess.draw();
                    }

                    //TOP / UP
                    if(y == y_axis - 1) {
                        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                        addVertex(vb, b.minX + x, b.maxY, b.minZ + z, texMinU, texMinV);
                        addVertex(vb, b.minX + x, b.maxY, b.minZ + z + 1, texMinU, texMaxV);
                        addVertex(vb, b.minX + x + 1, b.maxY, b.minZ + z + 1, texMaxU, texMaxV);
                        addVertex(vb, b.minX + x + 1, b.maxY, b.minZ + z, texMaxU, texMinV);

                        tess.draw();
                    }

                    //FRONT / NORTH
                    if(z == 0) {
                        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                        addVertex(vb, b.minX + x + 1, b.minY + y + 1, b.maxZ, texMaxU, texMinV);
                        addVertex(vb, b.minX + x, b.minY + y + 1, b.maxZ, texMinU, texMinV);
                        addVertex(vb, b.minX + x, b.minY + y, b.maxZ, texMinU, texMaxV);
                        addVertex(vb, b.minX + x + 1, b.minY + y, b.maxZ, texMaxU, texMaxV);

                        tess.draw();
                    }

                    //BACK / SOUTH
                    if(z == z_axis - 1) {
                        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                        addVertex(vb, b.minX + x + 1, b.minY + y, b.minZ, texMaxU, texMinV);
                        addVertex(vb, b.minX + x, b.minY + y, b.minZ, texMinU, texMinV);
                        addVertex(vb, b.minX + x, b.minY + y + 1, b.minZ, texMinU, texMaxV);
                        addVertex(vb, b.minX + x + 1, b.minY + y + 1, b.minZ, texMaxU, texMaxV);

                        tess.draw();
                    }

                    //LEFT / WEST
                    if(x == 0) {
                        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                        addVertex(vb, b.minX, b.minY + y + 1, b.minZ + z + 1, texMaxU, texMaxV);
                        addVertex(vb, b.minX, b.minY + y + 1, b.minZ + z, texMaxU, texMinV);
                        addVertex(vb, b.minX, b.minY + y, b.minZ + z, texMinU, texMinV);
                        addVertex(vb, b.minX, b.minY + y, b.minZ + z + 1, texMinU, texMaxV);

                        tess.draw();
                    }

                    //RIGHT / EAST
                    if(x == x_axis - 1) {
                        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                        addVertex(vb, b.maxX, b.minY + y + 1, b.minZ + z + 1, texMinU, texMaxV);
                        addVertex(vb, b.maxX, b.minY + y, b.minZ + z + 1, texMaxU, texMaxV);
                        addVertex(vb, b.maxX, b.minY + y, b.minZ + z, texMaxU, texMinV);
                        addVertex(vb, b.maxX, b.minY + y + 1, b.minZ + z, texMinU, texMinV);

                        tess.draw();
                    }

                }
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void addVertex(VertexBuffer vb, double posX, double posY, double posZ, float u, float v) {
        float alpha = 3f;
        vb.pos(posX, posY, posZ).tex(u, v).color(1f, 1f, 1f, Math.min(0.7f, alpha * ((float) ticksRemaining / maxTicks))).endVertex();
    }

}
