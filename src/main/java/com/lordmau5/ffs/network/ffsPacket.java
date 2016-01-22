package com.lordmau5.ffs.network;

import com.lordmau5.ffs.tile.ITankTile;
import com.lordmau5.ffs.tile.ITankValve;
import com.lordmau5.ffs.tile.TileEntityValve;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/**
 * Created by Dustin on 07.07.2015.
 */
public abstract class ffsPacket {
    public abstract void encode(ByteBuf buffer);

    public abstract void decode(ByteBuf buffer);

    public static abstract class Client {
    }

    public static class Server {
        public static class UpdateAutoOutput extends ffsPacket {
            public BlockPos pos;
            public boolean autoOutput;

            public UpdateAutoOutput(){
            }

            public UpdateAutoOutput(TileEntityValve valve) {
                this.pos = valve.getPos();
                this.autoOutput = valve.getAutoOutput();
            }

            @Override
            public void encode(ByteBuf buffer) {
                buffer.writeInt(this.pos.getX());
                buffer.writeInt(this.pos.getY());
                buffer.writeInt(this.pos.getZ());
                buffer.writeBoolean(this.autoOutput);
            }

            @Override
            public void decode(ByteBuf buffer) {
                this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
                this.autoOutput = buffer.readBoolean();
            }
        }

        public static class UpdateTileName extends ffsPacket {
            public BlockPos pos;
            public String name;

            public UpdateTileName(){
            }

            public UpdateTileName(ITankTile tankTile, String name) {
                this.pos = tankTile.getPos();
                this.name = name;
            }

            @Override
            public void encode(ByteBuf buffer) {
                buffer.writeInt(this.pos.getX());
                buffer.writeInt(this.pos.getY());
                buffer.writeInt(this.pos.getZ());
                ByteBufUtils.writeUTF8String(buffer, this.name);
            }

            @Override
            public void decode(ByteBuf buffer) {
                this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
                this.name = ByteBufUtils.readUTF8String(buffer);
            }
        }

        public static class UpdateFluidLock extends ffsPacket {
            public BlockPos pos;
            public boolean fluidLock;

            public UpdateFluidLock(){
            }

            public UpdateFluidLock(ITankValve valve) {
                this.pos = valve.getPos();
                this.fluidLock = valve.getTankConfig().isFluidLocked();
            }

            @Override
            public void encode(ByteBuf buffer) {
                buffer.writeInt(this.pos.getX());
                buffer.writeInt(this.pos.getY());
                buffer.writeInt(this.pos.getZ());
                buffer.writeBoolean(this.fluidLock);
            }

            @Override
            public void decode(ByteBuf buffer) {
                this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
                this.fluidLock = buffer.readBoolean();
            }
        }
    }
}
