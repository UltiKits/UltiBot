package com.ultikits.plugins.ultibot.nms.brokenlink;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.api.NMSBridge;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Test-only stand-in for a bridge class that is present in the build but fails to link, the
 * failure mode 13-REVIEW-UltiBot.md WR-01 documents {@link com.ultikits.plugins.ultibot.service.NMSLoader#load}
 * not naming: a declared entry whose class file loads fine but whose static initializer throws an
 * {@link Error} rather than an ordinary exception.
 * <p>
 * Per JLS 12.4.2, a static initializer that throws an {@link Error} subtype propagates that error
 * as-is out of class initialization -- it is <em>not</em> wrapped in {@code ExceptionInInitializerError}
 * the way a non-{@code Error} throwable would be. That makes {@link NoClassDefFoundError} thrown here
 * surface directly from {@code Class.forName(className)}, exactly reproducing (deterministically,
 * without depending on real classpath assembly) the {@code NoClassDefFoundError:
 * net/minecraft/network/Connection} this PR's own ledger documents hitting from the real bridge
 * class during test authoring.
 */
public class NMSBridgeBrokenlink implements NMSBridge {

    static {
        // The `if` (over a non-constant expression) exists only to satisfy javac's static-initializer
        // "must be able to complete normally" flow-analysis rule (JLS 8.7) -- it is always true at
        // runtime, so this always throws.
        if (System.currentTimeMillis() >= 0) {
            throw new NoClassDefFoundError("net/minecraft/network/Connection");
        }
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public BotPlayer createBot(String name, UUID uuid, Location spawnLocation) {
        return null;
    }
}
