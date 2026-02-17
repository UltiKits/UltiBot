package com.ultikits.plugins.ultibot.model;

import com.ultikits.plugins.ultibot.api.ActionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MacroEntry {
    private final ActionType actionType;
    private final long delayTicks;
}
