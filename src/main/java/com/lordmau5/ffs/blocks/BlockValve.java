package com.lordmau5.ffs.blocks;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.ValveRenderer;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Created by Dustin on 28.06.2015.
 */
public class BlockValve extends Block {

    public BlockValve() {
        super(Material.iron);
        setBlockName("blockValve");
        setBlockTextureName(FancyFluidStorage.modId + ":" + "blockValve");
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityValve();
    }

    @Override
    public void registerBlockIcons(IIconRegister iR) {
        super.registerBlockIcons(iR);

        FancyFluidStorage.proxy.registerIcons(iR);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        if(!world.isRemote) {
            TileEntityValve valve = (TileEntityValve) world.getTileEntity(x, y, z);
            valve.breakTank(null);
        }

        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)) {
            return true;
        }
        if (player.isSneaking()) return false;

        TileEntityValve valve = (TileEntityValve) world.getTileEntity(x, y, z);

        if(valve.isValid()) {
            if(GenericUtil.isFluidContainer(player.getHeldItem()))
                return GenericUtil.fluidContainerHandler(world, x, y, z, valve, player);

            player.openGui(FancyFluidStorage.instance, 0, world, x, y, z);
            return true;
        }
        else {
            valve.buildTank(ForgeDirection.getOrientation(side).getOpposite());
        }
        return true;

    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType() {
        return ValveRenderer.id;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        ForgeHooksClient.setRenderPass(pass);
        return true;
    }

    @Override
    public int getRenderBlockPass() {
        return 0;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isNormalCube() {
        return true;
    }
}
