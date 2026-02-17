package com.ultikits.plugins.ultibot.service;

import org.junit.jupiter.api.*;
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

        @Test
        @DisplayName("should map 1.20.1 to v1_20_R1")
        void shouldMap1201() {
            String className = NMSLoader.getClassNameForVersion("1.20.1");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_20_R1.NMSBridgeV1_20_R1");
        }

        @Test
        @DisplayName("should map 1.20.4 to v1_20_R3")
        void shouldMap1204() {
            String className = NMSLoader.getClassNameForVersion("1.20.4");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_20_R3.NMSBridgeV1_20_R3");
        }

        @Test
        @DisplayName("should map 1.20.6 to v1_20_R4")
        void shouldMap1206() {
            String className = NMSLoader.getClassNameForVersion("1.20.6");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_20_R4.NMSBridgeV1_20_R4");
        }

        @Test
        @DisplayName("should map 1.21.3 to v1_21_R2")
        void shouldMap1213() {
            String className = NMSLoader.getClassNameForVersion("1.21.3");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_21_R2.NMSBridgeV1_21_R2");
        }

        @Test
        @DisplayName("should map 1.21.4 to v1_21_R3")
        void shouldMap1214() {
            String className = NMSLoader.getClassNameForVersion("1.21.4");
            assertThat(className).isEqualTo(
                "com.ultikits.plugins.ultibot.nms.v1_21_R3.NMSBridgeV1_21_R3");
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
}
