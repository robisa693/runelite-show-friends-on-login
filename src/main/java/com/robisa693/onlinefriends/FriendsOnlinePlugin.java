package com.robisa693.onlinefriends;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
    name = "Show Friends On Login",
    description = "Shows a panel on login with online friends, clan chat members, and chat channel members",
    tags = {"friends", "online", "clan", "chat", "world", "overlay", "channel"}
)
public class FriendsOnlinePlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private FriendsOnlineConfig config;

    @Inject
    private OverlayManager overlayManager;

    private FriendsOnlineOverlay overlay;
    private List<PlayerLine> friendLines = Collections.emptyList();
    private List<PlayerLine> clanLines = Collections.emptyList();
    private List<PlayerLine> chatLines = Collections.emptyList();
    private int hideTicks = -1;
    private boolean shownOnce;
    private int dataRetries;

    private static final String[] RANK_SYMBOLS = {"", "\u25CB", "\u25CF", "\u25C7", "\u25A1", "\u2606", "\u2605", "\u2726", "\u265B", "\u265A", "\u2694", "\u2720"};

    @Provides
    FriendsOnlineConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FriendsOnlineConfig.class);
    }

    public List<PlayerLine> getFriendLines()
    {
        return friendLines;
    }

    public List<PlayerLine> getClanLines()
    {
        return clanLines;
    }

    public List<PlayerLine> getChatLines()
    {
        return chatLines;
    }

    public String getChannelName()
    {
        return "Chat-channel";
    }

    public int getLocalWorld()
    {
        return client.getWorld();
    }

    public boolean getShowRanks()
    {
        return config.showRanks();
    }

    public FriendsOnlineConfig.SortMode getSortMode()
    {
        return config.sortMode();
    }

    @Override
    protected void startUp()
    {
        overlay = new FriendsOnlineOverlay(this);
        overlay.setVisible(false);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        friendLines = Collections.emptyList();
        clanLines = Collections.emptyList();
        chatLines = Collections.emptyList();
        hideTicks = -1;
        shownOnce = false;
        dataRetries = 0;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            friendLines = Collections.emptyList();
            clanLines = Collections.emptyList();
            chatLines = Collections.emptyList();
            overlay.setVisible(false);
            hideTicks = -1;
            shownOnce = false;
            dataRetries = 0;
        }
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged event)
    {
        if (event.getClanChannel() == null)
        {
            return;
        }

        clanLines = buildClanLines();

        if (overlay.isVisible())
        {
            overlay.invalidateData();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals(FriendsOnlineConfig.GROUP))
        {
            return;
        }

        buildData();
        overlay.invalidateData();

        if (config.displayTime() == 0 && hasData())
        {
            overlay.setVisible(true);
            hideTicks = -1;
        }
        else if (overlay.isVisible())
        {
            hideTicks = config.displayTime() * 1000 / 600;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (hideTicks > 0)
        {
            hideTicks--;
            if (hideTicks == 0)
            {
                overlay.setVisible(false);
                friendLines = Collections.emptyList();
                clanLines = Collections.emptyList();
                chatLines = Collections.emptyList();
                hideTicks = -1;
            }
            return;
        }

        if (dataRetries > 10)
        {
            return;
        }

        if (client.getTickCount() % 5 != 0)
        {
            return;
        }

        dataRetries++;

        buildData();

        if (hasData())
        {
            if (!shownOnce)
            {
                overlay.setVisible(true);
                shownOnce = true;
                if (config.displayTime() > 0)
                {
                    hideTicks = config.displayTime() * 1000 / 600;
                }
            }
            else
            {
                overlay.invalidateData();
            }
        }
    }

    private void buildData()
    {
        friendLines = buildFriendLines();
        clanLines = buildClanLines();
        chatLines = buildChatLines();
    }

    private boolean hasData()
    {
        return !friendLines.isEmpty() || !clanLines.isEmpty() || !chatLines.isEmpty();
    }

    private List<PlayerLine> buildFriendLines()
    {
        if (!config.showFriends())
        {
            return Collections.emptyList();
        }

        net.runelite.api.FriendContainer container = client.getFriendContainer();
        if (container == null)
        {
            return Collections.emptyList();
        }

        Friend[] friends = container.getMembers();
        if (friends == null)
        {
            return Collections.emptyList();
        }

        List<PlayerLine> lines = new ArrayList<>();
        boolean showWorld = config.showWorld();
        boolean showRanks = config.showRanks();

        for (Friend friend : friends)
        {
            if (friend.getWorld() > 0)
            {
                String cleanName = Text.toJagexName(friend.getName());
                String displayName = cleanName;
                lines.add(new PlayerLine(displayName, cleanName, showWorld ? friend.getWorld() : -1, -1));
            }
        }

        sortLines(lines);
        return trimLines(lines);
    }

    private List<PlayerLine> buildClanLines()
    {
        if (!config.showClanChat())
        {
            return Collections.emptyList();
        }

        ClanChannel channel = client.getClanChannel();
        if (channel == null)
        {
            channel = client.getGuestClanChannel();
        }
        if (channel == null)
        {
            return Collections.emptyList();
        }

        List<ClanChannelMember> members = channel.getMembers();
        if (members == null || members.isEmpty())
        {
            return Collections.emptyList();
        }

        String localName = getLocalPlayerName();
        List<PlayerLine> lines = new ArrayList<>();
        boolean showWorld = config.showWorld();
        boolean showRanks = config.showRanks();

        for (ClanChannelMember member : members)
        {
            String cleanName = Text.toJagexName(member.getName());
            if (cleanName.equals(localName))
            {
                continue;
            }
            int rankOrdinal = member.getRank().getRank();
            String displayName = showRanks ? rankSymbol(rankOrdinal) + cleanName : cleanName;
            lines.add(new PlayerLine(displayName, cleanName, showWorld ? member.getWorld() : -1, rankOrdinal));
        }

        sortLines(lines);
        return trimLines(lines);
    }

    private List<PlayerLine> buildChatLines()
    {
        if (!config.showFriendsChat())
        {
            return Collections.emptyList();
        }

        FriendsChatManager manager = client.getFriendsChatManager();
        if (manager == null)
        {
            return Collections.emptyList();
        }

        FriendsChatMember[] members = manager.getMembers();
        if (members == null || members.length == 0)
        {
            return Collections.emptyList();
        }

        String localName = getLocalPlayerName();
        List<PlayerLine> lines = new ArrayList<>();
        boolean showWorld = config.showWorld();
        boolean showRanks = config.showRanks();

        for (FriendsChatMember member : members)
        {
            String cleanName = Text.toJagexName(member.getName());
            if (cleanName.equals(localName))
            {
                continue;
            }
            int rankOrdinal = member.getRank().getValue();
            String displayName = showRanks ? rankSymbol(rankOrdinal) + cleanName : cleanName;
            lines.add(new PlayerLine(displayName, cleanName, showWorld ? member.getWorld() : -1, rankOrdinal));
        }

        sortLines(lines);
        return trimLines(lines);
    }

    private String getLocalPlayerName()
    {
        return Optional.ofNullable(client.getLocalPlayer())
            .map(Player::getName)
            .map(Text::toJagexName)
            .orElse(null);
    }

    private static String rankSymbol(int ordinal)
    {
        if (ordinal <= 0) return "";
        int idx = Math.min(ordinal, RANK_SYMBOLS.length - 1);
        return RANK_SYMBOLS[idx] + " ";
    }

    private void sortLines(List<PlayerLine> lines)
    {
        FriendsOnlineConfig.SortMode mode = config.sortMode();
        Comparator<PlayerLine> c;

        if (mode == FriendsOnlineConfig.SortMode.RANK)
        {
            c = Comparator.<PlayerLine, Integer>comparing(pl -> pl.moreLine ? 0 : -pl.rank)
                .thenComparing(pl -> pl.moreLine ? "" : pl.sortName, String.CASE_INSENSITIVE_ORDER);
        }
        else
        {
            c = Comparator.<PlayerLine, Boolean>comparing(pl -> pl.moreLine)
                .thenComparing(pl -> pl.sortName, String.CASE_INSENSITIVE_ORDER);
        }

        lines.sort(c);
    }

    private List<PlayerLine> trimLines(List<PlayerLine> lines)
    {
        int max = config.maxPlayers();
        if (max > 0 && lines.size() > max)
        {
            int remaining = lines.size() - max;
            lines = new ArrayList<>(lines.subList(0, max));
            lines.add(new PlayerLine("+" + remaining + " more"));
        }
        return lines;
    }
}
