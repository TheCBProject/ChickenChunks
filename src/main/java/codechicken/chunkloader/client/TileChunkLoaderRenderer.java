package codechicken.chunkloader.client;

import codechicken.chunkloader.TileChunkLoader;
import codechicken.chunkloader.TileChunkLoaderBase;
import codechicken.chunkloader.TileSpotLoader;
import codechicken.core.ClientUtils;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import org.lwjgl.opengl.GL11;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;

public class TileChunkLoaderRenderer extends TileEntitySpecialRenderer<TileChunkLoaderBase> {
    public static class RenderInfo {
        int activationCounter;
        boolean showLasers;

        public void update(TileChunkLoaderBase chunkLoader) {
            if (activationCounter < 20 && chunkLoader.active) {
                activationCounter++;
            } else if (activationCounter > 0 && !chunkLoader.active) {
                activationCounter--;
            }
        }
    }

    @Override
    public void renderTileEntityAt(TileChunkLoaderBase tile, double d, double d1, double d2, float partialTicks, int destroyStage) {
        CCRenderState.reset();
        CCRenderState.setBrightness(tile.getWorld(), tile.getPos());

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
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            drawRays(d, d1, d2, rot, updown, tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ(), tile.getChunks());
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_FOG);
        }
        rot = ClientUtils.getRenderTime() * active / 3F;

        Matrix4 pearlMat = CCModelLibrary.getRenderMatrix(new Vector3(d + 0.5, d1 + height + (updown + 0.3) * active, d2 + 0.5), new Rotation(rot, new Vector3(0, 1, 0)), size);

        GL11.glDisable(GL11.GL_LIGHTING);
        CCRenderState.changeTexture("chickenchunks:textures/hedronmap.png");
        CCRenderState.startDrawing(4, DefaultVertexFormats.POSITION_TEX);
        CCModelLibrary.icosahedron4.render(pearlMat);
        CCRenderState.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
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
        return point.getX() >= Math.min(line.getX1(), line.getX2()) &&
                point.getX() <= Math.max(line.getX1(), line.getX2()) &&
                point.getY() >= Math.min(line.getY1(), line.getY2()) &&
                point.getY() <= Math.max(line.getY1(), line.getY2());
    }

    public void drawRays(double d, double d1, double d2, double rotationAngle, double updown, int x, int y, int z, Collection<ChunkCoordIntPair> chunkSet) {
        int cx = (x >> 4) << 4;
        int cz = (z >> 4) << 4;

        GL11.glPushMatrix();
        GL11.glTranslated(d + cx - x + 8, d1 + updown + 2, d2 + cz - z + 8);
        GL11.glRotatef((float) rotationAngle, 0, 1, 0);

        double[] distances = new double[4];

        Point2D.Double center = new Point2D.Double(cx + 8, cz + 8);

        final int[][] coords = new int[][] { { 0, 0 }, { 16, 0 }, { 16, 16 }, { 0, 16 } };

        Point2D.Double[] absRays = new Point2D.Double[4];

        for (int ray = 0; ray < 4; ray++) {
            double rayAngle = Math.toRadians(rotationAngle + 90 * ray);
            absRays[ray] = new Point2D.Double(Math.sin(rayAngle), Math.cos(rayAngle));
        }

        Line2D.Double[] rays = new Line2D.Double[] { new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[0].x, center.y + 1600 * absRays[0].y), new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[1].x, center.y + 1600 * absRays[1].y), new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[2].x, center.y + 1600 * absRays[2].y), new Line2D.Double(center.x, center.y, center.x + 1600 * absRays[3].x, center.y + 1600 * absRays[3].y) };

        for (ChunkCoordIntPair pair : chunkSet) {
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

        GL11.glColor4d(0.9, 0, 0, 1);
        for (int ray = 0; ray < 4; ray++) {
            distances[ray] = Math.sqrt(distances[ray]);
            GL11.glRotatef(90, 0, 1, 0);
            RenderAABB.renderAABB(new AxisAlignedBB(0, -0.05, -0.05, distances[ray], 0.05, 0.05));
        }
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(d + cx - x + 8, d1 - y, d2 + cz - z + 8);
        for (int ray = 0; ray < 4; ray++) {
            GL11.glPushMatrix();
            GL11.glTranslated(absRays[ray].x * distances[ray], 0, absRays[ray].y * distances[ray]);
            RenderAABB.renderAABB(new AxisAlignedBB(-0.05, 0, -0.05, 0.05, 256, 0.05));
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();

        double toCenter = Math.sqrt((cx + 7.5 - x) * (cx + 7.5 - x) + 0.8 * 0.8 + (cz + 7.5 - z) * (cz + 7.5 - z));

        GL11.glPushMatrix();
        GL11.glColor4d(0, 0.9, 0, 1);
        GL11.glTranslated(d + 0.5, d1 + 1.2 + updown, d2 + 0.5);
        GL11.glRotatef((float) (Math.atan2((cx + 7.5 - x), (cz + 7.5 - z)) * 180 / 3.1415) + 90, 0, 1, 0);
        GL11.glRotatef((float) (-Math.asin(0.8 / toCenter) * 180 / 3.1415), 0, 0, 1);
        RenderAABB.renderAABB(new AxisAlignedBB(-toCenter, -0.03, -0.03, 0, 0.03, 0.03));
        GL11.glPopMatrix();
    }
}
