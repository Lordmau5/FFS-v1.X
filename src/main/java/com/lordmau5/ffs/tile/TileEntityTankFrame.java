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
import net.minecraftforge.fml.common.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "framesapi.IMoveCheck", modid = "funkylocomotion")
})
public class TileEntityTankFrame extends TileEntity implements ITickable
        //IMoveCheck
{

    public BlockPos valvePos;
    private IBlockState blockState;
    private TileEntityValve masterValve;
    private boolean wantsUpdate = false;

    public TileEntityTankFrame() {
        super();
    }

    /*public TileEntityTankFrame(TileEntityValve masterValve, IBlockState blockState) {
        this.masterValve = masterValve;
        this.blockState = blockState;
    }*/

    public void initialize(BlockPos valvePos, IBlockState blockState) {
        this.valvePos = valvePos;
        this.blockState = blockState;
    }

    public void setBlockState(IBlockState blockState) {
        this.blockState = blockState;
    }

    public IBlockState getBlockState() {
        return blockState;
    }

    @Override
    public void update() {
        if(wantsUpdate && getWorld() != null) {
            worldObj.markBlockForUpdate(getPos());
            markDirty();
            wantsUpdate = false;
        }
    }

    public boolean isFrameInvalid() {
        TileEntity tile = worldObj.getTileEntity(getPos());
        return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
    }

    public List<EnumFacing> getNeighborBlockOrAir(Block block) {
        List<EnumFacing> dirs = new ArrayList<>();

        if(getValve() == null)
            return dirs;

        BlockPos pos = getPos();
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(block == Blocks.air) {
                if (worldObj.isAirBlock(pos.offset(dr)))
                    dirs.add(dr);
            }
            else {
                BlockPos oPos = getValve().getPos().offset(dr);
                IBlockState otherBlock = worldObj.getBlockState(oPos);
                if(block == otherBlock.getBlock() || worldObj.isAirBlock(oPos))
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
            if(block.isFlammable(worldObj, getPos(), dr)) {
                worldObj.setBlockState(getPos(), Blocks.fire.getDefaultState());
                return true;
            }
        }
        return false;
    }

    public void breakFrame() {
        if(isFrameInvalid()) {
            worldObj.setBlockToAir(getPos());
            return;
        }

        worldObj.removeTileEntity(getPos());
        if(getBlockState() != null)
            worldObj.setBlockState(getPos(), getBlockState());
        else
            worldObj.setBlockToAir(getPos());
    }

    public void onBreak() {
        if(getValve() != null && !worldObj.isRemote) {
            getValve().breakTank(this);
        }
    }

    public void setValvePos(BlockPos valvePos) {
        this.valvePos = valvePos;
        this.masterValve = null;
    }

    public TileEntityValve getValve() {
        if(masterValve == null && this.valvePos != null) {
            TileEntity tile = worldObj.getTileEntity(this.valvePos);
            masterValve = tile instanceof TileEntityValve ? (TileEntityValve) tile : null;
        }

        return masterValve;
    }

    @Override
    public void readFromNBT(NBTTagCompound _tag) {
        super.readFromNBT(_tag);

        NBTTagCompound tag = getTileData();
        if(tag.hasKey("valveX")) {
            setValvePos(new BlockPos(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ")));
        }
//        System.out.println("Reading nbt");
        if(tag.hasKey("blockName")) {
            setBlockState(Block.getBlockFromName(tag.getString("blockName")).getStateFromMeta(tag.getInteger("metadata")));
//            System.out.println("Read: " + tag.getString("blockName") + " : " + tag.getInteger("metadata"));
        }

        markForUpdate();
    }

    @Override
    public void writeToNBT(NBTTagCompound _tag) {
        NBTTagCompound tag = getTileData();

        if(getValve() != null) {
            BlockPos pos = getValve().getPos();
            tag.setInteger("valveX", pos.getX());
            tag.setInteger("valveY", pos.getY());
            tag.setInteger("valveZ", pos.getZ());
        }
        if(getBlockState() != null) {
            tag.setString("blockName", getBlockState().getBlock().getRegistryName());
            tag.setInteger("metadata", getBlockState().getBlock().getMetaFromState(getBlockState()));

//            System.out.println("Write: " + tag.getString("blockName") + " : " + tag.getInteger("metadata"));
        }

        super.writeToNBT(_tag);
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
        if(getWorld() != null && getPos() != null) {
            getWorld().markBlockForUpdate(getPos());
            markDirty();
        }
        else
            wantsUpdate = true;
    }

    /*
    @Optional.Method(modid = "funkylocomotion")
    @Override
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }
    */
}
