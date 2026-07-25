package com.robisa693.onlinefriends;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class FriendsOnlineOverlay extends OverlayPanel
{
    private FriendsOnlinePlugin plugin;
    private boolean visible;

    @Inject
    public FriendsOnlineOverlay(FriendsOnlinePlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!visible)
        {
            return null;
        }

        List<String> friendLines = plugin.getFriendLines();
        List<String> clanLines = plugin.getClanLines();
        List<String> chatLines = plugin.getChatLines();
        String channelName = plugin.getChannelName();

        if ((friendLines == null || friendLines.isEmpty())
            && (clanLines == null || clanLines.isEmpty())
            && (chatLines == null || chatLines.isEmpty()))
        {
            return null;
        }

        if (friendLines != null && !friendLines.isEmpty())
        {
            panelComponent.getChildren().add(TitleComponent.builder()
                .text("Friends (" + friendLines.size() + "):")
                .build());
            for (String line : friendLines)
            {
                String[] parts = parseLine(line);
                panelComponent.getChildren().add(LineComponent.builder()
                    .left(parts[0])
                    .right(parts[1])
                    .rightColor(parts[1] != null ? Color.GREEN : null)
                    .build());
            }
        }

        if (clanLines != null && !clanLines.isEmpty())
        {
            if (!panelComponent.getChildren().isEmpty())
            {
                panelComponent.getChildren().add(TitleComponent.builder().text("").build());
            }
            panelComponent.getChildren().add(TitleComponent.builder()
                .text("Clan Chat (" + clanLines.size() + "):")
                .build());
            for (String line : clanLines)
            {
                String[] parts = parseLine(line);
                panelComponent.getChildren().add(LineComponent.builder()
                    .left(parts[0])
                    .right(parts[1])
                    .rightColor(parts[1] != null ? Color.GREEN : null)
                    .build());
            }
        }

        if (chatLines != null && !chatLines.isEmpty())
        {
            if (!panelComponent.getChildren().isEmpty())
            {
                panelComponent.getChildren().add(TitleComponent.builder().text("").build());
            }
            String label = channelName != null ? channelName : "Chat Channel";
            panelComponent.getChildren().add(TitleComponent.builder()
                .text(label + " (" + chatLines.size() + "):")
                .build());
            for (String line : chatLines)
            {
                String[] parts = parseLine(line);
                panelComponent.getChildren().add(LineComponent.builder()
                    .left(parts[0])
                    .right(parts[1])
                    .rightColor(parts[1] != null ? Color.GREEN : null)
                    .build());
            }
        }

        return super.render(graphics);
    }

    private static String[] parseLine(String line)
    {
        if (line.startsWith("&&WORLD&&"))
        {
            String rest = line.substring(9);
            int sep = rest.indexOf("&&");
            if (sep >= 0)
            {
                return new String[]{rest.substring(0, sep), rest.substring(sep + 2)};
            }
            return new String[]{rest, null};
        }
        return new String[]{line, null};
    }
}
