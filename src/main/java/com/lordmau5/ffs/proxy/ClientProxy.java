package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.compat.WailaPluginTank;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;

/**
 * Created by Dustin on 29.06.2015.
 */
public class ClientProxy extends CommonProxy {

    public void preInit() {
        //ValveRenderer vr = new ValveRenderer();
        //ClientRegistry.bindTileEntitySpecialRenderer(TileEntityValve.class, vr);

        //RenderingRegistry.registerBlockHandler(vr);

        //ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(FancyFluidStorage.blockValve), 0, new ModelResourceLocation("ffs:blockValve", "inventory"));
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

    /*
    @Override
    public void registerIcons(IIconRegister iR) {
        tex_Valve       = iR.registerIcon(FancyFluidStorage.modId + ":blockValve");
        tex_ValveItem   = iR.registerIcon(FancyFluidStorage.modId + ":blockValve_Item");
        tex_SlaveValve  = new IIcon[]{iR.registerIcon(FancyFluidStorage.modId + ":tankValve_0"), iR.registerIcon(FancyFluidStorage.modId + ":tankValve_1")};
        tex_MasterValve = new IIcon[]{iR.registerIcon(FancyFluidStorage.modId + ":tankMaster_0"), iR.registerIcon(FancyFluidStorage.modId + ":tankMaster_1")};
    }
    */
}
