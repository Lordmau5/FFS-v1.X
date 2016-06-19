package com.lordmau5.ffs;

import com.lordmau5.ffs.client.FluidHelper;
import com.lordmau5.ffs.client.OverlayRenderHandler;
import com.lordmau5.ffs.client.TankFrameModel;
import com.lordmau5.ffs.compat.Compatibility;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.proxy.GuiHandler;
import com.lordmau5.ffs.proxy.IProxy;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.ModBlocksAndItems;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.crash.CrashReport;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fluids.Fluid;
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
@Mod(modid = FancyFluidStorage.modId, name = "Fancy Fluid Storage", dependencies="after:waila;after:OpenComputers;after:ComputerCraft;after:chisel")
public class FancyFluidStorage {

    public static final String modId = "FFS";

    public static Block blockFluidValve;
    public static Block blockMetaphaser;
    public static Block blockTankComputer;
    public static Block blockTankFrame;
    public static Block blockTankFrameOpaque;

    public static Fluid fluidMetaphasedFlux;

    public static Configuration CONFIG;

    @Mod.Instance(modId)
    public static FancyFluidStorage INSTANCE;

    @SidedProxy(clientSide = "com.lordmau5.ffs.proxy.ClientProxy", serverSide = "com.lordmau5.ffs.proxy.CommonProxy")
    public static IProxy PROXY;

    public int MB_PER_TANK_BLOCK = 16000;
    public boolean INSIDE_CAPACITY = false;
    public int MAX_SIZE = 7;
    public int MIN_BURNABLE_TEMPERATURE = 1300;
    public boolean SET_WORLD_ON_FIRE = true;
    public boolean SHOULD_TANKS_LEAK = true;

    public boolean TANK_RENDER_INSIDE = true;
    public boolean TANK_OVERLAY_RENDER = true;
    public int METAPHASED_FLUX_ENERGY_LOSS = 10;

    public enum TankFrameMode {
        SAME_BLOCK,
        DIFFERENT_METADATA,
        DIFFERENT_BLOCK
    }
    public TankFrameMode TANK_FRAME_MODE = TankFrameMode.SAME_BLOCK;

    public void loadConfig() {
        CONFIG.load();

        Property mbPerTankProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "mbPerVirtualTank", 16000);
        mbPerTankProp.setComment("How many millibuckets can each block within the tank store? (Has to be higher than 1!)\nDefault: 16000");
        MB_PER_TANK_BLOCK = Math.max(1, Math.min(Integer.MAX_VALUE, mbPerTankProp.getInt(16000)));
        if(mbPerTankProp.getInt(16000) < 1 || mbPerTankProp.getInt(16000) > Integer.MAX_VALUE)
            mbPerTankProp.set(16000);

