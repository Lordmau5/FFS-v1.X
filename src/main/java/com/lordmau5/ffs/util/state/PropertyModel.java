package com.lordmau5.ffs.util.state;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * This class was created by <williewillus>.
 *
 * Created by Dustin on 11.01.2016.
 */
public class PropertyModel implements IUnlistedProperty<IBakedModel> {

    private final String name;

    public PropertyModel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(IBakedModel value) {
        return true;
    }

    @Override
    public Class<IBakedModel> getType() {
        return IBakedModel.class;
    }

    @Override
    public String valueToString(IBakedModel value) {
        return value.toString();
    }

}