package codechicken.chunkloader.gui;

import codechicken.chunkloader.network.ChunkLoaderCPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.texture.TextureUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

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
        addButton(new Button(width / 2 - 20, height / 2 - 45, 20, 20, new StringTextComponent("+"), e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius + 1)));
        addButton(new Button(width / 2 - 80, height / 2 - 45, 20, 20, new StringTextComponent("-"), e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius - 1)));
        addButton(laserButton = new Button(width / 2 + 7, height / 2 - 60, 75, 20, new StringTextComponent("-"), e -> tile.renderInfo.showLasers = !tile.renderInfo.showLasers));
        addButton(shapeButton = new Button(width / 2 + 7, height / 2 - 37, 75, 20, new StringTextComponent("-"), e -> ChunkLoaderCPH.sendShapeChange(tile, lastButton == 1 ? tile.shape.prev() : tile.shape.next(), tile.radius)));
        updateNames();

        super.init();
    }

    public void updateNames() {
        laserButton.setMessage(new TranslationTextComponent(tile.renderInfo.showLasers ? "chickenchunks.gui.hidelasers" : "chickenchunks.gui.showlasers"));
        shapeButton.setMessage(tile.shape.getTranslation());
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
    public void render(MatrixStack mStack, int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground(mStack);
        GlStateManager.color4f(1F, 1F, 1F, 1F);
        TextureUtils.changeTexture("chickenchunks:textures/gui/gui_small.png");
        int posx = width / 2 - 88;
        int posy = height / 2 - 83;
        blit(mStack, posx, posy, 0, 0, 176, 166);

        super.render(mStack, p_render_1_, p_render_2_, p_render_3_);//buttons

        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();

        drawCentered(mStack, new TranslationTextComponent("chickenchunks.gui.name"), width / 2 - 40, height / 2 - 74, 0x303030);
        if (tile.owner != null) {
            drawCentered(mStack, tile.ownerName, width / 2 + 44, height / 2 - 72, 0x801080);
        }
        drawCentered(mStack, new TranslationTextComponent("chickenchunks.gui.radius"), width / 2 - 40, height / 2 - 57, 0x404040);
        drawCentered(mStack, new StringTextComponent("" + tile.radius), width / 2 - 40, height / 2 - 39, 0xFFFFFF);

        int chunks = tile.countLoadedChunks();
        drawCentered(mStack, new TranslationTextComponent(chunks == 1 ? "chickenchunks.gui.chunk" : "chickenchunks.gui.chunks", chunks), width / 2 - 39, height / 2 - 21, 0x108000);

        //TODO: sradius = "Total "+ChunkLoaderManager.activeChunkLoaders+"/"+ChunkLoaderManager.allowedChunkloaders+" Chunks";
        //fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2, height / 2 - 8, 0x108000);

        GlStateManager.enableLighting();
        GlStateManager.enableDepthTest();
    }

    private void drawCentered(MatrixStack mStack, ITextComponent s, int x, int y, int colour) {
        font.func_238422_b_(mStack, s.func_241878_f(), x - font.getStringPropertyWidth(s) / 2f, y, colour);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        lastButton = button;
        if (button == 1) {
            button = 0;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
