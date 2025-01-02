package codechicken.chunkloader.gui;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.network.ChunkLoaderCPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

public class GuiChunkLoader extends Screen {

    public @Nullable Button laserButton;
    public @Nullable Button shapeButton;
    public TileChunkLoader tile;

    private int lastRadius = -1;
    @Nullable
    private ChunkLoaderShape lastShape = null;

    private int lastButton;

    private long warningEnd = -1;
    @Nullable
    private Component warningText;

    public GuiChunkLoader(TileChunkLoader tile) {
        super(Component.empty());
        this.tile = tile;
    }

    @Override
    public void init() {
        addRenderableWidget(Button.builder(Component.literal("+"), e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius + 1)).bounds(width / 2 - 20, height / 2 - 45, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), e -> ChunkLoaderCPH.sendShapeChange(tile, tile.shape, tile.radius - 1)).bounds(width / 2 - 80, height / 2 - 45, 20, 20).build());
        addRenderableWidget(laserButton = Button.builder(Component.literal("-"), e -> tile.renderInfo.showLasers = !tile.renderInfo.showLasers).bounds(width / 2 + 7, height / 2 - 60, 75, 20).build());
        addRenderableWidget(shapeButton = Button.builder(Component.literal("-"), e -> ChunkLoaderCPH.sendShapeChange(tile, lastButton == 1 ? tile.shape.prev() : tile.shape.next(), tile.radius)).bounds(width / 2 + 7, height / 2 - 37, 75, 20).build());
        updateNames();

        super.init();
    }

    public void updateNames() {
        assert laserButton != null;
        assert shapeButton != null;
        assert tile.renderInfo != null;
        laserButton.setMessage(Component.translatable(tile.renderInfo.showLasers ? "chickenchunks.gui.hidelasers" : "chickenchunks.gui.showlasers"));
        shapeButton.setMessage(tile.shape.getTranslation());
        if (lastRadius != tile.radius || lastShape != tile.shape) {
            warningEnd = -1;
            warningText = null;
            lastRadius = tile.radius;
            lastShape = tile.shape;
        }
    }

    public void addWarning(Component text) {
        warningText = text;
        warningEnd = System.currentTimeMillis() + 3500;
    }

    @Override
    public void tick() {
        assert minecraft != null;
        assert minecraft.level != null;

        if (minecraft.level.getBlockEntity(tile.getBlockPos()) != tile) { //tile changed
            minecraft.screen = null;
            minecraft.mouseHandler.grabMouse();
        }
        updateNames();
        super.tick();
    }

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/gui_small.png");
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, texture);
        int posx = width / 2 - 88;
        int posy = height / 2 - 83;
        graphics.blit(texture, posx, posy, 0, 0, 176, 166);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);//buttons
        drawCentered(graphics, Component.translatable("chickenchunks.gui.name"), width / 2 - 40, height / 2 - 74, 0x303030);
        if (tile.owner != null) {
            assert tile.ownerName != null;
            drawCentered(graphics, tile.ownerName, width / 2 + 44, height / 2 - 72, 0x801080);
        }
        drawCentered(graphics, Component.translatable("chickenchunks.gui.radius"), width / 2 - 40, height / 2 - 57, 0x404040);
        drawCentered(graphics, Component.literal("" + tile.radius), width / 2 - 40, height / 2 - 39, 0xFFFFFF);

        int chunks = tile.countLoadedChunks();
        drawCentered(graphics, Component.translatable(chunks == 1 ? "chickenchunks.gui.chunk" : "chickenchunks.gui.chunks", chunks), width / 2 - 39, height / 2 - 21, 0x108000);

        if (warningText != null && warningEnd != -1) {
            float fade = (warningEnd - System.currentTimeMillis()) / 1000F;
            if (fade <= 0.1) {
                warningEnd = -1;
                warningText = null;
            } else {
                int alpha = fade <= 1.0F ? (int) (255 * fade) : 255;
                drawCentered(graphics, warningText, width / 2, height / 2 - 8, 0xFF5555 | (alpha & 0xFF) << 24);
            }
        }
    }

    private void drawCentered(GuiGraphics graphics, Component s, int x, int y, int colour) {
        graphics.drawString(font, s.getVisualOrderText(), x - font.width(s) / 2, y, colour, false);
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
