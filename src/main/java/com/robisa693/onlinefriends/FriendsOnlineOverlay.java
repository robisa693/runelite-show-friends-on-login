package com.robisa693.onlinefriends;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
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
    private static final int MIN_COLUMN_WIDTH = 100;
    private static final int LEFT_RIGHT_GAP = 10;
    private static final int HORIZONTAL_PADDING = 12;

    private FriendsOnlinePlugin plugin;
    private boolean visible;

    private final PanelComponent friendsPanel = new PanelComponent();
    private final PanelComponent clanPanel = new PanelComponent();
    private final PanelComponent chatPanel = new PanelComponent();
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
        populated = false;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void invalidateData()
    {
        populated = false;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!visible)
        {
            return null;
        }

        List<PlayerLine> friendLines = plugin.getFriendLines();
        List<PlayerLine> clanLines = plugin.getClanLines();
        List<PlayerLine> chatLines = plugin.getChatLines();

        if ((friendLines == null || friendLines.isEmpty())
            && (clanLines == null || clanLines.isEmpty())
            && (chatLines == null || chatLines.isEmpty()))
        {
            return null;
        }

        if (!populated)
        {
            int localWorld = plugin.getLocalWorld();
            populatePanel(friendsPanel, "Friends", friendLines, localWorld);
            populatePanel(clanPanel, "Clan Chat", clanLines, localWorld);
            populatePanel(chatPanel, "Chat-channel", chatLines, localWorld);
            populated = true;

            FontMetrics fm = graphics.getFontMetrics();
            sizePanelToContent(friendsPanel, fm, "Friends", friendLines);
            sizePanelToContent(clanPanel, fm, "Clan Chat", clanLines);
            sizePanelToContent(chatPanel, fm, "Chat-channel", chatLines);

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

    private static String worldString(PlayerLine line)
    {
        if (line.world > 0) return Integer.toString(line.world);
        return null;
    }

    private static int rankIconWidth(PlayerLine line)
    {
        if (line.rankImage != null) return line.rankImage.getWidth() + 3;
        return 0;
    }

    private static void populatePanel(PanelComponent panel, String title, List<PlayerLine> lines, int localWorld)
    {
        panel.getChildren().clear();

        if (lines == null || lines.isEmpty())
        {
            return;
        }

        long actualCount = lines.stream().filter(l -> !l.moreLine).count();
        panel.getChildren().add(TitleComponent.builder()
            .text(title + " (" + actualCount + "):")
            .build());

        for (PlayerLine line : lines)
        {
            if (line.moreLine)
            {
                panel.getChildren().add(LineComponent.builder()
                    .left(line.displayName)
                    .build());
                continue;
            }

            Color worldColor = null;
            String ws = worldString(line);
            if (ws != null)
            {
                worldColor = line.world == localWorld ? Color.GREEN : Color.YELLOW;
            }

            PlayerRowComponent row = new PlayerRowComponent();
            row.setRankIcon(line.rankImage);
            row.setName(line.displayName);
            row.setWorld(ws);
            row.setWorldColor(worldColor);
            panel.getChildren().add(row);
        }
    }

    private static void sizePanelToContent(PanelComponent panel, FontMetrics fm, String title, List<PlayerLine> lines)
    {
        if (lines == null || lines.isEmpty())
        {
            return;
        }

        long actualCount = lines.stream().filter(l -> !l.moreLine).count();
        int maxWidth = fm.stringWidth(title + " (" + actualCount + "):");

        for (PlayerLine line : lines)
        {
            int width = rankIconWidth(line) + fm.stringWidth(line.displayName);
            String ws = worldString(line);
            if (ws != null)
            {
                width += LEFT_RIGHT_GAP + fm.stringWidth(ws);
            }
            maxWidth = Math.max(maxWidth, width);
        }

        int panelWidth = Math.max(MIN_COLUMN_WIDTH, maxWidth + HORIZONTAL_PADDING);
        panel.setPreferredSize(new Dimension(panelWidth, 0));
    }
}
