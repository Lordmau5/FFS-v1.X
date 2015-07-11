package com.lordmau5.ffs;

import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.blocks.BlockValve;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.proxy.CommonProxy;
import com.lordmau5.ffs.proxy.GuiHandler;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Created by Dustin on 28.06.2015.
 */
@Mod(modid = FancyFluidStorage.modId, name = "Fancy Fluid Storage")
public class FancyFluidStorage {

    public static final String modId = "FFS";

    public static BlockValve blockValve;
    public static BlockTankFrame blockTankFrame;

    @Mod.Instance(modId)
    public static FancyFluidStorage instance;

    @SidedProxy(clientSide = "com.lordmau5.ffs.proxy.ClientProxy", serverSide = "com.lordmau5.ffs.proxy.CommonProxy")
    public static CommonProxy proxy;

    public int MB_PER_TANK_BLOCK = 16000;
    public boolean INSIDE_CAPACITY = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();

        Configuration config = new Configuration(event.getSuggestedConfigurationFile());

        config.load();

        Property mbPerTankProp = config.get(Configuration.CATEGORY_GENERAL, "mbPerVirtualTank", 16000);
        mbPerTankProp.comment = "How many millibuckets can each block within the tank store?";
        MB_PER_TANK_BLOCK = mbPerTankProp.getInt(16000);

        Property insideCapacityProp = config.get(Configuration.CATEGORY_GENERAL, "onlyCountInsideCapacity", false);
        insideCapacityProp.comment = "Should tank capacity only count the interior air blocks, rather than including the frame?";
        INSIDE_CAPACITY = insideCapacityProp.getBoolean(false);

        if (config.hasChanged()) {
            config.save();
        }

        GameRegistry.registerBlock(blockValve = new BlockValve(), "blockValve");
        GameRegistry.registerBlock(blockTankFrame = new BlockTankFrame(), "blockTankFrame");

        GameRegistry.registerTileEntity(TileEntityValve.class, "tileEntityValve");
        GameRegistry.registerTileEntity(TileEntityTankFrame.class, "tileEntityTankFrame");

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        NetworkHandler.registerChannels(event.getSide());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        GameRegistry.addRecipe(new ItemStack(blockValve), "IGI", "GBG", "IGI",
                'I', Items.iron_ingot,
                'G', Blocks.iron_bars,
                'B', Items.bucket);

        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        GenericUtil.init();
    }

}
