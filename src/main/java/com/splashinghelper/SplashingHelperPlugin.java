package com.splashinghelper;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Splashing Helper"
)
public class SplashingHelperPlugin extends Plugin
{
	private static final Duration SPLASHING_DURATION = Duration.ofSeconds(1200);

	private boolean notified;
	private boolean combatTimerExpiredNotify;
	private boolean active;
	private Instant combatTimerEndTime;
	private Actor splashingNPC;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private Notifier notifier;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private Client client;

	@Inject
	private SplashingHelperConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Splashing helper initialised.");
	}

	@Override
	protected void shutDown() throws Exception
	{
		removeTimer();
		combatTimerEndTime = null;
		active = false;
		notified = false;
		combatTimerExpiredNotify = false;
		splashingNPC = null;
		log.info("Splashing helper shut down.");
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		final Actor source = event.getSource();
		if (source != client.getLocalPlayer())
		{
			return;
		}

		final Actor target = event.getTarget();

		// If this is not NPC, or we have notified the 20 minute combat timer
		// return as we do not want to activate the timer again without interaction.
		if (!(target instanceof NPC) || combatTimerExpiredNotify)
		{
			active = false;
			return;
		}

		checkNpcInteraction((NPC) target);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();

		// Check if the despawned NPC is the one you are tracking
		if (npc == splashingNPC)
		{
			// NPC you were tracking has died or despawned
			this.sendNotification(NotificationType.NPC_DIED);
			active = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (combatTimerEndTime == null)
		{
			return;
		}

		final Player local = client.getLocalPlayer();

		if (local == null
				// If user has clicked in the last second then they're not idle so
				// don't send idle notification and reset the timer.
				|| System.currentTimeMillis() - client.getMouseLastPressedMillis() < 1000
				|| client.getKeyboardIdleTicks() < 10
				)
		{
			resetTimer();
			notified = false;
			combatTimerExpiredNotify = false;
        }

		if (Instant.now().isAfter(combatTimerEndTime.minusSeconds(config.notifyExpireBuffer())) && !notified)
		{
			this.sendNotification(NotificationType.TIMER_BUFFER);
		}

		if (Instant.now().isAfter(combatTimerEndTime) && !combatTimerExpiredNotify)
		{
			this.sendNotification(NotificationType.TIMER_EXPIRED);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// Still splashing. Required for a rare scenario which causes active
		// to be disabled, but I am too lazy to find where that is happening and fix it.
		// Arguably, tracking by Magic XP gained is better regardless...
		if (event.getSkill() == Skill.MAGIC &&
				(splashingNPC != null || combatTimerEndTime != null))
		{
			active = true;
		}
	}

	@Provides
	SplashingHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SplashingHelperConfig.class);
	}

	private void sendNotification (NotificationType _notificationType)
	{
		switch (_notificationType)
		{
			case NPC_DIED:
				notifier.notify(config.notifyNPCDeath(), "Your NPC has despawned!");
				splashingNPC = null; // Reset the target
				notified = true;
				break;
			case TIMER_BUFFER:
				if (config.notifyExpireBuffer() > 0)
				{
					notifier.notify(config.notifyExpire(), "Timer will soon expire. Interact with the client to continue splashing.");
					notified = true;
				}
				break;
			case TIMER_EXPIRED:
				notifier.notify(config.notifyExpire(), "20 Minute combat timer expired. You will now stop splashing.");
				combatTimerExpiredNotify = true;
				break;
		}
	}

	private void checkNpcInteraction(final NPC target)
	{
		final NPCComposition npcComposition = target.getComposition();
		final List<String> npcMenuActions = Arrays.asList(npcComposition.getActions());

		if (npcMenuActions.contains("Attack"))
		{
			// Player is most likely in combat with attack-able NPC
			active = true;
			splashingNPC = target;
			resetTimer();
		}
	}

	private int getSpellSpriteId()
	{
		Widget spellWidget = client.getWidget(WidgetInfo.COMBAT_SPELL_ICON);

		if (spellWidget != null)
		{
			return spellWidget.getSpriteId();
		}

		return SpriteID.SPELL_FIRE_STRIKE;
	}

	boolean shouldDisplayTimer()
	{
		return active && config.showTimer();
	}

	private void resetTimer()
	{
		createTimer();
	}

	private void removeTimer()
	{
		infoBoxManager.removeIf(t -> t instanceof CombatTimer);
		combatTimerEndTime = null;
	}

	private void createTimer()
	{
		removeTimer();
		combatTimerEndTime = Instant.now().plus(SplashingHelperPlugin.SPLASHING_DURATION);

		if (SplashingHelperPlugin.SPLASHING_DURATION.isNegative())
		{
			return;
		}

        BufferedImage image = spriteManager.getSprite(this.getSpellSpriteId(), 0);
		infoBoxManager.addInfoBox(new CombatTimer(SplashingHelperPlugin.SPLASHING_DURATION, image, this, config));
	}
}
