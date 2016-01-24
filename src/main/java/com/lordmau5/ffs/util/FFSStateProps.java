package com.lordmau5.ffs.util;

import com.lordmau5.ffs.util.state.PropertyModel;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.EnumFacing;

/**
 * Created by Dustin on 11.01.2016.
 */
public class FFSStateProps {

    public static final PropertyModel FRAME_MODEL = new PropertyModel("frame_model");

    public static final PropertyBool TILE_VALID = PropertyBool.create("tile_valid");
    public static final PropertyBool TILE_MASTER = PropertyBool.create("tile_master");
    public static final PropertyEnum<EnumFacing> TILE_INSIDE = PropertyEnum.create("tile_inside", EnumFacing.class);
    public static final PropertyEnum<EnumFacing.Axis> TILE_INSIDE_DUAL = PropertyEnum.create("tile_inside", EnumFacing.Axis.class);

}
