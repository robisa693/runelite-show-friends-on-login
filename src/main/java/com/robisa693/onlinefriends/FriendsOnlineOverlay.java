package com.robisa693.onlinefriends;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class FriendsOnlineOverlay extends OverlayPanel
{
    private static final int COLUMN_GAP = 15;

    private FriendsOnlinePlugin plugin;
    private boolean visible;

    private PanelComponent friendsPanel = new PanelComponent();
    private PanelComponent clanPanel = new PanelComponent();
    private PanelComponent chatPanel = new PanelComponent();
    private boolean populated;

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

        if (!populated)
        {
            populatePanel(friendsPanel, "Friends", friendLines);
            populatePanel(clanPanel, "Clan Chat", clanLines);
            populatePanel(chatPanel, channelName != null ? channelName : "Chat Channel", chatLines);
            populated = true;

            Graphics2D warmup = (Graphics2D) graphics.create();
            warmup.setClip(0, 0, 0, 0);
            for (PanelComponent p : new PanelComponent[]{friendsPanel, clanPanel, chatPanel})
            {
                if (!p.getChildren().isEmpty())
                {
                    p.setPreferredLocation(new Point(0, 0));
                    p.render(warmup);
                }
            }
            warmup.dispose();
        }

        PanelComponent[] panels = {friendsPanel, clanPanel, chatPanel};
        int x = 0;
        int totalHeight = 0;

        for (PanelComponent panel : panels)
        {
            if (panel.getChildren().isEmpty()) continue;

            panel.setPreferredLocation(new Point(x, 0));
            Dimension dim = panel.render(graphics);

            x += dim.width + COLUMN_GAP;
            totalHeight = Math.max(totalHeight, dim.height);
        }

        if (x == 0)
        {
            return null;
        }

        return new Dimension(x - COLUMN_GAP, totalHeight);
    }

    private static void populatePanel(PanelComponent panel, String title, List<String> lines)
    {
        panel.getChildren().clear();

        if (lines == null || lines.isEmpty())
        {
            return;
        }

        panel.getChildren().add(TitleComponent.builder()
            .text(title + " (" + lines.size() + "):")
            .build());

        for (String line : lines)
        {
            String[] parts = parseLine(line);
            panel.getChildren().add(LineComponent.builder()
                .left(parts[0])
                .right(parts[1])
                .rightColor(parts[1] != null ? Color.GREEN : null)
                .build());
        }
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
