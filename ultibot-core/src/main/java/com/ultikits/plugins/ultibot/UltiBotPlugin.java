package com.ultikits.plugins.ultibot;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.ComponentScan;
import com.ultikits.ultitools.annotations.EnableAutoRegister;
import com.ultikits.ultitools.annotations.UltiToolsModule;

@UltiToolsModule(scanBasePackages = {"com.ultikits.plugins.ultibot"})
@EnableAutoRegister
@ComponentScan(basePackages = {"com.ultikits.plugins.ultibot"})
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
