package org.Itemslore.itemslore.managers;

import org.Itemslore.itemslore.Itemslore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件依赖管理器，用于检测和管理其他插件的依赖
 */
public class PluginManager {
    private final Itemslore plugin;
    private boolean placeholderAPIEnabled = false;
    private final Map<String, Plugin> supportedPlugins = new HashMap<>();
    
    public PluginManager(Itemslore plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 检测服务器上已安装的支持变量的插件
     */
    public void detectSupportedPlugins() {
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
        Plugin detectedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (detectedPlugin != null && detectedPlugin.isEnabled()) {
            supportedPlugins.put(pluginName, detectedPlugin);
            if (isPlaceholderAPI) {
                placeholderAPIEnabled = true;
                plugin.getLogger().info("已检测到PlaceholderAPI，已启用变量支持！");
            } else {
                plugin.getLogger().info("已检测到" + pluginName + "，已启用相关变量支持！");
            }
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