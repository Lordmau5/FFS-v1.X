package com.lordmau5.ffs.tile.abstracts;

import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by Dustin on 20.01.2016.
 */
public abstract class AbstractTankTile extends TileEntity implements ITickable {

    public enum UpdateType {
        NONE,
        STATE,
        FULL
    }
    public UpdateType updateType = UpdateType.NONE;

    /**
     * Necessary stuff for the interfaces.
     * Current interface list:
     * INameableTile, IFacingTile
     */
    public EnumFacing tile_facing = null;
    public String tile_name = "";

    private BlockPos masterValvePos;

    public void setNeedsUpdate() {
        this.updateType = UpdateType.FULL;
    }

    public void setNeedsUpdate(UpdateType updateType) {
        this.updateType = updateType;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        setNeedsUpdate();
    }

    public boolean isValid() {
        return getMasterValve() != null && getMasterValve().isValid();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    public void setValvePos(BlockPos masterValvePos) {
        this.masterValvePos = masterValvePos;
    }

    public AbstractTankValve getMasterValve() {
        if(getWorld() != null && this.masterValvePos != null) {
            TileEntity tile = getWorld().getTileEntity(this.masterValvePos);
            return tile instanceof AbstractTankValve ? (AbstractTankValve) tile : null;
        }

        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("valveX")) { // TODO: Remove legacy support
            setValvePos(new BlockPos(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ")));
        }
        else if(tag.hasKey("valvePos")) {
            setValvePos(BlockPos.fromLong(tag.getLong("valvePos")));
        }
        else {
            setValvePos(null);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if(getMasterValve() != null) {
            tag.setLong("valvePos", getMasterValve().getPos().toLong());

//            BlockPos pos = getMasterValve().getPos();
//            tag.setInteger("valveX", pos.getX());
//            tag.setInteger("valveY", pos.getY());
//            tag.setInteger("valveZ", pos.getZ());
        }

        super.writeToNBT(tag);
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new SPacketUpdateTileEntity(getPos(), 0, tag);
    }

    public void markForUpdate() {
        if(getWorld() == null) {
            setNeedsUpdate();
            return;
        }

        if(updateType == UpdateType.FULL) {
            IBlockState state = getWorld().getBlockState(getPos());
            getWorld().notifyBlockUpdate(getPos(), state, state, 8);
            if (getWorld().isRemote) {
                getWorld().checkLight(getPos());
            }
        }
        else if(updateType == UpdateType.STATE) {
            GenericUtil.sendTileEntityPacketToPlayers(getUpdatePacket(), getWorld());
            if (getWorld().isRemote) {
                getWorld().checkLight(getPos());
            }
        }

        updateType = UpdateType.NONE;
    }

    @Override
    public void update() {
        if (updateType != UpdateType.NONE) {
            markForUpdate();
            updateType = UpdateType.NONE;
        }
    }

    //------------------------------

    public World getFakeWorld() {
        if(getWorld() == null)
            return null;

        return GenericUtil.getFakeWorld(getWorld());
    }
}
