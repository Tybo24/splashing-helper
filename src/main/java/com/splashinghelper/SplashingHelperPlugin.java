package com.splashinghelper;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.InventoryID;

import java.awt.*;
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
	private static final double SPLASH_ACCURACY = -64;

	private boolean notified;
	private boolean combatTimerExpiredNotify;
	private boolean active;
	private Instant combatTimerEndTime;
	private Actor splashingNPC;
	private double magicAccuracy;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Notifier notifier;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private Client client;

	@Inject
	private SplashingHelperConfig config;

	@Inject
	private ItemManager itemManager;

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
		magicAccuracy = 0;
		log.info("Splashing helper shut down.");
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (config.accuracyCheck() && !this.shouldSplash())
		{
			return;
		}

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
		if (config.accuracyCheck() && !this.shouldSplash())
		{
			return;
		}

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
	public void onGameStateChanged(GameStateChanged event)
	{
		// Calculate the accuracy once logged in
		if (event.getGameState().equals(GameState.LOGIN_SCREEN))
		{
			magicAccuracy = this.getAccuracy();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().equals("accuracyCheck"))
		{
			if (config.accuracyCheck())
			{
				clientThread.invoke(this::setMagicAccuracy);

				if (!this.shouldSplash())
				{
					this.removeTimer();
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (combatTimerEndTime == null)
		{
			return;
		}

		if (config.accuracyCheck() && !this.shouldSplash())
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
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (config.accuracyCheck())
		{
			int containerId = event.getContainerId();

			// Optimisation to only re-calculate this value when the equipment changes
			// so we aren't running this code every time we need to check the accuracy stat
			if(containerId == InventoryID.EQUIPMENT.getId())
			{
				this.setMagicAccuracy();
			}
		}
	}

	private void setMagicAccuracy()
	{
		magicAccuracy = this.getAccuracy();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (config.accuracyCheck() && !this.shouldSplash())
		{
			return;
		}

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

	private void sendNotification(NotificationType _notificationType)
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
		Widget spellWidget = client.getWidget(ComponentID.COMBAT_SPELL_ICON);

		if (spellWidget != null)
		{
			return spellWidget.getSpriteId();
		}

		return SpriteID.SPELL_FIRE_STRIKE;
	}

	private boolean shouldSplash()
	{
		return magicAccuracy <= SPLASH_ACCURACY;
	}

	private Item[] getEquippedItems()
	{
		Item[] playerEquipment;
		if (client.getItemContainer(InventoryID.EQUIPMENT) != null )
		{
			playerEquipment = client.getItemContainer(InventoryID.EQUIPMENT).getItems();
		}
		else
		{
			playerEquipment = null;
		}

		return playerEquipment;
	}

	private double getAccuracy()
	{
		Item[] playerEquipment = this.getEquippedItems();
		double accuracy = 0;

		if (playerEquipment != null)
		{
			// Get Magic accuracy bonus of each equipped item
			for (Item equipmentItem: playerEquipment)
			{
				if (equipmentItem != null && equipmentItem.getId() != -1)
				{
					int equipmentID = equipmentItem.getId();

					if(itemManager.getItemStats(equipmentID) != null)
					{
						accuracy += itemManager.getItemStats(equipmentID).getEquipment().getAmagic();
					}
				}
			}

		}

		return accuracy;
	}

	boolean shouldDisplayTimer()
	{
		boolean ret = false;

		if (config.showTimer())
		{
			if (config.accuracyCheck())
			{
				ret = active && this.shouldSplash();
			}
			else
			{
				ret = active;
			}
		}

		return ret;
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
