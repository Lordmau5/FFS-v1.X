package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.ValveRenderer;
import com.lordmau5.ffs.compat.waila.WailaPluginTank;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;

/**
 * Created by Dustin on 29.06.2015.
 */
public class ClientProxy extends CommonProxy {

    public void preInit() {
        ClientRegistry.bindTileEntitySpecialRenderer(AbstractTankValve.class, new ValveRenderer());

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(FancyFluidStorage.blockFluidValve), 0, new ModelResourceLocation("ffs:blockFluidValve", "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(FancyFluidStorage.blockEnergyValve), 0, new ModelResourceLocation("ffs:blockEnergyValve", "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(FancyFluidStorage.blockTankComputer), 0, new ModelResourceLocation("ffs:blockTankComputer", "inventory"));
    }

    public void init() {
        if(Loader.isModLoaded("Waila")) {
            WailaPluginTank.init();
        }
        super.init();
    }
}
