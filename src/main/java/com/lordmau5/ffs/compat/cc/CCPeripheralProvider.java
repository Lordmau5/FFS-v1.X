package com.lordmau5.ffs.compat.cc;

import com.lordmau5.ffs.tile.tanktiles.TileEntityTankComputer;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

/**
 * Created by Dustin on 08.07.2015.
 */
@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheralProvider", modid = "ComputerCraft")
public class CCPeripheralProvider implements IPeripheralProvider {

    public void register() {
        ComputerCraftAPI.registerPeripheralProvider(this);
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public IPeripheral getPeripheral(World world, BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankComputer)
            return (IPeripheral) tile;
        return null;
    }
}
