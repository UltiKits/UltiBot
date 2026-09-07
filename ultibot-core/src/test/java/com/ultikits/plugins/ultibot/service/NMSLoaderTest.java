package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.NMSBridge;
import com.ultikits.ultitools.exceptions.PluginModuleException;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NMSLoader")
class NMSLoaderTest {

    @Nested
    @DisplayName("version mapping")
    class VersionMapping {

        @Test
        @DisplayName("should map 1.21.1 to v1_21_R1 class")
        void shouldMap1211() {
            String className = NMSLoader.getClassNameForVersion("1.21.1");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_21_R1.NMSBridgeV1_21_R1");
        }

        @Test
        @DisplayName("should map 1.21 to v1_21_R1 class")
        void shouldMap121() {
            String className = NMSLoader.getClassNameForVersion("1.21");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_21_R1.NMSBridgeV1_21_R1");
        }

        @Test
        @DisplayName("should return null for unsupported version")
        void shouldReturnNullForUnsupported() {
            String className = NMSLoader.getClassNameForVersion("1.8.8");
            assertThat(className).isNull();
        }

        // 13-10/FIX-05: the reactor never built a v1_20_* or v1_21_R2/R3 module, so the table
        // was narrowed to declare only what ultibot-v1_21_R1 actually delivers. These five methods
        // used to assert the (undelivered) class name each version resolved to; they now assert
        // the version is unmapped, documenting that the removal was deliberate rather than letting
        // a future edit silently reintroduce an entry with no module behind it.

        @Test
        @DisplayName("should no longer map 1.20.1 -- no v1_20_R1 module is built")
        void shouldNoLongerMap1201() {
            assertThat(NMSLoader.getClassNameForVersion("1.20.1")).isNull();
        }

        @Test
        @DisplayName("should no longer map 1.20.4 -- no v1_20_R3 module is built")
        void shouldNoLongerMap1204() {
            assertThat(NMSLoader.getClassNameForVersion("1.20.4")).isNull();
        }

        @Test
        @DisplayName("should no longer map 1.20.6 -- no v1_20_R4 module is built")
        void shouldNoLongerMap1206() {
            assertThat(NMSLoader.getClassNameForVersion("1.20.6")).isNull();
        }

        @Test
        @DisplayName("should no longer map 1.21.3 -- no v1_21_R2 module is built")
        void shouldNoLongerMap1213() {
            assertThat(NMSLoader.getClassNameForVersion("1.21.3")).isNull();
        }

        @Test
        @DisplayName("should no longer map 1.21.4 -- no v1_21_R3 module is built")
        void shouldNoLongerMap1214() {
            assertThat(NMSLoader.getClassNameForVersion("1.21.4")).isNull();
        }
    }

    @Nested
    @DisplayName("extractMinecraftVersion")
    class ExtractVersion {

        @Test
        @DisplayName("should extract from Bukkit version string")
        void shouldExtractFromBukkitVersion() {
            String version = NMSLoader.extractMinecraftVersion("1.21.1-R0.1-SNAPSHOT");
            assertThat(version).isEqualTo("1.21.1");
        }

        @Test
        @DisplayName("should handle plain version")
        void shouldHandlePlain() {
            String version = NMSLoader.extractMinecraftVersion("1.20.4");
            assertThat(version).isEqualTo("1.20.4");
        }
    }

    @Nested
    @DisplayName("load")
    class Load {

        @Test
        @DisplayName("should refuse an unsupported MC version by name instead of warning and continuing")
        void shouldRefuseUnsupportedVersionByName() {
            Logger logger = Logger.getLogger("NMSLoaderTest.unsupported");

            assertThatThrownBy(() -> NMSLoader.load("1.8.8-R0.1-SNAPSHOT", logger))
                .isInstanceOf(PluginModuleException.class)
                .hasMessageContaining("1.8.8");
        }

        @Test
        @DisplayName("should refuse rather than instantiate when a declared version's class is missing from the build")
        void shouldRefuseWhenDeclaredClassIsMissingFromTheBuild() throws Exception {
            // Simulates the exact internal-consistency failure everyDeclaredVersionMapsToABridgeThatExists
            // exists to catch before shipping: a table entry pointing at a module that was never
            // built. Injects a bogus entry directly into VERSION_MAP (reflection, restored in
            // finally) rather than relying on any real removed module, since after this plan every
            // *real* declared entry is guaranteed present by that other test.
            Field field = NMSLoader.class.getDeclaredField("VERSION_MAP");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> versionMap = (Map<String, String>) field.get(null);
            versionMap.put("9.9.9", "vNeverBuilt");
            try {
                Logger logger = Logger.getLogger("NMSLoaderTest.declaredButMissing");
                assertThatThrownBy(() -> NMSLoader.load("9.9.9-R0.1-SNAPSHOT", logger))
                    .isInstanceOf(PluginModuleException.class)
                    .hasMessageContaining("9.9.9");
            } finally {
                versionMap.remove("9.9.9");
            }
        }

