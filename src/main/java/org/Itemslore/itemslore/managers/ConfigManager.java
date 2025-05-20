package org.Itemslore.itemslore.managers;

import org.Itemslore.itemslore.Itemslore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * 配置管理器，用于处理配置文件的加载、保存和访问
 */
public class ConfigManager {
    private final Itemslore plugin;
    private FileConfiguration config;
    
    public ConfigManager(Itemslore plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化配置
     */
    public void initialize() {
        // 保存默认配置
        saveDefaultConfig();
        // 重新加载配置
        reloadConfig();
    }
    
    /**
     * 保存默认配置文件
     */
    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }
    
    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        // 加载配置文件
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * 获取配置文件
     * @return 配置文件实例
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }
} 