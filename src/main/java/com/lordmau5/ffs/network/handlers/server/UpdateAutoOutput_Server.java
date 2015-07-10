package com.lordmau5.ffs.network.handlers.server;


import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.exTanksPacket;
import com.lordmau5.ffs.tile.TileEntityValve;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 07.07.2015.
 */
public class UpdateAutoOutput_Server extends SimpleChannelInboundHandler<exTanksPacket.Server.UpdateAutoOutput> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, exTanksPacket.Server.UpdateAutoOutput msg) throws Exception{
        World world = MinecraftServer.getServer().worldServerForDimension(NetworkHandler.getPlayer(ctx).dimension);
        if(world != null) {
            TileEntity tile = world.getTileEntity(msg.x, msg.y, msg.z);
            if(tile != null && tile instanceof TileEntityValve) {
                ((TileEntityValve) tile).setAutoOutput(msg.autoOutput);
                NetworkHandler.sendPacketToPlayer(new exTanksPacket.Client.UpdateAutoOutput(msg.x, msg.y, msg.z, msg.autoOutput), NetworkHandler.getPlayer(ctx));
            }
        }
    }
}
