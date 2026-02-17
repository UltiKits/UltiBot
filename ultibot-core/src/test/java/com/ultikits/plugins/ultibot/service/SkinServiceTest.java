package com.ultikits.plugins.ultibot.service;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("SkinService")
class SkinServiceTest {

    private SkinService skinService;

    @BeforeEach
    void setUp() {
        skinService = new SkinService();
    }

    @Nested
    @DisplayName("parseSkinTexture")
    class ParseSkinTexture {

        @Test
        @DisplayName("should parse valid profile JSON")
        void shouldParseValidProfileJson() {
            // Simulated session-server response with properties array
            String json = "{\"id\":\"069a79f444e94726a5befca90e38aaf5\","
                    + "\"name\":\"Notch\","
                    + "\"properties\":[{"
                    + "\"name\":\"textures\","
                    + "\"value\":\"dGV4dHVyZVZhbHVl\","
                    + "\"signature\":\"c2lnbmF0dXJlVmFsdWU=\""
                    + "}]}";

            SkinService.SkinData data = skinService.parseSkinTexture(json);
            assertThat(data).isNotNull();
            assertThat(data.getValue()).isEqualTo("dGV4dHVyZVZhbHVl");
            assertThat(data.getSignature()).isEqualTo("c2lnbmF0dXJlVmFsdWU=");
        }

        @Test
        @DisplayName("should return null for empty properties")
        void shouldReturnNullForEmptyProperties() {
            String json = "{\"id\":\"abc\",\"name\":\"Test\",\"properties\":[]}";
            SkinService.SkinData data = skinService.parseSkinTexture(json);
            assertThat(data).isNull();
        }

        @Test
        @DisplayName("should return null for malformed JSON")
        void shouldReturnNullForMalformedJson() {
            SkinService.SkinData data = skinService.parseSkinTexture("not json");
            assertThat(data).isNull();
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            SkinService.SkinData data = skinService.parseSkinTexture(null);
            assertThat(data).isNull();
        }

        @Test
        @DisplayName("should handle property without signature")
        void shouldHandlePropertyWithoutSignature() {
            String json = "{\"id\":\"abc\",\"name\":\"Test\","
                    + "\"properties\":[{"
                    + "\"name\":\"textures\","
                    + "\"value\":\"dGV4dHVyZVZhbHVl\""
                    + "}]}";
            SkinService.SkinData data = skinService.parseSkinTexture(json);
            assertThat(data).isNotNull();
            assertThat(data.getValue()).isEqualTo("dGV4dHVyZVZhbHVl");
            assertThat(data.getSignature()).isNull();
        }
    }

    @Nested
    @DisplayName("parseUuidFromResponse")
    class ParseUuid {

        @Test
        @DisplayName("should parse UUID from Mojang response")
        void shouldParseUuid() {
            String json = "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}";
            String uuid = skinService.parseUuidFromResponse(json);
            assertThat(uuid).isEqualTo("069a79f444e94726a5befca90e38aaf5");
        }

        @Test
        @DisplayName("should return null for malformed JSON")
        void shouldReturnNullForBadJson() {
            assertThat(skinService.parseUuidFromResponse("nope")).isNull();
        }

        @Test
        @DisplayName("should return null for null")
        void shouldReturnNullForNull() {
            assertThat(skinService.parseUuidFromResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("caching")
    class Caching {

        @Test
        @DisplayName("should cache and return skin data")
        void shouldCacheAndReturn() {
            SkinService.SkinData data = new SkinService.SkinData("val", "sig");
            skinService.cacheSkin("TestPlayer", data);

            SkinService.SkinData cached = skinService.getCachedSkin("TestPlayer");
            assertThat(cached).isNotNull();
            assertThat(cached.getValue()).isEqualTo("val");
            assertThat(cached.getSignature()).isEqualTo("sig");
        }

        @Test
        @DisplayName("should return null for uncached player")
        void shouldReturnNullForUncached() {
            assertThat(skinService.getCachedSkin("Unknown")).isNull();
        }

        @Test
        @DisplayName("should be case-insensitive for cache keys")
        void shouldBeCaseInsensitive() {
            skinService.cacheSkin("Steve", new SkinService.SkinData("v", "s"));
            assertThat(skinService.getCachedSkin("steve")).isNotNull();
            assertThat(skinService.getCachedSkin("STEVE")).isNotNull();
        }
    }
}
