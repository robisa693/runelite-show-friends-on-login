package com.robisa693.onlinefriends;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Friends Online",
    description = "Shows a panel on login with online friends, clan chat members, and chat channel members",
    tags = {"friends", "online", "clan", "chat", "world", "overlay", "channel"}
)
public class FriendsOnlinePlugin extends Plugin
{
    private static final int DATA_LOAD_TICKS = 15;
    private static final int MAX_RETRY_TICKS = 50;

    @Inject
    private Client client;

    @Inject
    private FriendsOnlineConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EventBus eventBus;

    private FriendsOnlineOverlay overlay;
    private EventBus.Subscriber gameStateSubscriber;
    private EventBus.Subscriber gameTickSubscriber;
    private int ticksUntilAction = -1;
    private boolean showing;
    private int retryTicks;

    @Provides
    FriendsOnlineConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FriendsOnlineConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlay = new FriendsOnlineOverlay();
        gameStateSubscriber = eventBus.register(GameStateChanged.class, this::onGameStateChanged, 0);
        gameTickSubscriber = eventBus.register(GameTick.class, this::onGameTick, 0);
    }

    @Override
    protected void shutDown()
    {
        if (gameStateSubscriber != null)
        {
            eventBus.unregister(gameStateSubscriber);
            gameStateSubscriber = null;
        }
        if (gameTickSubscriber != null)
        {
            eventBus.unregister(gameTickSubscriber);
            gameTickSubscriber = null;
        }
        hideOverlay();
        if (overlay != null)
        {
            overlayManager.remove(overlay);
            overlay = null;
        }
    }

    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            hideOverlay();
            ticksUntilAction = DATA_LOAD_TICKS;
            retryTicks = 0;
            showing = false;
        }
    }

    public void onGameTick(GameTick event)
    {
        if (ticksUntilAction < 0)
        {
            return;
        }

        ticksUntilAction--;

        if (!showing && ticksUntilAction <= 0)
        {
            if (shouldRetry())
            {
                retryTicks++;
                if (retryTicks < MAX_RETRY_TICKS)
                {
                    ticksUntilAction = 2;
                    return;
                }
            }
            showOverlay();
            return;
        }

        if (showing && ticksUntilAction <= 0)
        {
            hideOverlay();
        }
    }

    private boolean shouldRetry()
    {
        return config.showClanChat() && client.getClanChannel() == null;
    }

    private void showOverlay()
    {
        String channelName = null;
        FriendsChatManager fcm = client.getFriendsChatManager();
        if (fcm != null)
        {
            channelName = fcm.getName();
        }

        List<String> friendLines = buildFriendLines();
        List<String> clanLines = buildClanLines();
        List<String> chatLines = buildFriendsChatLines();

        if (friendLines.isEmpty() && clanLines.isEmpty() && chatLines.isEmpty())
        {
            ticksUntilAction = -1;
            return;
        }

        overlay.setData(friendLines, clanLines, chatLines, channelName);
        overlay.setVisible(true);
        overlayManager.add(overlay);
        showing = true;

        if (config.displayTime() > 0)
        {
            ticksUntilAction = (int) (config.displayTime() * 1000f / 600f);
        }
        else
        {
            ticksUntilAction = -1;
        }
    }

    private void hideOverlay()
    {
        if (overlay != null)
        {
            overlay.setVisible(false);
            overlayManager.remove(overlay);
        }
        showing = false;
        ticksUntilAction = -1;
    }

    private List<String> buildFriendLines()
    {
        List<String> lines = new ArrayList<>();
        if (!config.showFriends())
        {
            return lines;
        }

        Friend[] friends = client.getFriendContainer().getMembers();
        if (friends == null)
        {
            return lines;
        }

        boolean showWorld = config.showWorld();
        for (Friend friend : friends)
        {
            if (friend.getWorld() > 0)
            {
                if (showWorld)
                {
                    lines.add("&&WORLD&&" + friend.getName() + "&&(w" + friend.getWorld() + ")");
                }
                else
                {
                    lines.add(friend.getName());
                }
            }
        }

        return lines;
    }

    private List<String> buildClanLines()
    {
        List<String> lines = new ArrayList<>();
        if (!config.showClanChat())
        {
            return lines;
        }

        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null)
        {
            return lines;
        }

        List<ClanChannelMember> members = clanChannel.getMembers();
        boolean showWorld = config.showWorld();

        for (ClanChannelMember member : members)
        {
            if (showWorld)
            {
                lines.add("&&WORLD&&" + member.getName() + "&&(w" + member.getWorld() + ")");
            }
            else
            {
                lines.add(member.getName());
            }
        }

        return lines;
    }

    private List<String> buildFriendsChatLines()
    {
        List<String> lines = new ArrayList<>();
        if (!config.showFriendsChat())
        {
            return lines;
        }

        FriendsChatManager manager = client.getFriendsChatManager();
        if (manager == null)
        {
            return lines;
        }

        FriendsChatMember[] members = manager.getMembers();
        if (members == null)
        {
            return lines;
        }

        boolean showWorld = config.showWorld();
        for (FriendsChatMember member : members)
        {
            if (showWorld)
            {
                lines.add("&&WORLD&&" + member.getName() + "&&(w" + member.getWorld() + ")");
            }
            else
            {
                lines.add(member.getName());
            }
        }

        return lines;
    }
}
