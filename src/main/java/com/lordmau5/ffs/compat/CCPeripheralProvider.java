package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.common.Optional;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 08.07.2015.
 */
@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheralProvider", modid = "ComputerCraft")
public class CCPeripheralProvider implements IPeripheralProvider {
    @Override
    public IPeripheral getPeripheral(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityValve)
            return (IPeripheral) tile;
        return null;
    }
}
