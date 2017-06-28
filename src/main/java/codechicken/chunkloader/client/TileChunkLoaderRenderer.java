package codechicken.chunkloader.client;

import codechicken.chunkloader.proxy.ProxyClient;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.chunkloader.tile.TileSpotLoader;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.ChunkPos;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;

import static net.minecraft.client.renderer.GlStateManager.*;

public class TileChunkLoaderRenderer extends TileEntitySpecialRenderer<TileChunkLoaderBase> {

    public static class RenderInfo {

        public int activationCounter;
        public boolean showLasers;

        public void update(TileChunkLoaderBase chunkLoader) {
            if (activationCounter < 20 && chunkLoader.active) {
                activationCounter++;
            } else if (activationCounter > 0 && !chunkLoader.active) {
                activationCounter--;
            }
        }
    }

    @Override
    public void renderTileEntityAt(TileChunkLoaderBase tile, double x, double y, double z, float partialTicks, int destroyStage) {
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.setBrightness(tile.getWorld(), tile.getPos());
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

        RenderInfo renderInfo = tile.renderInfo;
        double active = (renderInfo.activationCounter) / 20D;
        if (tile.active && renderInfo.activationCounter < 20) {
            active += partialTicks / 20D;
        } else if (!tile.active && renderInfo.activationCounter > 0) {
            active -= partialTicks / 20D;
        }

        if (renderInfo.showLasers) {
            disableTexture2D();
            disableLighting();
            disableFog();
            drawRays(x, y, z, rot, updown, tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ(), tile.getChunks());
            enableTexture2D();
            enableLighting();
            enableFog();
        }
        rot = ClientUtils.getRenderTime() * active / 3F;

        Matrix4 pearlMat = RenderUtils.getMatrix(new Vector3(x + 0.5, y + height + (updown + 0.3) * active, z + 0.5), new Rotation(rot, new Vector3(0, 1, 0)), size);
        disableLighting();
        pushMatrix();
        TextureUtils.changeTexture("chickenchunks:textures/hedronmap.png");
        ccrs.startDrawing(7, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        CCModelLibrary.icosahedron7.render(ccrs, pearlMat);
        ccrs.draw();
        popMatrix();
        enableLighting();
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

    public void drawRays(double d, double d1, double d2, double rotationAngle, double updown, int x, int y, int z, Collection<ChunkPos> chunkSet) {
        int cx = (x >> 4) << 4;
        int cz = (z >> 4) << 4;

        pushMatrix();
        translate(d + cx - x + 8, d1 + updown + 2, d2 + cz - z + 8);
        rotate((float) rotationAngle, 0, 1, 0);

        double[] distances = new double[4];

        Point2D.Double center = new Point2D.Double(cx + 8, cz + 8);

        final int[][] coords = new int[][] { { 0, 0 }, { 16, 0 }, { 16, 16 }, { 0, 16 } };

        Point2D.Double[] absRays = new Point2D.Double[4];

        for (int ray = 0; ray < 4; ray++) {
            double rayAngle = Math.toRadians(rotationAngle + 90 * ray);
            absRays[ray] = new Point2D.Double(Math.sin(rayAngle), Math.cos(rayAngle));
        }

        Line2D.Double[] rays = new Line2D.Double[] { new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[0].x, center.y + 1600 * absRays[0].y), new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[1].x, center.y + 1600 * absRays[1].y), new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[2].x, center.y + 1600 * absRays[2].y), new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[3].x, center.y + 1600 * absRays[3].y) };

        for (ChunkPos pair : chunkSet) {
            int chunkBlockX = pair.chunkXPos << 4;
            int chunkBlockZ = pair.chunkZPos << 4;
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
        color(0.9F, 0F, 0F, 1F);
        for (int ray = 0; ray < 4; ray++) {
            distances[ray] = Math.sqrt(distances[ray]);
            rotate(90, 0, 1, 0);
            renderCuboid(new Cuboid6(0, -0.05, -0.05, distances[ray], 0.05, 0.05));
        }
        popMatrix();

        pushMatrix();
        translate(d + cx - x + 8, d1 - y, d2 + cz - z + 8);
        for (int ray = 0; ray < 4; ray++) {
            pushMatrix();
            translate(absRays[ray].x * distances[ray], 0, absRays[ray].y * distances[ray]);
            renderCuboid(new Cuboid6(-0.05, 0, -0.05, 0.05, 256, 0.05));
            popMatrix();
        }
        popMatrix();

        double toCenter = Math.sqrt((cx + 7.5 - x) * (cx + 7.5 - x) + 0.8 * 0.8 + (cz + 7.5 - z) * (cz + 7.5 - z));
        pushMatrix();
        color(0, 0.9F, 0, 1);
        translate(d + 0.5, d1 + 1.2 + updown, d2 + 0.5);
        rotate((float) (Math.atan2((cx + 7.5 - x), (cz + 7.5 - z)) * 180 / 3.1415) + 90, 0, 1, 0);
        rotate((float) (-Math.asin(0.8 / toCenter) * 180 / 3.1415), 0, 0, 1);
        renderCuboid(new Cuboid6(-toCenter, -0.03, -0.03, 0, 0.03, 0.03));
        popMatrix();
    }

    private static void renderCuboid(Cuboid6 cuboid) {
        if (ProxyClient.lasersRenderHollow) {
            RenderUtils.drawCuboidOutline(cuboid);
        } else {
            RenderUtils.drawCuboidSolid(cuboid);
        }
    }
}
