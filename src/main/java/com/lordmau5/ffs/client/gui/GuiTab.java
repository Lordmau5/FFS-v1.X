package com.lordmau5.ffs.client.gui;

import com.lordmau5.ffs.FancyFluidStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Created by Dustin on 09.10.2015.
 */
public class GuiTab extends GuiButton {

    protected static final ResourceLocation tabTexture = new ResourceLocation(FancyFluidStorage.modId + ":textures/gui/gui_tank.png");
    protected int textColor = 16777215;
    protected boolean active = false;

    public GuiTab(int id, int x, int y, String title) {
        super(id, x, y, 62, 15, title);
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible)
        {
            FontRenderer fontrenderer = mc.fontRendererObj;
            mc.getTextureManager().bindTexture(tabTexture);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 121, this.width, this.height + (isActive() ? 3 : 0));

            this.mouseDragged(mc, mouseX, mouseY);

            this.drawString(fontrenderer, this.displayString, this.xPosition + this.width + 4, this.yPosition, this.textColor);
        }
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return super.mousePressed(mc, mouseX, mouseY);
    }

    public void setActive(boolean state) {
        this.active = state;
    }

    public boolean isActive() {
        return this.active;
    }

}
