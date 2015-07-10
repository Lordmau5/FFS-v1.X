package com.lordmau5.ffs.network;

import cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by Dustin on 07.07.2015.
 */
public class PacketCodec extends FMLIndexedMessageToMessageCodec<exTanksPacket> {

    int lastDiscriminator = 0;

    public PacketCodec(){
        addPacket(exTanksPacket.Client.UpdateAutoOutput.class);

        addPacket(exTanksPacket.Server.UpdateAutoOutput.class);
    }

    void addPacket(Class<? extends exTanksPacket> type) {
        this.addDiscriminator(lastDiscriminator, type);
        lastDiscriminator++;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, exTanksPacket msg, ByteBuf target) throws Exception{
        msg.encode(target);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, exTanksPacket msg){
        msg.decode(source);
    }
}
