package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.compat.CCPeripheralProvider;
import cpw.mods.fml.common.Loader;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * Created by Dustin on 29.06.2015.
 */
public class CommonProxy {

    public IIcon tex_Valve, tex_ValveItem;
    public IIcon[] tex_SlaveValve, tex_MasterValve;

    public void preInit() { }

    public void registerIcons(IIconRegister iR) { }

    public void init() {
        if(Loader.isModLoaded("ComputerCraft")) {
            new CCPeripheralProvider().register();
        }
    }
}