        @Test
        @DisplayName("should refuse by name, not fall through to a generic wrapper, when a declared bridge is present but fails to link")
        void shouldRefuseByNameWhenDeclaredClassFailsToLink() throws Exception {
            // 13-REVIEW-UltiBot.md WR-01: the review's own traced propagation path shows that
            // without a LinkageError catch here, this exact failure loses its version-naming
            // message on the way up -- SimpleContainer.createBean's catch (Exception e) doesn't
            // catch Error/LinkageError either, so it would propagate raw out of load() and out of
            // BotManagerImpl's constructor, surfacing to the operator as an opaque
            // "Failed to create bean: botManagerImpl" with no version or class name at all.
            // NMSBridgeBrokenlink (test-only) reproduces the real NoClassDefFoundError this PR's
            // ledger documents hitting from Class.forName on the real bridge class, deterministically,
            // via a static initializer that throws an Error (propagated as-is per JLS 12.4.2, not
            // wrapped in ExceptionInInitializerError).
            Field field = NMSLoader.class.getDeclaredField("VERSION_MAP");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> versionMap = (Map<String, String>) field.get(null);
            versionMap.put("7.7.7", "brokenlink");
            try {
                Logger logger = Logger.getLogger("NMSLoaderTest.declaredButFailsToLink");
                assertThatThrownBy(() -> NMSLoader.load("7.7.7-R0.1-SNAPSHOT", logger))
                    .isInstanceOf(PluginModuleException.class)
                    .hasMessageContaining("7.7.7")
                    .hasMessageContaining("com.ultikits.plugins.ultibot.nms.brokenlink.NMSBridgeBrokenlink");
            } finally {
                versionMap.remove("7.7.7");
            }
        }
    }

    @Nested
    @DisplayName("table-versus-artifact agreement")
    class TableVersusArtifact {

        @SuppressWarnings("unchecked")
        private Map<String, String> versionMap() throws Exception {
            Field field = NMSLoader.class.getDeclaredField("VERSION_MAP");
            field.setAccessible(true);
            return (Map<String, String>) field.get(null);
        }

        private boolean classExists(String className) {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        @Test
        @DisplayName("every declared version maps to a bridge class that exists in this build")
        void everyDeclaredVersionMapsToABridgeThatExists() throws Exception {
            // Derived from the table itself, not a hard-coded list of today's supported version --
            // a future entry added here without its module being built fails this test, and a
            // module built without its entry landing here goes unused but does not fail it (the
            // asymmetry this plan's own text describes: "adding a table entry without building its
            // module fails here, and building a module without adding its entry does not silently
            // go unused").
            Set<String> declaredClassNames = versionMap().keySet().stream()
                .map(NMSLoader::getClassNameForVersion)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            List<String> missing = declaredClassNames.stream()
                .filter(name -> !classExists(name))
                .collect(Collectors.toList());

            assertThat(missing)
                .as("bridge classes the table declares but the build does not contain")
                .isEmpty();
        }

        @Test
        @DisplayName("a supported version still loads its bridge -- narrowing the table did not narrow what works")
        void aSupportedVersionLoadsItsBridge() {
            Logger logger = Logger.getLogger("NMSLoaderTest.supported");

            NMSBridge bridge = NMSLoader.load("1.21.1-R0.1-SNAPSHOT", logger);

            assertThat(bridge).isNotNull();
            assertThat(bridge.isSupported()).isTrue();
        }

        @Test
        @DisplayName("an unsupported version is refused by name naming the supported versions")
        void anUnsupportedVersionIsRefusedByName() {
            Logger logger = Logger.getLogger("NMSLoaderTest.refusalNamesSupported");

            assertThatThrownBy(() -> NMSLoader.load("1.20.4-R0.1-SNAPSHOT", logger))
                .isInstanceOf(PluginModuleException.class)
                .hasMessageContaining("1.20.4")
                .hasMessageContaining("1.21.1");
        }
    }
}
