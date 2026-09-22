package dev.origins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RaceSelectionScreen extends Screen {
    private static final String[] RACES = {"dragonborn", "dwarf", "giant", "warlock", "witch", "mermaid", "fairy", "celestid", "shadowkin", "arachnid", "ignis", "golem"};

    public RaceSelectionScreen() { super(Text.literal("Choose your race")); }

    @Override protected void init() {
        int columns = 2;
        int left = this.width / 2 - 180;
        int top = this.height / 2 - 120;
        for (int index = 0; index < RACES.length; index++) {
            String race = RACES[index];
            this.addDrawableChild(ButtonWidget.builder(Text.literal(capitalize(race)), button -> choose(race))
                .dimensions(left + (index % columns) * 185, top + (index / columns) * 24, 170, 20).build());
        }
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Use ability"), button -> {
            sendCommand("race ability");
            close();
        }).dimensions(this.width / 2 - 85, top + 6 * 24 + 8, 170, 20).build());
    }

    private void choose(String race) {
        sendCommand("race choose " + race);
        close();
    }

    private void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.networkHandler.sendChatCommand(command);
    }

    private static String capitalize(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 145, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}