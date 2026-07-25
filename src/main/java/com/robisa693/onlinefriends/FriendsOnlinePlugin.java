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
    private static final int INIT_TICKS = 3;
    private static final int RETRY_TICKS = 4;
    private static final int MAX_RETRIES = 15;

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
    private int retryCount;

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
            ticksUntilAction = INIT_TICKS;
            retryCount = 0;
        }
    }

    public void onGameTick(GameTick event)
    {
        if (ticksUntilAction < 0)
        {
            return;
        }

        ticksUntilAction--;

        if (ticksUntilAction > 0)
        {
            return;
        }

        if (showing)
        {
            hideOverlay();
            return;
        }

        attemptShowOverlay();
    }

    private void attemptShowOverlay()
    {
        refreshOverlayData();

        if (overlay.isEmpty())
        {
            retryCount++;
            if (retryCount < MAX_RETRIES)
            {
                ticksUntilAction = RETRY_TICKS;
                return;
            }
            ticksUntilAction = -1;
            return;
        }

        overlayManager.add(overlay);
        showing = true;

        int displayTicks = config.displayTime() * 1000 / 600;
        ticksUntilAction = displayTicks > 0 ? displayTicks : -1;
    }

    private void refreshOverlayData()
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

        overlay.setData(friendLines, clanLines, chatLines, channelName);
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
