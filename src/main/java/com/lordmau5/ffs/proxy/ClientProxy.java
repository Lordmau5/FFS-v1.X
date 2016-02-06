package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.client.ValveRenderer;
import com.lordmau5.ffs.compat.WailaPluginTank;
import com.lordmau5.ffs.tile.TileEntityTankValve;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
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
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTankValve.class, new ValveRenderer());

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(FancyFluidStorage.blockValve), 0, new ModelResourceLocation("ffs:blockValve", "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(FancyFluidStorage.blockTankComputer), 0, new ModelResourceLocation("ffs:blockTankComputer", "inventory"));
        ModelLoader.setCustomStateMapper(new BlockTankFrame(), new StateMapperBase(){
            protected ModelResourceLocation getModelResourceLocation(IBlockState p_178132_1_)
            {
                return new ModelResourceLocation("ffs:blockValve", "normal");
            }
        });
    }

    public void init() {
        if(Loader.isModLoaded("Waila")) {
            WailaPluginTank.init();
        }
        super.init();
    }
}
