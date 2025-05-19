package org.Itemslore.itemslore;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Itemslore extends JavaPlugin {
    
    private static Itemslore instance;
    private FileConfiguration config;
    private boolean placeholderAPIEnabled = false;
    private final Map<String, Plugin> supportedPlugins = new HashMap<>();
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置
        saveDefaultConfig();
        reloadConfig();
        
        // 检测变量支持插件
        detectSupportedPlugins();
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        
        // 注册命令
        PluginCommand command = getCommand("itemslore");
        if (command != null) {
            command.setExecutor(new ItemsloreCommand(this));
        } else {
            getLogger().warning("无法注册命令'itemslore'，请检查plugin.yml配置！");
        }
        
        getLogger().info("ItemsLore插件已成功启动！");
    }
    
    /**
     * 检测服务器上已安装的支持变量的插件
     */
    private void detectSupportedPlugins() {
        // 检测PlaceholderAPI
        detectPlugin("PlaceholderAPI", true);
        
        // 检测Vault
        detectPlugin("Vault", false);
        
        // 检测MMOItems
        detectPlugin("MMOItems", false);
        
        // 检测ItemsAdder
        detectPlugin("ItemsAdder", false);
        
        // 检测MythicMobs
        detectPlugin("MythicMobs", false);
        
        // 检测Multiverse-Core
        detectPlugin("Multiverse-Core", false);
        
        // 检测MultiWorld
        detectPlugin("MultiWorld", false);
    }
    
    /**
     * 检测并注册插件
     * @param pluginName 插件名称
     * @param isPlaceholderAPI 是否为PlaceholderAPI插件
     */
    private void detectPlugin(String pluginName, boolean isPlaceholderAPI) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null && plugin.isEnabled()) {
            supportedPlugins.put(pluginName, plugin);
            if (isPlaceholderAPI) {
                placeholderAPIEnabled = true;
                getLogger().info("已检测到PlaceholderAPI，已启用变量支持！");
            } else {
                getLogger().info("已检测到" + pluginName + "，已启用相关变量支持！");
            }
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("ItemsLore插件已关闭！");
    }
    
    public static Itemslore getInstance() {
        return instance;
    }
    
    @Override
    public void reloadConfig() {
        // 加载配置文件
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    @Override
    public FileConfiguration getConfig() {
        return config;
    }
    
    @Override
    public void saveConfig() {
        try {
            config.save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }
    
    /**
     * 检查PlaceholderAPI是否启用
     * @return 是否启用PlaceholderAPI
     */
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    /**
     * 获取指定插件
     * @param pluginName 插件名称
     * @return 插件实例，如果不存在则返回null
     */
    public Plugin getSupportedPlugin(String pluginName) {
        return supportedPlugins.get(pluginName);
    }
    
    /**
     * 检查指定插件是否已启用
     * @param pluginName 插件名称
     * @return 是否已启用
     */
    public boolean isPluginEnabled(String pluginName) {
        return supportedPlugins.containsKey(pluginName);
    }
}
