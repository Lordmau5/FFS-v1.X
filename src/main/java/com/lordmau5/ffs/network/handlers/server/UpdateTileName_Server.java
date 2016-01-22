package com.lordmau5.ffs.network.handlers.server;


import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.ffsPacket;
import com.lordmau5.ffs.tile.ITankTile;
import com.lordmau5.ffs.tile.ifaces.INameableTile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 07.07.2015.
 */
public class UpdateTileName_Server extends SimpleChannelInboundHandler<ffsPacket.Server.UpdateTileName> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ffsPacket.Server.UpdateTileName msg) throws Exception{
        World world = NetworkHandler.getPlayer(ctx).worldObj;
        if(world != null) {
            TileEntity w_Tile = world.getTileEntity(msg.pos);
            if(w_Tile != null && w_Tile instanceof ITankTile && w_Tile instanceof INameableTile) {
                ITankTile tile = (ITankTile) w_Tile;
                ((INameableTile)tile).setTileName(msg.name);
                tile.markForUpdate();
            }
        }
    }
}