        Property insideCapacityProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "onlyCountInsideCapacity", true);
        insideCapacityProp.setComment("Should tank capacity only count the interior air blocks, rather than including the frame?\nDefault: true");
        INSIDE_CAPACITY = insideCapacityProp.getBoolean(true);

        Property maxSizeProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "maxSize", 13);
        maxSizeProp.setComment("Define the maximum size a tank can have. This includes the whole tank, including the frame!\nMinimum: 3, Maximum: 13\nDefault: 7");
        MAX_SIZE = Math.max(3, Math.min(maxSizeProp.getInt(7), 13));
        if(maxSizeProp.getInt(7) < 3 || maxSizeProp.getInt(7) > 13)
            maxSizeProp.set(7);

        Property tankFrameModeProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "tankFrameMode", 1);
        tankFrameModeProp.setComment("Declare which mode you want the tank frames to be.\n0 = Only the same block with the same metadata is allowed\n1 = Only the same block is allowed, but the metadata can be different\n2 = Allow any block\nDefault: 1");
        int mode = tankFrameModeProp.getInt(1);
        if(mode < 0 || mode > 2) {
            mode = 1;
            tankFrameModeProp.set(1);
        }
        TANK_FRAME_MODE = TankFrameMode.values()[mode];

        Property minBurnProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "minimumBurnableTemperature", 1300);
        minBurnProp.setComment("At which temperature should a tank start burning on a random occasion? (Has to be positive!)\nThis only applies to blocks that are flammable, like Wood or Wool.\nDefault: 1300 (Temperature of Lava)\n0 to disable.");
        MIN_BURNABLE_TEMPERATURE = Math.max(0, minBurnProp.getInt(1300));
        if(minBurnProp.getInt(1300) < 0)
            minBurnProp.set(1300);

        Property setWorldOnFireProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "setWorldOnFire", true);
        setWorldOnFireProp.setComment("Do you want to set the world on fire? Or do you just want to create a flame in my heart?\n(Don't worry, this is harmless :))\nDefault: true");
        SET_WORLD_ON_FIRE = setWorldOnFireProp.getBoolean(true);

        Property tanksLeakProp = CONFIG.get(Configuration.CATEGORY_GENERAL, "shouldTanksLeak", true);
        tanksLeakProp.setComment("Should tanks with leaky materials start leaking randomly?\nDefault: true");
        SHOULD_TANKS_LEAK = tanksLeakProp.getBoolean(true);

        Property tankRenderInside = CONFIG.get(Configuration.CATEGORY_GENERAL, "tanksRenderInsideOnly", true);
        tankRenderInside.setComment("Should tanks only render the inside as fluid or extend to the frame-sides?\nDefault: true");
        TANK_RENDER_INSIDE = tankRenderInside.getBoolean(true);

        Property tankOverlayRender = CONFIG.get(Configuration.CATEGORY_CLIENT, "tankOverlayRender", true);
        tankOverlayRender.setComment("Should a tank overlay be temporarily rendered on the tank when you look at the one?\nDefault: true");
        TANK_OVERLAY_RENDER = tankOverlayRender.getBoolean(true);

        Property metaphasedFluxEnergyLoss = CONFIG.get(Configuration.CATEGORY_GENERAL, "metaphasedFluxEnergyLoss", 10);
        metaphasedFluxEnergyLoss.setComment("The amount of energy loss you have when you extract energy / metaphased flux from the tank.\nDefault: 10\nValue needs to be between 0 and 50!\n0 to disable.");
        METAPHASED_FLUX_ENERGY_LOSS = Math.max(0, metaphasedFluxEnergyLoss.getInt(10));
        if(metaphasedFluxEnergyLoss.getInt(10) < 0 || metaphasedFluxEnergyLoss.getInt(10) > 50)
            metaphasedFluxEnergyLoss.set(10);

        if (CONFIG.hasChanged()) {
            CONFIG.save();
        }
    }

    private void checkForBadChisel(FMLPreInitializationEvent event) {
        try {
            Class.forName("com.cricketcraft.chisel.Chisel");

            throw new ReportedException(new CrashReport("Compatibility error", new Exception("You are using an unsupported version of Chisel, which crashes my mod when being used.\n" +
                    "Please use the proper, supported version from one of the following links:\n" +
                    "http://minecraft.curseforge.com/projects/chisel\n" +
                    "http://ci.tterrag.com/job/Chisel/branch/1.9%252Fdev/")));
        }
        catch(ClassNotFoundException e) {
        }
    }

    @SuppressWarnings("deprecation")
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        checkForBadChisel(event);

        Compatibility.INSTANCE.init();

        CONFIG = new Configuration(event.getSuggestedConfigurationFile());
        loadConfig();

        ModBlocksAndItems.preInit(event);

        NetworkRegistry.INSTANCE.registerGuiHandler(FancyFluidStorage.INSTANCE, new GuiHandler());
        NetworkHandler.registerChannels(event.getSide());

        PROXY.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        GameRegistry.addRecipe(new ItemStack(blockFluidValve), "IGI", "GBG", "IGI",
                'I', Items.IRON_INGOT,
                'G', Blocks.IRON_BARS,
                'B', Items.BUCKET);

        GameRegistry.addRecipe(new ItemStack(blockTankComputer), "IGI", "GBG", "IGI",
                'I', Items.IRON_INGOT,
                'G', Blocks.IRON_BARS,
                'B', Blocks.REDSTONE_BLOCK);

        if(Compatibility.INSTANCE.isEnergyModSupplied()) {
            GameRegistry.addRecipe(new ItemStack(blockMetaphaser), "IGI", "GBG", "IGI",
                    'I', Items.IRON_INGOT,
                    'G', Blocks.IRON_BARS,
                    'B', Items.COMPARATOR);
        }

        PROXY.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        GenericUtil.init();

        ForgeChunkManager.setForcedChunkLoadingCallback(INSTANCE, (tickets, world) -> {
            if(tickets != null && tickets.size() > 0)
                GenericUtil.initChunkLoadTicket(world, tickets.get(0));
        });
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void loadTextures(TextureStitchEvent.Pre event) {
        FluidHelper.initTextures(event.getMap());

        OverlayRenderHandler.overlayTexture = event.getMap().registerSprite(new ResourceLocation("ffs", "blocks/overlay/tankOverlayAnim"));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void bakeSweetModels(ModelBakeEvent event) {
        event.getModelRegistry().putObject(new ModelResourceLocation("ffs:blockTankFrame", "normal"), new TankFrameModel());
        event.getModelRegistry().putObject(new ModelResourceLocation("ffs:blockTankFrameOpaque", "normal"), new TankFrameModel());
    }

}
