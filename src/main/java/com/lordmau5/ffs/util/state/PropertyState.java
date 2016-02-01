package com.lordmau5.ffs.util.state;

import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * This class was created by <williewillus>.
 *
 * Created by Dustin on 11.01.2016.
 */
public class PropertyState implements IUnlistedProperty<IBlockState> {

    private final String name;

    public PropertyState(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(IBlockState value) {
        return true;
    }

    @Override
    public Class<IBlockState> getType() {
        return IBlockState.class;
    }

    @Override
    public String valueToString(IBlockState value) {
        return value.toString();
    }

}