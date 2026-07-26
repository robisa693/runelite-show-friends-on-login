# Show Friends On Login

![Demo](docs/Recording%202026-07-26%20125910.gif)

A RuneLite plugin that shows an overlay on login with online friends, clan chat members, and friends chat members, including their worlds and rank icons.

## Features

- Displays online friends, clan chat members, and friends chat members in side-by-side columns
- Shows world numbers for each player (green if same world, yellow if different)
- Shows rank icons for clan chat and friends chat members
- Configurable auto-hide timer (0 = always visible)
- Configurable max players per category
- Toggle individual categories on/off
- Content-aware column widths

## Installation

### From source

```bash
git clone git@github.com:robisa693/runelite-show-friends-on-login.git
cd runelite-show-friends-on-login
./gradlew build
```

Copy `build/libs/online-friends-*.jar` to your RuneLite plugins folder (`~/.runelite/plugins/`).

### From RuneLite plugin hub

Search for "Show Friends On Login" in the RuneLite plugin hub (if published).

## Usage

The overlay appears automatically after logging in, showing all three categories side by side. It hides after the configured display time (default 15 seconds). Players on the same world as you are highlighted in green; players on other worlds are shown in yellow. Rank icons are shown for clan chat and friends chat members when enabled.

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| Display Time | 15 | Seconds the overlay stays visible (0 = always) |
| Show World | true | Show world numbers next to player names |
| Max Players | 20 | Max players shown per category (0 = no limit) |
| Show Friends | true | Show online friends |
| Show Clan Chat | true | Show clan chat members |
| Show Chat Channel | true | Show friends chat members |
| Show Ranks | true | Show rank icons next to names in clan chat and chat channel |
| Sort Mode | Alphabetical | How to sort players within each category (Alphabetical, Rank, World) |

## Compatibility

- RuneLite `latest.release`
- Java 11+

## License

MIT
