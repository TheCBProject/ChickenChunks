package codechicken.chunkloader.block;

import net.minecraft.util.IStringSerializable;

/**
 * Created by covers1624 on 3/5/2016.
 */
public enum EnumChunkLoaderType implements IStringSerializable {
    FULL,
    SPOT,
    FULL_DOWN,
    SPOT_DOWN;

    @Override
    public String getName() {
        return this.name().toLowerCase();
    }

    public boolean isDown() {
        return this == FULL_DOWN || this == SPOT_DOWN;
    }
}
