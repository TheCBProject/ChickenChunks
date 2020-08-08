package codechicken.chunkloader.gui;

import codechicken.chunkloader.network.ChunkLoaderCPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.texture.TextureUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;

public class GuiChunkLoader extends Screen {

    public Button laserButton;
    public Button shapeButton;
    public TileChunkLoader tile;

    private int lastButton;

    public GuiChunkLoader(TileChunkLoader tile) {
        super(new StringTextComponent("DOOOOOOOT"));
        this.tile = tile;
    }

    @Override
    public void init() {
        addButton(new Button(width / 2 - 20, height / 2 - 45, 20, 20, "+", e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius + 1)));
        addButton(new Button(width / 2 - 80, height / 2 - 45, 20, 20, "-", e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius - 1)));
        addButton(laserButton = new Button(width / 2 + 7, height / 2 - 60, 75, 20, "-", e -> tile.renderInfo.showLasers = !tile.renderInfo.showLasers));
        addButton(shapeButton = new Button(width / 2 + 7, height / 2 - 37, 75, 20, "-", e -> ChunkLoaderCPH.sendShapeChange(tile, lastButton == 1 ? tile.shape.prev() : tile.shape.next(), tile.radius)));
        updateNames();

        super.init();
    }

    public void updateNames() {
        laserButton.setMessage(I18n.format(tile.renderInfo.showLasers ? "chickenchunks.gui.hidelasers" : "chickenchunks.gui.showlasers"));
        shapeButton.setMessage(tile.shape.getTranslation().getFormattedText());
    }

    @Override
    public void tick() {
        if (minecraft.world.getTileEntity(tile.getPos()) != tile)//tile changed
        {
            minecraft.currentScreen = null;
            minecraft.mouseHelper.grabMouse();
        }
        updateNames();
        super.tick();
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground();
        drawContainerBackground();

        super.render(p_render_1_, p_render_2_, p_render_3_);//buttons

        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();

        drawCentered(I18n.format("chickenchunks.gui.name"), width / 2 - 40, height / 2 - 74, 0x303030);
        if (tile.owner != null) {
            drawCentered(tile.ownerName.getFormattedText(), width / 2 + 44, height / 2 - 72, 0x801080);
        }
        drawCentered(I18n.format("chickenchunks.gui.radius"), width / 2 - 40, height / 2 - 57, 0x404040);
        drawCentered("" + tile.radius, width / 2 - 40, height / 2 - 39, 0xFFFFFF);

        int chunks = tile.countLoadedChunks();
        drawCentered(I18n.format(chunks == 1 ? "chickenchunks.gui.chunk" : "chickenchunks.gui.chunks", chunks), width / 2 - 39, height / 2 - 21, 0x108000);

        //TODO: sradius = "Total "+ChunkLoaderManager.activeChunkLoaders+"/"+ChunkLoaderManager.allowedChunkloaders+" Chunks";
        //fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2, height / 2 - 8, 0x108000);

        GlStateManager.enableLighting();
        GlStateManager.enableDepthTest();
    }

    private void drawCentered(String s, int x, int y, int colour) {
        font.drawString(s, x - font.getStringWidth(s) / 2f, y, colour);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        lastButton = button;
        if (button == 1) {
            button = 0;
        }
        return super.mouseClicked(x, y, button);
    }

    private void drawContainerBackground() {
        GlStateManager.color4f(1F, 1F, 1F, 1F);
        TextureUtils.changeTexture("chickenchunks:textures/gui/gui_small.png");
        int posx = width / 2 - 88;
        int posy = height / 2 - 83;
        blit(posx, posy, 0, 0, 176, 166);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
