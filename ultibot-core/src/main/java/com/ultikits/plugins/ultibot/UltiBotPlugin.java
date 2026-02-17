package com.ultikits.plugins.ultibot;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

public class UltiBotPlugin extends UltiToolsPlugin {

    @Override
    public boolean registerSelf() {
        return true;
    }

    @Override
    public void unregisterSelf() {
        // Services are cleaned up by the IoC container
    }
}
