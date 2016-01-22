package com.lordmau5.ffs.tile.ifaces;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by Dustin on 22.01.2016.
 */
public interface INameableTile {

    void setTileName(String name);

    default String getTileName() {
        return "";
    }

    default void saveTileNameToNBT(NBTTagCompound tag) {
        if(!getTileName().isEmpty())
            tag.setString("tile_name", getTileName());
    }

    default void readTileNameFromNBT(NBTTagCompound tag) {
        if(tag.hasKey("tile_name"))
            setTileName(tag.getString("tile_name"));
    }

}
