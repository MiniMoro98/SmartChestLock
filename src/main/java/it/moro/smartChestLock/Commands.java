package it.moro.smartChestLock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class Commands implements CommandExecutor, TabCompleter {
    private static SmartChestLock plugin;
    private static FileConfiguration config;
    File fileConfig;
    Events event;

    public Commands(SmartChestLock plugin) {
        Commands.plugin = plugin;
        fileConfig = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(fileConfig);
        event = new Events(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("lock") || command.getName().equalsIgnoreCase("unlock")) {
            if (sender instanceof Player player) {
                elaborazione(player, command.getName(), args);
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("smartchestlock")) {
            if(args.length == 1){
                if(args[0].equalsIgnoreCase("reload")){
                    if (sender instanceof Player player) {
                        if(player.hasPermission("smartchestlock.reload")){
                            reloadConfig();
                            player.sendMessage("§a[SmartChestLock] Configuration reloaded!");
                        }
                    } else {
                        reloadConfig();
                        plugin.getLogger().info("\u001B[32m[SmartChestLock] Configuration reloaded!\u001B[0m");
                    }
                }
            }
        }
        return false;
    }

    void elaborazione(Player player,String command, String[] args){
        if(command.equalsIgnoreCase("lock")) {
            if (args.length == 0) {
                Block block = player.getTargetBlockExact(config.getInt("max-distance-lock"));
                if(block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)){
                    String uuid = event.getUUID(block);
                    if(uuid != null){
                        if(uuid.equalsIgnoreCase(player.getUniqueId().toString())){
                           event.printInfo(block, player);
                        } else {
                            player.sendMessage("Questa chest è protetta da " + event.getNameFromUUID(UUID.fromString(uuid)));
                        }
                    } else {
                        if(event.isDoubleChest(block)){
                            Block second = event.getDoubleChestBlock(block);
                            event.assegnaUUID(block, player, true);
                            event.assegnaUUID(second, player, false);
                            event.printInfo(block, player);
                        } else {
                            event.assegnaUUID(block, player, true);
                            event.printInfo(block, player);
                        }
                    }
                }
            } else if (args.length == 1) {
                if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")){
                    player.sendMessage("§eNon hai inserito il nome del giocatore!\n§eUsa: /lock add|remove <player>");
                }
            } else if (args.length == 2) {
                if(args[0].equalsIgnoreCase("add")){
                    OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
                    if(target != null){
                        Block block = player.getTargetBlockExact(config.getInt("max-distance-lock"));
                        if(block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)){
                            String uuid = event.getUUID(block);
                            if(uuid != null){
                                if(uuid.equalsIgnoreCase(player.getUniqueId().toString())){
                                    if(event.isDoubleChest(block)){
                                        event.aggiungiGiocatore(player, target.getName(), block, true);
                                        event.aggiungiGiocatore(player, target.getName(), event.getDoubleChestBlock(block), false);
                                    } else {
                                        event.aggiungiGiocatore(player, target.getName(), block, true);
                                    }
                                } else {
                                    player.sendMessage("§cQuesta chest non ti appartiene!");
                                }
                            }
                        }
                    }
                } else if(args[0].equalsIgnoreCase("remove")){
                    OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
                    if(target != null){
                        Block block = player.getTargetBlockExact(config.getInt("max-distance-lock"));
                        if(block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)){
                            String uuid = event.getUUID(block);
                            if(uuid != null){
                                if(uuid.equalsIgnoreCase(player.getUniqueId().toString())){
                                    if(event.isDoubleChest(block)){
                                        event.rimuoviGiocatore(player, target.getName(), block, true);
                                        event.rimuoviGiocatore(player, target.getName(), event.getDoubleChestBlock(block), false);
                                    } else {
                                        event.rimuoviGiocatore(player, target.getName(), block, true);
                                    }
                                } else {
                                    player.sendMessage(getString());
                                }
                            }
                        }
                    }
                }
            }
        } else if(command.equalsIgnoreCase("unlock")){
            if (args.length == 0) {
                Block block = player.getTargetBlockExact(config.getInt("max-distance-lock"));
                if(block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)){
                    String uuid = event.getUUID(block);
                    if(uuid != null){
                        if(uuid.equalsIgnoreCase(player.getUniqueId().toString())){
                            if(event.isDoubleChest(block)){
                                event.rimuoviUUID(block, player, true);
                                event.rimuoviUUID(event.getDoubleChestBlock(block), player, false);
                            } else {
                                event.rimuoviUUID(block, player,true);

                            }
                        } else {
                            player.sendMessage("§cNon puoi sbloccare chest di altri giocatori!");
                        }
                    } else {
                        player.sendMessage("§eQuesta chest non è bloccata!");
                    }
                }
            }
        }
    }

    void reloadConfig(){
        config = YamlConfiguration.loadConfiguration(fileConfig);
        Events event = new Events(plugin);
        event.reloadConfig();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase("smartchestlock")) {
            Player player = (Player) sender;
            if (player.hasPermission("smartchestlock.reload")) {
                if (args.length == 1) {
                    return List.of("reload");
                }
                return Collections.emptyList();
            }
        }
        return null;
    }

    String getString() {
        if (config.getString("message.msg2") != null) {
            return Objects.requireNonNull(config.getString("message.msg2")).replaceAll("&", "§");
        }
        plugin.getLogger().info("Entry not found: " + "message.msg2");
        return "";
    }
}
