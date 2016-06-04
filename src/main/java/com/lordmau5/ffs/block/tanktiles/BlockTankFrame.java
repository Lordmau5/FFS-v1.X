package com.lordmau5.ffs.block.tanktiles;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import com.lordmau5.ffs.tile.tanktiles.TileEntityTankFrame;
import com.lordmau5.ffs.util.FFSStateProps;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.Optional;
import team.chisel.api.IFacade;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Dustin on 02.07.2015.
 */
@SuppressWarnings("deprecation")
@Optional.InterfaceList(
        @Optional.Interface(iface = "team.chisel.api.IFacade", modid = "chisel")
)
public class BlockTankFrame extends Block implements IFacade {

    public BlockTankFrame() {
        super(Material.ROCK);
    }

    public BlockTankFrame(String name) {
        this();
        setUnlocalizedName(name);
        setRegistryName(name);
        setDefaultState(((IExtendedBlockState) blockState.getBaseState())
                .withProperty(FFSStateProps.FRAME_MODEL, null)
                .withProperty(FFSStateProps.FRAME_STATE, null));
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[] { FFSStateProps.FRAME_MODEL, FFSStateProps.FRAME_STATE });
    }

    @Override
    public BlockStateContainer getBlockState() {
        return super.getBlockState();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTankFrame();
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(pos);
            frame.setBlockState(null);
            frame.breakFrame();
            frame.onBreak();
        }
        super.onBlockDestroyedByExplosion(world, pos, explosion);
    }

    @Override
    public boolean removedByPlayer(IBlockState stateIn, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(pos);
            if(!player.capabilities.isCreativeMode) {
                ArrayList<ItemStack> items = new ArrayList<>();

                IBlockState state = frame.getBlockState();
                Block block = state.getBlock();

                if(block.canSilkHarvest(world, pos, state, player) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, player.getHeldItemMainhand()) > 0) {
                    ForgeEventFactory.fireBlockHarvesting(items, world, pos, state, 0, 1.0f, true, player);

                    ItemStack itemstack = new ItemStack(Item.getItemFromBlock(block), 1, block.getMetaFromState(state));
                    items.add(itemstack);

                    for (ItemStack is : items)
                    {
                        spawnAsEntity(world, pos, is);
                    }
                }
                else {
                    ForgeEventFactory.fireBlockHarvesting(items, world, pos, state, 0, 1.0f, false, player);

                    items.addAll(block.getDrops(world, pos, state, 0));
                    for (ItemStack is : items)
                    {
                        spawnAsEntity(world, pos, is);
                    }
                }
            }
        }
        return super.removedByPlayer(stateIn, world, pos, player, willHarvest);
    }

    @Override
    public Item getItemDropped(IBlockState block, Random rand, int fortune)
    {
        return null;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
         return true;
     }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getBlockState() == null)
                return state;

            return ((IExtendedBlockState) state).withProperty(FFSStateProps.FRAME_MODEL, frame.getFakeModel()).withProperty(FFSStateProps.FRAME_STATE, frame.getExtendedBlockState());
        } else {
            return state;
        }
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        int lightValue = 0;

        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && (tile instanceof TileEntityTankFrame)) {
            lightValue = ((TileEntityTankFrame)tile).getLightValue();
        }

        return lightValue;
    }

    @Override
    public boolean isVisuallyOpaque() {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        IBlockState fake_state = ((IExtendedBlockState) state).getValue(FFSStateProps.FRAME_STATE);
        return fake_state != null && fake_state.isOpaqueCube();
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return isOpaqueCube(state);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;

        AbstractTankTile tile = (AbstractTankTile) world.getTileEntity(pos);
        if (tile != null && tile.getMasterValve() != null) {
            AbstractTankValve valve = tile.getMasterValve();
            if (valve.isValid()) {
                if (GenericUtil.isFluidContainer(heldItem))
                    return GenericUtil.fluidContainerHandler(world, valve, player, side);

                player.openGui(FancyFluidStorage.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            frame.onBreak();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }

    @Override
    public float getAmbientOcclusionLightValue(IBlockState state) {
        return super.getAmbientOcclusionLightValue(state);
    }

    /**
     * Fake World Overrides!
     */

    private TileEntityTankFrame getFrameTile(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile != null && tile instanceof TileEntityTankFrame)
            return (TileEntityTankFrame) tile;

        return null;
    }

    private IBlockAccess getFakeBlockAccess(IBlockAccess world, BlockPos pos) {
        TileEntityTankFrame frame = getFrameTile(world, pos);

        return frame != null ? frame.getFakeWorld() : null;
    }

    private World getFakeWorld(World world, BlockPos pos) {
        return (World) getFakeBlockAccess(world, pos);
    }

    private IBlockState getFakeBlockState(IBlockAccess world, BlockPos pos) {
        TileEntityTankFrame frame = getFrameTile(world, pos);
        if(frame == null)
            return getDefaultState();

        return frame.getBlockState();
    }

    private Block getFakeBlock(IBlockAccess world, BlockPos pos) {
        IBlockState state = getFakeBlockState(world, pos);
        return state != null ? state.getBlock() : null;
    }


    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockAccess fakeWorld = getFakeBlockAccess(worldIn, pos);
        IBlockState fakeState = getFakeBlockState(worldIn, pos);
        return (fakeWorld != null && fakeState != null) ? fakeState.shouldSideBeRendered(fakeWorld, pos, side) : super.shouldSideBeRendered(state, worldIn, pos, side);
    }


    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager particleManager) {
        World fakeWorld = getFakeWorld(world, pos);
        Block fakeBlock = getFakeBlock(world, pos);

        return (fakeWorld != null && fakeBlock != null) ? fakeBlock.addDestroyEffects(fakeWorld, pos, particleManager) : super.addDestroyEffects(world, pos, particleManager);
    }

    @Override
    public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager particleManager) {
        World fakeWorld = getFakeWorld(world, target.getBlockPos());
        IBlockState fakeState = getFakeBlockState(world, target.getBlockPos());

        return (fakeWorld != null && fakeState != null && fakeState.getBlock() != null) ? fakeState.getBlock().addHitEffects(fakeState, fakeWorld, target, particleManager) : super.addHitEffects(state, world, target, particleManager);
    }

    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        World fakeWorld = getFakeWorld(world, pos);
        IBlockState fakeState = getFakeBlockState(world, pos);

        return (fakeWorld != null && fakeState != null) ? fakeState.getBlockHardness(fakeWorld, pos) : super.getBlockHardness(state, world, pos);
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        World fakeWorld = getFakeWorld(world, pos);
        Block fakeBlock = getFakeBlock(world, pos);

        return (fakeWorld != null && fakeBlock != null) ? fakeBlock.getExplosionResistance(fakeWorld, pos, exploder, explosion) : super.getExplosionResistance(world, pos, exploder, explosion);
    }

    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        IBlockAccess fakeWorld = getFakeBlockAccess(world, pos);
        Block fakeBlock = getFakeBlock(world, pos);

        return (fakeWorld != null && fakeBlock != null) ? fakeBlock.getFlammability(fakeWorld, pos, face) : super.getFlammability(world, pos, face);
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockAccess fakeWorld = getFakeBlockAccess(world, pos);
        IBlockState fakeState = getFakeBlockState(world, pos);

        return (fakeWorld != null && fakeState != null) ? fakeState.getLightOpacity(fakeWorld, pos) : super.getLightOpacity(state, world, pos);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        World fakeWorld = getFakeWorld(world, pos);
        IBlockState fakeState = getFakeBlockState(world, pos);

        return (fakeWorld != null && fakeState != null) ? fakeState.getBlock().getPickBlock(fakeState, target, fakeWorld, pos, player) : super.getPickBlock(state, target, world, pos, player);
    }

    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        World fakeWorld = getFakeWorld(world, pos);
        IBlockState fakeState = getFakeBlockState(world, pos);

        return (fakeWorld != null && fakeState != null) ? fakeState.getPlayerRelativeBlockHardness(player, fakeWorld, pos) : super.getPlayerRelativeBlockHardness(state, player, world, pos);
    }


    /**
     * Chisel!
     */
    @Optional.Method(modid = "chisel")
    @Override
    public IBlockState getFacade(IBlockAccess world, BlockPos blockPos, EnumFacing enumFacing) {
        TileEntity tile = world.getTileEntity(blockPos);
        if(tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if(frame.getMasterValve() == null)
                return null;

            return frame.getBlockState();
        }
        return world.getBlockState(blockPos);
    }
}
