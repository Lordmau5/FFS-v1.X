package com.lordmau5.ffs.tile;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "framesapi.IMoveCheck", modid = "funkylocomotion")
})
public class TileEntityTankFrame extends TileEntity implements ITickable {

    private BlockPos valvePos;
    private IBlockState camoBlockState;
    private TileEntityValve masterValve;
    private boolean wantsUpdate = false;

    public int oldLightValue, lightValue;

    public void initialize(BlockPos valvePos, IBlockState camoBlockState) {
        this.valvePos = valvePos;
        this.camoBlockState = camoBlockState;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        wantsUpdate = true;
    }

    public void setBlockState(IBlockState blockState) {
        this.camoBlockState = blockState;
    }

    public IBlockState getBlockState() {
        return camoBlockState;
    }

    public boolean isFrameInvalid() {
        TileEntity tile = getWorld().getTileEntity(getPos());
        return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
    }

    public List<EnumFacing> getNeighborBlockOrAir(Block block) {
        List<EnumFacing> dirs = new ArrayList<>();

        if(getValve() == null)
            return dirs;

        BlockPos pos = getPos();
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(block == Blocks.air) {
                if (getWorld().isAirBlock(pos.offset(dr)))
                    dirs.add(dr);
            }
            else {
                BlockPos oPos = getValve().getPos().offset(dr);
                IBlockState otherBlock = getWorld().getBlockState(oPos);
                if(block == otherBlock.getBlock() || getWorld().isAirBlock(oPos))
                    dirs.add(dr);
            }
        }
        return dirs;
    }

    public boolean tryBurning() {
        Block block = getBlockState().getBlock();
        if(block == null)
            return false;

        List<EnumFacing> air = getNeighborBlockOrAir(Blocks.air);
        for(EnumFacing dr : air) {
            if(block.isFlammable(getWorld(), getPos(), dr)) {
                getWorld().setBlockState(getPos(), Blocks.fire.getDefaultState());
                return true;
            }
        }
        return false;
    }

    public void breakFrame() {
        if(isFrameInvalid()) {
            getWorld().setBlockToAir(getPos());
            return;
        }

        getWorld().removeTileEntity(getPos());
        if(getBlockState() != null)
            getWorld().setBlockState(getPos(), getBlockState());
        else
            getWorld().setBlockToAir(getPos());
    }

    public void onBreak() {
        if(getWorld() != null && !getWorld().isRemote && getValve() != null) {
            getValve().breakTank(this);
        }
    }

    public void setValvePos(BlockPos valvePos) {
        this.valvePos = valvePos;
        this.masterValve = null;
    }

    public TileEntityValve getValve() {
        if(masterValve == null && this.valvePos != null) {
            TileEntity tile = getWorld().getTileEntity(this.valvePos);
            masterValve = tile instanceof TileEntityValve ? (TileEntityValve) tile : null;
        }

        return masterValve;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("valveX")) {
            setValvePos(new BlockPos(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ")));
        }

        if(tag.hasKey("blockName")) {
            setBlockState(Block.getBlockFromName(tag.getString("blockName")).getStateFromMeta(tag.getInteger("metadata")));
        }

        if(tag.hasKey("fluidLuminosity")) {
            lightValue = tag.getInteger("fluidLuminosity");
            if(oldLightValue != lightValue) {
                markForUpdate();
                oldLightValue = lightValue;
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        if(getValve() != null) {
            BlockPos pos = getValve().getPos();
            tag.setInteger("valveX", pos.getX());
            tag.setInteger("valveY", pos.getY());
            tag.setInteger("valveZ", pos.getZ());

            tag.setInteger("fluidLuminosity", getValve().getFluidLuminosity());
        }
        if(getBlockState() != null) {
            tag.setString("blockName", getBlockState().getBlock().getRegistryName());
            tag.setInteger("metadata", getBlockState().getBlock().getMetaFromState(getBlockState()));
        }

        super.writeToNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(getPos(), 0, tag);
    }

    public void markForUpdate() {
        if(getWorld() == null) {
            wantsUpdate = true;
            return;
        }

        getWorld().markBlockForUpdate(getPos());
        if(getWorld().isRemote)
            getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
    }

    @Override
    public void update() {
        if (wantsUpdate) {
            markForUpdate();
            wantsUpdate = false;
        }
    }
}
