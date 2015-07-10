package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.TankFrameRenderer;
import com.lordmau5.ffs.client.ValveRenderer;
import com.lordmau5.ffs.compat.WailaPluginTank;
import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInterModComms;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * Created by Dustin on 29.06.2015.
 */
public class ClientProxy extends CommonProxy {

    public void preInit() {
        ValveRenderer vr = new ValveRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityValve.class, vr);

        RenderingRegistry.registerBlockHandler(vr);
        RenderingRegistry.registerBlockHandler(new TankFrameRenderer());

        // Initialize the Waila Plugin
        FMLInterModComms.sendMessage("Waila", "register", WailaPluginTank.class.getName() + ".registerPlugin");
    }

    @Override
    public void registerIcons(IIconRegister iR) {
        tex_Valve       = iR.registerIcon(FancyFluidStorage.modId + ":blockValve");
        tex_ValveItem   = iR.registerIcon(FancyFluidStorage.modId + ":blockValve_Item");
        tex_SlaveValve  = new IIcon[]{iR.registerIcon(FancyFluidStorage.modId + ":tankValve_0"), iR.registerIcon(FancyFluidStorage.modId + ":tankValve_1")};
        tex_MasterValve = new IIcon[]{iR.registerIcon(FancyFluidStorage.modId + ":tankMaster_0"), iR.registerIcon(FancyFluidStorage.modId + ":tankMaster_1")};
    }
}
