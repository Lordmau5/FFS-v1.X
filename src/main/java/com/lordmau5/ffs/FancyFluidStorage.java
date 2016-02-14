package com.lordmau5.ffs;

import com.lordmau5.ffs.block.tanktiles.BlockTankComputer;
import com.lordmau5.ffs.block.tanktiles.BlockTankFrame;
import com.lordmau5.ffs.block.tanktiles.BlockTankFrameOpaque;
import com.lordmau5.ffs.block.valves.BlockEnergyValve;
import com.lordmau5.ffs.block.valves.BlockFluidValve;
import com.lordmau5.ffs.client.FluidHelper;
import com.lordmau5.ffs.client.TankFrameModel;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.proxy.CommonProxy;
import com.lordmau5.ffs.proxy.GuiHandler;
import com.lordmau5.ffs.tile.valves.TileEntityEnergyValve;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankComputer;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankFrame;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by Dustin on 28.06.2015.
 */
@Mod(modid = FancyFluidStorage.modId, name = "Fancy Fluid Storage", dependencies="after:waila;after:chisel;after:OpenComputers;after:ComputerCraft;after:BuildCraftAPI|transport;after:funkylocomotion")
public class FancyFluidStorage {

    public static final String modId = "FFS";

    public static BlockFluidValve blockFluidValve;
    public static BlockEnergyValve blockEnergyValve;
    public static BlockTankComputer blockTankComputer;
    public static BlockTankFrame blockTankFrame;
    public static BlockTankFrameOpaque blockTankFrameOpaque;

    public static Configuration config;

    @Mod.Instance(modId)
    public static FancyFluidStorage instance;

    @SidedProxy(clientSide = "com.lordmau5.ffs.proxy.ClientProxy", serverSide = "com.lordmau5.ffs.proxy.CommonProxy")
    public static CommonProxy proxy;

    public int MB_PER_TANK_BLOCK = 16000;
    public boolean INSIDE_CAPACITY = false;
    public int MAX_SIZE = 7;
    public int MIN_BURNABLE_TEMPERATURE = 1300;
    public boolean SET_WORLD_ON_FIRE = true;
    public boolean SHOULD_TANKS_LEAK = true;

    public boolean TANK_RENDER_INSIDE = true;

    public enum TankFrameMode {
        SAME_BLOCK,
        DIFFERENT_METADATA,
        DIFFERENT_BLOCK
    }
    public TankFrameMode TANK_FRAME_MODE = TankFrameMode.SAME_BLOCK;

