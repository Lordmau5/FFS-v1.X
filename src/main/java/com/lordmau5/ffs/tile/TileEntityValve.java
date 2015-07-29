package com.lordmau5.ffs.tile;

import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
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
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
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

import java.util.*;

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
    protected int minBurnableTemp = FancyFluidStorage.instance.MIN_BURNABLE_TEMPERATURE;

    private int frameBurnability = 0;

    private String valveName = "";
    public boolean isValid;
    private boolean isMaster;
    private int[] masterValvePos;
    public boolean initiated;

    private int updateTicks;
    private boolean needsUpdate;

    public int tankHeight = 0;
    public int valveHeightPosition = 0;
    private boolean autoOutput;

    private int randomBurnTicks = 20 * 5; // Every 5 seconds
    private int randomLeakTicks = 20 * 60; // Every minute

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
    public int initialWaitTick = 20;

    // TANK LOGIC
    private FluidStack fluidStack;
    private int fluidTemperature = 0;
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
                if (initialWaitTick-- <= 0) {
                    initiated = false;
                    buildTank(inside);
                    return;
                }
            }
        }

        if(!isMaster() && master == null) {
            if(masterValvePos != null) {
                TileEntity tile = worldObj.getTileEntity(masterValvePos[0], masterValvePos[1], masterValvePos[2]);
                if(tile != null && tile instanceof TileEntityValve)
                    master = (TileEntityValve) tile;
            }
            else {
                isValid = false;
                updateBlockAndNeighbors();
                return;
            }
        }

        if(!isValid())
            return;

        if(updateTicks-- == 0) {
            updateTicks = 20;
            if(needsUpdate) {
                getMaster().markForUpdate(false);
                needsUpdate = false;
            }
        }

        if(getFluid() == null)
            return;

        if(getAutoOutput()) { // Auto outputs at 50mB/t (1B/s) if enabled
            if (getFluidAmount() != 0) {
                float height = (float) getFluidAmount() / (float) getCapacity();
                boolean isNegativeDensity = getFluid().getFluid().getDensity(getFluid()) < 0 ;
                if (GenericUtil.canAutoOutput(height, getTankHeight(), valveHeightPosition, isNegativeDensity)) { // Valves can output until the liquid is at their halfway point.
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

        if(isMaster()) {
            if(minBurnableTemp > 0 && fluidTemperature >= minBurnableTemp && frameBurnability > 0) {
                if(randomBurnTicks-- <= 0) {
                    randomBurnTicks = 20 * 5;
                    Random random = new Random();

                    int temperatureDiff = fluidTemperature - minBurnableTemp;
                    int chanceOfBurnability = 300 - frameBurnability;
                    int rand = random.nextInt(300) + temperatureDiff + ((int) Math.floor((float) getFluidAmount() / (float) getCapacity() * 300));
                    if(rand >= chanceOfBurnability) {
                        boolean successfullyBurned = false;

                        List<TileEntityTankFrame> remainingFrames = new ArrayList<>();
                        remainingFrames.addAll(tankFrames);

                        List<TileEntityTankFrame> removingFrames = new ArrayList<>();
                        while(!successfullyBurned) { // Try to burn at least one
                            if(remainingFrames.size() == 0)
                                break;

                            boolean couldBurnOne = false;
                            for(int i=0; i<Math.min(10, remainingFrames.size()); i++) {
                                int id = random.nextInt(remainingFrames.size());
                                TileEntityTankFrame frame = remainingFrames.get(id);
                                couldBurnOne = frame.tryBurning();
                                if(!couldBurnOne)
                                    removingFrames.add(frame);
                            }
                            remainingFrames.removeAll(removingFrames);
                            removingFrames.clear();
                            if(couldBurnOne)
                                successfullyBurned = true;
                        }
                        if(!successfullyBurned) {
                            remainingFrames.clear();
                            remainingFrames.addAll(tankFrames);
                            List<Position3D> firePos = new ArrayList<>();
                            for(int i=0; i<3;) {
                                if(remainingFrames.size() == 0)
                                    break;

                                int id = random.nextInt(remainingFrames.size());
                                TileEntityTankFrame frame = remainingFrames.get(id);
                                if(frame.getBlock().getBlock().isFlammable(worldObj, frame.xCoord, frame.yCoord, frame.zCoord, ForgeDirection.UNKNOWN)) {
                                    firePos.add(new Position3D(frame.xCoord, frame.yCoord, frame.zCoord));
                                    i++;
                                }
                                else
                                    remainingFrames.remove(id);
                            }
                            for(Position3D pos : firePos) {
                                if(worldObj.getBlock(pos.getX(), pos.getY(), pos.getZ()).isFlammable(worldObj, pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN))
                                    worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), Blocks.fire);
                            }
                        }

                        frameBurnability = 0;

                        if(FancyFluidStorage.instance.SET_WORLD_ON_FIRE)
                            worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, FancyFluidStorage.modId + ":fire", 1.0F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
                    }
                }
            }

            if(FancyFluidStorage.instance.SHOULD_TANKS_LEAK) {
                if(randomLeakTicks-- <= 0 && fluidStack != null && fluidStack.getFluid().canBePlacedInWorld()) {
                    randomLeakTicks = 20 * 60;

                    Random random = new Random();
                    int amt = random.nextInt(3) + 1;

                    List<TileEntityTankFrame> validFrames = new ArrayList<>();

                    List<TileEntityTankFrame> remainingFrames = new ArrayList<>();
                    remainingFrames.addAll(tankFrames);

                    for (int i = 0; i < amt; ) {
                        if (remainingFrames.size() == 0)
                            break;

                        int id = random.nextInt(remainingFrames.size());
                        TileEntityTankFrame frame = remainingFrames.get(id);
                        Block block = frame.getBlock().getBlock();
                        if (GenericUtil.canBlockLeak(block) && !frame.getNeighborBlockOrAir(fluidStack.getFluid().getBlock()).isEmpty() && block.getBlockHardness(worldObj, frame.xCoord, frame.yCoord, frame.zCoord) <= 1.0F) {
                            validFrames.add(frame);
                            i++;
                        } else
                            remainingFrames.remove(id);
                    }

                    for (TileEntityTankFrame frame : validFrames) {
                        Block block = frame.getBlock().getBlock();
                        int hardness = (int) Math.ceil(block.getBlockHardness(worldObj, frame.xCoord, frame.yCoord, frame.zCoord) * 100);
                        int rand = random.nextInt(hardness) + 1;
                        int diff = (int) Math.ceil(50 * ((float) getFluidAmount() / (float) getCapacity()));
                        if (rand >= hardness - diff) {
                            ForgeDirection leakDir;
                            List<ForgeDirection> dirs = frame.getNeighborBlockOrAir(fluidStack.getFluid().getBlock());
                            if (dirs.size() == 0)
                                continue;

                            if (dirs.size() > 1) {
                                leakDir = dirs.get(random.nextInt(dirs.size()));
                            } else
                                leakDir = dirs.get(0);

                            Position3D leakPos = new Position3D(frame.xCoord + leakDir.offsetX, frame.yCoord + leakDir.offsetY, frame.zCoord + leakDir.offsetZ);
                            if (maps[2].containsKey(leakPos))
                                continue;

                            if (fluidStack.amount >= FluidContainerRegistry.BUCKET_VOLUME) {
                                worldObj.setBlock(frame.xCoord + leakDir.offsetX, frame.yCoord + leakDir.offsetY, frame.zCoord + leakDir.offsetZ, fluidStack.getFluid().getBlock(), 0, 3);
                                worldObj.notifyBlockOfNeighborChange(frame.xCoord + leakDir.offsetX, frame.yCoord + leakDir.offsetY, frame.zCoord + leakDir.offsetZ, fluidStack.getFluid().getBlock());
                                drain(FluidContainerRegistry.BUCKET_VOLUME, true);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<TileEntityValve> getAllValves() {
        if(!isMaster())
            return getMaster().getAllValves();

        List<TileEntityValve> valves = new ArrayList<>();
        valves.add(this);

        if(otherValves.isEmpty())
            return valves;

        for(TileEntityValve valve : otherValves)
            valves.add(valve);

        return valves;
    }

    private List<TileEntityValve> getValvesByName(String name) {
        if(!isMaster())
            return getMaster().getValvesByName(name);

        List<TileEntityValve> valves = new ArrayList<>();
        if(getAllValves().isEmpty())
            return valves;

        for(TileEntityValve valve : getAllValves()) {
            if(valve.getValveName().toLowerCase().equals(name.toLowerCase()))
                valves.add(valve);
        }
        return valves;
    }

    public String getValveName() {
        if(this.valveName.isEmpty())
            setValveName(GenericUtil.getUniqueValveName(this));

        return this.valveName;
    }

    public void setValveName(String valveName) {
        this.valveName = valveName;
    }

    public void setNeedsUpdate() {
        needsUpdate = true;
    }

    public int getTankHeight() {
        return isMaster() ? tankHeight : getMaster().tankHeight;
    }

    public void setInside(ForgeDirection inside) {
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

    public boolean calculateInside() {
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

    public void updateCornerFrames() {
        bottomDiagFrame = new Position3D(xCoord + inside.offsetX + length[ForgeDirection.WEST.ordinal()] * ForgeDirection.WEST.offsetX + ForgeDirection.WEST.offsetX,
                yCoord + inside.offsetY + length[ForgeDirection.DOWN.ordinal()] * ForgeDirection.DOWN.offsetY + ForgeDirection.DOWN.offsetY,
                zCoord + inside.offsetZ + length[ForgeDirection.NORTH.ordinal()] * ForgeDirection.NORTH.offsetZ + ForgeDirection.NORTH.offsetZ);
        topDiagFrame = new Position3D(xCoord + inside.offsetX + length[ForgeDirection.EAST.ordinal()] * ForgeDirection.EAST.offsetX + ForgeDirection.EAST.offsetX,
                yCoord + inside.offsetY + length[ForgeDirection.UP.ordinal()] * ForgeDirection.UP.offsetY + ForgeDirection.UP.offsetY,
                zCoord + inside.offsetZ + length[ForgeDirection.SOUTH.ordinal()] * ForgeDirection.SOUTH.offsetZ + ForgeDirection.SOUTH.offsetZ);
    }

    private void fetchMaps() {
        maps = GenericUtil.getTankFrame(worldObj, bottomDiagFrame, topDiagFrame);
    }

    private boolean setupTank() {
        updateCornerFrames();
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

        frameBurnability = bottomDiagBlock.getBlock().getFlammability(worldObj, bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ(), ForgeDirection.UNKNOWN);

        if(bottomDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = worldObj.getTileEntity(bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ());
            if(tile != null && tile instanceof TileEntityTankFrame)
                bottomDiagBlock = ((TileEntityTankFrame) tile).getBlock();
        }

        if(topDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = worldObj.getTileEntity(topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ());
            if(tile != null && tile instanceof TileEntityTankFrame)
                topDiagBlock = ((TileEntityTankFrame) tile).getBlock();
        }

        if(!GenericUtil.isValidTankBlock(worldObj, bottomDiagFrame, bottomDiagBlock))
            return false;

        if (!GenericUtil.areTankBlocksValid(bottomDiagBlock, topDiagBlock, worldObj, bottomDiagFrame))
            return false;

        for (Map.Entry<Position3D, ExtendedBlock> airCheck : maps[2].entrySet()) {
            pos = airCheck.getKey();
            if (!worldObj.isAirBlock(pos.getX(), pos.getY(), pos.getZ())) {
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
            Position3D fPos = frameCheck.getKey();
            ExtendedBlock fBlock = frameCheck.getValue();
            int burnability = fBlock.getBlock().getFlammability(worldObj, fPos.getX(), fPos.getY(), fPos.getZ(), ForgeDirection.UNKNOWN);
            if(burnability > frameBurnability)
                frameBurnability = burnability;

            if(fBlock.getBlock() instanceof BlockTankFrame) {
                TileEntity tile = worldObj.getTileEntity(fPos.getX(), fPos.getY(), fPos.getZ());
                if(tile != null && tile instanceof TileEntityTankFrame)
                    fBlock = ((TileEntityTankFrame) tile).getBlock();
            }
            if (!GenericUtil.areTankBlocksValid(fBlock, bottomDiagBlock, worldObj, fPos))
                return false;
        }

        List<TileEntityValve> valves = new ArrayList<>();
        for (Map.Entry<Position3D, ExtendedBlock> insideFrameCheck : maps[1].entrySet()) {
            pos = insideFrameCheck.getKey();
            ExtendedBlock check = insideFrameCheck.getValue();
            int burnability = check.getBlock().getFlammability(worldObj, pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN);
            if(burnability > frameBurnability)
                frameBurnability = burnability;

            if (GenericUtil.areTankBlocksValid(check, bottomDiagBlock, worldObj, pos) || GenericUtil.isBlockGlass(check.getBlock(), check.getMetadata()))
                continue;

            TileEntity tile = worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (tile != null) {
                if (tile instanceof TileEntityValve) {
                    TileEntityValve valve = (TileEntityValve) tile;
                    if (valve == this)
                        continue;

                    if (valve.fluidStack != null) {
                        this.fluidStack = valve.fluidStack;
                        updateFluidTemperature();
                    }
                    valves.add(valve);
                    continue;
                }
                else if (tile instanceof TileEntityTankFrame) {
                    continue;
                }
                return false;
            }

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
                worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, setTiles.getValue().getMetadata(), 3);
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

                else if (tile instanceof TileEntityTankFrame) {
                    ((TileEntityTankFrame) tile).setValve(this);
                    tankFrames.add((TileEntityTankFrame) tile);
                }
                else if (GenericUtil.isTileEntityAcceptable(setTiles.getValue().getBlock(), tile)) {
                    TileEntityTankFrame tankFrame = new TileEntityTankFrame(this, setTiles.getValue());
                    worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, setTiles.getValue().getMetadata(), 2);
                    worldObj.setTileEntity(pos.getX(), pos.getY(), pos.getZ(), tankFrame);
                    tankFrame.markForUpdate();
                    tankFrames.add(tankFrame);
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
            valve.updateFluidTemperature();
            valve.master = null;
            valve.isValid = false;
            valve.updateBlockAndNeighbors();
        }

        for(TileEntityTankFrame tankFrame : tankFrames) {
            if(frame == tankFrame)
                continue;

            tankFrame.breakFrame();
        }
        tankFrames.clear();
        otherValves.clear();

        isValid = false;

        this.updateBlockAndNeighbors();
        //worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
    }

    public boolean isValid() {
        return isValid;
    }

    public void updateBlockAndNeighbors() {
        updateBlockAndNeighbors(false);
    }

    public void updateBlockAndNeighbors(boolean onlyThis) {
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
        if(isMaster())
            return this;

        return master == null ? this : master;
    }

    public void setMaster(TileEntityValve master) {
        this.master = master;
    }

    public boolean getAutoOutput() {
        return isValid() && this.autoOutput;
    }

    public void setAutoOutput(boolean autoOutput) {
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
                if(tag.hasKey("fluidID"))
                    fluidStack = new FluidStack(FluidRegistry.getFluid(tag.getInteger("fluidID")), tag.getInteger("fluidAmount"));
                else if(tag.hasKey("fluidName"))
                    fluidStack = new FluidStack(FluidRegistry.getFluid(tag.getString("fluidName")), tag.getInteger("fluidAmount"));
                updateFluidTemperature();
            }
            else {
                fluidStack = null;
            }

            tankHeight = tag.getInteger("tankHeight");
            fluidCapacity = tag.getInteger("fluidCapacity");
        }
        else {
            if(master == null && tag.hasKey("masterValve")) {
                masterValvePos = tag.getIntArray("masterValve");
            }
        }

        autoOutput = tag.getBoolean("autoOutput");
        if(tag.hasKey("valveName"))
            setValveName(tag.getString("valveName"));
        else
            setValveName(GenericUtil.getUniqueValveName(this));

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
                tag.setString("fluidName", FluidRegistry.getFluidName(fluidStack));
                tag.setInteger("fluidAmount", fluidStack.amount);
            }

            tag.setInteger("tankHeight", tankHeight);
            tag.setInteger("fluidCapacity", fluidCapacity);
        }
        else {
            if(master != null) {
                int[] masterPos = new int[]{master.xCoord, master.yCoord, master.zCoord};
                tag.setIntArray("masterValve", masterPos);
            }
        }

        tag.setBoolean("autoOutput", autoOutput);
        if(!getValveName().isEmpty())
            tag.setString("valveName", getValveName());

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
            for (TileEntityTankFrame frame : tankFrames) {
                frame.markForUpdate();
            }
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

    public int getFluidLuminosity() {
        FluidStack fstack = getFluid();
        if(fstack == null)
            return 0;

        Fluid fluid = fstack.getFluid();
        if(fluid == null)
            return 0;

        return fluid.getLuminosity(fstack);
    }

    public void updateFluidTemperature() {
        FluidStack fstack = fluidStack;
        if(fstack == null)
            return;

        Fluid fluid = fstack.getFluid();
        if(fluid == null)
            return;

        this.fluidTemperature = fluid.getTemperature(fstack);
    }

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

            if (!doFill)
            {
                if (fluidStack == null) {
                    return Math.min(fluidCapacity, resource.amount);
                }

                return Math.min(fluidCapacity - fluidStack.amount, resource.amount);
            }

            if (fluidStack == null)
            {
                fluidStack = new FluidStack(resource, Math.min(fluidCapacity, resource.amount));
                updateFluidTemperature();
                setNeedsUpdate();
                return fluidStack.amount;
            }

            int filled = fluidCapacity - fluidStack.amount;
            if (resource.amount < filled) {
                fluidStack.amount += resource.amount;
                filled = resource.amount;
            }
            else {
                fluidStack.amount = fluidCapacity;
            }

            getMaster().setNeedsUpdate();
            //getMaster().markForUpdate(true);

            return filled;
        }
        else
            return getMaster().fill(resource, doFill);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if(getMaster() == this) {
            if(!isValid() || fluidStack == null)
                return null;

            int drained = maxDrain;
            if (fluidStack.amount < drained) {
                drained = fluidStack.amount;
            }

            FluidStack stack = new FluidStack(fluidStack, drained);
            if (doDrain) {
                fluidStack.amount -= drained;
                if (fluidStack.amount <= 0) {
                    fluidStack = null;
                    updateFluidTemperature();
                }
                getMaster().setNeedsUpdate();
                //getMaster().markForUpdate(true);
            }
            return stack;
        }
        else
            return getMaster().drain(maxDrain, doDrain);
    }

    // IFluidHandler

    /**
     * @return 0-100 in % of how much is filled
     */
    public double getFillPercentage() {
        if(!isValid() || getFluid() == null)
            return 0;

        return Math.floor((double) getFluidAmount() / (double) getCapacity() * 100);
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if(!canFill(from, resource.getFluid()))
            return 0;

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
        if(!isValid())
            return false;

        if(getFluid() != null && getFluid().getFluid() != fluid)
            return false;

        if(getFluidAmount() >= getCapacity())
            return false;

        if(autoOutput) {
            return valveHeightPosition > getTankHeight() || valveHeightPosition + 0.5f >= getTankHeight() * getFillPercentage();
        }
        return true;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        if(!isValid())
            return false;

        if(getFluid() == null)
            return false;

        return getFluid().getFluid() == fluid && getFluidAmount() > 0;
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
                if(arguments.length == 1) {
                    if(!(arguments[0] instanceof Boolean)) {
                        throw new LuaException("expected argument 1 to be of type \"boolean\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    for(TileEntityValve valve : getAllValves())
                        valve.setAutoOutput((boolean) arguments[0]);

                    return new Object[]{(boolean) arguments[0]};
                }
                else if(arguments.length == 2) {
                    if(!(arguments[0] instanceof String)) {
                        throw new LuaException("expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    if(!(arguments[1] instanceof Boolean)) {
                        throw new LuaException("expected argument 2 to be of type \"boolean\", found \"" + arguments[1].getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityValve> valves = getValvesByName((String) arguments[0]);
                    if(valves.isEmpty()) {
                        throw new LuaException("no valves found");
                    }

                    List<String> valveNames = new ArrayList<>();
                    for(TileEntityValve valve : valves) {
                        valve.setAutoOutput((boolean) arguments[1]);
                        valveNames.add(valve.getValveName());
                    }
                    return new Object[]{valveNames};
                }
                else {
                    throw new LuaException("insufficient number of arguments found - expected 1 or 2, got " + arguments.length);
                }
            }
            case 4: { // doesAutoOutput
                if(arguments.length == 0) {
                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityValve valve : getAllValves()) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else if(arguments.length == 1) {
                    if(!(arguments[0] instanceof String)) {
                        throw new LuaException("expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityValve> valves = getValvesByName((String) arguments[0]);
                    if(valves.isEmpty()) {
                        throw new LuaException("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityValve valve : valves) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else {
                    throw new LuaException("insufficient number of arguments found - expected 1, got " + arguments.length);
                }
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
                if(args.count() == 1) {
                    if(!(args.isBoolean(0))) {
                        throw new Exception("expected argument 1 to be of type \"boolean\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    for(TileEntityValve valve : getAllValves())
                        valve.setAutoOutput(args.checkBoolean(0));

                    return new Object[]{args.checkBoolean(0)};
                }
                else if(args.count() == 2) {
                    if(!(args.isString(0))) {
                        throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    if(!(args.isBoolean(1))) {
                        throw new Exception("expected argument 2 to be of type \"boolean\", found \"" + args.checkAny(1).getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityValve> valves = getValvesByName(args.checkString(0));
                    if(valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    List<String> valveNames = new ArrayList<>();
                    for(TileEntityValve valve : valves) {
                        valve.setAutoOutput(args.checkBoolean(1));
                        valveNames.add(valve.getValveName());
                    }
                    return new Object[]{valveNames};
                }
                else {
                    throw new Exception("insufficient number of arguments found - expected 1 or 2, got " + args.count());
                }
            }
            case "doesAutoOutput": { // doesAutoOutput
                if(args.count() == 0) {
                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityValve valve : getAllValves()) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else if(args.count() == 1) {
                    if(!(args.isString(0))) {
                        throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
                    }

                    List<TileEntityValve> valves = getValvesByName(args.checkString(0));
                    if(valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();
                    for(TileEntityValve valve : valves) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[]{valveOutputs};
                }
                else {
                    throw new Exception("insufficient number of arguments found - expected 1, got " + args.count());
                }
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
