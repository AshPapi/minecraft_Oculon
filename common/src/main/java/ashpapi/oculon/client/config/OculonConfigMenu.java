package ashpapi.oculon.client.config;

import ashpapi.oculon.client.config.SodiumConfigBuilder.BoolOption;
import ashpapi.oculon.client.config.SodiumConfigBuilder.EnumOption;
import ashpapi.oculon.client.config.SodiumConfigBuilder.Group;
import ashpapi.oculon.client.config.SodiumConfigBuilder.IntOption;
import ashpapi.oculon.client.config.SodiumConfigBuilder.Page;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public class OculonConfigMenu implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        OculonConfig cfg = OculonConfig.CONFIG;
        OculonConfig defaults = OculonConfig.DEFAULT_CONFIG;
        var modOptions = builder.registerModOptions("oculon", "Oculon", "by AshPapi")
                .setNonTintedIcon(Identifier.fromNamespaceAndPath("oculon", "icon.png"));

        SodiumConfigBuilder.buildToSodium(
                builder,
                modOptions,
                cfg::save,
                new Page(
                        Component.translatable("oculon.config.general"),
                        new Group(
                                new EnumOption<>(
                                        "oculon:mode",
                                        OculonConfig.ZoomMode.class,
                                        Component.translatable("oculon.config.mode"),
                                        cfg::getZoomMode,
                                        cfg::setZoomMode
                                ).setDefaultValue(defaults.getZoomMode())
                                        .setNameProvider(mode -> Component.translatable("oculon.config.mode." + mode.name().toLowerCase(Locale.ROOT))),
                                new IntOption(
                                        "oculon:initial_zoom",
                                        Component.translatable("oculon.config.initial_zoom"),
                                        () -> cfg.initialZoom,
                                        v -> cfg.initialZoom = v,
                                        new Range(2, 16, 1)
                                ).setDefaultValue(defaults.initialZoom)
                                        .setFormatter(v -> Component.literal(v + "x"))
                        ),
                        new Group(
                                new BoolOption(
                                        "oculon:remember_scroll_zoom",
                                        Component.translatable("oculon.config.remember_scroll_zoom"),
                                        () -> cfg.rememberScrollZoom,
                                        v -> cfg.rememberScrollZoom = v
                                ).setDefaultValue(defaults.rememberScrollZoom)
                        )
                )
        );
    }
}
