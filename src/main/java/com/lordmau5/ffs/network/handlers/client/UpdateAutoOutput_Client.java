package com.lordmau5.ffs.network.handlers.client;

import com.lordmau5.ffs.network.ffsPacket;
import com.lordmau5.ffs.tile.TileEntityValve;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 07.07.2015.
 */
public class UpdateAutoOutput_Client extends SimpleChannelInboundHandler<ffsPacket.Client.UpdateAutoOutput> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ffsPacket.Client.UpdateAutoOutput msg) throws Exception{
        World world = Minecraft.getMinecraft().theWorld;
        TileEntity tile = world.getTileEntity(msg.x, msg.y, msg.z);
        if(tile != null && tile instanceof TileEntityValve)
            ((TileEntityValve)tile).setAutoOutput(msg.autoOutput);
    }
}
