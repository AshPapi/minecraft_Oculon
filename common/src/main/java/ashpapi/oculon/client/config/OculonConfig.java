package ashpapi.oculon.client.config;

import ashpapi.oculon.platform.OculonPlatform;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class OculonConfig {
    public enum ZoomMode {
        HOLD,
        TOGGLE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("Oculon");
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .create();

    public static final OculonConfig DEFAULT_CONFIG = new OculonConfig();
    public static OculonConfig CONFIG = loadOrCreate();

    public String zoomMode = "hold";
    public int initialZoom = 4;
    public boolean rememberScrollZoom = false;

    public ZoomMode getZoomMode() {
        if (this.zoomMode == null) {
            return ZoomMode.HOLD;
        }
        try {
            return ZoomMode.valueOf(this.zoomMode.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ZoomMode.HOLD;
        }
    }

    public void setZoomMode(ZoomMode mode) {
        this.zoomMode = (mode != null ? mode : ZoomMode.HOLD).name().toLowerCase(Locale.ROOT);
    }

    public static OculonConfig loadOrCreate() {
        Path path = getConfigFilePath();
        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                OculonConfig loaded = GSON.fromJson(reader, OculonConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read oculon-config.json", e);
            }
        }
        OculonConfig config = new OculonConfig();
        config.sanitize();
        config.save();
        return config;
    }

    public void sanitize() {
        this.initialZoom = Math.clamp(this.initialZoom, 2, 16);
        this.setZoomMode(this.getZoomMode());
    }

    public void save() {
        this.sanitize();
        try {
            Path path = getConfigFilePath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Failed to write oculon-config.json", e);
        }
    }

    private static Path getConfigFilePath() {
        try {
            Path configDir = OculonPlatform.get().getConfigDir();
            if (configDir != null) {
                return configDir.resolve("oculon-config.json");
            }
        } catch (Throwable ignored) {
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "oculon-config.json");
    }
}