    public void loadConfig() {
        config.load();

        Property mbPerTankProp = config.get(Configuration.CATEGORY_GENERAL, "mbPerVirtualTank", 16000);
        mbPerTankProp.comment = "How many millibuckets can each block within the tank store? (Has to be higher than 1!)\nDefault: 16000";
        MB_PER_TANK_BLOCK = Math.max(1, Math.min(Integer.MAX_VALUE, mbPerTankProp.getInt(16000)));
        if(mbPerTankProp.getInt(16000) < 1 || mbPerTankProp.getInt(16000) > Integer.MAX_VALUE)
            mbPerTankProp.set(16000);

        Property insideCapacityProp = config.get(Configuration.CATEGORY_GENERAL, "onlyCountInsideCapacity", true);
        insideCapacityProp.comment = "Should tank capacity only count the interior air blocks, rather than including the frame?\nDefault: true";
        INSIDE_CAPACITY = insideCapacityProp.getBoolean(true);

        Property maxSizeProp = config.get(Configuration.CATEGORY_GENERAL, "maxSize", 13);
        maxSizeProp.comment = "Define the maximum size a tank can have. This includes the whole tank, including the frame!\nMinimum: 3, Maximum: 13\nDefault: 7";
        MAX_SIZE = Math.max(3, Math.min(maxSizeProp.getInt(7), 13));
        if(maxSizeProp.getInt(7) < 3 || maxSizeProp.getInt(7) > 13)
            maxSizeProp.set(7);

        Property tankFrameModeProp = config.get(Configuration.CATEGORY_GENERAL, "tankFrameMode", 1);
        tankFrameModeProp.comment = "Declare which mode you want the tank frames to be.\n0 = Only the same block with the same metadata is allowed\n1 = Only the same block is allowed, but the metadata can be different\n2 = Allow any block\nDefault: 1";
        int mode = tankFrameModeProp.getInt(1);
        if(mode < 0 || mode > 2) {
            mode = 1;
            tankFrameModeProp.set(1);
        }
        TANK_FRAME_MODE = TankFrameMode.values()[mode];

        Property minBurnProp = config.get(Configuration.CATEGORY_GENERAL, "minimumBurnableTemperature", 1300);
        minBurnProp.comment = "At which temperature should a tank start burning on a random occasion? (Has to be positive!)\nThis only applies to blocks that are flammable, like Wood or Wool.\nDefault: 1300 (Temperature of Lava)\n0 to disable.";
        MIN_BURNABLE_TEMPERATURE = Math.max(0, minBurnProp.getInt(1300));
        if(minBurnProp.getInt(1300) < 0)
            minBurnProp.set(1300);

        Property setWorldOnFireProp = config.get(Configuration.CATEGORY_GENERAL, "setWorldOnFire", true);
        setWorldOnFireProp.comment = "Do you want to set the world on fire? Or do you just want to create a flame in my heart?\n(Don't worry, this is harmless :))\nDefault: true";
        SET_WORLD_ON_FIRE = setWorldOnFireProp.getBoolean(true);

        Property tanksLeakProp = config.get(Configuration.CATEGORY_GENERAL, "shouldTanksLeak", true);
        tanksLeakProp.comment = "Should tanks with leaky materials start leaking randomly?\nDefault: true";
        SHOULD_TANKS_LEAK = tanksLeakProp.getBoolean(true);

        Property tankRenderInside = config.get(Configuration.CATEGORY_GENERAL, "tanksRenderInsideOnly", true);
        tankRenderInside.comment = "Should tanks only render the inside as fluid or extend to the frame-sides?\nDefault: true";
        TANK_RENDER_INSIDE = tankRenderInside.getBoolean(true);

        if (config.hasChanged()) {
            config.save();
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        loadConfig();

        GameRegistry.registerBlock(blockFluidValve = new BlockFluidValve(), "blockFluidValve");
        GameRegistry.registerBlock(blockEnergyValve = new BlockEnergyValve(), "blockEnergyValve");
        GameRegistry.registerBlock(blockTankComputer = new BlockTankComputer(), "blockTankComputer");
        GameRegistry.registerBlock(blockTankFrame = new BlockTankFrame("blockTankFrame"), "blockTankFrame");
        GameRegistry.registerBlock(blockTankFrameOpaque = new BlockTankFrameOpaque(), "blockTankFrameOpaque");

        GameRegistry.registerTileEntity(TileEntityFluidValve.class, "tileEntityFluidValve");
        GameRegistry.registerTileEntity(TileEntityEnergyValve.class, "tileEntityEnergyValve");
        GameRegistry.registerTileEntity(TileEntityTankComputer.class, "tileEntityTankComputer");
        GameRegistry.registerTileEntity(TileEntityTankFrame.class, "tileEntityTankFrame");

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        NetworkHandler.registerChannels(event.getSide());

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        GameRegistry.addRecipe(new ItemStack(blockFluidValve), "IGI", "GBG", "IGI",
                'I', Items.iron_ingot,
                'G', Blocks.iron_bars,
                'B', Items.bucket);

        GameRegistry.addRecipe(new ItemStack(blockTankComputer), "IGI", "GBG", "IGI",
                'I', Items.iron_ingot,
                'G', Blocks.iron_bars,
                'B', Items.comparator);

        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        GenericUtil.init();

        ForgeChunkManager.setForcedChunkLoadingCallback(instance, (tickets, world) -> {
            // Good day sir
        });
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void loadTextures(TextureStitchEvent.Post event) {
        FluidHelper.initTextures(event.map);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void bakeSweetModels(ModelBakeEvent event) {
        event.modelRegistry.putObject(new ModelResourceLocation("ffs:blockTankFrame", "normal"), new TankFrameModel());
        event.modelRegistry.putObject(new ModelResourceLocation("ffs:blockTankFrameOpaque", "normal"), new TankFrameModel());
    }

}
