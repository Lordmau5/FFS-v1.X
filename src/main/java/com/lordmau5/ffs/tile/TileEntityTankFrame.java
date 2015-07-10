package com.lordmau5.ffs.tile;

import com.lordmau5.ffs.util.ExtendedBlock;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * Created by Dustin on 28.06.2015.
 */
public class TileEntityTankFrame extends TileEntity {

    private ExtendedBlock block;
    private int valveX, valveY, valveZ;
    private TileEntityValve masterValve;
    private boolean initiated = false;

    public TileEntityTankFrame() {
        super();
    }

    public TileEntityTankFrame(TileEntityValve masterValve, ExtendedBlock block) {
        this.masterValve = masterValve;
        this.block = block;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if(!initiated && masterValve == null) {
            masterValve = (TileEntityValve) worldObj.getTileEntity(valveX, valveY, valveZ);
            if(masterValve != null) {
                initiated = true;
                markForUpdate();
            }
        }
    }

    public void onBreak() {
        if(masterValve != null && !worldObj.isRemote) {
            masterValve.breakTank(this);
        }
    }

    public void setValve(TileEntityValve valve) {
        this.masterValve = valve;
    }

    public TileEntityValve getValve() {
        return this.masterValve;
    }

    public ExtendedBlock getBlock() {
        return block;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("valveX")) {
            valveX = tag.getInteger("valveX");
            valveY = tag.getInteger("valveY");
            valveZ = tag.getInteger("valveZ");
        }
        if(tag.hasKey("blockId")) {
            block = new ExtendedBlock(Block.getBlockById(tag.getInteger("blockId")), tag.getInteger("metadata"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        if(masterValve != null) {
            tag.setInteger("valveX", masterValve.xCoord);
            tag.setInteger("valveY", masterValve.yCoord);
            tag.setInteger("valveZ", masterValve.zCoord);
        }
        if(block != null) {
            tag.setInteger("blockId", Block.getIdFromBlock(block.getBlock()));
            tag.setInteger("metadata", block.getMetadata());
        }
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        markForUpdate();
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    void markForUpdate() {
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

}
