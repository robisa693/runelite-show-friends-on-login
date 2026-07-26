package com.robisa693.onlinefriends;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.SpriteManager;
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

    @Inject
    private ChatIconManager chatIconManager;

    @Inject
    private SpriteManager spriteManager;

    private FriendsOnlineOverlay overlay;
    private List<PlayerLine> friendLines = Collections.emptyList();
    private List<PlayerLine> clanLines = Collections.emptyList();
    private List<PlayerLine> chatLines = Collections.emptyList();
    private int hideTicks = -1;
    private boolean shownOnce;
    private int dataRetries;

    private final Map<ClanRank, BufferedImage> clanRankCache = new HashMap<>();
    private final Map<Integer, BufferedImage> chatRankCache = new HashMap<>();

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
        clanRankCache.clear();
        chatRankCache.clear();
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

        try
        {
            buildData();
        }
        catch (Exception | Error e)
        {
        }

        overlay.invalidateData();

        if (hasData())
        {
            overlay.setVisible(true);
            hideTicks = config.displayTime() > 0
                ? config.displayTime() * 1000 / 600
                : -1;
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

        for (Friend friend : friends)
        {
            if (friend.getWorld() > 0)
            {
                String cleanName = Text.toJagexName(friend.getName());
                lines.add(new PlayerLine(cleanName, cleanName, showWorld ? friend.getWorld() : -1, -1));
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
        ClanSettings settings = client.getClanSettings();
        if (channel == null)
        {
            channel = client.getGuestClanChannel();
            settings = client.getGuestClanSettings();
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
            ClanRank clanRank = member.getRank();
            int rankValue = clanRank.getRank();
            PlayerLine pl = new PlayerLine(cleanName, cleanName, showWorld ? member.getWorld() : -1, rankValue);
            if (showRanks && rankValue > 0 && settings != null)
            {
                pl.rankImage = loadClanRankIcon(clanRank, settings);
            }
            lines.add(pl);
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
            FriendsChatRank rank = member.getRank();
            int rankValue = rank.getValue();
            PlayerLine pl = new PlayerLine(cleanName, cleanName, showWorld ? member.getWorld() : -1, rankValue);
            if (showRanks && rankValue > 0)
            {
                pl.rankImage = loadFriendsChatRankIcon(rank);
            }
            lines.add(pl);
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

    private BufferedImage loadClanRankIcon(ClanRank rank, ClanSettings clanSettings)
    {
        if (rank == null)
        {
            return null;
        }

        if (clanRankCache.containsKey(rank))
        {
            return clanRankCache.get(rank);
        }

        try
        {
            ClanTitle title = clanSettings.titleForRank(rank);
            if (title == null)
            {
                clanRankCache.put(rank, null);
                return null;
            }

            BufferedImage img = null;
            try
            {
                EnumComposition enumComp = client.getEnum(EnumID.CLAN_RANK_GRAPHIC);
                if (enumComp != null)
                {
                    int spriteId = enumComp.getIntValue(title.getId());
                    img = spriteManager.getSprite(spriteId, 0);
                }
            }
            catch (Exception e)
            {
            }

            clanRankCache.put(rank, img);
            return img;
        }
        catch (Error | Exception e)
        {
            clanRankCache.put(rank, null);
            return null;
        }
    }

    private BufferedImage loadFriendsChatRankIcon(FriendsChatRank rank)
    {
        int rankValue = rank.getValue();
        if (chatRankCache.containsKey(rankValue))
        {
            return chatRankCache.get(rankValue);
        }

        BufferedImage img = null;
        try
        {
            img = chatIconManager.getRankImage(rank);
        }
        catch (Exception e)
        {
        }

        chatRankCache.put(rankValue, img);
        return img;
    }

    private void sortLines(List<PlayerLine> lines)
    {
        FriendsOnlineConfig.SortMode mode = config.sortMode();

        if (mode == FriendsOnlineConfig.SortMode.WORLD)
        {
            Map<Integer, Integer> worldCounts = new HashMap<>();
            for (PlayerLine pl : lines)
            {
                if (!pl.moreLine && pl.world > 0)
                {
                    worldCounts.merge(pl.world, 1, Integer::sum);
                }
            }

            lines.sort((a, b) ->
            {
                if (a.moreLine) return 1;
                if (b.moreLine) return -1;

                boolean aHasWorld = a.world > 0;
                boolean bHasWorld = b.world > 0;
                if (aHasWorld != bHasWorld) return aHasWorld ? -1 : 1;

                if (aHasWorld)
                {
                    int cmp = Integer.compare(
                        worldCounts.getOrDefault(b.world, 0),
                        worldCounts.getOrDefault(a.world, 0));
                    if (cmp != 0) return cmp;
                    if (a.world != b.world) return Integer.compare(a.world, b.world);
                }

                return a.sortName.compareToIgnoreCase(b.sortName);
            });
            return;
        }

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
