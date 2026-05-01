package com.menusaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("menusaver")
public interface MenuSaverConfig extends Config
{
    @ConfigItem(
            keyName = "showGroupReminders",
            name = "Show group reminders",
            description = "Show a reminder in the panel when no group is open"
    )
    default boolean showGroupReminders()
    {
        return true;
    }
}