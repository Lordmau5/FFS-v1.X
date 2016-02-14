package com.lordmau5.ffs.tile.abstracts;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.block.tanktiles.BlockTankFrame;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankFrame;
import com.lordmau5.ffs.tile.interfaces.IFacingTile;
import com.lordmau5.ffs.tile.interfaces.INameableTile;
import com.lordmau5.ffs.tile.util.TankConfig;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by Dustin on 21.01.2016.
 */
public abstract class AbstractTankValve extends AbstractTankTile implements IFacingTile, INameableTile, IFluidTank {

    private final int maxSize = FancyFluidStorage.instance.MAX_SIZE;
    protected int mbPerVirtualTank = FancyFluidStorage.instance.MB_PER_TANK_BLOCK;
    protected int minBurnableTemp = FancyFluidStorage.instance.MIN_BURNABLE_TEMPERATURE;
    private TankConfig tankConfig;

    private int oldLuminosity;
    private boolean isValid;
    private boolean isMaster;
    private boolean initiated;

    private int frameBurnability = 0;

    public int valveHeightPosition = 0;

    private int randomBurnTicks = 20 * 5; // Every 5 seconds
    private int randomLeakTicks = 20 * 60; // Every minute

    public List<AbstractTankTile> tankTiles;

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

    public AbstractTankValve() {
        tankTiles = new ArrayList<>();
    }

    @Override
    public void validate() {
        super.validate();
        initiated = true;
        initialWaitTick = 20;
    }

