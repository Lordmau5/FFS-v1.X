package com.lordmau5.ffs.tile;

import buildcraft.api.transport.IPipeTile;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.util.GenericUtil;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fml.common.Optional;

import java.util.*;

/**
 * Created by Dustin on 28.06.2015.
 */

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "buildcraft.api.transport.IPipeConnection", modid = "BuildCraftAPI|Transport"),

        @Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
})
public class TileEntityValve extends TileEntity implements IFluidTank, IFluidHandler, ITickable,
        IPeripheral
{

    private final int maxSize = FancyFluidStorage.instance.MAX_SIZE;
    protected int mbPerVirtualTank = FancyFluidStorage.instance.MB_PER_TANK_BLOCK;
    protected int minBurnableTemp = FancyFluidStorage.instance.MIN_BURNABLE_TEMPERATURE;

    private boolean wantsUpdate;
    private int oldLuminosity;
    private String valveName = "";
    private boolean isValid;
    private boolean isMaster;
    private BlockPos masterValvePos;
    public boolean initiated;

    private int frameBurnability = 0;

    public int tankHeight = 0;
    public int valveHeightPosition = 0;
    private boolean autoOutput;

    private EnumFacing inside = EnumFacing.UP;

    private int randomBurnTicks = 20 * 5; // Every 5 seconds
    private int randomLeakTicks = 20 * 60; // Every minute

    private TileEntityValve master;
    public List<TileEntityTankFrame> tankFrames;
    public List<TileEntityValve> otherValves;

    private Map<BlockPos, IBlockState>[] maps;

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
    public BlockPos bottomDiagFrame, topDiagFrame;
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
    public void onLoad() {
        super.onLoad();

        wantsUpdate = true;
    }

    @Override
    public void update() {
        if(getWorld().isRemote)
            return;

        if(wantsUpdate) {
            wantsUpdate = false;
            markForUpdate(true);
        }

        if(initiated) {
            if (isMaster()) {
                if (bottomDiagFrame != null && topDiagFrame != null) { // Potential fix for huge-ass tanks not loading properly on master-valve chunk load.
                    Chunk chunkBottom = getWorld().getChunkFromBlockCoords(bottomDiagFrame);
                    Chunk chunkTop = getWorld().getChunkFromBlockCoords(topDiagFrame);

                    BlockPos pos_chunkBottom = new BlockPos(chunkBottom.xPosition, 0, chunkBottom.zPosition);
                    BlockPos pos_chunkTop = new BlockPos(chunkTop.xPosition, 0, chunkTop.zPosition);

                    BlockPos diff = pos_chunkTop.subtract(pos_chunkBottom);
                    for (int x = 0; x <= diff.getX(); x++) {
                        for (int z = 0; z <= diff.getZ(); z++) {
                            ForgeChunkManager.forceChunk(ForgeChunkManager.requestTicket(FancyFluidStorage.instance, getWorld(), ForgeChunkManager.Type.NORMAL), new ChunkCoordIntPair(pos_chunkTop.getX() + x, pos_chunkTop.getZ() + z));
                        }
                    }

                    updateBlockAndNeighbors();
                }
                if (initialWaitTick-- <= 0) {
                    initiated = false;
                    buildTank(getInside());
                    return;
                }
            }
        }

        if(!isValid())
            return;

        if(!isMaster() && getMaster() == null) {
            setValid(false);
            updateBlockAndNeighbors();
        }

        if(getFluid() == null)
            return;

        if(getAutoOutput() || valveHeightPosition == 0) { // Auto outputs at 50mB/t (1B/s) if enabled
            if (getFluidAmount() != 0) {
                float height = (float) getFluidAmount() / (float) getCapacity();
                boolean isNegativeDensity = getFluid().getFluid().getDensity(getFluid()) < 0 ;
                if (GenericUtil.canAutoOutput(height, getTankHeight(), valveHeightPosition, isNegativeDensity)) { // Valves can output until the liquid is at their halfway point.
                    EnumFacing out = getInside().getOpposite();
                    TileEntity tile = getWorld().getTileEntity(new BlockPos(getPos().getX() + out.getFrontOffsetX(), getPos().getY() + out.getFrontOffsetY(), getPos().getZ() + out.getFrontOffsetZ()));
                    if(tile != null) {
                        if(!(tile instanceof TileEntityValve) && !getAutoOutput() && valveHeightPosition == 0) {}
                        else {
                            int maxAmount = 0;
                            if (tile instanceof TileEntityValve)
                                maxAmount = 1000; // When two tanks are connected by valves, allow faster output
                            else if (tile instanceof IFluidHandler)
                                maxAmount = 50;

                            if (maxAmount != 0) {
                                IFluidHandler handler = (IFluidHandler) tile;
                                FluidStack fillStack = getFluid().copy();
                                fillStack.amount = Math.min(getFluidAmount(), maxAmount);
                                if (handler.fill(getInside(), fillStack, false) > 0) {
                                    drain(handler.fill(getInside(), fillStack, true), true);
                                }
                            }
                        }
                    }
                }
            }
        }

        if(getFluid().getFluid() == FluidRegistry.WATER) {
            if(getWorld().isRaining()) {
                int rate = (int) Math.floor(getWorld().rainingStrength * 5 * worldObj.getBiomeGenForCoords(getPos()).rainfall);
                if (getPos().getY() == getWorld().getPrecipitationHeight(getPos()).getY() - 1) {
                    FluidStack waterStack = getFluid().copy();
                    waterStack.amount = rate * 10;
                    //rainIntake += waterStack.amount;
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
                            List<BlockPos> firePos = new ArrayList<>();
                            for(int i=0; i<3;) {
                                if(remainingFrames.size() == 0)
                                    break;

                                int id = random.nextInt(remainingFrames.size());
                                TileEntityTankFrame frame = remainingFrames.get(id);
                                if(frame.getBlockState().getBlock().isFlammable(getWorld(), frame.getPos(), EnumFacing.UP)) {
                                    firePos.add(frame.getPos());
                                    i++;
                                }
                                else
                                    remainingFrames.remove(id);
                            }
                            for(BlockPos pos : firePos) {
                                getWorld().setBlockState(pos, Blocks.fire.getDefaultState());
                            }
                        }

                        frameBurnability = 0;

                        if(FancyFluidStorage.instance.SET_WORLD_ON_FIRE)
                            getWorld().playSoundEffect(getPos().getX() + 0.5D, getPos().getY() + 0.5D, getPos().getZ() + 0.5D, FancyFluidStorage.modId + ":fire", 1.0F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
                    }
                }
            }

            if(FancyFluidStorage.instance.SHOULD_TANKS_LEAK) {
                if(randomLeakTicks-- <= 0 && getFluid().getFluid().canBePlacedInWorld()) {
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
                        Block block = frame.getBlockState().getBlock();
                        if (GenericUtil.canBlockLeak(block) && !frame.getNeighborBlockOrAir(getFluid().getFluid().getBlock()).isEmpty() && block.getBlockHardness(worldObj, frame.getPos()) <= 1.0F) {
                            validFrames.add(frame);
                            i++;
                        } else
                            remainingFrames.remove(id);
                    }

                    for (TileEntityTankFrame frame : validFrames) {
                        Block block = frame.getBlockState().getBlock();
                        int hardness = (int) Math.ceil(block.getBlockHardness(getWorld(), frame.getPos()) * 100);
                        int rand = random.nextInt(hardness) + 1;
                        int diff = (int) Math.ceil(50 * ((float) getFluidAmount() / (float) getCapacity()));
                        if (rand >= hardness - diff) {
                            EnumFacing leakDir;
                            List<EnumFacing> dirs = frame.getNeighborBlockOrAir(getFluid().getFluid().getBlock());
                            if (dirs.size() == 0)
                                continue;

                            if (dirs.size() > 1) {
                                leakDir = dirs.get(random.nextInt(dirs.size()));
                            } else
                                leakDir = dirs.get(0);

                            BlockPos framePos = frame.getPos();
                            BlockPos leakPos = new BlockPos(framePos.getX() + leakDir.getFrontOffsetX(), framePos.getY() + leakDir.getFrontOffsetY(), framePos.getZ() + leakDir.getFrontOffsetZ());
                            if (maps[2].containsKey(leakPos))
                                continue;

                            if (getFluid().amount >= FluidContainerRegistry.BUCKET_VOLUME) {
                                getWorld().setBlockState(leakPos, getFluid().getFluid().getBlock().getDefaultState());
                                getWorld().notifyBlockOfStateChange(leakPos, getFluid().getFluid().getBlock());
                                drain(FluidContainerRegistry.BUCKET_VOLUME, true);
                            }
                        }
                    }
                }
            }
        }
    }

    public List<TileEntityValve> getAllValves() {
        if(!isMaster() && getMaster() != this)
            return getMaster().getAllValves();

        List<TileEntityValve> valves = new ArrayList<>();
        valves.add(this);

        if(otherValves.isEmpty())
            return valves;

        for(TileEntityValve valve : otherValves)
            valves.add(valve);

        return valves;
    }

    // Used by CC and OC
    public List<TileEntityValve> getValvesByName(String name) {
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
        wantsUpdate = true;
    }

    public int getTankHeight() {
        return isMaster() ? tankHeight : getMaster().tankHeight;
    }

    public void setInside(EnumFacing inside) {
        this.inside = inside;
    }

    public EnumFacing getInside() {
        return this.inside;
    }

    /**
     * Let's build a tank!
     * @param inside - The direction of the inside of the tank
     */
    public void buildTank(EnumFacing inside) {
        /**
         * Don't build if it's on the client!
         */
        if (getWorld().isRemote)
            return;

        /**
         * Let's first set the tank to be invalid,
         * since it should stay like that if the building fails.
         * Also, let's reset variables.
         */
        setValid(false);

        fluidCapacity = 0;
        tankFrames.clear();
        otherValves.clear();

        /**
         * Now, set the inside direction according to the variable,
         * *IF* our current inside is null.
         */
        if(inside != null)
            setInside(inside);

        if(!calculateInside())
            return;

        /**
         * Actually setup the tank here
         */
        if(!setupTank())
            return;

        /**
         * Just in case, set *initiated* to false again.
         * Also, update our neighbor blocks, e.g. pipes or similar.
         */
        initiated = false;
        updateBlockAndNeighbors();

        updateComparatorOutput();
    }

    /**
     * Over here, let's calculate the inside length,
     * which is required to get the corner points later on.
     * Also, to know if the inside of the tank is completely air.
     */
    public boolean calculateInside() {
        BlockPos insidePos = getPos().offset(getInside());

        for(EnumFacing dr : EnumFacing.VALUES) {
            for(int i=0; i<maxSize; i++) {
                BlockPos offsetPos = insidePos.offset(dr, i);
                if(!getWorld().isAirBlock(offsetPos)) {
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

    private void setSlaveValveInside(Map<BlockPos, IBlockState> airBlocks, TileEntityValve slave) {
        List<BlockPos> possibleAirBlocks = new ArrayList<>();
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(getWorld().isAirBlock(slave.getPos().offset(dr)))
                possibleAirBlocks.add(slave.getPos().offset(dr));
        }

        BlockPos insideAir = null;
        for(BlockPos pos : possibleAirBlocks) {
            if (airBlocks.containsKey(pos)) {
                insideAir = pos;
                break;
            }
        }

        if(insideAir == null)
            return;

        BlockPos dist = insideAir.subtract(slave.getPos());
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(dist.equals(new BlockPos(dr.getFrontOffsetX(), dr.getFrontOffsetY(), dr.getFrontOffsetZ()))) {
                slave.setInside(dr);
                break;
            }
        }
    }

    /**
     * Let's get the corner frames based on the inside position and length,
     * so we can set the BlockPos according to the corners.
     */
    public void updateCornerFrames() {
        bottomDiagFrame = getPos().add(inside.getDirectionVec()).add(length[EnumFacing.WEST.ordinal()] * EnumFacing.WEST.getFrontOffsetX() + EnumFacing.WEST.getFrontOffsetX(),
                length[EnumFacing.DOWN.ordinal()] * EnumFacing.DOWN.getFrontOffsetY() + EnumFacing.DOWN.getFrontOffsetY(),
                length[EnumFacing.NORTH.ordinal()] * EnumFacing.NORTH.getFrontOffsetZ() + EnumFacing.NORTH.getFrontOffsetZ());

        topDiagFrame = getPos().add(inside.getDirectionVec()).add(length[EnumFacing.EAST.ordinal()] * EnumFacing.EAST.getFrontOffsetX() + EnumFacing.EAST.getFrontOffsetX(),
                length[EnumFacing.UP.ordinal()] * EnumFacing.UP.getFrontOffsetY() + EnumFacing.UP.getFrontOffsetY(),
                length[EnumFacing.SOUTH.ordinal()] * EnumFacing.SOUTH.getFrontOffsetZ() + EnumFacing.SOUTH.getFrontOffsetZ());
    }

    /**
     * The maps contain the blocks on the:
     * inside,
     * outter frame,
     * inner frame
     */
    private void fetchMaps() {
        maps = GenericUtil.getTankFrame(getWorld(), bottomDiagFrame, topDiagFrame);
    }

    private boolean setupTank() {
        updateCornerFrames();
        fetchMaps();

        otherValves = new ArrayList<>();
        tankFrames = new ArrayList<>();

        valveHeightPosition = Math.abs(bottomDiagFrame.subtract(getPos()).getY());
        tankHeight = topDiagFrame.subtract(bottomDiagFrame).getY() - 1;

        IBlockState bottomDiagBlock = getWorld().getBlockState(bottomDiagFrame);
        IBlockState topDiagBlock = getWorld().getBlockState(topDiagFrame);

        if(bottomDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = getWorld().getTileEntity(bottomDiagFrame);
            if(tile != null && tile instanceof TileEntityTankFrame)
                bottomDiagBlock = ((TileEntityTankFrame) tile).getBlockState();
        }
        if(topDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = getWorld().getTileEntity(topDiagFrame);
            if(tile != null && tile instanceof TileEntityTankFrame)
                topDiagBlock = ((TileEntityTankFrame) tile).getBlockState();
        }

        if(!GenericUtil.isValidTankBlock(getWorld(), bottomDiagFrame, bottomDiagBlock))
            return false;

        if (!GenericUtil.areTankBlocksValid(bottomDiagBlock, topDiagBlock, getWorld(), bottomDiagFrame))
            return false;

        BlockPos pos;

        if (FancyFluidStorage.instance.INSIDE_CAPACITY) {
            fluidCapacity = (maps[2].size()) * mbPerVirtualTank;
        } else {
            fluidCapacity = (maps[0].size() + maps[1].size() + maps[2].size()) * mbPerVirtualTank;
        }

        for (Map.Entry<BlockPos, IBlockState> frameCheck : maps[0].entrySet()) {
            pos = frameCheck.getKey();
            IBlockState fBlock = frameCheck.getValue();
            int burnability = fBlock.getBlock().getFlammability(getWorld(), pos, EnumFacing.UP);
            if(burnability > frameBurnability)
                frameBurnability = burnability;

            if (!GenericUtil.areTankBlocksValid(fBlock, bottomDiagBlock, getWorld(), pos)) {
                return false;
            }
        }

        List<TileEntityValve> valves = new ArrayList<>();
        for (Map.Entry<BlockPos, IBlockState> insideFrameCheck : maps[1].entrySet()) {
            pos = insideFrameCheck.getKey();
            IBlockState check = insideFrameCheck.getValue();
            int burnability = check.getBlock().getFlammability(getWorld(), pos, EnumFacing.UP);
            if(burnability > frameBurnability)
                frameBurnability = burnability;

            if (GenericUtil.areTankBlocksValid(check, bottomDiagBlock, getWorld(), pos) || GenericUtil.isBlockGlass(check.getBlock(), check.getBlock().getMetaFromState(check)))
                continue;

            TileEntity tile = getWorld().getTileEntity(pos);
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
            pos = valve.getPos();
            valve.valveHeightPosition = Math.abs(bottomDiagFrame.subtract(pos).getY());

            valve.isMaster = false;
            valve.setMasterPos(getPos());
            setSlaveValveInside(maps[2], valve);
        }
        isMaster = true;

        for (Map.Entry<BlockPos, IBlockState> setTiles : maps[0].entrySet()) {
            pos = setTiles.getKey();
            Block block = setTiles.getValue().getBlock();
            if (!block.isOpaqueCube() && block != FancyFluidStorage.blockTankFrame) {
                getWorld().setBlockState(pos, FancyFluidStorage.blockTankFrame.getDefaultState());
            }
            else if(block.isOpaqueCube() && block != FancyFluidStorage.blockTankFrameOpaque) {
                getWorld().setBlockState(pos, FancyFluidStorage.blockTankFrameOpaque.getDefaultState());
            }
            TileEntityTankFrame tankFrame = (TileEntityTankFrame) getWorld().getTileEntity(pos);
            tankFrame.initialize(getPos(), setTiles.getValue());
            tankFrames.add(tankFrame);
        }

        for (Map.Entry<BlockPos, IBlockState> setTiles : maps[1].entrySet()) {
            pos = setTiles.getKey();
            TileEntity tile = getWorld().getTileEntity(pos);
            if (tile != null) {
                if (tile instanceof TileEntityValve && tile != this)
                    otherValves.add((TileEntityValve) tile);

                else if (tile instanceof TileEntityTankFrame) {
                    ((TileEntityTankFrame) tile).setValvePos(getPos());
                    tankFrames.add((TileEntityTankFrame) tile);
                }
                else if (GenericUtil.isTileEntityAcceptable(setTiles.getValue().getBlock(), tile)) {
                    Block block = setTiles.getValue().getBlock();
                    if (!block.isOpaqueCube() && block != FancyFluidStorage.blockTankFrame) {
                        getWorld().setBlockState(pos, FancyFluidStorage.blockTankFrame.getDefaultState());
                    }
                    else if(block.isOpaqueCube() && block != FancyFluidStorage.blockTankFrameOpaque) {
                        getWorld().setBlockState(pos, FancyFluidStorage.blockTankFrameOpaque.getDefaultState());
                    }
                    TileEntityTankFrame tankFrame = (TileEntityTankFrame) getWorld().getTileEntity(pos);
                    tankFrame.initialize(getPos(), setTiles.getValue());
                    tankFrames.add(tankFrame);
                }
            } else {
                Block block = setTiles.getValue().getBlock();
                if (!block.isOpaqueCube() && block != FancyFluidStorage.blockTankFrame) {
                    getWorld().setBlockState(pos, FancyFluidStorage.blockTankFrame.getDefaultState());
                }
                else if(block.isOpaqueCube() && block != FancyFluidStorage.blockTankFrameOpaque) {
                    getWorld().setBlockState(pos, FancyFluidStorage.blockTankFrameOpaque.getDefaultState());
                }
                TileEntityTankFrame tankFrame = (TileEntityTankFrame) getWorld().getTileEntity(pos);
                tankFrame.initialize(getPos(), setTiles.getValue());
                tankFrames.add(tankFrame);
            }
        }

        setValid(true);
        return true;
    }

    public void breakTank(TileEntity frame) {
        if (getWorld().isRemote)
            return;

        if(!isMaster() && getMaster() != null) {
            if(getMaster() != this)
                getMaster().breakTank(frame);

            return;
        }

        setValid(false);

        for(TileEntityValve valve : otherValves) {
            valve.fluidStack = getFluid();
            valve.updateFluidTemperature();
            valve.master = null;
            valve.setValid(false);
            valve.updateBlockAndNeighbors();
        }

        for(TileEntityTankFrame tankFrame : tankFrames) {
            if(frame == tankFrame)
                continue;

            tankFrame.breakFrame();
        }
        tankFrames.clear();
        otherValves.clear();

        this.updateBlockAndNeighbors();
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean isValid() {
        return getMaster() != null && getMaster().isValid;

    }

    public void updateBlockAndNeighbors() {
        updateBlockAndNeighbors(false);
    }

    public void updateBlockAndNeighbors(boolean onlyThis) {
        if(getWorld().isRemote)
            return;

        this.markForUpdate(onlyThis);

        if(otherValves != null) {
            for(TileEntityValve otherValve : otherValves) {
                otherValve.markForUpdate(true);
            }
        }

        EnumFacing outside = getInside().getOpposite();
        TileEntity outsideTile = worldObj.getTileEntity(getPos().offset(outside));
        if (outsideTile != null) {
            //BC Check
            if(FancyFluidStorage.proxy.BUILDCRAFT_LOADED) {
                if(outsideTile instanceof IPipeTile)
                    ((IPipeTile) outsideTile).scheduleNeighborChange();
            }
        }
    }

    private void updateComparatorOutput() {
        if(this.lastComparatorOut != getComparatorOutput()) {
            this.lastComparatorOut = getComparatorOutput();
            if(isMaster()) {
                for(TileEntityValve otherValve : otherValves) {
                    getWorld().updateComparatorOutputLevel(otherValve.getPos(), otherValve.getBlockType());
                }
            }
            getWorld().updateComparatorOutputLevel(getPos(), getBlockType());
        }
    }

    private void markForUpdate(boolean onlyThis) {
        updateComparatorOutput();

        if (!onlyThis) {
            for (TileEntityTankFrame frame : tankFrames) {
                frame.markForUpdate();
            }
        }

        // Do Luminosity update
        if (getFluidLuminosity() != oldLuminosity) {
            oldLuminosity = getFluidLuminosity();
            for (TileEntityTankFrame frame : tankFrames) {
                frame.markForUpdate();
            }
        }

        getWorld().markBlockForUpdate(getPos());
    }

    public boolean isMaster() {
        return isMaster;
    }

    public TileEntityValve getMaster() {
        if(isMaster())
            return this;

        if(masterValvePos != null) {
            TileEntity tile = getWorld().getTileEntity(masterValvePos);
            master = tile instanceof TileEntityValve ? (TileEntityValve) tile : null;
        }

        return master;
    }

    public void setMasterPos(BlockPos masterValvePos) {
        this.masterValvePos = masterValvePos;
        this.master = null;
    }

    public boolean getAutoOutput() {
        return isValid() && this.autoOutput;
    }

    public void setAutoOutput(boolean autoOutput) {
        this.autoOutput = autoOutput;
        updateBlockAndNeighbors(true);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        setInside(EnumFacing.getFront(tag.getInteger("inside")));

        isMaster = tag.getBoolean("master");
        if(isMaster()) {
            setValid(tag.getBoolean("isValid"));

            if(tag.getBoolean("hasFluid")) {
                if(tag.hasKey("fluidName"))
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
            if(getMaster() == null && tag.hasKey("masterValve")) {
                int[] masterValveP = tag.getIntArray("masterValve");
                setMasterPos(new BlockPos(masterValveP[0], masterValveP[1], masterValveP[2]));
            }
        }

        autoOutput = tag.getBoolean("autoOutput");
        if(tag.hasKey("valveName"))
            setValveName(tag.getString("valveName"));
        else
            setValveName(GenericUtil.getUniqueValveName(this));

        if(tag.hasKey("bottomDiagF") && tag.hasKey("topDiagF")) {
            int[] bottomDiagF = tag.getIntArray("bottomDiagF");
            int[] topDiagF = tag.getIntArray("topDiagF");
            bottomDiagFrame = new BlockPos(bottomDiagF[0], bottomDiagF[1], bottomDiagF[2]);
            topDiagFrame = new BlockPos(topDiagF[0], topDiagF[1], topDiagF[2]);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("inside", getInside().ordinal());

        tag.setBoolean("master", isMaster());
        if(isMaster()) {
            tag.setBoolean("isValid", isValid());
            tag.setBoolean("hasFluid", fluidStack != null);
            if(fluidStack != null) {
                tag.setString("fluidName", FluidRegistry.getFluidName(fluidStack));
                tag.setInteger("fluidAmount", fluidStack.amount);
            }

            tag.setInteger("tankHeight", tankHeight);
            tag.setInteger("fluidCapacity", fluidCapacity);
        }
        else {
            if(getMaster() != null) {
                BlockPos pos = getMaster().getPos();
                int[] masterPos = new int[]{pos.getX(), pos.getY(), pos.getZ()};
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
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(getPos(), 0, tag);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(bottomDiagFrame == null || topDiagFrame == null)
            return super.getRenderBoundingBox();

        return AxisAlignedBB.fromBounds(bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ(), topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ());
    }

    // Tank logic!

    public int getFluidLuminosity() {
        FluidStack fstack = getFluid();
        if(fstack == null)
            return 0;

        Fluid fluid = fstack.getFluid();
        if(fluid == null)
            return 0;

        return Math.max(0, fluid.getLuminosity(fstack) - 1);
    }

    public void updateFluidTemperature() {
        FluidStack fstack = getFluid();
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
        if(getFluid() == null)
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
            if (!isValid() || fluidStack != null && !fluidStack.isFluidEqual(resource))
                return 0;

            if (getFluidAmount() >= getCapacity()) {
                for(TileEntityValve valve : getAllValves()) {
                    if (valve == this)
                        continue;

                    EnumFacing outside = valve.getInside().getOpposite();
                    TileEntity tile = getWorld().getTileEntity(valve.getPos().offset(outside));
                    if (tile != null && tile instanceof TileEntityValve) {
                        return ((TileEntityValve) tile).fill(getInside(), resource, doFill);
                    }
                }
            }

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
            setNeedsUpdate();
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
                }
                setNeedsUpdate();
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
        if(getFluid() == null)
            return 0;

        return Math.floor((double) getFluidAmount() / (double) getCapacity() * 100);
    }

    public int fillFromContainer(EnumFacing from, FluidStack resource, boolean doFill) {
        if(!canFillIncludingContainers(from, resource.getFluid()))
            return 0;

        return getMaster() == this ? fill(resource, doFill) : getMaster().fill(resource, doFill);
    }

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if(!canFill(from, resource.getFluid()))
            return 0;

        return getMaster() == this ? fill(resource, doFill) : getMaster().fill(resource, doFill);
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        return getMaster() == this ? drain(resource.amount, doDrain) : getMaster().drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return getMaster() == this ? drain(maxDrain, doDrain) : getMaster().drain(maxDrain, doDrain);
    }

    public boolean canFillIncludingContainers(EnumFacing from, Fluid fluid) {
        if (getFluid() != null && getFluid().getFluid() != fluid)
            return false;

        if (getFluidAmount() >= getCapacity()) {
            for (TileEntityValve valve : getAllValves()) {
                if (valve == this)
                    continue;

                if (valve.valveHeightPosition > getTankHeight()) {
                    EnumFacing outside = valve.getInside().getOpposite();
                    TileEntity tile = getWorld().getTileEntity(valve.getPos().offset(outside));
                    if (tile != null && tile instanceof TileEntityValve) {
                        return ((TileEntityValve) tile).canFill(valve.getInside(), fluid);
                    }
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        if(!canFillIncludingContainers(from, fluid))
            return false;

        return !getAutoOutput() || valveHeightPosition > getTankHeight() || valveHeightPosition + 0.5f >= getTankHeight() * getFillPercentage();

    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return getFluid() != null && getFluid().getFluid() == fluid && getFluidAmount() > 0;

    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if(!isValid())
            return null;

        return getMaster() == this ? new FluidTankInfo[]{ getInfo() } : getMaster().getTankInfo(from);
    }

    public int getComparatorOutput() {
        if(!isValid())
            return 0;

        return MathHelper.floor_float(((float) this.getFluidAmount() / this.getCapacity()) * 14.0F);
    }

    // BC
    /*
    @Optional.Method(modid = "BuildCraftAPI|Transport")
    @Override
    public ConnectOverride overridePipeConnection(IPipeTile.PipeType pipeType, EnumFacing from) {
        if(!isValid())
            return ConnectOverride.DISCONNECT;

        return ConnectOverride.CONNECT;
    }
    */

    // ComputerCraft
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

    /*
    @Optional.Method(modid = "funkylocomotion")
    @Override
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }
    */
}
