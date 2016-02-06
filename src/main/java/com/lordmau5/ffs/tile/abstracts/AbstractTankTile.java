package com.lordmau5.ffs.tile.abstracts;

import com.lordmau5.ffs.tile.TileEntityTankValve;
import com.lordmau5.ffs.util.FakeWorldWrapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

/**
 * Created by Dustin on 20.01.2016.
 */
public abstract class AbstractTankTile extends TileEntity implements ITickable {

    /**
     * Necessary stuff for the interfaces.
     * Current interface list:
     * INameableTile, IFacingTile
     */
    public EnumFacing tile_facing = null;
    public String tile_name = "";

    private boolean needsUpdate;

    private BlockPos masterValvePos;
    private TileEntityTankValve masterValve;

    public void setNeedsUpdate() {
        this.needsUpdate = true;
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
        this.masterValve = null;
    }

    public AbstractTankValve getMasterValve() {
        if(getWorld() != null && masterValve == null && this.masterValvePos != null) {
            TileEntity tile = getWorld().getTileEntity(this.masterValvePos);
            masterValve = tile instanceof TileEntityTankValve ? (TileEntityTankValve) tile : null;
        }

        return masterValve;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("valveX")) {
            setValvePos(new BlockPos(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ")));
        }
        else {
            setValvePos(null);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        if(getMasterValve() != null) {
            BlockPos pos = getMasterValve().getPos();
            tag.setInteger("valveX", pos.getX());
            tag.setInteger("valveY", pos.getY());
            tag.setInteger("valveZ", pos.getZ());
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
            needsUpdate = true;
            return;
        }

        getWorld().markBlockForUpdate(getPos());
        if(getWorld().isRemote) {
            getWorld().checkLight(getPos());
        }
    }

    @Override
    public void update() {
        if (needsUpdate) {
            markForUpdate();
            needsUpdate = false;
        }
    }

    //------------------------------

    private FakeWorldWrapper wrapper;

    public World getFakeWorld() {
        if(worldObj == null)
            return null;

        if(wrapper == null || wrapper.wrappedWorld != worldObj)
            wrapper = new FakeWorldWrapper(worldObj);

        return wrapper;
    }
}
