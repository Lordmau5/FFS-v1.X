package com.lordmau5.ffs.network;

import com.lordmau5.ffs.network.handlers.server.UpdateAutoOutput_Server;
import com.lordmau5.ffs.network.handlers.server.UpdateFluidLock_Server;
import com.lordmau5.ffs.network.handlers.server.UpdateTileName_Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumMap;

/**
 * Created by Dustin on 07.07.2015.
 */
public class NetworkHandler {
    private static EnumMap<Side, FMLEmbeddedChannel> channels;

    public static void registerChannels(Side side){
        channels = NetworkRegistry.INSTANCE.newChannel("ffs", new PacketCodec());

        ChannelPipeline pipeline = channels.get(Side.SERVER).pipeline();
        String targetName = channels.get(Side.SERVER).findChannelHandlerNameForType(PacketCodec.class);

        pipeline.addAfter(targetName, "UpdateAutoOutput_Server", new UpdateAutoOutput_Server());
        pipeline.addAfter(targetName, "UpdateTileName_Server", new UpdateTileName_Server());
        pipeline.addAfter(targetName, "UpdateFluidLock_Server", new UpdateFluidLock_Server());

        if(side.isClient()){
            registerClientHandlers();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerClientHandlers(){
        ChannelPipeline pipeline = channels.get(Side.CLIENT).pipeline();
        String targetName = channels.get(Side.CLIENT).findChannelHandlerNameForType(PacketCodec.class);

        // pipeline.addAfter(targetName, "IDENTIFIER_NAME", new CLIENT_CLASS());
    }

    public static Packet getProxyPacket(FFSPacket packet){
        return channels.get(FMLCommonHandler.instance().getEffectiveSide()).generatePacketFrom(packet);
    }

    public static void sendPacketToPlayer(FFSPacket packet, EntityPlayer player){
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channel.writeOutbound(packet);
    }

    public static void sendPacketToAllPlayers(FFSPacket packet){
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
        channel.writeOutbound(packet);
    }

    public static void sendPacketToServer(FFSPacket packet){
        FMLEmbeddedChannel channel = channels.get(Side.CLIENT);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        channel.writeOutbound(packet);
    }

    public static EntityPlayerMP getPlayer(ChannelHandlerContext ctx){
        return ((NetHandlerPlayServer) ctx.channel().attr(NetworkRegistry.NET_HANDLER).get()).playerEntity;
    }
}
