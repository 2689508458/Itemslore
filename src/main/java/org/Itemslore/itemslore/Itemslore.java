package org.Itemslore.itemslore;

import org.Itemslore.itemslore.commands.CommandHandler;
import org.Itemslore.itemslore.commands.CommandTabCompleter;
import org.Itemslore.itemslore.listeners.ItemEventListener;
import org.Itemslore.itemslore.managers.ConfigManager;
import org.Itemslore.itemslore.managers.LoreManager;
import org.Itemslore.itemslore.managers.PluginManager;
import org.Itemslore.itemslore.utils.ColorManager;
import org.Itemslore.itemslore.utils.VariableProcessor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ItemsLore 插件主类
 */
public final class Itemslore extends JavaPlugin {
    
    private static Itemslore instance;
    
    // 管理器
    private ConfigManager configManager;
    private PluginManager pluginManager;
    private LoreManager loreManager;
    
    // 工具类
    private ColorManager colorManager;
    private VariableProcessor variableProcessor;
    
    @Override
    public void onEnable() {
        // 设置单例实例
        instance = this;
        
        // 初始化管理器和工具类
        initManagers();
        
        // 注册事件监听器
        registerListeners();
        
        // 注册命令
        registerCommands();
        
        getLogger().info("ItemsLore插件已成功启动！");
    }
    
    /**
     * 初始化管理器和工具类
     */
    private void initManagers() {
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.initialize();
        
        // 初始化插件管理器
        pluginManager = new PluginManager(this);
        pluginManager.detectSupportedPlugins();
        
        // 初始化工具类
        colorManager = new ColorManager(this);
        variableProcessor = new VariableProcessor(this);
        
        // 初始化Lore管理器
        loreManager = new LoreManager(this, colorManager, variableProcessor);
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        // 注册物品事件监听器
        getServer().getPluginManager().registerEvents(
                new ItemEventListener(this, loreManager), this);
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        // 注册主命令
        PluginCommand command = getCommand("itemslore");
        if (command != null) {
            CommandHandler commandHandler = new CommandHandler(this, colorManager, loreManager, configManager);
            command.setExecutor(commandHandler);
            
            // 注册Tab补全器
            CommandTabCompleter tabCompleter = new CommandTabCompleter(this, colorManager);
            command.setTabCompleter(tabCompleter);
        } else {
            getLogger().warning("无法注册命令'itemslore'，请检查plugin.yml配置！");
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("ItemsLore插件已关闭！");
    }
    
    /**
     * 获取单例实例
     * @return 插件实例
     */
    public static Itemslore getInstance() {
        return instance;
    }
    
    /**
     * 获取配置文件
     * @return 配置文件
     */
    @Override
    public FileConfiguration getConfig() {
        return configManager.getConfig();
    }
    
    /**
     * 重新加载配置
     */
    @Override
    public void reloadConfig() {
        configManager.reloadConfig();
    }
    
    /**
     * 保存配置
     */
    @Override
    public void saveConfig() {
        configManager.saveConfig();
    }
    
    /**
     * 获取插件管理器
     * @return 插件管理器
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    /**
     * 获取配置管理器
     * @return 配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取Lore管理器
     * @return Lore管理器
     */
    public LoreManager getLoreManager() {
        return loreManager;
    }
    
    /**
     * 获取颜色管理器
     * @return 颜色管理器
     */
    public ColorManager getColorManager() {
        return colorManager;
    }
    
    /**
     * 获取变量处理器
     * @return 变量处理器
     */
    public VariableProcessor getVariableProcessor() {
        return variableProcessor;
    }
    
    /**
     * 检查PlaceholderAPI是否启用
     * @return 是否启用PlaceholderAPI
     */
    public boolean isPlaceholderAPIEnabled() {
        return pluginManager.isPlaceholderAPIEnabled();
    }
}
