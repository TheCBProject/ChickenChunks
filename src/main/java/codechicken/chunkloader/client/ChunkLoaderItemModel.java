package codechicken.chunkloader.client;

import codechicken.lib.model.bakedmodels.WrappedItemModel;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.util.TransformUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.TransformationMatrix;

/**
 * Created by covers1624 on 5/07/2017.
 */
public class ChunkLoaderItemModel extends WrappedItemModel implements IItemRenderer {

    private final boolean spotLoader;

    public ChunkLoaderItemModel(IBakedModel wrappedModel, boolean spotLoader) {
        super(wrappedModel);
        this.spotLoader = spotLoader;
    }

    @Override
    public void renderItem(ItemStack stack, TransformType transformType, MatrixStack mStack, IRenderTypeBuffer getter, int packedLight, int packedOverlay) {
        renderWrapped(stack, transformType, mStack, getter, packedLight, packedOverlay, false);

        double rot = ClientUtils.getRenderTime() / 6F;
        double height;
        double size;

        if (!spotLoader) {
            height = 0.9;
            size = 0.08;
        } else {
            height = 0.55;
            size = 0.05;
        }

        CCRenderState ccrs = CCRenderState.instance();
        ccrs.brightness = packedLight;
        ccrs.overlay = packedOverlay;
        ccrs.reset();

        Matrix4 pearlMat = RenderUtils.getMatrix(new Matrix4(mStack), new Vector3(0.5, height, 0.5), new Rotation(rot, Vector3.Y_POS), size);
        ccrs.brightness = 15728880;
        ccrs.bind(TileChunkLoaderRenderer.pearlType, getter);
        CCModelLibrary.icosahedron4.render(ccrs, pearlMat);
        ccrs.reset();
    }

    @Override
    public ImmutableMap<TransformType, TransformationMatrix> getTransforms() {
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

    @Override
    public boolean isSideLit() {
        return true;
    }
}
