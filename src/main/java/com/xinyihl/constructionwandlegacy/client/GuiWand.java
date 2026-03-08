package com.xinyihl.constructionwandlegacy.client;

import com.xinyihl.constructionwandlegacy.basics.option.IOption;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.network.ModMessages;
import com.xinyihl.constructionwandlegacy.network.PacketWandOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.Collections;

public class GuiWand extends GuiScreen {
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING_WIDTH = 50;
    private static final int SPACING_HEIGHT = 30;

    private static final int COLS = 2;
    private static final int ROWS = 3;

    private static final int FIELD_WIDTH = COLS * (BUTTON_WIDTH + SPACING_WIDTH) - SPACING_WIDTH;
    private static final int FIELD_HEIGHT = ROWS * (BUTTON_HEIGHT + SPACING_HEIGHT) - SPACING_HEIGHT;

    private final ItemStack wand;
    private final WandOptions options;

    public GuiWand(ItemStack wand) {
        this.wand = wand;
        this.options = new WandOptions(wand);
    }

    private static String getButtonLabel(IOption<?> option) {
        return I18n.format(option.getKeyTranslation()) + I18n.format(option.getValueTranslation());
    }

    @Override
    public void initGui() {
        buttonList.clear();
        createButton(0, 0, options.cores);
        createButton(0, 1, options.lock);
        createButton(0, 2, options.direction);
        createButton(1, 0, options.replace);
        createButton(1, 1, options.match);
        createButton(1, 2, options.random);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, wand.getDisplayName(), width / 2, height / 2 - FIELD_HEIGHT / 2 - SPACING_HEIGHT, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiButton button : buttonList) {
            if (button instanceof OptionButton && button.isMouseOver()) {
                OptionButton optionButton = (OptionButton) button;
                drawHoveringText(Collections.singletonList(I18n.format(optionButton.option.getDescTranslation())), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!(button instanceof OptionButton)) {
            return;
        }

        OptionButton optionButton = (OptionButton) button;
        optionButton.option.next(true);
        optionButton.displayString = getButtonLabel(optionButton.option);
        ModMessages.sendToServer(new PacketWandOption(optionButton.option, false));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (Minecraft.getMinecraft().gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void createButton(int col, int row, IOption<?> option) {
        OptionButton button = new OptionButton(buttonList.size(), getX(col), getY(row), option);
        button.enabled = option.isEnabled();
        buttonList.add(button);
    }

    private int getX(int col) {
        return width / 2 - FIELD_WIDTH / 2 + col * (BUTTON_WIDTH + SPACING_WIDTH);
    }

    private int getY(int row) {
        return height / 2 - FIELD_HEIGHT / 2 + row * (BUTTON_HEIGHT + SPACING_HEIGHT);
    }

    private static final class OptionButton extends GuiButton {
        private final IOption<?> option;

        private OptionButton(int buttonId, int x, int y, IOption<?> option) {
            super(buttonId, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, getButtonLabel(option));
            this.option = option;
        }
    }
}