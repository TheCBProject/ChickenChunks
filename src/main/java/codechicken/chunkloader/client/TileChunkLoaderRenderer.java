package codechicken.chunkloader.client;

import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.chunkloader.tile.TileSpotLoader;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.TransformingVertexBuilder;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.opengl.GL11;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;

import static codechicken.lib.util.SneakyUtils.none;

public class TileChunkLoaderRenderer extends TileEntityRenderer<TileChunkLoaderBase> {

    public static final RenderType laserType = RenderType.create("lasers", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, RenderType.State.builder()//
            .setTexturingState(new RenderState.TexturingState("disable_lighting", RenderSystem::disableLighting, none()))//
            .setFogState(RenderState.NO_FOG)//
            .createCompositeState(false)//
    );
    public static final RenderType pearlType = CCModelLibrary.getIcos4RenderType(new ResourceLocation("chickenchunks:textures/hedronmap.png"), false);

    public TileChunkLoaderRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(TileChunkLoaderBase tile, float partialTicks, MatrixStack mStack, IRenderTypeBuffer getter, int packedLight, int packedOverlay) {
        Matrix4 mat = new Matrix4(mStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = packedOverlay;
        ccrs.overlay = packedOverlay;

        double rot = ClientUtils.getRenderTime() * 2;
        double height;
        double size;
        double updown = (ClientUtils.getRenderTime() % 50) / 25F;

        updown = (float) Math.sin(updown * 3.141593);
        updown *= 0.2;

        if (tile instanceof TileChunkLoader) {
            TileChunkLoader ctile = (TileChunkLoader) tile;
            rot /= Math.pow(ctile.radius, 0.2);
            height = 0.9;
            size = 0.08;
        } else if (tile instanceof TileSpotLoader) {
            height = 0.5;
            size = 0.05;
        } else {
            return;
        }

        TileChunkLoaderBase.RenderInfo renderInfo = tile.renderInfo;
        double active = (renderInfo.activationCounter) / 20D;
        if (tile.active && renderInfo.activationCounter < 20) {
            active += partialTicks / 20D;
        } else if (!tile.active && renderInfo.activationCounter > 0) {
            active -= partialTicks / 20D;
        }

        if (renderInfo.showLasers) {
            IVertexBuilder builder = getter.getBuffer(laserType);
            drawRays(builder, mat, rot, updown, tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ(), tile.getChunks());
        }
        rot = ClientUtils.getRenderTime() * active / 3F;

        Matrix4 pearlMat = RenderUtils.getMatrix(mat, new Vector3(0.5, height + (updown + 0.3) * active, 0.5), new Rotation(rot, Vector3.Y_POS), size);
        ccrs.brightness = 15728880;
        ccrs.bind(pearlType, getter);
        CCModelLibrary.icosahedron4.render(ccrs, pearlMat);
        ccrs.reset();
    }

    public Point2D.Double findIntersection(Line2D line1, Line2D line2) {
        // calculate differences
        double xD1 = line1.getX2() - line1.getX1();
        double yD1 = line1.getY2() - line1.getY1();
        double xD2 = line2.getX2() - line2.getX1();
        double yD2 = line2.getY2() - line2.getY1();

        double xD3 = line1.getX1() - line2.getX1();
        double yD3 = line1.getY1() - line2.getY1();

        // find intersection Pt between two lines
        Point2D.Double pt = new Point2D.Double(0, 0);
        double div = yD2 * xD1 - xD2 * yD1;
        if (div == 0)//lines are parallel
        {
            return null;
        }
        double ua = (xD2 * yD3 - yD2 * xD3) / div;
        pt.x = line1.getX1() + ua * xD1;
        pt.y = line1.getY1() + ua * yD1;

        if (ptOnLineInSegment(pt, line1) && ptOnLineInSegment(pt, line2)) {
            return pt;
        }

        return null;
    }

    public boolean ptOnLineInSegment(Point2D point, Line2D line) {
        return point.getX() >= Math.min(line.getX1(), line.getX2()) && point.getX() <= Math.max(line.getX1(), line.getX2()) && point.getY() >= Math.min(line.getY1(), line.getY2()) && point.getY() <= Math.max(line.getY1(), line.getY2());
    }

    public void drawRays(IVertexBuilder builder, Matrix4 mat, double rot, double updown, int x, int y, int z, Collection<ChunkPos> chunkSet) {
        int cx = (x >> 4) << 4;
        int cz = (z >> 4) << 4;

        double[] distances = new double[4];

        Point2D.Double center = new Point2D.Double(cx + 8, cz + 8);

        final int[][] coords = new int[][] { { 0, 0 }, { 16, 0 }, { 16, 16 }, { 0, 16 } };

        Point2D.Double[] absRays = new Point2D.Double[4];

        for (int ray = 0; ray < 4; ray++) {
            double rayAngle = Math.toRadians(rot + 90 * ray);
            absRays[ray] = new Point2D.Double(Math.sin(rayAngle), Math.cos(rayAngle));
        }

        Line2D.Double[] rays = new Line2D.Double[] { //
                new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[0].x, center.y + 1600 * absRays[0].y), //
                new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[1].x, center.y + 1600 * absRays[1].y), //
                new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[2].x, center.y + 1600 * absRays[2].y), //
                new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[3].x, center.y + 1600 * absRays[3].y) //
        };

        for (ChunkPos pair : chunkSet) {
            int chunkBlockX = pair.x << 4;
            int chunkBlockZ = pair.z << 4;
            for (int side = 0; side < 4; side++) {
                int[] offset1 = coords[side];
                int[] offset2 = coords[(side + 1) % 4];
                Line2D.Double line1 = new Line2D.Double(chunkBlockX + offset1[0], chunkBlockZ + offset1[1], chunkBlockX + offset2[0], chunkBlockZ + offset2[1]);
                for (int ray = 0; ray < 4; ray++) {
                    Point2D.Double isct = findIntersection(line1, rays[ray]);
                    if (isct == null) {
                        continue;
                    }

                    isct.setLocation(isct.x - center.x, isct.y - center.y);

                    double lenPow2 = isct.x * isct.x + isct.y * isct.y;
                    if (lenPow2 > distances[ray]) {
                        distances[ray] = lenPow2;
                    }
                }
            }
        }

        Matrix4 hozMat = mat.copy();
        hozMat.translate(cx - x + 8, updown + 2, cz - z + 8);
        hozMat.rotate(rot * MathHelper.torad, Vector3.Y_POS);
        //Horizontal lines.
        for (int ray = 0; ray < 4; ray++) {
            distances[ray] = Math.sqrt(distances[ray]);
            hozMat.rotate(90 * MathHelper.torad, Vector3.Y_POS);
            renderCuboid(builder, hozMat, new Cuboid6(0, -0.05, -0.05, distances[ray], 0.05, 0.05), 0.9F, 0F, 0F, 1F);
        }

        //Vertical lines.
        Matrix4 vertMat = mat.copy();
        vertMat.translate(cx - x + 8, 0, cz - z + 8);
        for (int ray = 0; ray < 4; ray++) {
            Matrix4 rayMat = vertMat.copy();
            rayMat.translate(absRays[ray].x * distances[ray], 0, absRays[ray].y * distances[ray]);
            renderCuboid(builder, rayMat, new Cuboid6(-0.05, 0, -0.05, 0.05, 256, 0.05), 0.9F, 0F, 0F, 1F);
        }

        //Line to center.
        double toCenter = Math.sqrt((cx + 7.5 - x) * (cx + 7.5 - x) + 0.8 * 0.8 + (cz + 7.5 - z) * (cz + 7.5 - z));
        Matrix4 centerMat = mat.copy();
        centerMat.translate(0.5, 1.2 + updown, 0.5);
        centerMat.rotate(((float) (Math.atan2((cx + 7.5 - x), (cz + 7.5 - z)) * 180 / 3.1415) + 90) * MathHelper.torad, Vector3.Y_POS);
        centerMat.rotate(((float) (-Math.asin(0.8 / toCenter) * 180 / 3.1415)) * MathHelper.torad, Vector3.Z_POS);
        renderCuboid(builder, centerMat, new Cuboid6(-toCenter, -0.03, -0.03, 0, 0.03, 0.03), 0, 0.9F, 0, 1);
    }

    private static void renderCuboid(IVertexBuilder builder, Matrix4 mat, Cuboid6 cuboid, float r, float g, float b, float a) {
        RenderUtils.bufferCuboidSolid(new TransformingVertexBuilder(builder, mat), cuboid, r, g, b, a);
    }
}
