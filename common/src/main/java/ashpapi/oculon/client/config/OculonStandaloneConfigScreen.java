package ashpapi.oculon.client.config;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class OculonStandaloneConfigScreen extends Screen {
    private final Screen parent;

    public OculonStandaloneConfigScreen(Screen parent) {
        super(Component.translatable("oculon.config.general"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        OculonConfig cfg = OculonConfig.CONFIG;
        int centerX = this.width / 2;
        int y = this.height / 6;
        int buttonWidth = 220;
        int buttonHeight = 20;

        this.addRenderableWidget(
                CycleButton.<OculonConfig.ZoomMode>builder(
                                mode -> Component.translatable("oculon.config.mode." + mode.name().toLowerCase(Locale.ROOT)),
                                cfg::getZoomMode
                        )
                        .withValues(OculonConfig.ZoomMode.values())
                        .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight, Component.translatable("oculon.config.mode"), (btn, val) -> {
                            cfg.setZoomMode(val);
                            cfg.save();
                        })
        );
        y += 26;

        this.addRenderableWidget(new IntSlider(
                centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                "oculon.config.initial_zoom", 2, 16, cfg.initialZoom,
                val -> {
                    cfg.initialZoom = val;
                    cfg.save();
                }
        ));
        y += 26;

        this.addRenderableWidget(
                CycleButton.onOffBuilder(cfg.rememberScrollZoom)
                        .create(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight, Component.translatable("oculon.config.remember_scroll_zoom"), (btn, val) -> {
                            cfg.rememberScrollZoom = val;
                            cfg.save();
                        })
        );
        y += 34;

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                        .bounds(centerX - 100, y, 200, buttonHeight)
                        .build()
        );
    }

    @Override
    public void onClose() {
        OculonConfig.CONFIG.save();
        if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String translationKey;
        private final int min;
        private final int max;
        private final java.util.function.IntConsumer consumer;

        private IntSlider(int x, int y, int width, int height, String translationKey, int min, int max, int current, java.util.function.IntConsumer consumer) {
            super(x, y, width, height, CommonComponents.EMPTY, Math.clamp((double) (current - min) / (double) Math.max(1, max - min), 0.0, 1.0));
            this.translationKey = translationKey;
            this.min = min;
            this.max = max;
            this.consumer = consumer;
            this.updateMessage();
        }

        private int currentValue() {
            return (int) Math.round(this.min + this.value * (this.max - this.min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(this.translationKey).append(": " + this.currentValue() + "x"));
        }

        @Override
        protected void applyValue() {
            this.consumer.accept(this.currentValue());
        }
    }
}
