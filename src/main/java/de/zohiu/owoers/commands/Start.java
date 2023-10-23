package de.zohiu.owoers.commands;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import de.zohiu.owoers.Game;
import de.zohiu.owoers.OwOers;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class Start implements CommandExecutor {

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MVWorldManager worldManager = OwOers.mv_core.getMVWorldManager();

        if (OwOers.running_games.containsKey("game")) {
            sender.sendMessage( OwOers.style_prefix + "Game already exists. Please end it first.");
            return true;
        }

        String game_ID = "game";

        String[] maps = new String[]{"Plains_Map", "Jungle_Map", "Christmas_Map"};
        int rnd = new Random().nextInt(maps.length);

        // Create map
        worldManager.cloneWorld(maps[rnd], game_ID);

        World world = Bukkit.getWorld(game_ID);
        assert world != null;
        MultiverseWorld mv_world = worldManager.getMVWorld(game_ID);

        mv_world.setTime("noon");

        Bukkit.broadcastMessage(OwOers.style_prefix + "Game map created!");

        // Create game
        Game game = new Game(game_ID);
        game.human_spawn = new Location(world, -58.5, 177, -49.5);
        game.human_spawn.setYaw(180);
        game.infected_spawn = new Location(world, -70.5, 167, -109.5);
        game.infected_spawn.setYaw(45);

        game.players.addAll(Bukkit.getOnlinePlayers());

        // First make everyone a human
        game.humans.addAll(Bukkit.getOnlinePlayers());

        // Now make a random person infected
        Collections.shuffle(game.players);

        Player infected = game.players.get(0);
        game.humans.remove(infected);
        game.infected.add(infected);


        // Start the game
        game.start();

        return true;
    }
}