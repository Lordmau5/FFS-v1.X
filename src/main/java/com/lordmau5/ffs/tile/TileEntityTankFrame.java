package com.lordmau5.ffs.tile;

import com.lordmau5.ffs.util.ExtendedBlock;
import cpw.mods.fml.common.Optional;
import framesapi.IMoveCheck;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "framesapi.IMoveCheck", modid = "funkylocomotion")
})
public class TileEntityTankFrame extends TileEntity implements IMoveCheck {

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

        if(!initiated) {
            if(masterValve == null) {
                masterValve = (TileEntityValve) worldObj.getTileEntity(valveX, valveY, valveZ);
                if(masterValve != null) {
                    if (!masterValve.tankFrames.contains(this) && !worldObj.isRemote) {
                        worldObj.removeTileEntity(xCoord, yCoord, zCoord);
                        worldObj.setBlock(xCoord, yCoord, zCoord, block.getBlock(), block.getMetadata(), 2);
                        return;
                    }
                }
            }
            initiated = true;
            markForUpdate();
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

    @Optional.Method(modid = "funkylocomotion")
    @Override
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }
}
