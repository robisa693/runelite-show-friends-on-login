package com.robisa693.onlinefriends;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class FriendsOnlineOverlay extends Overlay
{
    private static final int PADDING = 8;
    private static final int COL_GAP = 16;
    private static final int LINE_HEIGHT = 16;
    private static final Color BG_COLOR = new Color(0, 0, 0, 180);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 50);
    private static final Color HEADER_COLOR = new Color(200, 200, 150);
    private static final Color NAME_COLOR = new Color(220, 220, 255);
    private static final Color WORLD_COLOR = new Color(140, 200, 140);

    private List<String> friendLines;
    private List<String> clanLines;
    private List<String> chatLines;
    private String channelName;
    private boolean visible;

    public FriendsOnlineOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_LOW);
    }

    public void setData(List<String> friendLines, List<String> clanLines, List<String> chatLines, String channelName)
    {
        this.friendLines = friendLines;
        this.clanLines = clanLines;
        this.chatLines = chatLines;
        this.channelName = channelName;
    }

    public boolean isEmpty()
    {
        return (friendLines == null || friendLines.isEmpty())
            && (clanLines == null || clanLines.isEmpty())
            && (chatLines == null || chatLines.isEmpty());
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!visible)
        {
            return null;
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = graphics.getFontMetrics();

        Column[] cols = new Column[3];
        int colCount = 0;

        if (friendLines != null && !friendLines.isEmpty())
        {
            cols[colCount++] = new Column("Friends", friendLines, fm, graphics);
        }
        if (clanLines != null && !clanLines.isEmpty())
        {
            cols[colCount++] = new Column("Clan Chat", clanLines, fm, graphics);
        }
        if (chatLines != null && !chatLines.isEmpty())
        {
            String label = channelName != null ? channelName : "Chat Channel";
            cols[colCount++] = new Column(label, chatLines, fm, graphics);
        }

        if (colCount == 0)
        {
            visible = false;
            return null;
        }

        int maxRows = 0;
        int totalW = PADDING;
        for (int i = 0; i < colCount; i++)
        {
            if (cols[i].rowCount > maxRows) maxRows = cols[i].rowCount;
            if (i > 0) totalW += COL_GAP;
            totalW += cols[i].width;
        }
        totalW += PADDING;

        int headH = LINE_HEIGHT;
        int bodyH = maxRows * LINE_HEIGHT;
        int totalH = PADDING + headH + bodyH + PADDING + 4;

        graphics.setColor(BG_COLOR);
        graphics.fillRoundRect(0, 0, totalW, totalH, 8, 8);
        graphics.setColor(BORDER_COLOR);
        graphics.drawRoundRect(0, 0, totalW, totalH, 8, 8);

        int x = PADDING;
        for (int i = 0; i < colCount; i++)
        {
            drawColumn(graphics, fm, cols[i], x, PADDING);
            x += cols[i].width + COL_GAP;
        }

        return new Dimension(totalW, totalH);
    }

    private void drawColumn(Graphics2D graphics, FontMetrics fm, Column col, int x, int y)
    {
        int ascent = fm.getAscent();

        graphics.setColor(HEADER_COLOR);
        graphics.drawString(col.header, x, y + ascent);

        int y2 = y + LINE_HEIGHT;
        for (int i = 0; i < col.rowCount; i++)
        {
            Row row = col.rows[i];
            graphics.setColor(NAME_COLOR);
            graphics.drawString(row.name, x, y2 + ascent);
            if (row.world != null)
            {
                graphics.setColor(WORLD_COLOR);
                graphics.drawString(row.world, x + fm.stringWidth(row.name), y2 + ascent);
            }
            y2 += LINE_HEIGHT;
        }
    }

    private static class Row
    {
        final String name;
        final String world;

        Row(String name, String world)
        {
            this.name = name;
            this.world = world;
        }
    }

    private static class Column
    {
        final String header;
        final Row[] rows;
        final int rowCount;
        final int width;

        Column(String label, List<String> lines, FontMetrics fm, Graphics2D graphics)
        {
            this.header = label + " (" + lines.size() + "):";
            this.rows = new Row[lines.size()];
            this.rowCount = lines.size();

            int maxW = fm.stringWidth(header);
            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i);
                if (line.startsWith("&&WORLD&&"))
                {
                    String rest = line.substring(9);
                    int sep = rest.indexOf("&&");
                    String n = sep >= 0 ? rest.substring(0, sep) : rest;
                    String w = sep >= 0 ? rest.substring(sep + 2) : null;
                    rows[i] = new Row(n, w);
                    int nw = fm.stringWidth(n);
                    if (w != null) nw += fm.stringWidth(w);
                    if (nw > maxW) maxW = nw;
                }
                else
                {
                    rows[i] = new Row(line, null);
                    int nw = fm.stringWidth(line);
                    if (nw > maxW) maxW = nw;
                }
            }
            this.width = maxW;
        }
    }
}
