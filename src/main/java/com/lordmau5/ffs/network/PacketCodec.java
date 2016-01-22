package com.lordmau5.ffs.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec;

/**
 * Created by Dustin on 07.07.2015.
 */
public class PacketCodec extends FMLIndexedMessageToMessageCodec<ffsPacket> {

    int lastDiscriminator = 0;

    public PacketCodec(){
        addPacket(ffsPacket.Server.UpdateAutoOutput.class);
        addPacket(ffsPacket.Server.UpdateTileName.class);
        addPacket(ffsPacket.Server.UpdateFluidLock.class);
    }

    void addPacket(Class<? extends ffsPacket> type) {
        this.addDiscriminator(lastDiscriminator, type);
        lastDiscriminator++;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ffsPacket msg, ByteBuf target) throws Exception{
        msg.encode(target);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, ffsPacket msg){
        msg.decode(source);
    }
}
