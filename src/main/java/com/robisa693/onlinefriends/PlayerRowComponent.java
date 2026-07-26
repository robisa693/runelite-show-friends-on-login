package com.robisa693.onlinefriends;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

class PlayerRowComponent implements LayoutableRenderableEntity
{
    private static final int ICON_GAP = 3;
    private static final int RIGHT_PAD = 2;

    private BufferedImage rankIcon;
    private String name;
    private String world;
    private Color worldColor;
    private Point preferredLocation = new Point();
    private Dimension preferredSize = new Dimension();
    private Rectangle bounds = new Rectangle();

    void setRankIcon(BufferedImage icon)
    {
        this.rankIcon = icon;
    }

    void setName(String name)
    {
        this.name = name;
    }

    void setWorld(String world)
    {
        this.world = world;
    }

    void setWorldColor(Color worldColor)
    {
        this.worldColor = worldColor;
    }

    @Override
    public void setPreferredLocation(Point p)
    {
        this.preferredLocation = p;
    }

    @Override
    public void setPreferredSize(Dimension d)
    {
        this.preferredSize = d;
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        int x = preferredLocation.x;
        int y = preferredLocation.y;
        FontMetrics fm = graphics.getFontMetrics();
        int baseline = y + fm.getAscent();
        int rowHeight = fm.getHeight();

        if (rankIcon != null)
        {
            int iconY = y + (rowHeight - rankIcon.getHeight()) / 2;
            graphics.drawImage(rankIcon, x, iconY, null);
            x += rankIcon.getWidth() + ICON_GAP;
            rowHeight = Math.max(rowHeight, rankIcon.getHeight());
        }

        graphics.setColor(Color.WHITE);
        graphics.drawString(name, x, baseline);

        if (world != null)
        {
            int rightX = preferredLocation.x + preferredSize.width - fm.stringWidth(world) - RIGHT_PAD;
            graphics.setColor(worldColor != null ? worldColor : Color.WHITE);
            graphics.drawString(world, rightX, baseline);
        }

        bounds.setBounds(preferredLocation.x, preferredLocation.y, preferredSize.width, rowHeight);
        return new Dimension(preferredSize.width, rowHeight);
    }
}
