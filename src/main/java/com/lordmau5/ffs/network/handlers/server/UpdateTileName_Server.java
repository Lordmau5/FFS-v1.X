package com.lordmau5.ffs.network.handlers.server;


import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.interfaces.INameableTile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Dustin on 07.07.2015.
 */
public class UpdateTileName_Server extends SimpleChannelInboundHandler<FFSPacket.Server.UpdateTileName> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FFSPacket.Server.UpdateTileName msg) throws Exception{
        World world = NetworkHandler.getPlayer(ctx).worldObj;
        if(world != null) {
            TileEntity w_Tile = world.getTileEntity(msg.pos);
            if(w_Tile != null && w_Tile instanceof AbstractTankTile && w_Tile instanceof INameableTile) {
                AbstractTankTile tile = (AbstractTankTile) w_Tile;
                ((INameableTile)tile).setTileName(msg.name);
                tile.setNeedsUpdate(AbstractTankTile.UpdateType.STATE);
            }
        }
    }
}
