package com.lordmau5.ffs.tile;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.FrameBlockAccessWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.fml.common.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "framesapi.IMoveCheck", modid = "funkylocomotion")
})
public class TileEntityTankFrame extends ITankTile {

    private IBlockState camoBlockState;
    public IBakedModel fake_model;

    public boolean needsNewModel;

    public int lightValue;

    public void initialize(BlockPos valvePos, IBlockState camoBlockState) {
        setValvePos(valvePos);
        this.fake_model = null;
        this.camoBlockState = camoBlockState;
    }

    public void setBlockState(IBlockState blockState) {
        this.camoBlockState = blockState;
    }

    private IBlockState getBlockStateForNBT() {
        return camoBlockState;
    }

    public IBakedModel getFakeModel() {
        //if(fake_model == null) {
            IBakedModel fake_model = FancyFluidStorage.minecraft.getBlockRendererDispatcher().getModelFromBlockState(getBlockState(), new FrameBlockAccessWrapper(getWorld()), getPos());
            if(fake_model instanceof ISmartBlockModel) {
                fake_model = ((ISmartBlockModel) fake_model).handleBlockState(getExtendedBlockState());
            }
        //}
        return fake_model;
    }

    public IBlockState getBlockState() {
        IBlockState state = null;
        if(camoBlockState != null)
            state = camoBlockState.getBlock().getActualState(camoBlockState,  new FrameBlockAccessWrapper(getWorld()), getPos());

        return state;
    }

    public IBlockState getExtendedBlockState() {
        IBlockState state = getBlockState();

        if(camoBlockState != null && getWorld().isRemote)
            state = camoBlockState.getBlock().getExtendedState(state, new FrameBlockAccessWrapper(getWorld()), getPos());

        return state;
    }

    public boolean isFrameInvalid() {
        TileEntity tile = getWorld().getTileEntity(getPos());
        return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
    }

    public List<EnumFacing> getNeighborBlockOrAir(Block block) {
        List<EnumFacing> dirs = new ArrayList<>();

        if(getMasterValve() == null)
            return dirs;

        BlockPos pos = getPos();
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(block == Blocks.air) {
                if (getWorld().isAirBlock(pos.offset(dr)))
                    dirs.add(dr);
            }
            else {
                BlockPos oPos = getMasterValve().getPos().offset(dr);
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
        if(getWorld() != null && !getWorld().isRemote && getMasterValve() != null) {
            getMasterValve().breakTank(this);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("needsNewModel"))
            fake_model = null;

        if(tag.hasKey("blockName")) {
            setBlockState(Block.getBlockFromName(tag.getString("blockName")).getStateFromMeta(tag.getInteger("metadata")));
        }

        int newLightValue = (getMasterValve() != null ? getMasterValve().getFluidLuminosity() : 0);
        if(newLightValue != lightValue) {
            lightValue = newLightValue;
            markForUpdate();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        if(needsNewModel) {
            tag.setBoolean("needsNewModel", true);
            needsNewModel = false;
        }

        if(getBlockStateForNBT() != null) {
            tag.setString("blockName", getBlockStateForNBT().getBlock().getRegistryName());
            tag.setInteger("metadata", getBlockStateForNBT().getBlock().getMetaFromState(getBlockStateForNBT()));
        }

        super.writeToNBT(tag);
    }

    public int getLightValue() {
        if(getBlockStateForNBT() == null)
            return 0;

        Block block = getBlockStateForNBT().getBlock();
        if(block.isOpaqueCube())
            return 0;

        return lightValue;
    }
}
