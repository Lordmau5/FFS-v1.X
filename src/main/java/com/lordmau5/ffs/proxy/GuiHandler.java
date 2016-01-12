package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.client.gui.GuiValve;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

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
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if(tile == null)
            return null;

        if(tile instanceof TileEntityValve)
            return new GuiValve((TileEntityValve) tile, false); // Open the GUI for the corresponding Valve, show corresponding button-states
        else if(tile instanceof TileEntityTankFrame)
            return new GuiValve(((TileEntityTankFrame) tile).getValve(), true); // Open the GUI for the master valve, disable buttons
        return null;
    }

}
