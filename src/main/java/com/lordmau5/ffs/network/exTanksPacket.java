package com.lordmau5.ffs.network;

import com.lordmau5.ffs.tile.TileEntityValve;
import io.netty.buffer.ByteBuf;

/**
 * Created by Dustin on 07.07.2015.
 */
public abstract class exTanksPacket {
    public abstract void encode(ByteBuf buffer);

    public abstract void decode(ByteBuf buffer);

    public static abstract class Client {
        public static class UpdateAutoOutput extends exTanksPacket {
            public int x, y, z;
            public boolean autoOutput;

            public UpdateAutoOutput(){
            }

            public UpdateAutoOutput(TileEntityValve valve, boolean autoOutput) {
                this.x = valve.xCoord;
                this.y = valve.yCoord;
                this.z = valve.zCoord;
                this.autoOutput = autoOutput;
            }

            public UpdateAutoOutput(int x, int y, int z, boolean autoOutput) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.autoOutput = autoOutput;
            }

            @Override
            public void encode(ByteBuf buffer) {
                buffer.writeInt(this.x);
                buffer.writeInt(this.y);
                buffer.writeInt(this.z);
                buffer.writeBoolean(this.autoOutput);
            }

            @Override
            public void decode(ByteBuf buffer) {
                this.x = buffer.readInt();
                this.y = buffer.readInt();
                this.z = buffer.readInt();
                this.autoOutput = buffer.readBoolean();
            }
        }
    }

    public static class Server {
        public static class UpdateAutoOutput extends exTanksPacket {
            public int x, y, z;
            public boolean autoOutput;

            public UpdateAutoOutput(){
            }

            public UpdateAutoOutput(TileEntityValve valve, boolean autoOutput) {
                this.x = valve.xCoord;
                this.y = valve.yCoord;
                this.z = valve.zCoord;
                this.autoOutput = autoOutput;
            }

            public UpdateAutoOutput(int x, int y, int z, boolean autoOutput) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.autoOutput = autoOutput;
            }

            @Override
            public void encode(ByteBuf buffer) {
                buffer.writeInt(this.x);
                buffer.writeInt(this.y);
                buffer.writeInt(this.z);
                buffer.writeBoolean(this.autoOutput);
            }

            @Override
            public void decode(ByteBuf buffer) {
                this.x = buffer.readInt();
                this.y = buffer.readInt();
                this.z = buffer.readInt();
                this.autoOutput = buffer.readBoolean();
            }
        }
    }
}
