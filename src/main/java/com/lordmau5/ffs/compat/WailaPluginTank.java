package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Created by max on 7/4/15.
 */
@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
public class WailaPluginTank implements IWailaDataProvider {

    public static void init() {
        FMLInterModComms.sendMessage("Waila", "register", WailaPluginTank.class.getName() + ".registerPlugin");
    }

    @Optional.Method(modid = "Waila")
    public static void registerPlugin(IWailaRegistrar registrar) {
        WailaPluginTank instance = new WailaPluginTank();
        registrar.registerStackProvider(instance, TileEntityTankFrame.class);
        registrar.registerBodyProvider(instance, TileEntityValve.class);
        registrar.registerBodyProvider(instance, TileEntityTankFrame.class);
    }

    @Optional.Method(modid = "Waila")
    @Override
    public ItemStack getWailaStack(IWailaDataAccessor iWailaDataAccessor, IWailaConfigHandler iWailaConfigHandler) {
        TileEntity te = iWailaDataAccessor.getTileEntity();
        if (te instanceof TileEntityTankFrame) {
            ExtendedBlock eb = ((TileEntityTankFrame) te).getBlock();
            try {
                return new ItemStack(eb.getBlock(), 1, eb.getMetadata());
            } catch (Exception e) {
                return null; // Catch this just in case something goes bad here. It's pretty rare but possible.
            }
        }
        return null;
    }

    @Optional.Method(modid = "Waila")
    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> list, IWailaDataAccessor iWailaDataAccessor, IWailaConfigHandler iWailaConfigHandler) {
        TileEntity te = iWailaDataAccessor.getTileEntity();
        TileEntityValve valve;
        if (te instanceof TileEntityValve) {
            valve = (TileEntityValve) te;
        } else {
            valve = ((TileEntityTankFrame) te).getValve();
            list.add("Part of a tank");
        }

        if (valve == null) return list;

        int fluidAmount = valve.getFluidAmount();
        int capacity = valve.getCapacity();
        if (!valve.isValid()) {
            list.add("Invalid tank");
            return list;
        }

        if(te instanceof TileEntityValve) {
            list.add("Name: " + EnumChatFormatting.ITALIC + valve.getValveName() + EnumChatFormatting.RESET);
            String autoOutput = valve.getAutoOutput() ? "true" : "false";
            list.add("Auto Output: " + (valve.getAutoOutput() ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + EnumChatFormatting.ITALIC + autoOutput + EnumChatFormatting.RESET);
        }

        if (fluidAmount == 0) {
            list.add("Fluid: None");
            list.add("Amount: 0/" + GenericUtil.intToFancyNumber(capacity) + " mB");
        } else {
            String fluid = valve.getFluid().getFluid().getLocalizedName(valve.getFluid());
            list.add("Fluid: " + fluid);
            list.add("Amount: " + GenericUtil.intToFancyNumber(fluidAmount) + "/" + GenericUtil.intToFancyNumber(capacity) + " mB");
        }
        return list;
    }

    @Optional.Method(modid = "Waila")
    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> list, IWailaDataAccessor iWailaDataAccessor, IWailaConfigHandler iWailaConfigHandler) {
        // Unused, implemented because of the interface
        return list;
    }

    @Optional.Method(modid = "Waila")
    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> list, IWailaDataAccessor iWailaDataAccessor, IWailaConfigHandler iWailaConfigHandler) {
        // Unused, implemented because of the interface
        return list;
    }

    @Optional.Method(modid = "Waila")
    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP entityPlayerMP, TileEntity tileEntity, NBTTagCompound nbtTagCompound, World world, int i, int i1, int i2) {
        // Unused, implemented because of the interface
        return nbtTagCompound;
    }
}
