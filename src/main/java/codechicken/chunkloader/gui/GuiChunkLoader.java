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
    public void func_231160_c_() {
        func_230480_a_(new Button(field_230708_k_ / 2 - 20, field_230709_l_ / 2 - 45, 20, 20, new StringTextComponent("+"), e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius + 1)));
        func_230480_a_(new Button(field_230708_k_ / 2 - 80, field_230709_l_ / 2 - 45, 20, 20, new StringTextComponent("-"), e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius - 1)));
        func_230480_a_(laserButton = new Button(field_230708_k_ / 2 + 7, field_230709_l_ / 2 - 60, 75, 20, new StringTextComponent("-"), e -> tile.renderInfo.showLasers = !tile.renderInfo.showLasers));
        func_230480_a_(shapeButton = new Button(field_230708_k_ / 2 + 7, field_230709_l_ / 2 - 37, 75, 20, new StringTextComponent("-"), e -> ChunkLoaderCPH.sendShapeChange(tile, lastButton == 1 ? tile.shape.prev() : tile.shape.next(), tile.radius)));
        updateNames();

        super.func_231160_c_();
    }

    public void updateNames() {
        laserButton.func_238482_a_(new TranslationTextComponent(tile.renderInfo.showLasers ? "chickenchunks.gui.hidelasers" : "chickenchunks.gui.showlasers"));
        shapeButton.func_238482_a_(tile.shape.getTranslation());
    }

    @Override
    public void func_231023_e_() {
        if (field_230706_i_.world.getTileEntity(tile.getPos()) != tile)//tile changed
        {
            field_230706_i_.currentScreen = null;
            field_230706_i_.mouseHelper.grabMouse();
        }
        updateNames();
        super.func_231023_e_();
    }

    @Override
    public void func_230430_a_(MatrixStack mStack, int p_render_1_, int p_render_2_, float p_render_3_) {
        func_230446_a_(mStack);
        GlStateManager.color4f(1F, 1F, 1F, 1F);
        TextureUtils.changeTexture("chickenchunks:textures/gui/gui_small.png");
        int posx = field_230708_k_ / 2 - 88;
        int posy = field_230709_l_ / 2 - 83;
        func_238474_b_(mStack, posx, posy, 0, 0, 176, 166);

        super.func_230430_a_(mStack, p_render_1_, p_render_2_, p_render_3_);//buttons

        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();

        drawCentered(mStack, new TranslationTextComponent("chickenchunks.gui.name"), field_230708_k_ / 2 - 40, field_230709_l_ / 2 - 74, 0x303030);
        if (tile.owner != null) {
            drawCentered(mStack, tile.ownerName, field_230708_k_ / 2 + 44, field_230709_l_ / 2 - 72, 0x801080);
        }
        drawCentered(mStack, new TranslationTextComponent("chickenchunks.gui.radius"), field_230708_k_ / 2 - 40, field_230709_l_ / 2 - 57, 0x404040);
        drawCentered(mStack, new StringTextComponent("" + tile.radius), field_230708_k_ / 2 - 40, field_230709_l_ / 2 - 39, 0xFFFFFF);

        int chunks = tile.countLoadedChunks();
        drawCentered(mStack, new TranslationTextComponent(chunks == 1 ? "chickenchunks.gui.chunk" : "chickenchunks.gui.chunks", chunks), field_230708_k_ / 2 - 39, field_230709_l_ / 2 - 21, 0x108000);

        //TODO: sradius = "Total "+ChunkLoaderManager.activeChunkLoaders+"/"+ChunkLoaderManager.allowedChunkloaders+" Chunks";
        //fontRenderer.drawString(sradius, width / 2 - fontRenderer.getStringWidth(sradius) / 2, height / 2 - 8, 0x108000);

        GlStateManager.enableLighting();
        GlStateManager.enableDepthTest();
    }

    private void drawCentered(MatrixStack mStack, ITextComponent s, int x, int y, int colour) {
        field_230712_o_.func_238422_b_(mStack, s, x - field_230712_o_.func_238414_a_(s) / 2f, y, colour);
    }

    @Override
    public boolean func_231044_a_(double x, double y, int button) {
        lastButton = button;
        if (button == 1) {
            button = 0;
        }
        return super.func_231044_a_(x, y, button);
    }

    @Override
    public boolean func_231177_au__() {
        return false;
    }
}
