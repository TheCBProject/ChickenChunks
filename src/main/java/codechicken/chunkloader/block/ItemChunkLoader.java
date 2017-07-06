package codechicken.chunkloader.block;

import codechicken.chunkloader.ChickenChunks;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemChunkLoader extends ItemBlock {

    public ItemChunkLoader(Block block) {
        super(block);
        setHasSubtypes(true);
        setUnlocalizedName(ChickenChunks.MOD_ID + ":chunk_loader");
    }

    @Override
    public int getMetadata(int meta) {
        return meta;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "|" + stack.getItemDamage();
    }
}
