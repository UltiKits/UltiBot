package com.ultikits.plugins.ultibot.config;

import com.ultikits.ultitools.abstracts.AbstractConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.Range;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigEntity("config.yml")
public class BotConfig extends AbstractConfigEntity {

    public BotConfig() {
        super("config.yml");
    }

    @ConfigEntry(path = "max-bots-per-player", comment = "Maximum bots each player can spawn")
    @Range(min = 1, max = 100)
    private int maxBotsPerPlayer = 5;

    @ConfigEntry(path = "max-total-bots", comment = "Server-wide bot limit")
    @Range(min = 1, max = 200)
    private int maxTotalBots = 20;

    @ConfigEntry(path = "default-skin", comment = "Default skin name for bots")
    @NotEmpty
    private String defaultSkin = "Steve";

    @ConfigEntry(path = "tick-bots", comment = "Enable bot ticking for physics")
    private boolean tickBots = true;

    @ConfigEntry(path = "allow-chunk-loading", comment = "Allow bots to keep chunks loaded")
    private boolean allowChunkLoading = false;

    @ConfigEntry(path = "bot-prefix", comment = "Prefix shown in chat/tab for bots")
    private String botPrefix = "[Bot] ";

    @ConfigEntry(path = "auto-remove-on-quit", comment = "Remove bots when owner disconnects")
    private boolean autoRemoveOnQuit = true;

    @ConfigEntry(path = "auto-respawn", comment = "Auto-respawn bots after death")
    private boolean autoRespawn = true;
}
