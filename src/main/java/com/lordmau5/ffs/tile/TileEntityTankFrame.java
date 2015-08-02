package com.lordmau5.ffs.tile;

import com.lordmau5.ffs.util.ExtendedBlock;

import cpw.mods.fml.common.Optional;
import framesapi.IMoveCheck;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "framesapi.IMoveCheck", modid = "funkylocomotion")
})
public class TileEntityTankFrame extends TileEntity implements IMoveCheck {

    private ExtendedBlock block;
    public int valveX, valveY, valveZ;
    private TileEntityValve masterValve;
    private boolean hasValve = false;
    private Fluid lastFluid = null;

    public TileEntityTankFrame() {
        super();
    }

    public TileEntityTankFrame(TileEntityValve masterValve, ExtendedBlock block) {
        this.masterValve = masterValve;
        this.block = block;
    }

    @Override
    public void updateEntity() {
        if(masterValve == null && hasValve) {
            TileEntity tile = worldObj.getTileEntity(valveX, valveY, valveZ);
            if(tile != null && tile instanceof TileEntityValve)
                setValve((TileEntityValve) tile);
        }
        else if (!worldObj.isRemote && getValve() != null)
        {
            Fluid held = getValve().getFluid() == null ? null : getValve().getFluid().getFluid();
            if (lastFluid != held)
            {
                lastFluid = held;
                worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
            }
        }
    }

    public boolean isFrameInvalid() {
        TileEntity tile = worldObj.getTileEntity(xCoord, yCoord, zCoord);
        return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
    }

    public List<ForgeDirection> getNeighborBlockOrAir(Block block) {
        List<ForgeDirection> dirs = new ArrayList<>();
        for(ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            if(block == Blocks.air) {
                if (worldObj.isAirBlock(xCoord + dr.offsetX, yCoord + dr.offsetY, zCoord + dr.offsetZ))
                    dirs.add(dr);
            }
            else {
                Block otherBlock = worldObj.getBlock(xCoord + dr.offsetX, yCoord + dr.offsetY, zCoord + dr.offsetZ);
                if(block == otherBlock || worldObj.isAirBlock(xCoord + dr.offsetX, yCoord + dr.offsetY, zCoord + dr.offsetZ))
                    dirs.add(dr);
            }
        }
        return dirs;
    }

    public boolean tryBurning() {
        Block block = getBlock().getBlock();
        if(block == null)
            return false;

        List<ForgeDirection> air = getNeighborBlockOrAir(Blocks.air);
        for(ForgeDirection dr : air) {
            if(block.isFlammable(worldObj, xCoord, yCoord, zCoord, dr)) {
                worldObj.setBlock(xCoord + dr.offsetX, yCoord + dr.offsetY, zCoord + dr.offsetZ, Blocks.fire, 0, 3);
                return true;
            }
        }
        return false;
    }

    public void breakFrame() {
        if(isFrameInvalid())
            return;

        worldObj.removeTileEntity(xCoord, yCoord, zCoord);
        if(block != null && block.getBlock() != null)
            worldObj.setBlock(xCoord, yCoord, zCoord, block.getBlock(), block.getMetadata(), 3);
        else
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
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
        if(this.masterValve == null && hasValve) {
            TileEntity tile = worldObj.getTileEntity(valveX, valveY, valveZ);
            if(tile != null && tile instanceof TileEntityValve)
                setValve((TileEntityValve) tile);
        }
        return this.masterValve;
    }

    public void setBlock(ExtendedBlock block) {
        this.block = block;
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
            hasValve = true;
        }
        if(tag.hasKey("blockId")) {
            this.block = new ExtendedBlock(Block.getBlockById(tag.getInteger("blockId")), tag.getInteger("metadata"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        if(getValve() != null) {
            tag.setInteger("valveX", getValve().xCoord);
            tag.setInteger("valveY", getValve().yCoord);
            tag.setInteger("valveZ", getValve().zCoord);
        }
        if(getBlock() != null) {
            tag.setInteger("blockId", Block.getIdFromBlock(getBlock().getBlock()));
            tag.setInteger("metadata", getBlock().getMetadata());
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

    public void markForUpdate() {
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Optional.Method(modid = "funkylocomotion")
    @Override
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }
}
