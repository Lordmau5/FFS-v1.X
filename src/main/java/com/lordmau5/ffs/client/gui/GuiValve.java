package com.lordmau5.ffs.client.gui;

import buildcraft.core.lib.render.FluidRenderer;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.ffsPacket;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 05.07.2015.
 */
public class GuiValve extends GuiScreen {

    protected static final ResourceLocation tex = new ResourceLocation(FancyFluidStorage.modId + ":textures/gui/gui_tank.png");
    protected static final int AUTO_FLUID_OUTPUT_BTN_ID = 23442;

    TileEntityValve valve;

    int xSize = 256, ySize = 121;
    int left = 0, top = 0;
    int mouseX, mouseY;

    public GuiValve(TileEntityValve valve) {
        super();

        this.valve = valve;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.left = (this.width - this.xSize) / 2;
        this.top = (this.height - this.ySize) / 2;
        this.buttonList.add(new GuiToggle(AUTO_FLUID_OUTPUT_BTN_ID, this.left + 80, this.top + 20, "Auto fluid output", this.valve.getAutoOutput(), 16777215));
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode())
        {
            this.mc.thePlayer.closeScreen();
            this.mc.setIngameFocus();
        }
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
        if(this.valve.getFluid() != null)
            fluid = this.valve.getFluid().getLocalizedName();

        this.drawCenteredString(this.fontRendererObj, fluid + " Tank", this.left + 163, this.top + 6, 16777215);

        if(this.valve.getFluid() != null)
            this.drawFluid(this.left, this.top);

        // call to super to draw buttons and other such fancy things
        super.drawScreen(x, y, partialTicks);

        if(this.valve.getFluid() != null)
            fluidHoveringText(fluid);
    }

    private void fluidHoveringText(String fluid) {
        if(mouseX >= left + 10 && mouseX < left + 10 + 64 &&
                mouseY >= top + 10 && mouseY < top + 10 + 101) {
            List<String> texts = new ArrayList<>();
            texts.add(fluid);
            texts.add("\u00A7" + EnumChatFormatting.GRAY.getFormattingCode() + (GenericUtil.intToFancyNumber(this.valve.getFluidAmount()) + " / " + GenericUtil.intToFancyNumber(this.valve.getCapacity())) + " mB");

            GL11.glPushMatrix();
            drawHoveringText(texts, mouseX, mouseY, fontRendererObj);
            GL11.glPopMatrix();
        }
    }

    public void actionPerformed(GuiButton btn) {
        if (btn.id == AUTO_FLUID_OUTPUT_BTN_ID && btn instanceof GuiToggle) {
            GuiToggle toggle = (GuiToggle)btn;

            this.valve.setAutoOutput(toggle.getState());
            NetworkHandler.sendPacketToServer(new ffsPacket.Server.UpdateAutoOutput(this.valve, this.valve.getAutoOutput()));
        }
    }

    private void drawFluid(int x, int y) {
        IIcon fluidIcon = FluidRenderer.getFluidTexture(valve.getFluid(), false);
        if(fluidIcon == null)
            return;

        this.mc.getTextureManager().bindTexture(FluidRenderer.getFluidSheet(valve.getFluid()));

        int height = (int) Math.ceil((float) valve.getFluidAmount() / (float) valve.getCapacity() * 101);

        ScaledResolution r = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sc = r.getScaleFactor();

        GL11.glScissor((x + 10) * sc, (y + 10 - mc.gameSettings.guiScale) * sc - sc, 64 * sc, (height + mc.gameSettings.guiScale + 1) * sc + (sc % 2) - 1);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        for(int iX = 0; iX < 4; iX++) {
            for(int iY = 7; iY > 0; iY--) {
                drawTexturedModelRectFromIcon(x + 10 + (iX * 16), y - 1 + ((iY - 1) * 16), fluidIcon, 16, 16);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
