package com.mailmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Slf4j
@Component
@Profile("desktop")
public class DesktopBrowserLauncher {

    @Value("${app.desktop.open-browser:true}")
    private boolean openBrowser;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!openBrowser) {
            return;
        }

        int port = event.getApplicationContext().getEnvironment().getProperty("local.server.port", Integer.class, 18080);
        String url = "http://127.0.0.1:" + port + "/";

        if (!Desktop.isDesktopSupported()) {
            log.info("Desktop integration unavailable, open {} manually.", url);
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) {
            log.warn("Unable to open browser automatically, open {} manually.", url, ex);
        }
    }
}
