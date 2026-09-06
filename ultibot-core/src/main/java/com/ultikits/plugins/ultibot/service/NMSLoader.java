package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.NMSBridge;
import com.ultikits.ultitools.exceptions.PluginModuleException;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

public class NMSLoader {

    private static final String PACKAGE = "com.ultikits.plugins.ultibot.nms";
    private static final Map<String, String> VERSION_MAP = new HashMap<>();

    static {
        // 13-10/FIX-05 (UltiBot#12): this table used to declare 7 distinct bridge classes
        // (v1_20_R1..R4, v1_21_R1..R3) across 11 version keys while the reactor built exactly 1
        // (ultibot-v1_21_R1) -- every other entry was a promise this artifact could not keep, and
        // an operator on one of those versions found out only when a spawned bot did nothing.
        // Narrowed to the versions the one built module actually covers.
        // NMSLoaderTest#everyDeclaredVersionMapsToABridgeThatExists derives its expectation from
        // this table itself: a future entry added here without its bridge module being built
        // fails that test, and a bridge module built without its entry landing here goes unused
        // but does not fail it.
        VERSION_MAP.put("1.21", "v1_21_R1");
        VERSION_MAP.put("1.21.1", "v1_21_R1");
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

    /**
     * Resolves and instantiates the NMS bridge for the running server's Minecraft version.
     * <p>
     * A version this table does not declare -- or, as an internal-consistency backstop, a
     * declared version whose bridge class turns out to be missing from the built artifact --
     * refuses loudly by throwing {@link PluginModuleException} instead of the previous behaviour
     * of warning and returning {@code null} for the caller to silently carry forward.
     * <p>
     * {@link BotManagerImpl}'s constructor calls this with nothing wrapping the exception, so it
     * propagates out of {@code @Service} bean construction. UltiTools-API's {@code PluginManager}
     * catches that failure during plugin registration and refuses to register the whole plugin,
     * logging this exception's own message (naming the detected version and every version this
     * table currently supports) at {@code WARNING} rather than the plugin silently loading with a
     * bot subsystem that does nothing the first time an operator tries to use it.
     *
     * @param bukkitVersion the raw {@code Bukkit.getBukkitVersion()} string
     * @param logger        logger used only to report a successful load
     * @return the instantiated, supported bridge -- never {@code null}
     * @throws PluginModuleException naming the detected version and the versions this table declares
     */
    public static NMSBridge load(String bukkitVersion, Logger logger) {
        String mcVersion = extractMinecraftVersion(bukkitVersion);
        String className = getClassNameForVersion(mcVersion);
        if (className == null) {
            throw new PluginModuleException(unsupportedVersionMessage(mcVersion));
        }
        try {
            Class<?> clazz = Class.forName(className);
            NMSBridge bridge = (NMSBridge) clazz.getDeclaredConstructor().newInstance();
            if (!bridge.isSupported()) {
                throw new PluginModuleException(unsupportedVersionMessage(mcVersion));
            }
            logger.info("Loaded NMS bridge for MC " + mcVersion + ": " + className);
            return bridge;
        } catch (ClassNotFoundException e) {
            // The table declared this version but its bridge class is not in this build's
            // artifact -- NMSLoaderTest#everyDeclaredVersionMapsToABridgeThatExists exists to
            // catch exactly this before shipping, so reaching here at runtime is a build defect,
            // not a legitimate "unsupported version" outcome, and is reported as such.
            throw new PluginModuleException(
                    "UltiBot's version table declares MC " + mcVersion + " (" + className
                            + ") but that class is not present in this build. This is a build "
                            + "defect (a declared version with no bridge module behind it), not "
                            + "an unsupported-version refusal.", e);
        } catch (LinkageError e) {
            // 13-REVIEW-UltiBot.md WR-01: the declared class IS present and Class.forName
            // loads its bytes, but resolving/linking a type it references -- a net.minecraft.*
            // internal signature, exactly the kind of thing NMSBridgeV1_21_R1's own comments
            // call out ("Paper 1.21+ removed versioned packages") -- fails instead
            // (NoClassDefFoundError, ExceptionInInitializerError, etc.). Without this catch the
            // Error propagates raw past load(), past SimpleContainer.createBean's
            // catch (Exception e) (which doesn't catch Error either), and surfaces to the
            // operator as an opaque "Failed to create bean: botManagerImpl" that names neither
            // the detected version nor the class that failed -- the exact diagnostic-quality gap
            // this plan exists to close, just from a different failure mode than a missing class.
            throw new PluginModuleException(
                    "UltiBot's bridge for MC " + mcVersion + " (" + className + ") is present in "
                            + "this build but failed to link -- likely a server-internal API "
                            + "mismatch (e.g. a net.minecraft.* signature this bridge references "
                            + "no longer matches the running server). " + e, e);
        } catch (PluginModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginModuleException(
                    "Failed to instantiate NMS bridge for MC " + mcVersion + ": " + e.getMessage(), e);
        }
    }

    private static String unsupportedVersionMessage(String mcVersion) {
        return "UltiBot does not support Minecraft version " + mcVersion
                + ". Supported versions: " + String.join(", ", new TreeSet<>(VERSION_MAP.keySet())) + ".";
    }
}
