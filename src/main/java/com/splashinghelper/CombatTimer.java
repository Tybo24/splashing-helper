package com.splashinghelper;

import net.runelite.client.ui.overlay.infobox.Timer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CombatTimer extends Timer
{
    private final SplashingHelperPlugin plugin;

    CombatTimer(Duration duration, BufferedImage image, SplashingHelperPlugin plugin, SplashingHelperConfig config)
    {
        super(duration.toMillis(), ChronoUnit.MILLIS, image, plugin);
        setTooltip("Time until splashing automatically stops.");
        this.plugin = plugin;
    }

    @Override
    public Color getTextColor()
    {
        Duration timeLeft = Duration.between(Instant.now(), getEndTime());

        if (timeLeft.getSeconds() < 60)
        {
            return Color.RED.brighter();
        }

        return Color.WHITE;
    }

    @Override
    public boolean render()
    {
        return plugin.shouldDisplayTimer() && super.render();
    }
}
