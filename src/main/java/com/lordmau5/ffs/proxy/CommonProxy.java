package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.block.fluid.FluidMetaphasedFlux;
import com.lordmau5.ffs.block.tanktiles.BlockTankComputer;
import com.lordmau5.ffs.block.tanktiles.BlockTankFrame;
import com.lordmau5.ffs.block.tanktiles.BlockTankFrameOpaque;
import com.lordmau5.ffs.block.valves.BlockFluidValve;
import com.lordmau5.ffs.block.valves.BlockMetaphaser;
import com.lordmau5.ffs.compat.oc.OCCompatibility;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankComputer;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankFrame;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Created by Dustin on 29.06.2015.
 */
public class CommonProxy implements IProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerWithItem(FancyFluidStorage.blockFluidValve = new BlockFluidValve());
        GameRegistry.registerWithItem(FancyFluidStorage.blockTankComputer = new BlockTankComputer());
        GameRegistry.registerWithItem(FancyFluidStorage.blockTankFrame = new BlockTankFrame("blockTankFrame"));
        GameRegistry.registerWithItem(FancyFluidStorage.blockTankFrameOpaque = new BlockTankFrameOpaque());

        GameRegistry.registerTileEntity(TileEntityFluidValve.class, "tileEntityFluidValve");
        GameRegistry.registerTileEntity(TileEntityTankComputer.class, "tileEntityTankComputer");
        GameRegistry.registerTileEntity(TileEntityTankFrame.class, "tileEntityTankFrame");

        FluidRegistry.registerFluid(FancyFluidStorage.fluidMetaphasedFlux = new FluidMetaphasedFlux());
        GameRegistry.registerWithItem(FancyFluidStorage.blockMetaphaser = new BlockMetaphaser());
        GameRegistry.registerTileEntity(TileEntityMetaphaser.class, "tileEntityMetaphaser");

        NetworkRegistry.INSTANCE.registerGuiHandler(FancyFluidStorage.INSTANCE, new GuiHandler());

        NetworkHandler.registerChannels(event.getSide());
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new FancyFluidStorage());

        if(Loader.isModLoaded("ComputerCraft")) {
            //new CCPeripheralProvider().register();
        }

        if(Loader.isModLoaded("OpenComputers")) {
            new OCCompatibility().init();
        }
    }
}
