package it.moro.smartChestLock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Events implements Listener {

    private static SmartChestLock plugin;
    private static FileConfiguration config;
    File fileConfig;

    public Events(SmartChestLock plugin) {
        Events.plugin = plugin;
        fileConfig = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(fileConfig);
    }

    void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(fileConfig);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (config.getBoolean("break-protection")) {
            Block block = event.getBlock();
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                if (getUUID(block) != null) {
                    Player player = event.getPlayer();
                    if (!getUUID(block).equalsIgnoreCase(player.getUniqueId().toString())) {
                        player.sendMessage(getString("message.msg1"));
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (config.getBoolean("explosion-protection")) {
            Iterator<Block> it = event.blockList().iterator();
            Entity entity = event.getEntity();
            Player player = null;
            if (entity instanceof TNTPrimed tnt) {
                if (tnt.getSource() instanceof Player sourcePlayer) {
                    player = sourcePlayer;
                }
            }
            while (it.hasNext()) {
                Block block = it.next();
                if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                    if (getUUID(block) != null) {
                        if (player != null) {
                            if (!getUUID(block).equalsIgnoreCase(player.getUniqueId().toString())) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHopperPlace(BlockPlaceEvent event) {
        if (!config.getBoolean("hopper-protection")) return;
        if (event.getBlock().getType() == Material.HOPPER) {
            Player player = event.getPlayer();
            Block blockAbove = event.getBlock().getRelative(BlockFace.UP);
            if (blockAbove.getType() == Material.CHEST || blockAbove.getType() == Material.TRAPPED_CHEST) {
                String chestOwner = getUUID(blockAbove);
                if (chestOwner != null && !chestOwner.equalsIgnoreCase(player.getUniqueId().toString())) {
                    event.setCancelled(true);
                    player.sendMessage(getString("message.msg28"));
                }
            }
        }
    }

    void aggiungiGiocatore(Player giocatore, String playerName, Block block, boolean message) {
        if (!giocatore.hasPermission("smartchestlock.add")) {
            if(message){ giocatore.sendMessage(getString("message.msg32"));}
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (block.getState() instanceof Chest chest) {
            PersistentDataContainer data = chest.getPersistentDataContainer();
            for (int i = 1; i <= 10; i++) {
                NamespacedKey key = new NamespacedKey("smartchestlock", "uuid_" + i);
                if (data.has(key, PersistentDataType.STRING)) {
                    String value = data.get(key, PersistentDataType.STRING);
                    if (value != null && value.equals("0")) {
                        data.set(key, PersistentDataType.STRING, player.getUniqueId().toString());
                        chest.update();
                        if(message) {
                            giocatore.sendMessage(getString("message.msg15").replace("%player%", Objects.requireNonNull(player.getName())));
                        }
                        return;
                    }
                }
            }
            if(message){
                giocatore.sendMessage(getString("message.msg33"));
            }
        }
    }

    void rimuoviGiocatore(Player giocatore, String playerName, Block block, boolean message) {
        if (!giocatore.hasPermission("smartchestlock.remove")) {
            if(message){giocatore.sendMessage(getString("message.msg31"));}
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (block.getState() instanceof Chest chest) {
            PersistentDataContainer data = chest.getPersistentDataContainer();
            for (int i = 1; i <= 10; i++) {
                NamespacedKey key = new NamespacedKey("smartchestlock", "uuid_" + i);
                if (data.has(key, PersistentDataType.STRING)) {
                    String value = data.get(key, PersistentDataType.STRING);
                    if (value != null) {
                        if (value.equalsIgnoreCase(player.getUniqueId().toString())) {
                            data.set(key, PersistentDataType.STRING, "0");
                            chest.update();
                            if(message) {
                                giocatore.sendMessage(getString("message.msg14").replace("%player%", Objects.requireNonNull(player.getName())));
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    void printInfo(Block block, Player player) {
        if ((block.getState() instanceof Chest chest)) {
            List<String> info = new ArrayList<>();
            List<String> uuidList = new ArrayList<>();
            PersistentDataContainer data = chest.getPersistentDataContainer();
            for (int i = 1; i <= 10; i++) {
                NamespacedKey key = new NamespacedKey("smartchestlock", "uuid_" + i);
                if (data.has(key, PersistentDataType.STRING)) {
                    String value = data.get(key, PersistentDataType.STRING);
                    if (value != null && !value.equals("0")) {
                        uuidList.add(getNameFromUUID(UUID.fromString(value)));
                    }
                }
            }
            info.add("\n");
            info.add(" " + "§6===== SmartChestLock =====");
            info.add(" " + "Giocatori assegnati: (" + uuidList.size() + "/10)");
            for (String s : uuidList) {
                info.add(" §e> " + s);
            }
            info.add(" " + "§6========================");

            for (String text : info) {
                player.sendMessage(text);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractTest(PlayerInteractEvent event) {
        if (event.getPlayer().hasPermission("smartchestlock.investigate")) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            Block block = event.getClickedBlock();
            if (block != null && item.getType() == Material.STICK && block.getState() instanceof Chest chest) {
                event.setCancelled(true);
                PersistentDataContainer data = chest.getPersistentDataContainer();
                NamespacedKey uuidKey = new NamespacedKey("smartchestlock", "uuid");
                if (data.has(uuidKey, PersistentDataType.STRING)) {
                    String uuid = data.get(uuidKey, PersistentDataType.STRING);
                    if (uuid != null) {
                        player.sendMessage("Owner: " + getNameFromUUID(UUID.fromString(uuid)));
                    }
                } else {
                    player.sendMessage("Owner: null");
                }
                for (int i = 1; i <= 10; i++) {
                    NamespacedKey key = new NamespacedKey("smartchestlock", "uuid_" + i);
                    if (data.has(key, PersistentDataType.STRING)) {
                        String value = data.get(key, PersistentDataType.STRING);
                        if (value != null && !value.equals("0")) {
                            player.sendMessage("  ├──> " + getNameFromUUID(UUID.fromString(value)));
                        }
                    }
                }
            }
        }
    }

    void assegnaUUID(Block block, Player player, boolean message) {
        if (block.getState() instanceof Chest chest) {
            PersistentDataContainer data = chest.getPersistentDataContainer();
            NamespacedKey key1 = new NamespacedKey("smartchestlock", "uuid");
            data.set(key1, PersistentDataType.STRING, player.getUniqueId().toString());
            for (int i = 1; i <= 10; i++) {
                NamespacedKey key = new NamespacedKey("smartchestlock", "uuid_" + i);
                data.set(key, PersistentDataType.STRING, "0");
            }
            chest.update();
            if (message) {
                player.sendMessage("&aLa chest è stata bloccata!");
            }
        }
    }

    void rimuoviUUID(Block block, Player player, boolean message) {
        if (block.getState() instanceof Chest chest) {
            PersistentDataContainer data = chest.getPersistentDataContainer();
            NamespacedKey key1 = new NamespacedKey("smartchestlock", "uuid");
            data.remove(key1);
            for (int i = 1; i <= 10; i++) {
                NamespacedKey key = new NamespacedKey("smartchestlock", "uuid_" + i);
                data.remove(key);
            }
            chest.update();
            if(message){
                player.sendMessage("§aLa chest è stata sbloccata!");
            }
        }
    }

    public String getNameFromUUID(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        } else {
            return null;
        }
    }

    public String getUUID(Block block) {
        if (block.getState() instanceof Chest chest) {
            PersistentDataContainer data = chest.getPersistentDataContainer();
            NamespacedKey uuidKey = new NamespacedKey("smartchestlock", "uuid");
            if (data.has(uuidKey, PersistentDataType.STRING)) {
                return data.get(uuidKey, PersistentDataType.STRING);
            }
        }
        return null;
    }

    public boolean isDoubleChest(Block block) {
        if (!(block.getState() instanceof Chest chest)) {
            return false;
        }
        InventoryHolder holder = chest.getInventory().getHolder();
        return holder instanceof DoubleChest;
    }

    public Block getDoubleChestBlock(Block block) {
        if ((block.getState() instanceof Chest)) {
            BlockData blockData = block.getBlockData();
            Pattern pattern = Pattern.compile("type=(right|left)");
            Matcher matcher = pattern.matcher(blockData.getAsString());
            if (matcher.find()) {
                String position = matcher.group(1);
                if (blockData instanceof Directional chestData) {
                    BlockFace face = chestData.getFacing();
                    Block adjacentBlock = null;
                    switch (position) {
                        case "left":
                            switch (face) {
                                case NORTH -> adjacentBlock = block.getRelative(BlockFace.EAST);
                                case EAST -> adjacentBlock = block.getRelative(BlockFace.SOUTH);
                                case SOUTH -> adjacentBlock = block.getRelative(BlockFace.WEST);
                                case WEST -> adjacentBlock = block.getRelative(BlockFace.NORTH);
                            }
                            break;
                        case "right":
                            switch (face) {
                                case NORTH -> adjacentBlock = block.getRelative(BlockFace.WEST);
                                case EAST -> adjacentBlock = block.getRelative(BlockFace.NORTH);
                                case SOUTH -> adjacentBlock = block.getRelative(BlockFace.EAST);
                                case WEST -> adjacentBlock = block.getRelative(BlockFace.SOUTH);
                            }
                            break;
                    }
                    return adjacentBlock;
                }
            }
        }
        return null;
    }


    String getString(String text) {
        if (config.getString(text) != null) {
            return Objects.requireNonNull(config.getString(text)).replaceAll("&", "§");
        }
        plugin.getLogger().info("Entry not found: " + text);
        return "";
    }

}
