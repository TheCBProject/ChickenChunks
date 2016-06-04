package codechicken.chunkloader.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static codechicken.chunkloader.init.ModBlocks.blockChunkLoader;

/**
 * Created by covers1624 on 3/5/2016.
 */
public class ModRecipes {

    public static void init() {
        GameRegistry.addRecipe(new ItemStack(blockChunkLoader, 1, 0), " p ", "ggg", "gEg", 'p', Items.ENDER_PEARL, 'g', Items.GOLD_INGOT, 'd', Items.DIAMOND, 'E', Blocks.ENCHANTING_TABLE);

        GameRegistry.addRecipe(new ItemStack(blockChunkLoader, 10, 1), "ppp", "pcp", "ppp", 'p', Items.ENDER_PEARL, 'c', new ItemStack(blockChunkLoader, 1, 0));
    }

}
