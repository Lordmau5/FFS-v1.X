package com.lordmau5.ffs.network;

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

            public UpdateAutoOutput(TileEntityValve valve, boolean autoOutput) {
                this.pos = valve.getPos();
                this.autoOutput = autoOutput;
            }

            public UpdateAutoOutput(BlockPos pos, boolean autoOutput) {
                this.pos = pos;
                this.autoOutput = autoOutput;
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

        public static class UpdateValveName extends ffsPacket {
            public BlockPos pos;
            public String name;

            public UpdateValveName(){
            }

            public UpdateValveName(TileEntityValve valve, String name) {
                this.pos = valve.getPos();
                this.name = name;
            }

            public UpdateValveName(BlockPos pos, String name) {
                this.pos = pos;
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
    }
}
