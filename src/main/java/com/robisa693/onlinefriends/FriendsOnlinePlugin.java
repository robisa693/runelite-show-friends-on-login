package com.robisa693.onlinefriends;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
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
    private List<String> friendLines = Collections.emptyList();
    private List<String> clanLines = Collections.emptyList();
    private List<String> chatLines = Collections.emptyList();
    private int hideTicks = -1;
    private boolean shownOnce;
    private int dataRetries;

    @Provides
    FriendsOnlineConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FriendsOnlineConfig.class);
    }

    public List<String> getFriendLines()
    {
        return friendLines;
    }

    public List<String> getClanLines()
    {
        return clanLines;
    }

    public List<String> getChatLines()
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

    private List<String> buildFriendLines()
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

        List<String> lines = new ArrayList<>();
        boolean showWorld = config.showWorld();

        for (Friend friend : friends)
        {
            if (friend.getWorld() > 0)
            {
                String name = Text.toJagexName(friend.getName());
                if (showWorld)
                {
                    lines.add("&&WORLD&&" + name + "&&w" + friend.getWorld());
                }
                else
                {
                    lines.add(name);
                }
            }
        }

        lines.sort(null);
        return trimLines(lines);
    }

    private List<String> buildClanLines()
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
        List<String> lines = new ArrayList<>();
        boolean showWorld = config.showWorld();

        for (ClanChannelMember member : members)
        {
            String name = Text.toJagexName(member.getName());
            if (name.equals(localName))
            {
                continue;
            }
            if (showWorld)
            {
                lines.add("&&WORLD&&" + name + "&&w" + member.getWorld());
            }
            else
            {
                lines.add(name);
            }
        }

        lines.sort(null);
        return trimLines(lines);
    }

    private List<String> buildChatLines()
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
        List<String> lines = new ArrayList<>();
        boolean showWorld = config.showWorld();

        for (FriendsChatMember member : members)
        {
            String name = Text.toJagexName(member.getName());
            if (name.equals(localName))
            {
                continue;
            }
            if (showWorld)
            {
                lines.add("&&WORLD&&" + name + "&&w" + member.getWorld());
            }
            else
            {
                lines.add(name);
            }
        }

        lines.sort(null);
        return trimLines(lines);
    }

    private String getLocalPlayerName()
    {
        return Optional.ofNullable(client.getLocalPlayer())
            .map(Player::getName)
            .map(Text::toJagexName)
            .orElse(null);
    }

    private List<String> trimLines(List<String> lines)
    {
        int max = config.maxPlayers();
        if (max > 0 && lines.size() > max)
        {
            int remaining = lines.size() - max;
            lines = new ArrayList<>(lines.subList(0, max));
            lines.add("+" + remaining + " more");
        }
        return lines;
    }
}
