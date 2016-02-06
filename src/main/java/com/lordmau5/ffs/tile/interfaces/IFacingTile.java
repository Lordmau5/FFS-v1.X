package com.lordmau5.ffs.tile.interfaces;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * Created by Dustin on 22.01.2016.
 */
public interface IFacingTile {

    void setTileFacing(EnumFacing facing);

    default EnumFacing getTileFacing() {
        return null;
    }

    default void saveTileFacingToNBT(NBTTagCompound tag) {
        if(getTileFacing() != null)
            tag.setString("tile_facing", getTileFacing().getName());
    }

    default void readTileFacingFromNBT(NBTTagCompound tag) {
        if(tag.hasKey("tile_facing"))
            setTileFacing(EnumFacing.byName(tag.getString("tile_facing")));
    }

}
