package com.lordmau5.ffs.network.handlers.server;


import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.tile.TileEntityTankValve;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 07.07.2015.
 */
public class UpdateFluidLock_Server extends SimpleChannelInboundHandler<FFSPacket.Server.UpdateFluidLock> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FFSPacket.Server.UpdateFluidLock msg) throws Exception{
        World world = NetworkHandler.getPlayer(ctx).worldObj;
        if(world != null) {
            TileEntity tile = world.getTileEntity(msg.pos);
            if(tile != null && tile instanceof TileEntityTankValve) {
                TileEntityTankValve valve = (TileEntityTankValve) tile;
                valve.toggleFluidLock(msg.fluidLock);
            }
        }
    }
}
