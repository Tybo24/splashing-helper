package com.splashinghelper;

import net.runelite.client.config.*;

@ConfigGroup("Splashing config")
public interface SplashingHelperConfig extends Config
{
	@ConfigItem(
			keyName = "splashingTimer",
			name = "Show timer",
			description = "Display a timer until splashing automatically stops.",
			position = 1
	)
	default boolean showTimer()
	{
		return true;
	}

	@ConfigItem(
			keyName = "splashingOnly",
			name = "Only show timer when splashing",
			description = "Only show the timer when your total magic attack bonus is -65 or lower and 'staff' is in your weapon name, indicating you're splashing.",
			position = 2
	)
	default boolean splashingOnly()
	{
		return true;
	}
	
	@ConfigItem(
			keyName = "notifyExpire",
			name = "Notify expiration",
			description = "Sends a notification when the timer expires.",
			position = 3
	)
	default Notification notifyExpire()
	{
		return Notification.OFF;
	}

	@ConfigItem(
			keyName = "notifyNPCDeath",
			name = "Notify NPC death",
			description = "Sends a notification when the NPC you are currently splashing on dies (pesky cats!).",
			position = 4
	)
	default Notification notifyNPCDeath()
	{
		return Notification.OFF;
	}

	@ConfigItem(
			keyName = "notifyExpireBuffer",
			name = "Notify expiration buffer",
			description = "Sends a alert X amount of time before the timer expires. " +
					"0 means do not send an alert before the timer is up.",
			position = 5
	)
	@Units(Units.SECONDS)
	default int notifyExpireBuffer()
	{
		return 60;
	}
}
