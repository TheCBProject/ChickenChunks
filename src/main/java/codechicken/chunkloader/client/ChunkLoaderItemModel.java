package codechicken.chunkloader.client;

import codechicken.lib.model.PerspectiveModelState;
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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import net.covers1624.quack.gson.JsonUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.Objects;
import java.util.function.Function;

/**
 * Created by covers1624 on 5/07/2017.
 */
public class ChunkLoaderItemModel implements IGeometryLoader<ChunkLoaderItemModel.Geometry> {

    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new Geometry(
                JsonUtils.getString(jsonObject, "childModel"),
                JsonUtils.getAsPrimitive(jsonObject, "isSpotLoader").getAsBoolean()
        );
    }

    public record Geometry(String childModel, boolean spotLoader) implements IUnbakedGeometry<Geometry> {

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            return new Model(
                    Objects.requireNonNull(baker.getModel(new ResourceLocation(childModel))
                            .bake(baker, spriteGetter, modelState, modelLocation)),
                    spotLoader
            );
        }
    }

    private static class Model extends WrappedItemModel implements IItemRenderer {
        private final boolean spotLoader;

        public Model(BakedModel wrappedModel, boolean spotLoader) {
            super(wrappedModel);
            this.spotLoader = spotLoader;
        }

        @Override
        public void renderItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
            renderWrapped(stack, mStack, getter, packedLight, packedOverlay, false);

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
        public PerspectiveModelState getModelState() {
            return TransformUtils.DEFAULT_BLOCK;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }
    }
}
