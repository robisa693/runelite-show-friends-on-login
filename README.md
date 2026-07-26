# Friends Online

A RuneLite plugin that shows an overlay on login with online friends, clan chat members, and friends chat members, including their worlds.

## Features

- Displays online friends, clan chat members, and friends chat members in side-by-side columns
- Shows world numbers for each player (green if same world, yellow if different)
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

Search for "Friends Online" in the RuneLite plugin hub (if published).

## Usage

The overlay appears automatically after logging in, showing all three categories side by side. It hides after the configured display time (default 15 seconds). Players on the same world as you are highlighted in green; players on other worlds are shown in yellow.

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| Display Time | 15 | Seconds the overlay stays visible (0 = always) |
| Show World | true | Show world numbers next to player names |
| Max Players | 20 | Max players shown per category (0 = no limit) |
| Show Friends | true | Show online friends |
| Show Clan Chat | true | Show clan chat members |
| Show Chat Channel | true | Show friends chat members |

## Compatibility

- RuneLite `latest.release`
- Java 11+

## License

MIT
