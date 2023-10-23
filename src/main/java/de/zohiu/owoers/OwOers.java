package de.zohiu.owoers;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.commands.MultiverseCommand;
import de.zohiu.owoers.commands.Start;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class OwOers extends JavaPlugin {

    public static MultiverseCore mv_core;
    public static HashMap<String, Game> running_games = new HashMap<>();
    public static HashMap<Player, Game> player_game_lookup = new HashMap<>();
    public static Location spawn_location;

    public static OwOers instance;

    public static String style_prefix = ChatColor.translateAlternateColorCodes('&', "&5&lOwOPure &8&l| &7");

    @Override
    public void onEnable() {
        instance = this;

        mv_core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        this.getCommand("start").setExecutor(new Start());
        this.getServer().getPluginManager().registerEvents(new Events(), this);

        spawn_location = Bukkit.getWorld("spawn").getSpawnLocation();

        // Clean up wrong worlds
        MVWorldManager worldManager = OwOers.mv_core.getMVWorldManager();
        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            if (name.contains("game")) {
                worldManager.deleteWorld(name);
            }
        }

        // Clean up players:
        for (Player player : Bukkit.getOnlinePlayers()) {
            Game.resetPlayerState(player);
        }
    }

    @Override
    public void onDisable() {
        // Delete all running games
        MVWorldManager world_manager = OwOers.mv_core.getMVWorldManager();
        for (HashMap.Entry<String, Game> set : OwOers.running_games.entrySet()) {
            Game game = set.getValue();
            game.endGame();
        }
    }
}

// Day 1
// It too me ~4 hours to get a playable, good working gameplay loop. Ingame, only styling is missing now.
// That time includes setting up server and coding stuff btw. I started from 0.
// The biggest thing I haven't done yet is game creation / lobby system.
// Games are made with multiple at once in mind. They are already seperate sessions and tablist shows correctly.
// I still need to seperate the chats between worlds, but that shouldn't be too hard.
// I'm thinking about making it 100% functional with ranks and all, but a proof-of-concept is probably enough for this.

// Day 2
// Start at 16:30
// Worked on heartbeat until 19:00. It's at least functional now.

// Okay so I just spent like 1 more hour adding styling and effects.
// Total for day 2: ~3.5 hours
// Currently at around 7.5 hours. Minecraft's playtime statistic show 7.48 hours so that seems correct.
