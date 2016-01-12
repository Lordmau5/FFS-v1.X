package com.lordmau5.ffs.util;

import com.lordmau5.ffs.util.state.PropertyState;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.EnumFacing;

/**
 * Created by Dustin on 11.01.2016.
 */
public class FFSStateProps {

    public static final PropertyState FRAME_STATE = new PropertyState("frame_state");

    public static final PropertyBool VALVE_VALID = PropertyBool.create("valve_valid");
    public static final PropertyBool VALVE_MASTER = PropertyBool.create("valve_master");
    public static final PropertyEnum<EnumFacing.Axis> VALVE_INSIDE = PropertyEnum.create("valve_inside", EnumFacing.Axis.class);

}
