package codechicken.chunkloader.block.property;

import codechicken.chunkloader.block.EnumChunkLoaderType;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import net.minecraft.block.properties.PropertyEnum;

import java.util.Collection;

/**
 * Created by covers1624 on 3/5/2016.
 */
public class PropertyChunkLoaderType extends PropertyEnum<EnumChunkLoaderType> {

    protected PropertyChunkLoaderType(String name, Collection<EnumChunkLoaderType> allowedValues) {
        super(name, EnumChunkLoaderType.class, allowedValues);
    }

    public static PropertyChunkLoaderType create(String name) {
        return new PropertyChunkLoaderType(name, Collections2.filter(Lists.newArrayList(EnumChunkLoaderType.values()), Predicates.<EnumChunkLoaderType>alwaysTrue()));
    }

}
