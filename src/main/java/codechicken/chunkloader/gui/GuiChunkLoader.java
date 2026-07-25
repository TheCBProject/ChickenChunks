package codechicken.chunkloader.gui;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.tile.TileChunkLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
        addRenderableWidget(Button.builder(Component.literal("+"), e -> ChickenChunksNetwork.sendSetShape(tile, tile.shape, tile.radius + 1)).bounds(width / 2 - 20, height / 2 - 45, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), e -> ChickenChunksNetwork.sendSetShape(tile, tile.shape, tile.radius - 1)).bounds(width / 2 - 80, height / 2 - 45, 20, 20).build());
        addRenderableWidget(laserButton = Button.builder(Component.literal("-"), e -> tile.renderInfo.showLasers = !tile.renderInfo.showLasers).bounds(width / 2 + 7, height / 2 - 60, 75, 20).build());
        // TODO we can use a custom Button instance here which allows both left and right-clicks, the action can consume the 'was left/right'
        //      We can then nuke the lastButton and flipping in the onClicked method.
        addRenderableWidget(shapeButton = Button.builder(Component.literal("-"), e -> {
            ChunkLoaderShape shape = lastButton == 1 ? tile.shape.prev() : tile.shape.next();
            ChickenChunksNetwork.sendSetShape(tile, shape, tile.radius);
        }).bounds(width / 2 + 7, height / 2 - 37, 75, 20).build());
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
        Identifier texture = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/gui_small.png");
        int posx = width / 2 - 88;
        int posy = height / 2 - 83;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, posx, posy, 0, 0, 176, 166, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);//buttons
        drawCentered(graphics, Component.translatable("chickenchunks.gui.name"), width / 2 - 40, height / 2 - 74, 0xFF303030);
        if (tile.owner != null) {
            assert tile.ownerName != null;
            drawCentered(graphics, tile.ownerName, width / 2 + 44, height / 2 - 72, 0xFF801080);
        }
        drawCentered(graphics, Component.translatable("chickenchunks.gui.radius"), width / 2 - 40, height / 2 - 57, 0xFF404040);
        drawCentered(graphics, Component.literal("" + tile.radius), width / 2 - 40, height / 2 - 39, 0xFFFFFFFF);

        int chunks = tile.countLoadedChunks();
        drawCentered(graphics, Component.translatable(chunks == 1 ? "chickenchunks.gui.chunk" : "chickenchunks.gui.chunks", chunks), width / 2 - 39, height / 2 - 21, 0xFF108000);

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
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        // Store the last _real_ button click.
        lastButton = event.button();
        if (event.button() == 1) {
            // Flip right-click to a left-click.
            // This ensures that left and right click activates the buttons.
            event = new MouseButtonEvent(event.x(), event.y(), new MouseButtonInfo(0, event.modifiers()));
        }
        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
