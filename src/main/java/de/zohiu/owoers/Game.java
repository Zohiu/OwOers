package de.zohiu.owoers;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Game {
    @NotNull
    public String game_ID;

    public boolean grace_period = false;
    public boolean game_started = false;

    public int grace_time = 60;
    private int grace_time_remaining = grace_time;
    public int game_time = 200;
    private int game_time_remaining = game_time;

    public int current_timer_task;
    public BossBar current_boss_bar;
    private String current_boss_bar_title;

    public int heartbeat_task;

    public MVWorldManager world_manager;
    public World world;
    public MultiverseWorld mv_world;

    public ArrayList<Player> players = new ArrayList<>();
    public ArrayList<Player> humans = new ArrayList<>();
    public ArrayList<Player> infected = new ArrayList<>();

    public Location human_spawn;
    public Location infected_spawn;

    public Game(@NotNull String game_ID) {
        this.game_ID = game_ID;
        this.world_manager = OwOers.mv_core.getMVWorldManager();
        this.world = Bukkit.getWorld(game_ID);
        this.mv_world = world_manager.getMVWorld(game_ID);

        mv_world.setGameMode(GameMode.SURVIVAL);
        mv_world.setDifficulty(Difficulty.PEACEFUL);
        mv_world.setEnableWeather(false);

        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        world.setClearWeatherDuration(500);
    }

    public void sendMessage(String message, ArrayList<Player> targets) {
        for (Player player : targets) {
            player.sendMessage(message);
        }
    }

    public void start() {
        current_boss_bar_title = ChatColor.DARK_PURPLE +  "Grace time remaining: ";
        current_boss_bar = Bukkit.createBossBar(current_boss_bar_title + grace_time, BarColor.PURPLE, BarStyle.SOLID);

        for (Player player : players) {
            OwOers.running_games.put(game_ID, this);
            OwOers.player_game_lookup.put(player, this);
            current_boss_bar.addPlayer(player);

            // setup player traits
            if (infected.contains(player)) {
                setupInfected(player);
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&2&lINFECTED"), ChatColor.GRAY + "Find and kill all humans!");
                player.sendMessage(OwOers.style_prefix + "You will be able to attack in " + grace_time_remaining + " seconds.");
                player.playSound(player, "block.conduit.activate", SoundCategory.MASTER, 1, 1);

            } else {
                setupHuman(player);
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&e&lHUMAN"), "Protect yourself and don't die!");
                player.sendMessage(OwOers.style_prefix + "You are invincible for " + grace_time_remaining + " more seconds.");
                player.playSound(player, "block.beacon.activate", SoundCategory.MASTER, 1, 0.5f);
            }
        }

        this.sendMessage(OwOers.style_prefix + "The game has started!", this.players);
        game_started = true;
        grace_period = true;

        // Start grace period
        current_timer_task = OwOers.instance.getServer().getScheduler().scheduleSyncRepeatingTask(
                OwOers.instance,
                () -> {
                    grace_time_remaining--;
                    current_boss_bar.setProgress((double) grace_time_remaining / (double) grace_time);
                    current_boss_bar.setTitle(current_boss_bar_title + grace_time_remaining);

                    if (grace_time_remaining == 0) {
                        sendMessage(OwOers.style_prefix + "Infected can now attack!", this.humans);
                        sendMessage(OwOers.style_prefix + "You can now attack!", this.infected);
                        for (Player player : players) {
                            player.playSound(player, "entity.ender_dragon.growl", SoundCategory.MASTER, 1, 1);
                        }
                        stopGracePeriod();
                        return;
                    }
                    if (grace_time_remaining == 10) {
                        sendMessage(OwOers.style_prefix + "Infected can attack in 10 seconds!", this.humans);
                        sendMessage(OwOers.style_prefix + "You can attack in 10 seconds!", this.infected);
                        for (Player player : players) {
                            player.playSound(player, "block.note_block.hat", SoundCategory.MASTER, 1, 1);
                        }
                    } else if (grace_time_remaining == 5) {
                        sendMessage(OwOers.style_prefix + "Infected can attack in 5 seconds!", this.humans);
                        sendMessage(OwOers.style_prefix + "You can attack in 5 seconds!", this.infected);
                        for (Player player : players) {
                            player.playSound(player, "block.note_block.hat", SoundCategory.MASTER, 1, 1);
                        }
                    }

                }, 0L, 20L
        );

        heartbeat_task = heartbeat();
    }

    public void stopGracePeriod() {
        grace_period = false;
        OwOers.instance.getServer().getScheduler().cancelTask(current_timer_task);

        current_boss_bar_title = ChatColor.LIGHT_PURPLE +  "Time remaining: ";
        current_boss_bar.setTitle(current_boss_bar_title + game_time);
        current_boss_bar.setStyle(BarStyle.SEGMENTED_20);
        current_boss_bar.setColor(BarColor.PINK);

        // Start main game
        current_timer_task = OwOers.instance.getServer().getScheduler().scheduleSyncRepeatingTask(
                OwOers.instance,
                () -> {
                    game_time_remaining--;
                    current_boss_bar.setProgress((double) game_time_remaining / (double) game_time);
                    current_boss_bar.setTitle(current_boss_bar_title + game_time_remaining);

                    if (game_time_remaining == 0) {
                        endGame();
                    }

                    if (game_time_remaining == 60) {
                        current_boss_bar_title = ChatColor.RED +  "Time remaining: ";
                        current_boss_bar.setColor(BarColor.RED);
                        sendMessage(OwOers.style_prefix + "60 seconds remaining! All humans have been revealed!", this.players);
                        for (Player player : players) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0));
                        }
                    }

                }, 0L, 20L
        );
    }

    // This is good enough. I **can** make a better version, but that takes a lot more time.
    public int heartbeat() {
        return OwOers.instance.getServer().getScheduler().scheduleSyncRepeatingTask(
                OwOers.instance,
                () -> {
                    if (grace_period) {
                        return;
                    }

                    // Check distance to all infected
                    for (Player human : humans) {
                        double max_distance = 16;
                        double current_distance = max_distance;
                        for (Player infected : infected) {
                            double distance = human.getLocation().distance(infected.getLocation());
                            if (distance < current_distance) {
                                current_distance = distance;
                            }
                        }

                        if (current_distance < max_distance) {
                            int wb_size = 256;

                            double distance_percent = 1 - (current_distance / max_distance);
                            double beats_per_second = 1 + (2 * distance_percent);

                            long tick_delay_between_beats = (long) (20 / beats_per_second);

                            long tick_beat_speed = 2 + (long) (6 * (1 - distance_percent));

                            WorldBorder tint = OwOers.instance.getServer().createWorldBorder();
                            tint.setCenter(human.getLocation());
                            tint.setSize(wb_size);
                            tint.setWarningDistance((wb_size / 2) + ((int) (distance_percent * (wb_size / 2))) + 1);
                            human.setWorldBorder(tint);
                            human.playSound(human, "minecraft:block.note_block.basedrum", SoundCategory.MASTER, 1, 0.7f);

                            for (int i = 0; i < Math.round(beats_per_second); i++) {
                                Bukkit.getScheduler().runTaskLater(OwOers.instance, () -> {
                                    WorldBorder tint1 = OwOers.instance.getServer().createWorldBorder();
                                    tint1.setCenter(human.getLocation());
                                    tint1.setSize(wb_size);
                                    tint1.setWarningDistance((wb_size / 2) + ((int) (distance_percent * (wb_size / 2))) + 1);
                                    human.setWorldBorder(tint1);
                                    human.playSound(human, "minecraft:block.note_block.basedrum", SoundCategory.MASTER, 1, 0.7f);

                                    Bukkit.getScheduler().runTaskLater(OwOers.instance, () -> {
                                        tint1.setWarningDistance(0);

                                        human.setWorldBorder(tint1);
                                        human.playSound(human, "minecraft:block.note_block.basedrum", SoundCategory.MASTER, 1, 1);
                                    }, tick_beat_speed);

                                }, tick_delay_between_beats * i);
                            }

                        } else {
                            WorldBorder notint = OwOers.instance.getServer().createWorldBorder();
                            notint.setCenter(human_spawn);
                            notint.setSize(1024);
                            notint.setWarningDistance(0);

                            human.setWorldBorder(notint);
                        }

                    }
                }, 0L, 20L
        );
    }

    public void setupHuman(Player player) {
        player.teleport(human_spawn);
        resetPlayerState(player);
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SHOVEL));

        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
    }

    public void setupInfected(Player player) {
        player.teleport(infected_spawn);
        resetPlayerState(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));
        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));

        player.getInventory().setItem(EquipmentSlot.FEET, new ItemStack(Material.LEATHER_BOOTS));
        player.getInventory().setItem(EquipmentSlot.LEGS, new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setItem(EquipmentSlot.CHEST, new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setItem(EquipmentSlot.HEAD, new ItemStack(Material.ZOMBIE_HEAD));
    }

    public void infectPlayer(Player player, Player killer) {
        // Return if the player is already infected
        if (infected.contains(player)) {
            // Schedule setup infected after 2 tick to give time for respawn
            new BukkitRunnable() { public void run() { setupInfected(player); } }.runTaskLater(OwOers.instance, 2L);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 20, 0, 1, 0, 0.1, null);

            if (killer != null) {
                this.sendMessage(OwOers.style_prefix + ChatColor.DARK_GREEN + player.getName() + ChatColor.GRAY + " has been killed by " + ChatColor.YELLOW + killer.getName() + ChatColor.GRAY + "!", this.players);
            } else {
                this.sendMessage(OwOers.style_prefix + ChatColor.DARK_GREEN + player.getName() + ChatColor.GRAY + " has killed themselves!", this.players);
            }

            return;
        }

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0, 1, 0, 0.1, null);

        if (killer != null) {
            this.sendMessage(OwOers.style_prefix + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " has been infected by " + ChatColor.DARK_GREEN + killer.getName() + ChatColor.GRAY + "!", this.players);
        } else {
            this.sendMessage(OwOers.style_prefix + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " has been infected by suicide!", this.players);
        }

        humans.remove(player);
        infected.add(player);

        if (checkIfTheGameEndsNow()) {
            Firework firework = player.getWorld().spawn(player.getEyeLocation(), Firework.class);
            FireworkMeta firework_meta = firework.getFireworkMeta();
            firework_meta.addEffect(FireworkEffect.builder()
                    .withFlicker()
                    .withTrail()
                    .withColor(Color.RED)
                    .withColor(Color.BLUE)
                    .withColor(Color.PURPLE)
                    .withFade(Color.BLACK)
                    .build()
            );
            firework.setFireworkMeta(firework_meta);
            firework_meta.setPower(1);
            firework.setAttachedTo(player);
            firework.detonate();

            for (Player loop_player : players) {
                loop_player.setGameMode(GameMode.SPECTATOR);
            }

            return;
        }

        // Schedule setup infected after 2 tick to give time for respawn
        new BukkitRunnable() { public void run() { setupInfected(player); } }.runTaskLater(OwOers.instance, 2L);
    }

    public void removePlayer(Player player) {
        OwOers.player_game_lookup.remove(player);
        resetPlayerState(player);
        players.remove(player);
        if (infected.contains(player)) {
            infected.remove(player);
            this.sendMessage(OwOers.style_prefix + ChatColor.DARK_GREEN + player.getName() + ChatColor.GRAY + " has left!", this.players);
        } else {
            humans.remove(player);
            this.sendMessage(OwOers.style_prefix + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " has left!", this.players);
        }

        checkIfTheGameEndsNow();
    }

    public boolean checkIfTheGameEndsNow() {
        if (infected.isEmpty() || humans.isEmpty()) {
            endGame();
            return true;
        }

        return false;
    }

    public static void resetPlayerState(Player player) {
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setSaturation(20.0f);
        player.setFoodLevel(20);
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    public void endGame() {
        // Always fully reset players first, in case this was a crash.
        for (Player player : players) {
            resetPlayerState(player);
            OwOers.player_game_lookup.remove(player);
            current_boss_bar.removePlayer(player);
            player.playSound(player, "entity.player.levelup", SoundCategory.MASTER, 1, 1);
            player.playSound(player, "entity.firework_rocket.twinkle_far", SoundCategory.MASTER, 1, 1);
        }

        OwOers.instance.getServer().getScheduler().cancelTask(current_timer_task);
        OwOers.instance.getServer().getScheduler().cancelTask(heartbeat_task);

        if (game_time_remaining <= 0) {
            this.sendMessage(OwOers.style_prefix + ChatColor.translateAlternateColorCodes('&', "&7Time is up! The &ehumans &7have won."), this.players);
        } else if ((infected.isEmpty() && humans.isEmpty()) || (!infected.isEmpty() && !humans.isEmpty())) {
            this.sendMessage(OwOers.style_prefix + "It's a tie!", this.players);
        } else if (infected.isEmpty()) {
            this.sendMessage(OwOers.style_prefix + ChatColor.translateAlternateColorCodes('&', "&7The &ehumans &7have won."), this.players);
        } else {
            this.sendMessage(OwOers.style_prefix + ChatColor.translateAlternateColorCodes('&', "&7The &2infected &7have won."), this.players);
        }

        // Wait like 5 seconds before actually ending the game
        new BukkitRunnable() {
            public void run() {
                for (Player player : players) {
                    player.teleport(OwOers.spawn_location);
                }

                world_manager.deleteWorld(game_ID);
                OwOers.running_games.remove(game_ID);
            }
        }.runTaskLater(OwOers.instance, 100L);
    }
}
