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
    private boolean hasValve = false;

    public TileEntityTankFrame() {
        super();
    }

    /*public TileEntityTankFrame(TileEntityValve masterValve, IBlockState blockState) {
        this.masterValve = masterValve;
        this.blockState = blockState;
    }*/

    public void initialize(TileEntityValve masterValve, IBlockState blockState) {
        this.masterValve = masterValve;
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
        //worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
        //IBlockState state = FancyFluidStorage.blockTankFrame.getExtendedState(worldObj.getBlockState(getPos()), worldObj, getPos());
        //System.out.println((worldObj.isRemote ? "Client" : "Server") + " - " + ((state != null) ? state.toString() : "null"));
        if(masterValve == null && hasValve) {
            TileEntity tile = worldObj.getTileEntity(valvePos);
            if(tile != null && tile instanceof TileEntityValve)
                setValve((TileEntityValve) tile);
        }
    }

    public boolean isFrameInvalid() {
        TileEntity tile = worldObj.getTileEntity(getPos());
        return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
    }

    public List<EnumFacing> getNeighborBlockOrAir(Block block) {
        List<EnumFacing> dirs = new ArrayList<>();
        BlockPos pos = getPos();
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(block == Blocks.air) {
                if (worldObj.isAirBlock(new BlockPos(pos.getX() + dr.getFrontOffsetX(), pos.getY() + dr.getFrontOffsetY(), pos.getZ() + dr.getFrontOffsetZ())))
                    dirs.add(dr);
            }
            else {
                BlockPos oPos = new BlockPos(valvePos.getX() + dr.getFrontOffsetX(), valvePos.getY() + dr.getFrontOffsetY(), valvePos.getZ() + dr.getFrontOffsetZ());
                IBlockState otherBlock = worldObj.getBlockState(new BlockPos(oPos));
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
            if(block.isFlammable(worldObj, pos, dr)) {
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
        if(masterValve != null && !worldObj.isRemote) {
            masterValve.breakTank(this);
        }
    }

    public void setValve(TileEntityValve valve) {
        this.masterValve = valve;
    }

    public TileEntityValve getValve() {
        if(this.masterValve == null && hasValve) {
            TileEntity tile = worldObj.getTileEntity(valvePos);
            if(tile != null && tile instanceof TileEntityValve)
                setValve((TileEntityValve) tile);
        }
        return this.masterValve;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("valveX")) {
            valvePos = new BlockPos(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ"));
            hasValve = true;
        }
        if(tag.hasKey("blockId")) {
            this.blockState = Block.getBlockById(tag.getInteger("blockId")).getStateFromMeta(tag.getInteger("metadata"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        if(getValve() != null) {
            BlockPos pos = getValve().getPos();
            tag.setInteger("valveX", pos.getX());
            tag.setInteger("valveY", pos.getY());
            tag.setInteger("valveZ", pos.getZ());
        }
        if(getBlockState() != null) {
            tag.setInteger("blockId", Block.getIdFromBlock(getBlockState().getBlock()));
            tag.setInteger("metadata", getBlockState().getBlock().getMetaFromState(getBlockState()));
        }
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
        markForUpdate();
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(pos, 0, tag);
    }

    public void markForUpdate() {
        worldObj.markBlockForUpdate(pos);
    }

    /*
    @Optional.Method(modid = "funkylocomotion")
    @Override
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }
    */
}