    @Override
    public void update() {
        super.update();

        if (getWorld().isRemote)
            return;

        if (initiated) {
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
                    buildTank(getTileFacing());
                    return;
                }
            }
        }

        if (!isValid())
            return;

        if (!isMaster() && getMasterValve() == null) {
            setValid(false);
            updateBlockAndNeighbors();
            return;
        }

        if(getFluid() == null)
            return;

        if(isMaster()) {
            if (minBurnableTemp > 0 && fluidTemperature >= minBurnableTemp && frameBurnability > 0) {
                if (randomBurnTicks-- <= 0) {
                    randomBurnTicks = 20 * 5;
                    Random random = new Random();

                    int temperatureDiff = fluidTemperature - minBurnableTemp;
                    int chanceOfBurnability = 300 - frameBurnability;
                    int rand = random.nextInt(300) + temperatureDiff + ((int) Math.floor((float) getFluidAmount() / (float) getCapacity() * 300));
                    if (rand >= chanceOfBurnability) {
                        boolean successfullyBurned = false;

                        List<TileEntityTankFrame> remainingFrames = getTankTiles(TileEntityTankFrame.class);

                        List<TileEntityTankFrame> removingFrames = new ArrayList<>();
                        while (!successfullyBurned) { // Try to burn at least one
                            if (remainingFrames.size() == 0)
                                break;

                            boolean couldBurnOne = false;
                            for (int i = 0; i < Math.min(10, remainingFrames.size()); i++) {
                                int id = random.nextInt(remainingFrames.size());
                                TileEntityTankFrame frame = remainingFrames.get(id);
                                couldBurnOne = frame.tryBurning();
                                if (!couldBurnOne)
                                    removingFrames.add(frame);
                            }
                            remainingFrames.removeAll(removingFrames);
                            removingFrames.clear();
                            if (couldBurnOne)
                                successfullyBurned = true;
                        }
                        if (!successfullyBurned) {
                            remainingFrames = getTankTiles(TileEntityTankFrame.class);
                            List<BlockPos> firePos = new ArrayList<>();
                            for (int i = 0; i < 3; ) {
                                if (remainingFrames.size() == 0)
                                    break;

                                int id = random.nextInt(remainingFrames.size());
                                TileEntityTankFrame frame = remainingFrames.get(id);
                                if (frame.getBlockState().getBlock().isFlammable(getFakeWorld(), frame.getPos(), EnumFacing.UP)) {
                                    firePos.add(frame.getPos());
                                    i++;
                                } else
                                    remainingFrames.remove(id);
                            }
                            for (BlockPos pos : firePos) {
                                getWorld().setBlockState(pos, Blocks.fire.getDefaultState());
                            }
                        }

                        frameBurnability = 0;

                        if (FancyFluidStorage.instance.SET_WORLD_ON_FIRE)
                            getWorld().playSoundEffect(getPos().getX() + 0.5D, getPos().getY() + 0.5D, getPos().getZ() + 0.5D, FancyFluidStorage.modId + ":fire", 1.0F, getWorld().rand.nextFloat() * 0.1F + 0.9F);
                    }
                }
            }

            if (FancyFluidStorage.instance.SHOULD_TANKS_LEAK) {
                if (randomLeakTicks-- <= 0 && getFluid().getFluid().canBePlacedInWorld()) {
                    randomLeakTicks = 20 * 60;

                    Random random = new Random();
                    int amt = random.nextInt(3) + 1;

                    List<TileEntityTankFrame> validFrames = new ArrayList<>();

                    List<TileEntityTankFrame> remainingFrames = getTankTiles(TileEntityTankFrame.class);

                    for (int i = 0; i < amt; ) {
                        if (remainingFrames.size() == 0)
                            break;

                        int id = random.nextInt(remainingFrames.size());
                        TileEntityTankFrame frame = remainingFrames.get(id);
                        Block block = frame.getBlockState().getBlock();
                        if (GenericUtil.canBlockLeak(block) && !frame.getNeighborBlockOrAir(getFluid().getFluid().getBlock()).isEmpty() && block.getBlockHardness(getFakeWorld(), frame.getPos()) <= 1.0F) {
                            validFrames.add(frame);
                            i++;
                        } else
                            remainingFrames.remove(id);
                    }

                    for (TileEntityTankFrame frame : validFrames) {
                        Block block = frame.getBlockState().getBlock();
                        int hardness = (int) Math.ceil(block.getBlockHardness(getFakeWorld(), frame.getPos()) * 100);
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

    public void setTankConfig(TankConfig tankConfig) {
        this.tankConfig = tankConfig;
    }

    public TankConfig getTankConfig() {
        if(!isMaster() && getMasterValve() != null && getMasterValve() != this)
            return getMasterValve().getTankConfig();

        if(this.tankConfig == null)
            this.tankConfig = new TankConfig();
        return this.tankConfig;
    }

    public void toggleFluidLock(boolean state) {
        if(!state) {
            getTankConfig().unlockFluid();
        }
        else {
            if(getFluid() == null)
                return;

            getTankConfig().lockFluid(getFluid());
        }
        getMasterValve().setNeedsUpdate(UpdateType.STATE);
    }

    public <T> List<T> getTankTiles(Class<T> type) {
        return tankTiles.stream().filter(p -> type.isAssignableFrom(p.getClass())).map(p -> (T) p).collect(Collectors.toList());
    }

    public List<AbstractTankValve> getAllValves() {
        if(!isMaster() && getMasterValve() != null && getMasterValve() != this)
            return getMasterValve().getAllValves();

        List<AbstractTankValve> valves = getTankTiles(AbstractTankValve.class);
        valves.add(this);
        return valves;
    }

    public int getTankHeight() {
        return getTankConfig().getTankHeight();
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

        tankTiles.clear();

        /**
         * Now, set the inside direction according to the variable.
         */
        if(inside != null)
            setTileFacing(inside);

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
        BlockPos insidePos = getPos().offset(getTileFacing());

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

    private void setTankTileFacing(Map<BlockPos, IBlockState> airBlocks, TileEntity tankTile) {
        List<BlockPos> possibleAirBlocks = new ArrayList<>();
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(getWorld().isAirBlock(tankTile.getPos().offset(dr)))
                possibleAirBlocks.add(tankTile.getPos().offset(dr));
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

        BlockPos dist = insideAir.subtract(tankTile.getPos());
        for(EnumFacing dr : EnumFacing.VALUES) {
            if(dist.equals(new BlockPos(dr.getFrontOffsetX(), dr.getFrontOffsetY(), dr.getFrontOffsetZ()))) {
                ((IFacingTile)tankTile).setTileFacing(dr);
                break;
            }
        }
    }

    /**
     * Let's get the corner frames based on the inside position and length,
     * so we can set the BlockPos according to the corners.
     */
    public void updateCornerFrames() {
        bottomDiagFrame = getPos().add(getTileFacing().getDirectionVec()).add(length[EnumFacing.WEST.ordinal()] * EnumFacing.WEST.getFrontOffsetX() + EnumFacing.WEST.getFrontOffsetX(),
                length[EnumFacing.DOWN.ordinal()] * EnumFacing.DOWN.getFrontOffsetY() + EnumFacing.DOWN.getFrontOffsetY(),
                length[EnumFacing.NORTH.ordinal()] * EnumFacing.NORTH.getFrontOffsetZ() + EnumFacing.NORTH.getFrontOffsetZ());

        topDiagFrame = getPos().add(getTileFacing().getDirectionVec()).add(length[EnumFacing.EAST.ordinal()] * EnumFacing.EAST.getFrontOffsetX() + EnumFacing.EAST.getFrontOffsetX(),
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
        maps = GenericUtil.getTankFrame(getFakeWorld(), bottomDiagFrame, topDiagFrame);
    }

    private boolean setupTank() {
        updateCornerFrames();
        fetchMaps();

        valveHeightPosition = Math.abs(bottomDiagFrame.subtract(getPos()).getY());
        getTankConfig().setTankHeight(topDiagFrame.subtract(bottomDiagFrame).getY() - 1);

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
            int burnability = fBlock.getBlock().getFlammability(getFakeWorld(), pos, EnumFacing.UP);
            if(burnability > frameBurnability)
                frameBurnability = burnability;

            if (!GenericUtil.areTankBlocksValid(fBlock, bottomDiagBlock, getWorld(), pos)) {
                return false;
            }
        }

        List<TileEntity> facingTiles = new ArrayList<>();
        for (Map.Entry<BlockPos, IBlockState> insideFrameCheck : maps[1].entrySet()) {
            pos = insideFrameCheck.getKey();
            IBlockState check = insideFrameCheck.getValue();
            int burnability = check.getBlock().getFlammability(getFakeWorld(), pos, EnumFacing.UP);
            if(burnability > frameBurnability)
                frameBurnability = burnability;

            TileEntity tile = getWorld().getTileEntity(pos);
            if (tile != null) {
                if(tile instanceof IFacingTile)
                    facingTiles.add(tile);

                if(tile instanceof AbstractTankValve) {
                    AbstractTankValve valve = (AbstractTankValve) tile;
                    if (valve == this)
                        continue;

                    if (valve.fluidStack != null) {
                        this.fluidStack = valve.fluidStack;
                        updateFluidTemperature();
                    }
                    continue;
                }
                else if(tile instanceof AbstractTankTile) {
                    continue;
                }
            }

            if (!GenericUtil.areTankBlocksValid(check, bottomDiagBlock, getWorld(), pos) && !GenericUtil.isBlockGlass(check.getBlock(), check.getBlock().getMetaFromState(check)))
                return false;
        }

        // Make sure we don't overfill a tank. If the new tank is smaller than the old one, excess liquid disappear.
        if (this.fluidStack != null)
            this.fluidStack.amount = Math.min(this.fluidStack.amount, this.fluidCapacity);

        for (TileEntity facingTile : facingTiles) {
            setTankTileFacing(maps[2], facingTile);
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
            tankTiles.add(tankFrame);
        }

        for (Map.Entry<BlockPos, IBlockState> setTiles : maps[1].entrySet()) {
            pos = setTiles.getKey();

            TileEntity tile = getWorld().getTileEntity(pos);
            if(tile == this)
                continue;

            if (tile != null) {
                if (tile instanceof TileEntityTankFrame) {
                    ((TileEntityTankFrame) tile).setValvePos(getPos());
                    tankTiles.add((TileEntityTankFrame) tile);
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
                    tankTiles.add(tankFrame);
                }
                else if(tile instanceof AbstractTankValve) {
                    AbstractTankValve valve = (AbstractTankValve) tile;

                    pos = valve.getPos();
                    valve.valveHeightPosition = Math.abs(bottomDiagFrame.subtract(pos).getY());

                    valve.isMaster = false;
                    valve.setValvePos(getPos());
                    valve.setTankConfig(getTankConfig());
                    tankTiles.add(valve);
                }
                else if(tile instanceof AbstractTankTile) {
                    AbstractTankTile tankTile = (AbstractTankTile) tile;
                    tankTile.setValvePos(getPos());
                    tankTiles.add((AbstractTankTile) tile);
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
                tankTiles.add(tankFrame);
            }
        }

        setValid(true);
        return true;
    }

    public void breakTank(TileEntity frame) {
        if (getWorld().isRemote)
            return;

        if(!isMaster() && getMasterValve() != null && getMasterValve() != this) {
            getMasterValve().breakTank(frame);

            return;
        }

        for(AbstractTankValve valve : getAllValves()) {
            valve.setTankConfig(getTankConfig());
            valve.fluidStack = getFluid();
            valve.updateFluidTemperature();
            valve.setValvePos(null);
            valve.setValid(false);
            valve.updateBlockAndNeighbors(true);
        }
        tankTiles.removeAll(getTankTiles(AbstractTankValve.class));
        for(TileEntityTankFrame tankFrame : getTankTiles(TileEntityTankFrame.class)) {
            if(frame == tankFrame)
                continue;

            tankFrame.breakFrame();
        }
        tankTiles.removeAll(getTankTiles(TileEntityTankFrame.class));
        for(AbstractTankTile tankTile : tankTiles) {
            tankTile.setValvePos(null);
        }

        tankTiles.clear();

        this.updateBlockAndNeighbors(true);
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    @Override
    public boolean isValid() {
        if(getMasterValve() == null || getMasterValve() == this)
            return this.isValid;

        return getMasterValve().isValid;
    }

    public void updateBlockAndNeighbors() {
        updateBlockAndNeighbors(false);
    }

    public void updateBlockAndNeighbors(boolean onlyThis) {
        if(getWorld() == null || getWorld().isRemote)
            return;

        setNeedsUpdate();
        this.markForUpdate(onlyThis);

        if(!tankTiles.isEmpty() && !onlyThis) {
            for(AbstractTankTile tile : tankTiles) {
                if(tile == this)
                    continue;

                if(tile instanceof AbstractTankValve)
                    ((AbstractTankValve) tile).updateBlockAndNeighbors(true);
                else
                    tile.setNeedsUpdate();
            }
        }

        EnumFacing facing = getTileFacing();
        if(facing == null)
            return;

        EnumFacing outside = facing.getOpposite();
        BlockPos outsidePos = getPos().offset(outside);
        if(!getWorld().isAirBlock(outsidePos))
            getWorld().markBlockForUpdate(outsidePos);
    }

    private void updateComparatorOutput() {
        if(this.lastComparatorOut != getComparatorOutput()) {
            this.lastComparatorOut = getComparatorOutput();
            if(isMaster()) {
                for(AbstractTankValve otherValve : getTankTiles(AbstractTankValve.class)) {
                    getWorld().updateComparatorOutputLevel(otherValve.getPos(), otherValve.getBlockType());
                }
            }
            getWorld().updateComparatorOutputLevel(getPos(), getBlockType());
        }
    }

    @Override
    public void markForUpdate() {
        if (getFluidLuminosity() != oldLuminosity) {
            oldLuminosity = getFluidLuminosity();
            for (TileEntityTankFrame tile : getTankTiles(TileEntityTankFrame.class)) {
                tile.setNeedsUpdate(UpdateType.FULL);
            }
        }

        updateComparatorOutput();

        super.markForUpdate();
    }

    private void markForUpdate(boolean onlyThis) {
        if (!onlyThis) {
            oldLuminosity = -1;
        }

        markForUpdate();
    }

    public boolean isMaster() {
        return isMaster;
    }

    @Override
    public AbstractTankValve getMasterValve() {
        return isMaster() ? this : super.getMasterValve();
    }

    @Override
    public void setTileName(String name) {
        this.tile_name = name;
    }

    @Override
    public String getTileName() {
        if(this.tile_name.isEmpty())
            setTileName(GenericUtil.getUniquePositionName(this));

        return this.tile_name;
    }

    @Override
    public void setTileFacing(EnumFacing facing) {
        this.tile_facing = facing;
    }

    @Override
    public EnumFacing getTileFacing() {
        return this.tile_facing;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

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

            fluidCapacity = tag.getInteger("fluidCapacity");

            getTankConfig().readFromNBT(tag);
        }

        if(tag.hasKey("bottomDiagF") && tag.hasKey("topDiagF")) {
            int[] bottomDiagF = tag.getIntArray("bottomDiagF");
            int[] topDiagF = tag.getIntArray("topDiagF");
            bottomDiagFrame = new BlockPos(bottomDiagF[0], bottomDiagF[1], bottomDiagF[2]);
            topDiagFrame = new BlockPos(topDiagF[0], topDiagF[1], topDiagF[2]);
        }

        readTileNameFromNBT(tag);
        readTileFacingFromNBT(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("master", isMaster());
        if(isMaster()) {
            tag.setBoolean("isValid", isValid());
            tag.setBoolean("hasFluid", fluidStack != null);
            if(fluidStack != null) {
                tag.setString("fluidName", FluidRegistry.getFluidName(fluidStack));
                tag.setInteger("fluidAmount", fluidStack.amount);
            }

            tag.setInteger("fluidCapacity", fluidCapacity);

            getTankConfig().writeToNBT(tag);
        }

        if(bottomDiagFrame != null && topDiagFrame != null) {
            tag.setIntArray("bottomDiagF", new int[]{bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ()});
            tag.setIntArray("topDiagF", new int[]{topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ()});
        }

        saveTileNameToNBT(tag);
        saveTileFacingToNBT(tag);

        super.writeToNBT(tag);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(bottomDiagFrame == null || topDiagFrame == null)
            return super.getRenderBoundingBox();

        return AxisAlignedBB.fromBounds(bottomDiagFrame.getX(), bottomDiagFrame.getY(), bottomDiagFrame.getZ(), topDiagFrame.getX(), topDiagFrame.getY(), topDiagFrame.getZ());
    }

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

        return getMasterValve() == this ? fluidStack : getMasterValve().fluidStack;
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

        return getMasterValve() == this ? fluidCapacity : getMasterValve().fluidCapacity;
    }

    @Override
    public FluidTankInfo getInfo() {
        if(!isValid())
            return null;

        return new FluidTankInfo(getMasterValve());
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if(getMasterValve() == this) {
            if (!isValid() || fluidStack != null && !fluidStack.isFluidEqual(resource))
                return 0;

            if (getFluidAmount() >= getCapacity()) {
                for(AbstractTankValve valve : getAllValves()) {
                    if (valve == this)
                        continue;

                    EnumFacing outside = valve.getTileFacing().getOpposite();
                    TileEntity tile = getWorld().getTileEntity(valve.getPos().offset(outside));
                    if (tile != null && tile instanceof TileEntityFluidValve) {
                        return ((TileEntityFluidValve) tile).fill(getTileFacing(), resource, doFill);
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
            return getMasterValve().fill(resource, doFill);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if(getMasterValve() == this) {
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
            return getMasterValve().drain(maxDrain, doDrain);
    }

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

        return getMasterValve() == this ? fill(resource, doFill) : getMasterValve().fill(resource, doFill);
    }

    public boolean canFillIncludingContainers(EnumFacing from, Fluid fluid) {
        if (getFluid() != null && getFluid().getFluid() != fluid)
            return false;

        if(getTankConfig().isFluidLocked() && getTankConfig().getLockedFluid().getFluid() != fluid)
            return false;

        if (getFluidAmount() >= getCapacity()) {
            for (AbstractTankValve valve : getAllValves()) {
                if (valve == this)
                    continue;

                if (valve.valveHeightPosition > getTankHeight()) {
                    EnumFacing outside = valve.getTileFacing().getOpposite();
                    TileEntity tile = getWorld().getTileEntity(valve.getPos().offset(outside));
                    if (tile != null && tile instanceof TileEntityFluidValve) {
                        return ((TileEntityFluidValve) tile).canFill(valve.getTileFacing(), fluid);
                    }
                }
            }
            return false;
        }

        return true;
    }

    public int getComparatorOutput() {
        if (!isValid())
            return 0;

        return MathHelper.floor_float(((float) this.getFluidAmount() / this.getCapacity()) * 14.0F);
    }

}
