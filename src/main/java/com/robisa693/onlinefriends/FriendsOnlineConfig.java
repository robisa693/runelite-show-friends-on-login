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
        description = "Display settings for the overlay",
        position = 0
    )
    String displaySection = "display";

    @ConfigSection(
        name = "Categories",
        description = "Which categories to show",
        position = 1
    )
    String categoriesSection = "categories";

    @ConfigSection(
        name = "Appearance",
        description = "Visual settings for the overlay",
        position = 2
    )
    String appearanceSection = "appearance";

    @Range(min = 0, max = 120)
    @ConfigItem(
        keyName = "displayTime",
        name = "Display Time (seconds)",
        description = "How many seconds the overlay stays visible before hiding (0 = never hide)",
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
        return 20;
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

    @ConfigItem(
        keyName = "showRanks",
        name = "Show Ranks",
        description = "Show rank icons next to player names in clan chat and chat channel",
        section = appearanceSection,
        position = 0
    )
    default boolean showRanks()
    {
        return true;
    }

    @ConfigItem(
        keyName = "sortMode",
        name = "Sort Mode",
        description = "How to sort players within each category",
        section = appearanceSection,
        position = 1
    )
    default SortMode sortMode()
    {
        return SortMode.ALPHABETICAL;
    }

    enum SortMode
    {
        ALPHABETICAL,
        RANK,
        WORLD
    }
}
