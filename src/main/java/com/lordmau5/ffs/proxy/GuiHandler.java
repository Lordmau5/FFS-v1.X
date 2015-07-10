package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.client.gui.GuiValve;
import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 05.07.2015.
 */
public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityValve)
            return new GuiValve((TileEntityValve) tile);
        return null;
    }

}
