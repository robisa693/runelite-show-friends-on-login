package com.robisa693.onlinefriends;

class PlayerLine
{
    final String displayName;
    final String sortName;
    final int world;
    final int rank;
    final boolean moreLine;

    PlayerLine(String displayName, String sortName, int world, int rank)
    {
        this.displayName = displayName;
        this.sortName = sortName;
        this.world = world;
        this.rank = rank;
        this.moreLine = false;
    }

    PlayerLine(String moreText)
    {
        this.displayName = moreText;
        this.sortName = moreText;
        this.world = -1;
        this.rank = -1;
        this.moreLine = true;
    }
}
