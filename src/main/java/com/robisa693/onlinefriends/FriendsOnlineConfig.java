package com.robisa693.onlinefriends;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(FriendsOnlineConfig.GROUP)
public interface FriendsOnlineConfig extends Config
{
    String GROUP = "friendsonline";

    @ConfigSection(
        name = "Display",
        description = "Display settings for the infobox",
        position = 0
    )
    String displaySection = "display";

    @ConfigSection(
        name = "Categories",
        description = "Which categories to show",
        position = 1
    )
    String categoriesSection = "categories";

    @Range(min = 0, max = 120)
    @ConfigItem(
        keyName = "displayTime",
        name = "Display Time",
        description = "How many seconds the infobox stays visible before disappearing (0 = always visible)",
        section = displaySection,
        position = 0
    )
    default int displayTime()
    {
        return 15;
    }

    @ConfigItem(
        keyName = "showWorld",
        name = "Show World",
        description = "Show world numbers next to player names",
        section = displaySection,
        position = 1
    )
    default boolean showWorld()
    {
        return true;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "maxPlayers",
        name = "Max Players",
        description = "Maximum number of players to show per category (0 = no limit)",
        section = displaySection,
        position = 2
    )
    default int maxPlayers()
    {
        return 0;
    }

    @ConfigItem(
        keyName = "showFriends",
        name = "Show Friends",
        description = "Show online friends from your friends list",
        section = categoriesSection,
        position = 0
    )
    default boolean showFriends()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showClanChat",
        name = "Show Clan Chat",
        description = "Show members currently in your clan chat channel",
        section = categoriesSection,
        position = 1
    )
    default boolean showClanChat()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showFriendsChat",
        name = "Show Chat Channel",
        description = "Show members currently in your chat channel",
        section = categoriesSection,
        position = 2
    )
    default boolean showFriendsChat()
    {
        return true;
    }
}
