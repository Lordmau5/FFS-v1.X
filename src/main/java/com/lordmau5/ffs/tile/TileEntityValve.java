package com.lordmau5.ffs.tile;

import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.Position3D;
import cpw.mods.fml.common.Optional;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import framesapi.IMoveCheck;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "buildcraft.api.transport.IPipeConnection", modid = "BuildCraftAPI|Transport"),

        @Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"),

        @Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers"),
        @Optional.Interface(iface = "li.cil.oc.api.network.ManagedPeripheral", modid = "OpenComputers"),
        @Optional.Interface(iface = "framesapi.IMoveCheck", modid = "funkylocomotion")
})
public class TileEntityValve extends TileEntity implements IFluidTank, IFluidHandler,
        IPipeConnection, // BuildCraft
        IPeripheral, // ComputerCraft
        SimpleComponent, ManagedPeripheral, // OpenComputers
        IMoveCheck // Funky Locomotion
{

    private final int maxSize = FancyFluidStorage.instance.MAX_SIZE;
    protected int mbPerVirtualTank = FancyFluidStorage.instance.MB_PER_TANK_BLOCK;

    public boolean isValid;
    private boolean isMaster;
    private boolean initiated;

    public int tankHeight = 0;
    public int valveHeightPosition = 0;
    private boolean autoOutput;

    private ForgeDirection inside = ForgeDirection.UNKNOWN;

    private TileEntityValve master;
    public List<TileEntityTankFrame> tankFrames;
    public List<TileEntityValve> otherValves;

    private Map<Position3D, ExtendedBlock>[] maps;

    /**
     * Length of the inside
     *
     * 0 = Down
     * 1 = Up
     * 2 = North
     * 3 = South
     * 4 = West
     * 5 = East
     */
    private int[] length = new int[6];
    public Position3D bottomDiagFrame, topDiagFrame;
    private int initialWaitTick = 20;

    // TANK LOGIC
    private FluidStack fluidStack;
    private int fluidCapacity = 0;
    private int lastComparatorOut = 0;
    // ---------------

    public TileEntityValve() {
        tankFrames = new ArrayList<>();
        otherValves = new ArrayList<>();
    }

    @Override
    public void validate() {
        super.validate();
        initiated = true;
        initialWaitTick = 20;
    }

    @Override
    public void updateEntity() {
        if(worldObj.isRemote)
            return;

        if(initiated) {
            if (isMaster()) {
                if(bottomDiagFrame != null && topDiagFrame != null) { // Potential fix for huge-ass tanks not loading properly on master-valve chunk load.
                    Chunk chunkBottom = worldObj.getChunkFromBlockCoords(bottomDiagFrame.getX(), bottomDiagFrame.getZ());
                    Chunk chunkTop = worldObj.getChunkFromBlockCoords(topDiagFrame.getX(), topDiagFrame.getZ());

                    Position3D pos_chunkBottom = new Position3D(chunkBottom.xPosition, 0, chunkBottom.zPosition);
                    Position3D pos_chunkTop = new Position3D(chunkTop.xPosition, 0, chunkTop.zPosition);

                    Position3D diff = pos_chunkTop.getDistance(pos_chunkBottom);
                    for(int x = 0; x <= diff.getX(); x++) {
                        for(int z = 0; z <= diff.getZ(); z++) {
                            worldObj.getChunkProvider().loadChunk(pos_chunkTop.getX() + x, pos_chunkTop.getZ() + z);
                        }
                    }

                    updateBlockAndNeighbors();
                }
                if (initialWaitTick-- == 0) {
                    initiated = false;
                    buildTank(inside);
                    return;
                }
            }
        }

        if(!isMaster() && master == null) {
            isValid = false;
            updateBlockAndNeighbors();
            return;
        }

        if(!isValid())
            return;

        if(getFluid() == null)
            return;

        if(getAutoOutput()) { // Auto outputs at 50mB/t (1B/s) if enabled
            if (getFluidAmount() != 0) {
                float height = (float) getFluidAmount() / (float) getCapacity() * (float) getTankHeight();
                if (height > (valveHeightPosition - 0.5f)) { // Valves can output until the liquid is at their halfway point.
                    ForgeDirection out = inside.getOpposite();
                    TileEntity tile = worldObj.getTileEntity(xCoord + out.offsetX, yCoord + out.offsetY, zCoord + out.offsetZ);
                    if(tile != null) {
                        int maxAmount = 0;
                        if(tile instanceof TileEntityValve)
                            maxAmount = 1000; // When two tanks are connected by valves, allow faster output
                        else if(tile instanceof IFluidHandler)
                            maxAmount = 50;

                        if(maxAmount != 0) {
                            IFluidHandler handler = (IFluidHandler) tile;
                            FluidStack fillStack = getFluid().copy();
                            fillStack.amount = Math.min(getFluidAmount(), maxAmount);
                            if (handler.fill(inside, fillStack, false) > 0) {
                                drain(handler.fill(inside, fillStack, true), true);
                            }
                        }
                    }
                }
            }
        }

        if(getFluid() != null && getFluid().getFluid() == FluidRegistry.WATER) {
            if(worldObj.isRaining()) {
                int rate = (int) Math.floor(worldObj.rainingStrength * 5 * worldObj.getBiomeGenForCoords(xCoord, zCoord).rainfall);
                if (yCoord == worldObj.getPrecipitationHeight(xCoord, zCoord) - 1) {
                    FluidStack waterStack = getFluid().copy();
                    waterStack.amount = rate * 10;
                    fill(waterStack, true);
                }
            }
        }
    }

    public int getTankHeight() {
        return isMaster() ? tankHeight : getMaster().tankHeight;
    }

    private void setInside(ForgeDirection inside) {
        this.inside = inside;
    }

    public ForgeDirection getInside() {
        return this.inside;
    }

    public void buildTank(ForgeDirection inside) {
        if (worldObj.isRemote)
            return;

        isValid = false;

        fluidCapacity = 0;
        tankFrames.clear();
        otherValves.clear();

        if(this.inside == ForgeDirection.UNKNOWN)
            setInside(inside);

        if(!calculateInside())
            return;

        if(!setupTank())
            return;

        initiated = false;
        updateBlockAndNeighbors();
    }

    private boolean calculateInside() {
        int xIn = xCoord + inside.offsetX;
        int yIn = yCoord + inside.offsetY;
        int zIn = zCoord + inside.offsetZ;

        for(ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            for(int i=0; i<maxSize; i++) {
                if (!worldObj.isAirBlock(xIn + dr.offsetX * i, yIn + dr.offsetY * i, zIn + dr.offsetZ * i)) {
                    length[dr.ordinal()] = i - 1;
                    break;
                }
            }
        }

        for(int i=0; i<6; i += 2) {
            if(length[i] + length[i + 1] > maxSize)
                return false;
        }
        return length[0] != -1;
    }

    private void setSlaveValveInside(Map<Position3D, ExtendedBlock> airBlocks, TileEntityValve slave) {
        List<Position3D> possibleAirBlocks = new ArrayList<>();
        for(ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            if(worldObj.isAirBlock(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ))
                possibleAirBlocks.add(new Position3D(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ));
        }

        Position3D insideAir = null;
        for(Position3D pos : possibleAirBlocks) {
            if (airBlocks.containsKey(pos)) {
                insideAir = pos;
                break;
            }
        }

        if(insideAir == null)
            return;

        Position3D dist = insideAir.getDistance(new Position3D(slave.xCoord, slave.yCoord, slave.zCoord));
        for(ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            if(dist.equals(new Position3D(dr.offsetX, dr.offsetY, dr.offsetZ))) {
                slave.setInside(dr);
                break;
            }
        }
    }

    private void fetchMaps() {
        bottomDiagFrame = new Position3D(xCoord + inside.offsetX + length[ForgeDirection.WEST.ordinal()] * ForgeDirection.WEST.offsetX + ForgeDirection.WEST.offsetX,
                yCoord + inside.offsetY + length[ForgeDirection.DOWN.ordinal()] * ForgeDirection.DOWN.offsetY + ForgeDirection.DOWN.offsetY,
                zCoord + inside.offsetZ + length[ForgeDirection.NORTH.ordinal()] * ForgeDirection.NORTH.offsetZ + ForgeDirection.NORTH.offsetZ);
        topDiagFrame = new Position3D(xCoord + inside.offsetX + length[ForgeDirection.EAST.ordinal()] * ForgeDirection.EAST.offsetX + ForgeDirection.EAST.offsetX,
                yCoord + inside.offsetY + length[ForgeDirection.UP.ordinal()] * ForgeDirection.UP.offsetY + ForgeDirection.UP.offsetY,
                zCoord + inside.offsetZ + length[ForgeDirection.SOUTH.ordinal()] * ForgeDirection.SOUTH.offsetZ + ForgeDirection.SOUTH.offsetZ);

        maps = GenericUtil.getTankFrame(worldObj, bottomDiagFrame, topDiagFrame);
    }

    private boolean setupTank() {
        fetchMaps();

        otherValves = new ArrayList<>();
        tankFrames = new ArrayList<>();

        Position3D pos = new Position3D(xCoord, yCoord, zCoord);
        valveHeightPosition = Math.abs(bottomDiagFrame.getDistance(pos).getY());
        tankHeight = topDiagFrame.getDistance(bottomDiagFrame).getY() - 1;

        ExtendedBlock bottomDiagBlock = new ExtendedBlock(worldObj.getBlock(bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ()),
                worldObj.getBlockMetadata(bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ()));
        ExtendedBlock topDiagBlock = new ExtendedBlock(worldObj.getBlock(topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ()),
                worldObj.getBlockMetadata(topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ()));

        if (!bottomDiagBlock.equals(topDiagBlock) && (!FancyFluidStorage.instance.ALLOW_DIFFERENT_METADATA || !bottomDiagBlock.equalsIgnoreMetadata(topDiagBlock)) && !GenericUtil.isValidTankBlock(worldObj, bottomDiagFrame, bottomDiagBlock))
            return false;

        for (Map.Entry<Position3D, ExtendedBlock> airCheck : maps[2].entrySet()) {
            if (!worldObj.isAirBlock(airCheck.getKey().getX(), airCheck.getKey().getY(), airCheck.getKey().getZ())) {
                if (airCheck.getValue().getBlock().getUnlocalizedName().equals("railcraft.residual.heat"))
                    continue; // Just to be /sure/ that railcraft isn't messing with us

                return false;
            }
        }

        if (FancyFluidStorage.instance.INSIDE_CAPACITY) {
            fluidCapacity = (maps[2].size()) * mbPerVirtualTank;
        } else {
            fluidCapacity = (maps[0].size() + maps[1].size() + maps[2].size()) * mbPerVirtualTank;
        }

        for (Map.Entry<Position3D, ExtendedBlock> frameCheck : maps[0].entrySet()) {
            if (!frameCheck.getValue().equals(bottomDiagBlock) && (!FancyFluidStorage.instance.ALLOW_DIFFERENT_METADATA || !frameCheck.getValue().equalsIgnoreMetadata(bottomDiagBlock)))
                return false;
        }

        List<TileEntityValve> valves = new ArrayList<>();
        for (Map.Entry<Position3D, ExtendedBlock> insideFrameCheck : maps[1].entrySet()) {
            pos = insideFrameCheck.getKey();
            ExtendedBlock check = insideFrameCheck.getValue();
            TileEntity tile = worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (tile != null) {
                if (tile instanceof TileEntityValve) {
                    TileEntityValve valve = (TileEntityValve) tile;
                    if (valve == this)
                        continue;

                    if (valve.fluidStack != null) {
                        this.fluidStack = valve.fluidStack;
                    }
                    valves.add(valve);
                    continue;
                }
                else if (tile instanceof TileEntityTankFrame) {
                    continue;
                }
                return false;
            }

            if (check.equals(bottomDiagBlock) || FancyFluidStorage.instance.ALLOW_DIFFERENT_METADATA && check.equalsIgnoreMetadata(bottomDiagBlock) || GenericUtil.isBlockGlass(check.getBlock(), check.getMetadata()))
                continue;

            return false;
        }

        // Make sure we don't overfill a tank. If the new tank is smaller than the old one, excess liquid disappear.
        if (this.fluidStack != null)
            this.fluidStack.amount = Math.min(this.fluidStack.amount, this.fluidCapacity);

        for (TileEntityValve valve : valves) {
            pos = new Position3D(valve.xCoord, valve.yCoord, valve.zCoord);
            valve.valveHeightPosition = Math.abs(bottomDiagFrame.getDistance(pos).getY());

            valve.isMaster = false;
            valve.setMaster(this);
            setSlaveValveInside(maps[2], valve);
        }
        isMaster = true;

        for (Map.Entry<Position3D, ExtendedBlock> setTiles : maps[0].entrySet()) {
            pos = setTiles.getKey();
            TileEntityTankFrame tankFrame;
            if (setTiles.getValue().getBlock() != FancyFluidStorage.blockTankFrame) {
                tankFrame = new TileEntityTankFrame(this, setTiles.getValue());
                worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, setTiles.getValue().getMetadata(), 2);
                worldObj.setTileEntity(pos.getX(), pos.getY(), pos.getZ(), tankFrame);
                tankFrame.markForUpdate();
            } else {
                tankFrame = (TileEntityTankFrame) worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                tankFrame.setValve(this);
            }
            tankFrames.add(tankFrame);
        }

        for (Map.Entry<Position3D, ExtendedBlock> setTiles : maps[1].entrySet()) {
            pos = setTiles.getKey();
            TileEntity tile = worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (tile != null) {
                if (tile instanceof TileEntityValve && tile != this)
                    otherValves.add((TileEntityValve) tile);

                if (tile instanceof TileEntityTankFrame) {
                    ((TileEntityTankFrame) tile).setValve(this);
                    tankFrames.add((TileEntityTankFrame) tile);
                }
            } else {
                TileEntityTankFrame tankFrame = new TileEntityTankFrame(this, setTiles.getValue());
                worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, setTiles.getValue().getMetadata(), 2);
                worldObj.setTileEntity(pos.getX(), pos.getY(), pos.getZ(), tankFrame);
                tankFrame.markForUpdate();
                tankFrames.add(tankFrame);
            }
        }

        isValid = true;
        return true;
    }

    public void breakTank(TileEntity frame) {
        if (worldObj.isRemote)
            return;

        if(!isMaster()) {
            if(getMaster() != this)
                getMaster().breakTank(frame);

            return;
        }

        for(TileEntityValve valve : otherValves) {
            valve.fluidStack = getFluid();
            valve.master = null;
            valve.isValid = false;
            valve.autoOutput = autoOutput;
            valve.updateBlockAndNeighbors();
        }

        for(TileEntityTankFrame tankFrame : tankFrames) {
            if(frame == tankFrame)
                continue;

            tankFrame.breakFrame();
        }
        tankFrames = new ArrayList<>();
        otherValves = new ArrayList<>();

        isValid = false;

        this.updateBlockAndNeighbors();
    }

    public boolean isValid() {
        return isValid;
    }

    private void updateBlockAndNeighbors() {
        updateBlockAndNeighbors(false);
    }

    private void updateBlockAndNeighbors(boolean onlyThis) {
        if(worldObj.isRemote)
            return;

        this.markForUpdate(onlyThis);

        if(otherValves != null) {
            for(TileEntityValve otherValve : otherValves) {
                otherValve.isValid = isValid;
                otherValve.markForUpdate(true);
            }
        }

        ForgeDirection outside = getInside().getOpposite();
        TileEntity outsideTile = worldObj.getTileEntity(xCoord + outside.offsetX, yCoord + outside.offsetY, zCoord + outside.offsetZ);
        if (outsideTile != null) {
            //BC Check
            if(FancyFluidStorage.proxy.BUILDCRAFT_LOADED) {
                if(outsideTile instanceof IPipeTile)
                    ((IPipeTile) outsideTile).scheduleNeighborChange();
            }
        }
        // notify change for comparators
        worldObj.notifyBlockChange(xCoord, yCoord, zCoord, FancyFluidStorage.blockValve);
        worldObj.markBlockForUpdate(xCoord + outside.offsetX, yCoord + outside.offsetY, zCoord + outside.offsetZ);
    }

    public boolean isMaster() {
        return isMaster;
    }

    public TileEntityValve getMaster() {
        return master == null ? this : master;
    }

    public void setMaster(TileEntityValve master) {
        this.master = master;
    }

    public boolean getAutoOutput() {
        return isValid() && (isMaster() ? this.autoOutput : getMaster().getAutoOutput());

    }

    public void setAutoOutput(boolean autoOutput) {
        if(!isMaster()) {
            getMaster().setAutoOutput(autoOutput);
            return;
        }

        this.autoOutput = autoOutput;
        updateBlockAndNeighbors(true);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        isValid = tag.getBoolean("isValid");
        inside = ForgeDirection.getOrientation(tag.getInteger("inside"));

        isMaster = tag.getBoolean("master");
        if(isMaster()) {
            if(tag.getBoolean("hasFluid")) {
                fluidStack = new FluidStack(FluidRegistry.getFluid(tag.getInteger("fluidID")), tag.getInteger("fluidAmount"));
            }
            else {
                fluidStack = null;
            }

            autoOutput = tag.getBoolean("autoOutput");
            tankHeight = tag.getInteger("tankHeight");
            fluidCapacity = tag.getInteger("fluidCapacity");
        }
        else {
            if(master == null && tag.hasKey("masterValve")) {
                int[] masterPos = tag.getIntArray("masterValve");
                TileEntity tile = worldObj.getTileEntity(masterPos[0], masterPos[1], masterPos[2]);
                if(tile != null && tile instanceof TileEntityValve)
                    master = (TileEntityValve) tile;
            }
        }

        if(tag.hasKey("bottomDiagF")) {
            int[] bottomDiagF = tag.getIntArray("bottomDiagF");
            int[] topDiagF = tag.getIntArray("topDiagF");
            bottomDiagFrame = new Position3D(bottomDiagF[0], bottomDiagF[1], bottomDiagF[2]);
            topDiagFrame = new Position3D(topDiagF[0], topDiagF[1], topDiagF[2]);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("isValid", isValid);
        tag.setInteger("inside", inside.ordinal());

        tag.setBoolean("master", isMaster());
        if(isMaster()) {
            tag.setBoolean("hasFluid", fluidStack != null);
            if(fluidStack != null) {
                tag.setInteger("fluidID", fluidStack.getFluidID());
                tag.setInteger("fluidAmount", fluidStack.amount);
            }

            tag.setBoolean("autoOutput", autoOutput);
            tag.setInteger("tankHeight", tankHeight);
            tag.setInteger("fluidCapacity", fluidCapacity);
        }
        else {
            if(master != null) {
                int[] masterPos = new int[]{master.xCoord, master.yCoord, master.zCoord};
                tag.setIntArray("masterValve", masterPos);
            }
        }

        if(bottomDiagFrame != null && topDiagFrame != null) {
            tag.setIntArray("bottomDiagF", new int[]{bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ()});
            tag.setIntArray("topDiagF", new int[]{topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ()});
        }

        super.writeToNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());

        if ((!isMaster() || master == null) && pkt.func_148857_g().hasKey("masterValve")) {
            int[] masterCoords = pkt.func_148857_g().getIntArray("masterValve");
            TileEntity tile = worldObj.getTileEntity(masterCoords[0], masterCoords[1], masterCoords[2]);
            if(tile != null && tile instanceof TileEntityValve) {
                master = (TileEntityValve) tile;
            }
        }

         markForUpdate(true);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);

        if (!isMaster() && master != null) {
            tag.setIntArray("masterValve", new int[]{master.xCoord, master.yCoord, master.zCoord});
        }

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    private void markForUpdate(boolean onlyThis) {
        if(!onlyThis || this.lastComparatorOut != getComparatorOutput()) {
            this.lastComparatorOut = getComparatorOutput();
            for (TileEntityValve valve : otherValves) {
                valve.updateBlockAndNeighbors();
            }
        }
        if (!onlyThis) {
            for (TileEntityTankFrame frame : tankFrames)
                worldObj.markBlockForUpdate(frame.xCoord, frame.yCoord, frame.zCoord);
        }
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(bottomDiagFrame == null || topDiagFrame == null)
            return super.getRenderBoundingBox();

        return AxisAlignedBB.getBoundingBox(bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ(), topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ());
    }

    // Tank logic!

    @Override
    public FluidStack getFluid() {
        if(!isValid())
            return null;

        return getMaster() == this ? fluidStack : getMaster().fluidStack;
    }

    @Override
    public int getFluidAmount() {
        if(!isValid() || getFluid() == null)
            return 0;

        return getFluid().amount;
    }

    @Override
    public int getCapacity() {
        if(!isValid())
            return 0;
            
        return getMaster() == this ? fluidCapacity : getMaster().fluidCapacity;
    }

    @Override
    public FluidTankInfo getInfo() {
        if(!isValid())
            return null;

        return new FluidTankInfo(getMaster());
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if(getMaster() == this) {
            if(!isValid() || fluidStack != null && !fluidStack.isFluidEqual(resource))
                return 0;

            int possibleAmount = resource.amount;
            if(fluidStack != null)
                possibleAmount += getFluid().amount;

            int rest = resource.amount;
            if(possibleAmount > fluidCapacity) {
                rest = possibleAmount - fluidCapacity;
                possibleAmount = fluidCapacity;
            }

            if(doFill) {
                if (fluidStack == null)
                    fluidStack = resource;
                fluidStack.amount = possibleAmount;

                getMaster().markForUpdate(true);
            }

            return rest;
        }
        else
            return getMaster().fill(resource, doFill);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if(getMaster() == this) {
            if(!isValid() || fluidStack == null)
                return null;

            int possibleAmount = fluidStack.amount - maxDrain;

            int drained = maxDrain;
            if(possibleAmount < 0) {
                drained += possibleAmount;
                possibleAmount = 0;
            }

            FluidStack returnStack = new FluidStack(fluidStack, drained);

            if(doDrain) {
                fluidStack.amount = possibleAmount;
                if (possibleAmount == 0)
                    fluidStack = null;

                getMaster().markForUpdate(true);
            }

            return returnStack;
        }
        else
            return getMaster().drain(maxDrain, doDrain);
    }

    // IFluidHandler

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return getMaster() == this ? fill(resource, doFill) : getMaster().fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return getMaster() == this ? drain(resource.amount, doDrain) : getMaster().drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return getMaster() == this ? drain(maxDrain, doDrain) : getMaster().drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return isValid() && ((getFluid() != null && getFluid().getFluid() == fluid && getFluid().amount < getCapacity()) || getFluid() == null);

    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return isValid() && getFluid() != null && getFluid().getFluid() == fluid && getFluid().amount > 0;

    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        if(!isValid())
            return null;

        return getMaster() == this ? new FluidTankInfo[]{ getInfo() } : getMaster().getTankInfo(from);
    }

    @Optional.Method(modid = "BuildCraftAPI|Transport")
    @Override
    public ConnectOverride overridePipeConnection(IPipeTile.PipeType pipeType, ForgeDirection from) {
        if(!isValid())
            return ConnectOverride.DISCONNECT;

        return ConnectOverride.CONNECT;
    }

    public String[] methodNames() {
        return new String[]{"getFluidName", "getFluidAmount", "getFluidCapacity", "setAutoOutput", "doesAutoOutput"};
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public String getType() {
        return "ffs_valve";
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public String[] getMethodNames() {
        return methodNames();
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
        switch(method) {
            case 0: { // getFluidName
                if(this.getFluid() == null)
                    return null;
                return new Object[]{this.getFluid().getLocalizedName()};
            }
            case 1: { // getFluidAmount
                return new Object[]{this.getFluidAmount()};
            }
            case 2: { // getFluidCapacity
                return new Object[]{this.getCapacity()};
            }
            case 3: { // setAutoOutput
                if(arguments.length == 0) {
                    arguments = new Object[]{!this.getAutoOutput()};
                }
                if(!(arguments[0] instanceof Boolean)) {
                    throw new LuaException("expected argument 1 to be of type \"boolean\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                }
                this.setAutoOutput((boolean) arguments[0]);
                return new Object[]{this.getAutoOutput()};
            }
            case 4: { // doesAutoOutput
                return new Object[]{this.getAutoOutput()};
            }
            default:
        }
        return null;
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public void attach(IComputerAccess computer) {

    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public void detach(IComputerAccess computer) {

    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public boolean equals(IPeripheral other) {
        return false;
    }

    @Optional.Method(modid = "OpenComputers")
    @Override
    public String getComponentName() {
        return "ffs_valve";
    }

    @Optional.Method(modid = "OpenComputers")
    @Override
    public String[] methods() {
        return methodNames();
    }

    @Optional.Method(modid = "OpenComputers")
    @Override
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        switch(method) {
            case "getFluidName": { // getFluidName
                if(this.getFluid() == null)
                    return null;
                return new Object[]{this.getFluid().getLocalizedName()};
            }
            case "getFluidAmount": { // getFluidAmount
                return new Object[]{this.getFluidAmount()};
            }
            case "getFluidCapacity": { // getCapacity
                return new Object[]{this.getCapacity()};
            }
            case "setAutoOutput": { // setAutoOutput
                this.setAutoOutput(args.optBoolean(0, !this.getAutoOutput()));
                return new Object[]{this.getAutoOutput()};
            }
            case "doesAutoOutput": { // doesAutoOutput
                return new Object[]{this.getAutoOutput()};
            }
            default:
        }
        return null;
    }

    @Optional.Method(modid = "funkylocomotion")
    @Override
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }

    public int getComparatorOutput() {
        return MathHelper.floor_float(((float) this.getFluidAmount() / this.getCapacity()) * 14.0F);
    }
}
