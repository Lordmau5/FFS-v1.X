package com.lordmau5.ffs.network;

import com.lordmau5.ffs.tile.abstracts.AbstractTankTile;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import com.lordmau5.ffs.tile.valves.TileEntityFluidValve;
import com.lordmau5.ffs.tile.valves.TileEntityMetaphaser;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/**
 * Created by Dustin on 07.07.2015.
 */
public abstract class FFSPacket {
    public abstract void encode(ByteBuf buffer);

    public abstract void decode(ByteBuf buffer);

    public static abstract class Client {
    }

    public static class Server {
        public static class UpdateAutoOutput extends FFSPacket {
            public BlockPos pos;
            public boolean autoOutput;

            public UpdateAutoOutput(){
            }

            public UpdateAutoOutput(TileEntityFluidValve valve) {
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

        public static class UpdateMetaphaserMode extends FFSPacket {
            public BlockPos pos;
            public boolean extractMode;

            public UpdateMetaphaserMode(){
            }

            public UpdateMetaphaserMode(TileEntityMetaphaser metaphaser) {
                this.pos = metaphaser.getPos();
                this.extractMode = metaphaser.getExtract();
            }

            @Override
            public void encode(ByteBuf buffer) {
                buffer.writeInt(this.pos.getX());
                buffer.writeInt(this.pos.getY());
                buffer.writeInt(this.pos.getZ());
                buffer.writeBoolean(this.extractMode);
            }

            @Override
            public void decode(ByteBuf buffer) {
                this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
                this.extractMode = buffer.readBoolean();
            }
        }

        public static class UpdateTileName extends FFSPacket {
            public BlockPos pos;
            public String name;

            public UpdateTileName(){
            }

            public UpdateTileName(AbstractTankTile tankTile, String name) {
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

        public static class UpdateFluidLock extends FFSPacket {
            public BlockPos pos;
            public boolean fluidLock;

            public UpdateFluidLock(){
            }

            public UpdateFluidLock(AbstractTankValve valve) {
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
