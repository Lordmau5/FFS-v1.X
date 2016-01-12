package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import me.planetguy.remaininmotion.api.event.BlockPreMoveEvent;
import me.planetguy.remaininmotion.api.event.BlockRotateEvent;
import me.planetguy.remaininmotion.api.event.MotionFinalizeEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by Dustin on 21.07.2015.
 */
public class RiMEventHandler {

    @SubscribeEvent
    public void motionFinalize(MotionFinalizeEvent event) {
        TileEntity tile = event.location.entity();
        if(tile instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve) tile;
            if(valve.isMaster()) {
                valve.initiated = true;
                valve.initialWaitTick = 0;
                valve.calculateInside();
                valve.updateCornerFrames();
            }
        }
        else if(tile instanceof TileEntityTankFrame) { // Necessary for the frames to "die"
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            frame.breakFrame();
        }
    }

    @SubscribeEvent
    public void blockPreMove(BlockPreMoveEvent event) {
        TileEntity tile = event.location.entity();
        if(tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            frame.onBreak();
            frame.breakFrame();
        }
    }

    @SubscribeEvent
    public void tileRotation(BlockRotateEvent event) {
        TileEntity tile = event.location.entity();
        if(tile instanceof TileEntityValve) {
            TileEntityValve valve = (TileEntityValve) tile;
            if(valve.isMaster()) {
                //EnumFacing rot = valve.getInside().getRotation(event.axis.getRotation(event.axis));
                //valve.setInside(rot);
            }
        }
        else if(tile instanceof TileEntityTankFrame) { // Necessary for the frames to "die"
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            frame.breakFrame();
        }
    }

}
