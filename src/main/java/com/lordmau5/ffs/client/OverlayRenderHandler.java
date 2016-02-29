package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
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

    private ResourceLocation overlayTexture;

    public OverlayRenderHandler() {
        overlayTexture = new ResourceLocation(FancyFluidStorage.modId + ":textures/blocks/overlay/tankOverlayAnim.png");
    }

    @SubscribeEvent
    public void clientEndTick(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.END)
            return;

        if(ticksRemaining > 0)
            ticksRemaining--;

        if(mc.objectMouseOver == null)
            return;

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if(!pos.equals(lastPos))
            ticksRemaining = maxTicks;
        lastPos = pos;
    }

    @SubscribeEvent
    public void overlayRender(DrawBlockHighlightEvent event) {
        EntityPlayer player = event.player;
        BlockPos pos = event.target.getBlockPos();
        World world = player.getEntityWorld();
        TileEntity tile = world.getTileEntity(pos);
        if(tile == null || !(tile instanceof AbstractTankTile))
            return;

        AbstractTankTile tankTile = (AbstractTankTile) tile;
        if(!tankTile.isValid())
            return;

        AbstractTankValve valve = tankTile.getMasterValve();
        if(valve == null)
            return;

        playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) event.partialTicks;
        playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) event.partialTicks;
        playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) event.partialTicks;

        //drawAll(modifyBoundingBox(valve.getRenderBoundingBox()));
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
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        mc.getTextureManager().bindTexture(overlayTexture);

        int x_axis = (int) (b.maxX - b.minX);
        int y_axis = (int) (b.maxY - b.minY);
        int z_axis = (int) (b.maxZ - b.minZ);

        //Top / UP
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addVertex(wr, b.minX, b.maxY, b.minZ, 0, 0);
        addVertex(wr, b.minX, b.maxY, b.maxZ, 0, z_axis);
        addVertex(wr, b.maxX, b.maxY, b.maxZ, x_axis, z_axis);
        addVertex(wr, b.maxX, b.maxY, b.minZ, x_axis, 0);
        tess.draw();

        //Bottom / DOWN
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addVertex(wr, b.maxX, b.minY, b.minZ, x_axis, 0);
        addVertex(wr, b.maxX, b.minY, b.maxZ, x_axis, z_axis);
        addVertex(wr, b.minX, b.minY, b.maxZ, 0, z_axis);
        addVertex(wr, b.minX, b.minY, b.minZ, 0, 0);
        tess.draw();

        //Front / NORTH
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addVertex(wr, b.maxX, b.maxY, b.maxZ, x_axis, 0);
        addVertex(wr, b.minX, b.maxY, b.maxZ, 0, 0);
        addVertex(wr, b.minX, b.minY, b.maxZ, 0, y_axis);
        addVertex(wr, b.maxX, b.minY, b.maxZ, x_axis, y_axis);
        tess.draw();

        //Back / SOUTH
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addVertex(wr, b.maxX, b.minY, b.minZ, x_axis, 0);
        addVertex(wr, b.minX, b.minY, b.minZ, 0, 0);
        addVertex(wr, b.minX, b.maxY, b.minZ, 0, y_axis);
        addVertex(wr, b.maxX, b.maxY, b.minZ, x_axis, y_axis);
        tess.draw();

        //Left / WEST
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addVertex(wr, b.minX, b.maxY, b.maxZ, y_axis, z_axis);
        addVertex(wr, b.minX, b.maxY, b.minZ, y_axis, 0);
        addVertex(wr, b.minX, b.minY, b.minZ, 0, 0);
        addVertex(wr, b.minX, b.minY, b.maxZ, 0, z_axis);
        tess.draw();

        //Right / EAST
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addVertex(wr, b.maxX, b.maxY, b.maxZ, y_axis, z_axis);
        addVertex(wr, b.maxX, b.minY, b.maxZ, 0, z_axis);
        addVertex(wr, b.maxX, b.minY, b.minZ, 0, 0);
        addVertex(wr, b.maxX, b.maxY, b.minZ, y_axis, 0);
        tess.draw();

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void addVertex(WorldRenderer wr, double posX, double posY, double posZ, int u, int v) {
        float alpha = 3f;
        wr.pos(posX, posY, posZ).tex(u, v).color(1f, 1f, 1f, Math.min(1, alpha * ((float) ticksRemaining / maxTicks))).endVertex();
    }

}
