package codechicken.chunkloader.client;

import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.ExtentsConsumer;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 7/7/26.
 */
public record ChunkLoaderPearlItemRenderer(double height, double size) implements NoDataSpecialModelRenderer {

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        Matrix4 mat = new Matrix4();
        mat.translate(0.5, height, 0.5);
        mat.scale(size);
        var ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.bind(new ExtentsConsumer(output), DefaultVertexFormat.POSITION);
        CCModelLibrary.icosahedron4.render(ccrs, mat);
    }

    @Override
    public void submit(ItemDisplayContext displayContext, PoseStack pose, SubmitNodeCollector collector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        collector.cc$submitCCRS(pose, TileChunkLoaderRenderer.pearlType, (mat, ccrs) -> {
            double rot = ClientUtils.getRenderTime() / 6F;
            ccrs.brightness = 15728880;
            ccrs.overlay = packedOverlay;

            CCModelLibrary.icosahedron4.render(
                    ccrs,
                    RenderUtils.getMatrix(mat, new Vector3(0.5, height, 0.5), new Rotation(rot, Vector3.Y_POS), size)
            );
        });
    }

    public record Unbaked(double height, double size) implements SpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                        Codec.DOUBLE.fieldOf("height").forGetter(Unbaked::height),
                        Codec.DOUBLE.fieldOf("size").forGetter(Unbaked::size)
                ).apply(builder, Unbaked::new)
        );

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new ChunkLoaderPearlItemRenderer(height, size);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

    }
}
