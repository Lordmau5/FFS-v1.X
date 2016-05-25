package com.lordmau5.ffs.client.gui;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.FluidHelper;
import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import com.lordmau5.ffs.tile.interfaces.INameableTile;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import com.lordmau5.ffs.util.GenericUtil;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 05.07.2015.
 */
public class GuiValve extends GuiScreen {

    private static final ResourceLocation tex = new ResourceLocation(FancyFluidStorage.modId + ":textures/gui/gui_tank.png");

    private GuiToggle autoFluidOutputButton;
    private GuiToggle metaphaserToggleButton;

    private GuiButtonLockFluid lockFluidButton;

    private AbstractTankTile tile;
    private AbstractTankValve valve;
    private AbstractTankValve masterValve;

    private GuiTextField tileName;

    private int xSize = 256, ySize = 121;
    private int left = 0, top = 0;
    private int mouseX, mouseY;

    public GuiValve(AbstractTankTile tile) {
        super();

        this.tile = tile;
        if(tile instanceof AbstractTankValve)
            valve = (AbstractTankValve) tile;
        else
            valve = tile.getMasterValve();

        masterValve = tile.getMasterValve();
    }

    @Override
    public void initGui() {
        super.initGui();

        this.left = (this.width - this.xSize) / 2;
        this.top = (this.height - this.ySize) / 2;
        if(tile instanceof TileEntityFluidValve) {
            this.buttonList.add(autoFluidOutputButton = new GuiToggle(this.left + 80, this.top + 20, "Auto fluid output", ((TileEntityFluidValve)tile).getAutoOutput(), 16777215));
        }
        else if(tile instanceof TileEntityMetaphaser) {
            this.buttonList.add(metaphaserToggleButton = new GuiToggle(this.left + 80, this.top + 20, "Metaphaser extract", ((TileEntityMetaphaser)tile).getExtract(), 16777215));
        }
        if(tile instanceof INameableTile) {
            tileName = new GuiTextField(0, this.fontRendererObj, this.left + 80, this.top + 100, 120, 10);
            tileName.setText(valve.getTileName());
            tileName.setMaxStringLength(32);
        }
        this.buttonList.add(lockFluidButton = new GuiButtonLockFluid(this.left + 67, this.top + 9, masterValve.getTankConfig().isFluidLocked()));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        if(tile instanceof INameableTile)
            if(!tileName.getText().isEmpty())
                NetworkHandler.sendPacketToServer(new FFSPacket.Server.UpdateTileName(tile, tileName.getText()));
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if(tile instanceof INameableTile) {
            if(tileName.isFocused()) {
                tileName.textboxKeyTyped(keyChar, keyCode);
                return;
            }
        }

        if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode())
        {
            this.mc.thePlayer.closeScreen();
            this.mc.setIngameFocus();
        }
    }

    @Override
    protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) throws IOException {
        super.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);

        if(tile instanceof INameableTile)
            tileName.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        this.mouseX = x;
        this.mouseY = y;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(tex);
        this.drawTexturedModalRect(this.left, this.top, 0, 0, xSize, ySize);

        String fluid = "Empty";
        if(this.valve.getFluid() != null) {
            fluid = this.valve.getFluid().getLocalizedName();
        }

        this.drawCenteredString(this.fontRendererObj, fluid + " Tank", this.left + 163, this.top + 6, 16777215);

        mc.renderEngine.bindTexture(tex);
        this.drawTexturedModalRect(this.left + 9, this.top + 9, 0, 137, 66, 103);

        if(this.valve.getFluid() != null)
            this.drawFluid(this.left, this.top);

        // call to super to draw buttons and other such fancy things
        super.drawScreen(x, y, partialTicks);

        if(tile instanceof INameableTile) {
            drawTileName(x, y);
        }

        if(mouseX >= left + 66 && mouseX < left + 66 + 9 &&
        mouseY >= top + 9 && mouseY < top + 9 + 9) {
            lockedFluidHoveringText();
        }
        else {
            if (this.valve.getFluid() != null)
                fluidHoveringText(fluid);
        }
    }

    private void drawTileName(int x, int y) {
        this.drawString(this.fontRendererObj, "Tile Name:", this.left + 80, this.top + 88, 16777215);
        tileName.drawTextBox();
    }

    private void lockedFluidHoveringText() {
        List<String> texts = new ArrayList<>();
        texts.add("Fluid " + (valve.getTankConfig().isFluidLocked() ? (ChatFormatting.RED + "Locked") : (ChatFormatting.GREEN + "Unlocked")));

        if(valve.getTankConfig().isFluidLocked()) {
            texts.add(ChatFormatting.GRAY + "Locked to: " + valve.getTankConfig().getLockedFluid().getLocalizedName());
        }

        GlStateManager.pushMatrix();
        //GlStateManager.pushAttrib(GL11.GL_LIGHTING_BIT);
        drawHoveringText(texts, mouseX, mouseY, fontRendererObj);
        //GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void fluidHoveringText(String fluid) {
        if(mouseX >= left + 10 && mouseX < left + 10 + 64 &&
                mouseY >= top + 10 && mouseY < top + 10 + 101) {
            List<String> texts = new ArrayList<>();
            texts.add(fluid);
            texts.add(ChatFormatting.GRAY + (GenericUtil.intToFancyNumber(this.valve.getFluidAmount()) + " / " + GenericUtil.intToFancyNumber(this.valve.getCapacity())) + " mB");

            GlStateManager.pushMatrix();
            //GlStateManager.pushAttrib(GL11.GL_LIGHTING_BIT);
            drawHoveringText(texts, mouseX, mouseY, fontRendererObj);
            //GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    }

    public void actionPerformed(GuiButton btn) {
        if (btn == autoFluidOutputButton) {
            GuiToggle toggle = (GuiToggle)btn;

            ((TileEntityFluidValve)valve).setAutoOutput(toggle.getState());
            NetworkHandler.sendPacketToServer(new FFSPacket.Server.UpdateAutoOutput((TileEntityFluidValve) this.valve));
        }
        else if (btn == metaphaserToggleButton) {
            GuiToggle toggle = (GuiToggle)btn;

            ((TileEntityMetaphaser)valve).setExtract(toggle.getState());
            NetworkHandler.sendPacketToServer(new FFSPacket.Server.UpdateMetaphaserMode((TileEntityMetaphaser) this.valve));
        }
        else if (btn == lockFluidButton) {
            GuiButtonLockFluid toggle = (GuiButtonLockFluid) btn;

            this.masterValve.toggleFluidLock(toggle.getState());
            toggle.setState(this.masterValve.getTankConfig().isFluidLocked());
            NetworkHandler.sendPacketToServer(new FFSPacket.Server.UpdateFluidLock(this.masterValve));
        }
    }

    private void drawFluid(int x, int y) {
        TextureAtlasSprite fluidIcon = FluidHelper.getFluidTexture(valve.getFluid().getFluid(), FluidHelper.FluidType.STILL);
        if(fluidIcon == null)
            return;

        this.mc.getTextureManager().bindTexture(FluidHelper.BLOCK_TEXTURE);

        int height = Math.min(101, (int) Math.ceil((float) valve.getFluidAmount() / (float) valve.getCapacity() * 101));

        int loopHeight = (int) Math.floor(height / 16);
        for(int iX = 0; iX < 4; iX++) {
            for(int iY = 7; iY > 7 - loopHeight; iY--) {
                drawTexturedModalRect(x + 10 + (iX * 16), y - 1 + ((iY - 1) * 16), fluidIcon, 16, 16);
            }
        }

        // Render the one furthest at the top
        for(int iX = 0; iX < 4; iX++) {
            drawFluid(x + 10 + (iX * 16), y - 1 + ((6 - loopHeight) * 16) + (16 - height % 16), fluidIcon, 16, 16, height % 16);
        }
    }

    public void drawFluid(int xCoord, int yCoord, TextureAtlasSprite textureSprite, int widthIn, int heightIn, int heightAdjustment)
    {
        double heightAdjust = (textureSprite.getMaxV() - textureSprite.getMinV()) / heightIn * heightAdjustment;

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        vertexBuffer.pos((double)(xCoord + 0), (double)(yCoord + heightAdjustment), (double)this.zLevel).tex((double)textureSprite.getMinU(), (double)textureSprite.getMaxV()).endVertex();
        vertexBuffer.pos((double)(xCoord + widthIn), (double)(yCoord + heightAdjustment), (double)this.zLevel).tex((double)textureSprite.getMaxU(), (double)textureSprite.getMaxV()).endVertex();
        vertexBuffer.pos((double)(xCoord + widthIn), (double)(yCoord + 0), (double)this.zLevel).tex((double)textureSprite.getMaxU(), (double)textureSprite.getMaxV() - heightAdjust).endVertex();
        vertexBuffer.pos((double)(xCoord + 0), (double)(yCoord + 0), (double)this.zLevel).tex((double)textureSprite.getMinU(), (double)textureSprite.getMaxV() - heightAdjust).endVertex();
        tessellator.draw();
    }
}
