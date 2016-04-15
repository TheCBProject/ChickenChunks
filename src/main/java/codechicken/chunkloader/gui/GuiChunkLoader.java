package codechicken.chunkloader.gui;

import codechicken.chunkloader.network.ChunkLoaderCPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.util.LangProxy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class GuiChunkLoader extends GuiScreen {
    private static LangProxy lang = new LangProxy("chickenchunks.gui");

    public GuiButton laserButton;
    public GuiButton shapeButton;
    public TileChunkLoader tile;

    public GuiChunkLoader(TileChunkLoader tile) {
        this.tile = tile;
    }

    public void initGui() {
        buttonList.clear();

        buttonList.add(new GuiButton(1, width / 2 - 20, height / 2 - 45, 20, 20, "+"));
        buttonList.add(new GuiButton(2, width / 2 - 80, height / 2 - 45, 20, 20, "-"));
        buttonList.add(laserButton = new GuiButton(3, width / 2 + 7, height / 2 - 60, 75, 20, "-"));
        buttonList.add(shapeButton = new GuiButton(4, width / 2 + 7, height / 2 - 37, 75, 20, "-"));
        updateNames();

        super.initGui();
    }

    public void updateNames() {
        laserButton.displayString = tile.renderInfo.showLasers ? lang.translate("hidelasers") : lang.translate("showlasers");
        shapeButton.displayString = tile.shape.getName();
    }

    public void updateScreen() {
        if (mc.theWorld.getTileEntity(tile.getPos()) != tile)//tile changed
        {
            mc.currentScreen = null;
            mc.setIngameFocus();
        }
        updateNames();
        super.updateScreen();
    }

    public void drawScreen(int i, int j, float f) {
        drawDefaultBackground();
        drawContainerBackground();

        super.drawScreen(i, j, f);//buttons

        //GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.disableLighting();
        //GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.disableDepth();

        drawCentered(lang.translate("name"), width / 2 - 40, height / 2 - 74, 0x303030);
        if (tile.owner != null) {
            drawCentered(tile.owner, width / 2 + 44, height / 2 - 72, 0x801080);
        }
        drawCentered(lang.translate("radius"), width / 2 - 40, height / 2 - 57, 0x404040);
        drawCentered("" + tile.radius, width / 2 - 40, height / 2 - 39, 0xFFFFFF);

        int chunks = tile.countLoadedChunks();
        drawCentered(lang.format(chunks == 1 ? "chunk" : "chunks", chunks), width / 2 - 39, height / 2 - 21, 0x108000);

        //TODO: sradius = "Total "+ChunkLoaderManager.activeChunkLoaders+"/"+ChunkLoaderManager.allowedChunkloaders+" Chunks";
        //fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2, height / 2 - 8, 0x108000);

        GlStateManager.enableLighting();
        //GL11.glEnable(GL11.GL_LIGHTING);
        GlStateManager.enableDepth();
        //GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void drawCentered(String s, int x, int y, int colour) {
        fontRendererObj.drawString(s, x - fontRendererObj.getStringWidth(s) / 2, y, colour);
    }

    int button;

    @Override
    protected void mouseClicked(int par1, int par2, int par3) throws IOException {
        button = par3;
        if (par3 == 1) {
            par3 = 0;
        }
        super.mouseClicked(par1, par2, par3);
    }

    protected void actionPerformed(GuiButton guibutton) {
        if (guibutton.id == 1) {
            ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius + 1);
        }
        if (guibutton.id == 2 && tile.radius > 1) {
            ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius - 1);
        }
        if (guibutton.id == 3) {
            tile.renderInfo.showLasers = !tile.renderInfo.showLasers;
        }
        if (guibutton.id == 4) {
            ChunkLoaderCPH.sendShapeChange(tile, button == 1 ? tile.shape.prev() : tile.shape.next(), tile.radius);
        }
    }

    private void drawContainerBackground() {
        GL11.glColor4f(1, 1, 1, 1);
        CCRenderState.changeTexture("chickenchunks:textures/gui/guiSmall.png");
        int posx = width / 2 - 88;
        int posy = height / 2 - 83;
        drawTexturedModalRect(posx, posy, 0, 0, 176, 166);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }
}
