package com.lordmau5.ffs.client;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

/**
 * Created by Dustin on 02.07.2015.
 */
public class TankFrameRenderer implements ISimpleBlockRenderingHandler {

    public static final int id = RenderingRegistry.getNextAvailableRenderId();

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {

    }

    public static int getPassForFrameRender(RenderBlocks rb) {
        return MinecraftForgeClient.getRenderPass();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        return renderWorldBlock(world, x, y, z, block, renderer, getPassForFrameRender(renderer));
    }

    public boolean renderWorldBlock(IBlockAccess ba, int x, int y, int z, Block block, RenderBlocks rb, int pass) {
        // Here to prevent Minecraft from crashing when nothing renders on a render pass
        // (rarely in pass 0, often in pass 1)
        // This is a 1.7 bug.
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);
        Tessellator.instance.addVertexWithUV(x, y, z, 0, 0);

        TileEntity tile = ba.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityTankFrame)) {
            return false;
        }

        TileEntityTankFrame te = (TileEntityTankFrame) tile;
        if(te.getBlock() == null)
            return false;

        Block exBlock = te.getBlock().getBlock();
        if (exBlock == null) {
            exBlock = block;
        }

        IBlockAccess origBa = rb.blockAccess;
        boolean isFrameBlockOpaque = exBlock.isOpaqueCube();

        if (((isFrameBlockOpaque || exBlock.canRenderInPass(0)) && pass == 0) || ((!isFrameBlockOpaque || exBlock.canRenderInPass(1)) && pass == 1)) {
            rb.blockAccess = new FrameBlockAccessWrapper(origBa);
            try {
                rb.renderBlockByRenderType(exBlock, x, y, z);
            } catch (Exception e) {
                rb.renderStandardBlock(Blocks.stone, x, y, z);
            }

            rb.blockAccess = origBa;
        }
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return id;
    }
}
