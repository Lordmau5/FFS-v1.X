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
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Created by Dustin on 28.06.2015.
 */
public class BlockValve extends Block {

    public BlockValve() {
        super(Material.iron);
        setBlockName("blockValve");
        setBlockTextureName(FancyFluidStorage.modId + ":" + "blockValve");
        setCreativeTab(CreativeTabs.tabRedstone);
        setHardness(5.0F); // Same hardness as an iron block
        setResistance(10.0F); // Same as hardness
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
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve) world.getTileEntity(x, y, z);
            valve.breakTank(null);
        }
        super.onBlockDestroyedByExplosion(world, x, y, z, explosion);
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

    @Override
    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
        return Item.getItemFromBlock(FancyFluidStorage.blockValve);
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

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity te = world.getTileEntity(x, y, z);
        if(te instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve)te;
            return valve.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        int light = super.getLightValue(world, x, y, z);

        TileEntity tile = world.getTileEntity(x, y, z);
        if(tile != null && tile instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve) tile;

            if(valve.isValid() && valve.getFluid() != null) {
                light = valve.getFluidLuminosity();
            }
        }
        return light;
    }
}
