package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.NMSBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NMSLoader {

    private static final String PACKAGE = "com.ultikits.plugins.ultibot.nms";
    private static final Map<String, String> VERSION_MAP = new HashMap<>();

    static {
        // 1.20.x
        VERSION_MAP.put("1.20.1", "v1_20_R1");
        VERSION_MAP.put("1.20.2", "v1_20_R2");
        VERSION_MAP.put("1.20.3", "v1_20_R2");
        VERSION_MAP.put("1.20.4", "v1_20_R3");
        VERSION_MAP.put("1.20.5", "v1_20_R4");
        VERSION_MAP.put("1.20.6", "v1_20_R4");
        // 1.21.x
        VERSION_MAP.put("1.21", "v1_21_R1");
        VERSION_MAP.put("1.21.1", "v1_21_R1");
        VERSION_MAP.put("1.21.2", "v1_21_R2");
        VERSION_MAP.put("1.21.3", "v1_21_R2");
        VERSION_MAP.put("1.21.4", "v1_21_R3");
    }

    public static String getClassNameForVersion(String mcVersion) {
        String module = VERSION_MAP.get(mcVersion);
        if (module == null) {
            return null;
        }
        return PACKAGE + "." + module + ".NMSBridge" + module.substring(0, 1).toUpperCase()
                + module.substring(1);
    }

    public static String extractMinecraftVersion(String bukkitVersion) {
        int dashIndex = bukkitVersion.indexOf('-');
        if (dashIndex > 0) {
            return bukkitVersion.substring(0, dashIndex);
        }
        return bukkitVersion;
    }

    public static NMSBridge load(String bukkitVersion, Logger logger) {
        String mcVersion = extractMinecraftVersion(bukkitVersion);
        String className = getClassNameForVersion(mcVersion);
        if (className == null) {
            logger.warning("UltiBot does not support MC version " + mcVersion);
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            NMSBridge bridge = (NMSBridge) clazz.getDeclaredConstructor().newInstance();
            if (!bridge.isSupported()) {
                logger.warning("NMS bridge reports unsupported for " + mcVersion);
                return null;
            }
            logger.info("Loaded NMS bridge for MC " + mcVersion + ": " + className);
            return bridge;
        } catch (ClassNotFoundException e) {
            logger.warning("NMS module not found for MC " + mcVersion
                    + " — is ultibot-" + VERSION_MAP.get(mcVersion) + " in the JAR?");
            return null;
        } catch (Exception e) {
            logger.severe("Failed to instantiate NMS bridge: " + e.getMessage());
            return null;
        }
    }
}
