package com.lordmau5.ffs.network.handlers.server;


import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.ffsPacket;
import com.lordmau5.ffs.tile.TileEntityValve;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 07.07.2015.
 */
public class UpdateAutoOutput_Server extends SimpleChannelInboundHandler<ffsPacket.Server.UpdateAutoOutput> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ffsPacket.Server.UpdateAutoOutput msg) throws Exception{
        World world = NetworkHandler.getPlayer(ctx).worldObj;
        if(world != null) {
            TileEntity tile = world.getTileEntity(msg.x, msg.y, msg.z);
            if(tile != null && tile instanceof TileEntityValve) {
                ((TileEntityValve) tile).setAutoOutput(msg.autoOutput);
            }
        }
    }
}
