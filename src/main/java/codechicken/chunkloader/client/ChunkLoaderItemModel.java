package codechicken.chunkloader.client;

import codechicken.lib.model.bakedmodels.WrappedItemModel;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.util.TransformUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.model.IModelState;

import java.util.function.Supplier;

import static net.minecraft.client.renderer.GlStateManager.*;

/**
 * Created by covers1624 on 5/07/2017.
 */
public class ChunkLoaderItemModel extends WrappedItemModel implements IItemRenderer {

    public ChunkLoaderItemModel(Supplier<ModelResourceLocation> wrappedModel) {
        super(wrappedModel);
    }

    @Override
    public void renderItem(ItemStack stack, TransformType transformType) {
        renderWrapped(stack);

        double rot = ClientUtils.getRenderTime() / 3F;
        double height;
        double size;
        double updown = (float) Math.sin(((ClientUtils.getRenderTime() % 50) / 25F) * 3.141593) * 0.2;

        if (stack.getMetadata() == 0) {
            height = 0.9;
            size = 0.08;
        } else {
            height = 0.5;
            size = 0.05;
        }

        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();

        Matrix4 pearlMat = RenderUtils.getMatrix(new Vector3(0.5, height + (updown + 0.3), 0.5), new Rotation(rot, new Vector3(0, 1, 0)), size);

        disableLighting();
        pushMatrix();
        TextureUtils.changeTexture("chickenchunks:textures/hedronmap.png");
        ccrs.startDrawing(4, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        CCModelLibrary.icosahedron4.render(ccrs, pearlMat);
        ccrs.draw();
        popMatrix();
        enableLighting();
    }

    @Override
    public IModelState getTransforms() {
        return TransformUtils.DEFAULT_BLOCK;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }
}
