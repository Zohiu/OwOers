package de.zohiu.owoers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Events implements Listener {

    // Hide players from other world
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent ev) {
        Player p = ev.getPlayer();

        for(Player op : Bukkit.getOnlinePlayers()) {
            if(p.getWorld() != op.getWorld()) {
                p.hidePlayer(op);
                op.hidePlayer(p);
            } else {
                p.showPlayer(op);
                op.showPlayer(p);
            }
        }
    }

    // Handle attacking in general
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (OwOers.player_game_lookup.containsKey(attacker)) {
            Game player_game = OwOers.player_game_lookup.get(attacker);

            // Disable friendly fire
            if (    player_game.infected.contains(attacker) && player_game.infected.contains(victim)
                    || player_game.humans.contains(attacker) && player_game.humans.contains(victim) )  {
                event.setCancelled(true);
                return;
            }

            // Disable on grace period
            if (player_game.grace_period) {
                event.setCancelled(true);
                attacker.sendMessage(OwOers.style_prefix + "Please wait for the grace period to end.");
            }
        }
    }

    // Infect players on death
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (OwOers.player_game_lookup.containsKey(player)) {
            Game player_game = OwOers.player_game_lookup.get(player);

            Player killer = null;
            EntityDamageEvent e = event.getEntity().getLastDamageCause();
            if ((e instanceof EntityDamageByEntityEvent)) {
                EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) e;

                if ((nEvent.getDamager() instanceof Player)) {
                    killer = (Player)nEvent.getDamager();
                }
            }

            player_game.infectPlayer(player, killer);
        }
    }

    // Remove players on leave
    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (OwOers.player_game_lookup.containsKey(player)) {
            Game player_game = OwOers.player_game_lookup.get(player);
            player_game.removePlayer(player);
        }
    }

    // Reset players on join
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Game.resetPlayerState(player);
        player.teleport(OwOers.spawn_location);
    }

    // Lock players in map
    // Block break stuff like autosmelt
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (OwOers.player_game_lookup.containsKey(player)) {
            Material block_type = event.getBlock().getType();

            if (block_type == Material.WARPED_STEM) {
                event.setCancelled(true);
                return;
            }

            Material smelted;
            switch (block_type) {
                case IRON_ORE: smelted = Material.IRON_INGOT; break;
                case GOLD_ORE: smelted = Material.GOLD_INGOT; break;
                default: smelted = null;
            }

            if (smelted != null) {
                event.getBlock().setType(Material.AIR);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(smelted, 1));
            }
        }
    }

    // Prevent moving before game starts
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (OwOers.player_game_lookup.containsKey(player)) {
            Game player_game = OwOers.player_game_lookup.get(player);
            if (!player_game.game_started) {
                event.setCancelled(true);
            }
        }
    }

    // Lock armor slots and crafting for infected
    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (OwOers.player_game_lookup.containsKey(player)) {
            Game player_game = OwOers.player_game_lookup.get(player);
            if (player_game.infected.contains(player)) {
                if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.getSlotType() == InventoryType.SlotType.CRAFTING) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();

        // Send to players in lobby
        if (!OwOers.player_game_lookup.containsKey(player)) {
            for (Player loop_player : Bukkit.getOnlinePlayers()) {
                if (!OwOers.player_game_lookup.containsKey(loop_player)) {
                    loop_player.sendMessage(event.getMessage());
                    loop_player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lPLAYER &r&7" + player.getName() + " &8&l» &7") + event.getMessage());
                }
            }

            return;
        }

        // Send to players ingame
        Game player_game = OwOers.player_game_lookup.get(player);
        if (player_game.humans.contains(player)) {
            player_game.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&lHUMAN &r&e" + player.getName() + " &8&l» &7") + event.getMessage(), player_game.players);
        } else {
            player_game.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&lINFECTED &r&2" + player.getName() + " &8&l» &7") + event.getMessage(), player_game.players);
        }
    }
}
