package com.lordmau5.ffs.block.valves;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.block.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.valves.TileEntityEnergyValve;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * Created by Dustin on 08.02.2016.
 */
public class BlockEnergyValve extends AbstractBlockValve {

    public BlockEnergyValve() {
        super("blockEnergyValve", FancyFluidStorage.blockEnergyValve);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityEnergyValve();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntityEnergyValve valve = (TileEntityEnergyValve) world.getTileEntity(pos);

        if(valve.isValid()) {
            if(GenericUtil.isFluidContainer(player.getHeldItem()))
                return GenericUtil.fluidContainerHandler(world, pos, valve, player, side);

            if (player.isSneaking()) {
                valve.isExtract = !valve.isExtract;
                player.addChatComponentMessage(new ChatComponentText("Now: " + valve.isExtract));
                return true;
            }
            player.openGui(FancyFluidStorage.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        else {
            if (player.isSneaking()) return false;

            valve.buildTank(side.getOpposite());
        }
        return true;
    }
}
