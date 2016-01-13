package com.lordmau5.ffs.client.gui;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.FluidHelper;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.ffsPacket;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 05.07.2015.
 */
public class GuiValve extends GuiScreen {

    protected static final ResourceLocation tex = new ResourceLocation(FancyFluidStorage.modId + ":textures/gui/gui_tank.png");
    protected static final int AUTO_FLUID_OUTPUT_BTN_ID = 23442;
    //protected static final int TAB_TANK = 23443;
    //protected static final int TAB_SETTINGS = 23444;

    //protected int activeTab = TAB_TANK;

    TileEntityValve valve;
    boolean isFrame;

    GuiTextField valveName;

    int xSize = 256, ySize = 121;
    int left = 0, top = 0;
    int mouseX, mouseY;

    public GuiValve(TileEntityValve valve, boolean isFrame) {
        super();

        this.valve = valve;
        this.isFrame = isFrame;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.left = (this.width - this.xSize) / 2;
        this.top = (this.height - this.ySize) / 2;
        if(!isFrame) {
            this.buttonList.add(new GuiToggle(AUTO_FLUID_OUTPUT_BTN_ID, this.left + 80, this.top + 20, "Auto fluid output", this.valve.getAutoOutput(), 16777215));
            valveName = new GuiTextField(0, this.fontRendererObj, this.left + 80, this.top + 100, 120, 10);
            valveName.setText(valve.getValveName());
            valveName.setMaxStringLength(32);
        }

        //this.buttonList.add(new GuiTab(TAB_TANK, this.left + 10, this.top - 15, "Tank"));
        //this.buttonList.add(new GuiTab(TAB_SETTINGS, this.left + 72, this.top - 15, "Settings"));

        /*for(Object button : this.buttonList) {
            if(button instanceof GuiTab)
                ((GuiTab) button).setActive(false);
        }
        */

        //((GuiTab) this.buttonList.get(activeTab)).setActive(true);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        if(!isFrame)
            if(!valveName.getText().isEmpty())
                NetworkHandler.sendPacketToServer(new ffsPacket.Server.UpdateValveName(valve, valveName.getText()));
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if(!isFrame) {
            if(valveName.isFocused()) {
                valveName.textboxKeyTyped(keyChar, keyCode);
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

        if(!isFrame)
            valveName.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
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

        if(!isFrame) {
            drawValveName(x, y);
        }

        if(this.valve.getFluid() != null)
            fluidHoveringText(fluid);
    }

    private void drawValveName(int x, int y) {
        this.drawString(this.fontRendererObj, "Valve Name:", this.left + 80, this.top + 88, 16777215);
        valveName.drawTextBox();
    }

    private void fluidHoveringText(String fluid) {
        if(mouseX >= left + 10 && mouseX < left + 10 + 64 &&
                mouseY >= top + 10 && mouseY < top + 10 + 101) {
            List<String> texts = new ArrayList<>();
            texts.add(fluid);
            texts.add("\u00A7" + EnumChatFormatting.GRAY.name() + (GenericUtil.intToFancyNumber(this.valve.getFluidAmount()) + " / " + GenericUtil.intToFancyNumber(this.valve.getCapacity())) + " mB");

            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
            drawHoveringText(texts, mouseX, mouseY, fontRendererObj);
            GL11.glPopAttrib();
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
        TextureAtlasSprite fluidIcon = FluidHelper.getFluidTexture(valve.getFluid().getFluid(), FluidHelper.FluidType.STILL);
        if(fluidIcon == null)
            return;

        this.mc.getTextureManager().bindTexture(FluidHelper.BLOCK_TEXTURE);

        int height = (int) Math.ceil((float) valve.getFluidAmount() / (float) valve.getCapacity() * 101);

        ScaledResolution r = new ScaledResolution(mc);
        int sc = r.getScaleFactor();

        GL11.glScissor((x + 10) * sc, (y + 10 - mc.gameSettings.guiScale) * sc - sc, 64 * sc, (height + mc.gameSettings.guiScale + 1) * sc + (sc % 2) - 1);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        for(int iX = 0; iX < 4; iX++) {
            for(int iY = 7; iY > 0; iY--) {
                drawTexturedModalRect(x + 10 + (iX * 16), y - 1 + ((iY - 1) * 16), fluidIcon, 16, 16);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
