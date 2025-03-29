package it.moro.smartChestLock;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class SmartChestLock extends JavaPlugin {

    @Getter
    private static SmartChestLock instance;

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("§cUnable to create plugin data folder! Check permissions.");
        }
        File configuration = new File(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SmartChestLock")).getDataFolder(), "config.yml");
        if(!configuration.exists()){
            saveResource("config.yml", false);
            getLogger().info("File config.yml created!");
        }
        Objects.requireNonNull(getCommand("lock")).setExecutor(new Commands(this));
        Objects.requireNonNull(getCommand("unlock")).setExecutor(new Commands(this));
        Objects.requireNonNull(getCommand("smartchestlock")).setExecutor(new Commands(this));
        Events events = new Events(this);
        getServer().getPluginManager().registerEvents(events, this);
        getLogger().info("\u001B[32mEnabled!\u001B[0m");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled!");
    }
}
