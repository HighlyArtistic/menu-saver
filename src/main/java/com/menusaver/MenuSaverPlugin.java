package com.menusaver;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "Menu Saver",
		description = "Save right-click menus into groups for quick access",
		tags = {"menu", "saver", "quick", "actions"}
)
public class MenuSaverPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private MenuSaverConfig config;

	private MenuSaverPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		panel = new MenuSaverPanel(this, config);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/menu_saver_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Menu Saver")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		log.debug("Menu Saver started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		log.debug("Menu Saver stopped!");
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		// Grab all entries except Cancel and the Save Menu entry itself
		List<MenuEntry> entries = Arrays.stream(client.getMenuEntries())
				.filter(e -> !e.getOption().equalsIgnoreCase("Cancel"))
				.filter(e -> !e.getOption().equalsIgnoreCase("Save Menu"))
				.collect(Collectors.toList());

		if (entries.isEmpty())
		{
			return;
		}

		// Add our custom Save Menu option at the bottom of every menu
		client.createMenuEntry(-1)
				.setOption("Save Menu")
				.setTarget("")
				.setType(net.runelite.api.MenuAction.RUNELITE)
				.onClick(e -> panel.saveCurrentMenu(entries));
	}

	public void invokeMenuEntry(MenuEntry entry)
	{
		entry.onClick();
	}

	@Provides
	MenuSaverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuSaverConfig.class);
	}
}