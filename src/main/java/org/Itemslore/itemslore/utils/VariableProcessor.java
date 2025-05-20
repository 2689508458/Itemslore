package org.Itemslore.itemslore.utils;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.managers.PluginManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量处理器，用于处理文本中的各种变量替换
 */
public class VariableProcessor {
    private final Itemslore plugin;
    private final Pattern variablePattern = Pattern.compile("%([a-zA-Z0-9_]+)_([a-zA-Z0-9_]+)%");
    
    public VariableProcessor(Itemslore plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理文本中的所有变量
     * @param text 待处理的文本
     * @param player 玩家对象
     * @param item 物品对象
     * @return 处理后的文本
     */
    public String parseAllVariables(String text, Player player, ItemStack item) {
        // 首先处理自定义的ItemsLore插件变量
        text = parseItemsLoreVariables(text, player, item);
        
        // 然后尝试使用PlaceholderAPI处理所有变量
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (pluginManager.isPlaceholderAPIEnabled() && player != null) {
            try {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                plugin.getLogger().warning("处理PlaceholderAPI变量时出错: " + e.getMessage());
            }
        }
        
        // 然后检查是否还有未处理的变量，尝试用其他插件变量处理
        Matcher matcher = variablePattern.matcher(text);
        StringBuilder resultBuilder = new StringBuilder(text);
        int offset = 0;
        
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String pluginName = matcher.group(1).toLowerCase();
            String variableName = matcher.group(2).toLowerCase();
            
            String replacement = processPluginVariable(pluginName, variableName, player, item);
            
            if (!fullMatch.equals(replacement)) {
                int start = matcher.start() + offset;
                int end = matcher.end() + offset;
                resultBuilder.replace(start, end, replacement);
                offset += replacement.length() - fullMatch.length();
            }
        }
        
        return resultBuilder.toString();
    }
    
    /**
     * 处理ItemsLore插件的自定义变量
     * @param text 待处理的文本
     * @param player 玩家对象
     * @param item 物品对象
     * @return 处理后的文本
     */
    private String parseItemsLoreVariables(String text, Player player, ItemStack item) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 替换与物品相关的变量
        if (item != null) {
            text = text.replace("%ilore_material_name%", item.getType().name());
            text = text.replace("%material_name%", item.getType().name());
            
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                text = text.replace("%ilore_item_name%", item.getItemMeta().getDisplayName());
                text = text.replace("%item_name%", item.getItemMeta().getDisplayName());
            } else {
                text = text.replace("%ilore_item_name%", formatMaterialName(item.getType().name()));
                text = text.replace("%item_name%", formatMaterialName(item.getType().name()));
            }
        }
        
        // 替换与玩家相关的变量
        if (player != null) {
            text = text.replace("%ilore_player%", player.getName());
            text = text.replace("%ilore_player_name%", player.getName());
            text = text.replace("%ilore_player_displayname%", player.getDisplayName());
            
            // 替换世界相关变量
            text = text.replace("%ilore_world%", player.getWorld().getName());
            text = text.replace("%ilore_world_name%", player.getWorld().getName());
        }
        
        return text;
    }
    
    /**
     * 格式化物品材质名称为更易读的格式
     * @param materialName 物品材质名
     * @return 格式化后的名称
     */
    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder formattedName = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                formattedName.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        
        return formattedName.toString().trim();
    }
    
    /**
     * 处理特定插件的变量
     * @param pluginName 插件名称
     * @param variableName 变量名称
     * @param player 玩家对象
     * @param item 物品对象
     * @return 替换后的文本
     */
    private String processPluginVariable(String pluginName, String variableName, Player player, ItemStack item) {
        // 尝试处理已知插件的变量
        switch (pluginName.toLowerCase()) {
            case "vault":
                return processVaultVariable(variableName, player);
            case "mmoitems":
                return processMMOItemsVariable(variableName, item);
            case "itemsadder":
                return processItemsAdderVariable(variableName, item);
            case "mythicmobs":
                return processMythicMobsVariable(variableName, item);
            case "mv":
            case "multiverse":
                return processMultiverseVariable(variableName, player);
            case "mw":
            case "multiworld":
                return processMultiWorldVariable(variableName, player);
            default:
                // 尝试动态调用其他插件的变量处理方法
                return processDynamicPluginVariable(pluginName, variableName, player, item);
        }
    }
    
    /**
     * 处理Multiverse-Core插件变量
     */
    private String processMultiverseVariable(String variableName, Player player) {
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (!pluginManager.isPluginEnabled("Multiverse-Core") || player == null) {
            return "%" + "mv" + "_" + variableName + "%";
        }
        
        try {
            Plugin multiversePlugin = pluginManager.getSupportedPlugin("Multiverse-Core");
            if (multiversePlugin == null) return "%" + "mv" + "_" + variableName + "%";
            
            World world = player.getWorld();
            
            switch (variableName) {
                case "world":
                case "worldname":
                    return world.getName();
                    
                case "world_alias":
                case "worldalias":
                    // 尝试获取世界别名
                    try {
                        Class<?> mvCoreClass = Class.forName("com.onarandombox.MultiverseCore.MultiverseCore");
                        Object mvCore = multiversePlugin;
                        
                        Method getWorldManagerMethod = mvCoreClass.getMethod("getMVWorldManager");
                        Object worldManager = getWorldManagerMethod.invoke(mvCore);
                        
                        Class<?> mvWorldManagerClass = worldManager.getClass();
                        Method getMVWorldMethod = mvWorldManagerClass.getMethod("getMVWorld", String.class);
                        Object mvWorld = getMVWorldMethod.invoke(worldManager, world.getName());
                        
                        if (mvWorld != null) {
                            Class<?> mvWorldClass = mvWorld.getClass();
                            Method getAliasMethod = mvWorldClass.getMethod("getAlias");
                            String alias = (String) getAliasMethod.invoke(mvWorld);
                            return alias != null && !alias.isEmpty() ? alias : world.getName();
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("获取Multiverse世界别名时出错: " + e.getMessage());
                    }
                    return world.getName();
                    
                case "world_env":
                case "worldenv":
                    return world.getEnvironment().toString();
                    
                default:
                    return "%" + "mv" + "_" + variableName + "%";
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理Multiverse变量时出错: " + e.getMessage());
        }
        
        return "%" + "mv" + "_" + variableName + "%";
    }
    
    /**
     * 处理MultiWorld插件变量
     */
    private String processMultiWorldVariable(String variableName, Player player) {
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (!pluginManager.isPluginEnabled("MultiWorld") || player == null) {
            return "%" + "mw" + "_" + variableName + "%";
        }
        
        try {
            Plugin multiWorldPlugin = pluginManager.getSupportedPlugin("MultiWorld");
            if (multiWorldPlugin == null) return "%" + "mw" + "_" + variableName + "%";
            
            World world = player.getWorld();
            
            switch (variableName) {
                case "world":
                case "worldname":
                    return world.getName();
                    
                case "world_alias":
                case "worldalias":
                    // 尝试获取世界别名，如果插件支持的话
                    try {
                        Class<?> mwApiClass = Class.forName("org.multiworld.api.MultiWorldAPI");
                        Method getInstanceMethod = mwApiClass.getMethod("getInstance");
                        Object mwApi = getInstanceMethod.invoke(null);
                        
                        Method getWorldMethod = mwApiClass.getMethod("getWorld", String.class);
                        Object mwWorld = getWorldMethod.invoke(mwApi, world.getName());
                        
                        if (mwWorld != null) {
                            Class<?> mwWorldClass = mwWorld.getClass();
                            Method getAliasMethod = mwWorldClass.getMethod("getAlias");
                            String alias = (String) getAliasMethod.invoke(mwWorld);
                            return alias != null && !alias.isEmpty() ? alias : world.getName();
                        }
                    } catch (Exception e) {
                        // 如果方法不存在或调用失败，返回世界名
                        return world.getName();
                    }
                    return world.getName();
                    
                case "world_env":
                case "worldenv":
                    return world.getEnvironment().toString();
                    
                default:
                    return "%" + "mw" + "_" + variableName + "%";
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理MultiWorld变量时出错: " + e.getMessage());
        }
        
        return "%" + "mw" + "_" + variableName + "%";
    }
    
    /**
     * 处理Vault插件变量
     */
    private String processVaultVariable(String variableName, Player player) {
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (!pluginManager.isPluginEnabled("Vault") || player == null) {
            return "%" + "vault" + "_" + variableName + "%";
        }
        
        try {
            // 尝试获取Vault经济服务
            Plugin vaultPlugin = pluginManager.getSupportedPlugin("Vault");
            if (vaultPlugin == null) return "%" + "vault" + "_" + variableName + "%";
            
            // 动态尝试使用Vault API，避免直接依赖
            Class<?> vaultClass = Class.forName("net.milkbowl.vault.Vault");
            if (vaultClass != null) {
                switch (variableName) {
                    case "balance":
                        // 尝试获取玩家余额
                        try {
                            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
                            Object econ = Bukkit.getServicesManager().getRegistration(econClass).getProvider();
                            Method getBalanceMethod = econClass.getMethod("getBalance", Object.class);
                            double balance = (double) getBalanceMethod.invoke(econ, player);
                            return String.format("%.2f", balance);
                        } catch (Exception e) {
                            plugin.getLogger().warning("获取Vault余额时出错: " + e.getMessage());
                            return "0.00";
                        }
                    default:
                        return "%" + "vault" + "_" + variableName + "%";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("处理Vault变量时出错: " + e.getMessage());
        }
        
        return "%" + "vault" + "_" + variableName + "%";
    }
    
    /**
     * 处理MMOItems插件变量
     */
    private String processMMOItemsVariable(String variableName, ItemStack item) {
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (!pluginManager.isPluginEnabled("MMOItems") || item == null) {
            return "%" + "mmoitems" + "_" + variableName + "%";
        }
        
        try {
            // 尝试动态使用MMOItems API
            Plugin mmoItemsPlugin = pluginManager.getSupportedPlugin("MMOItems");
            if (mmoItemsPlugin == null) return "%" + "mmoitems" + "_" + variableName + "%";
            
            // 这里可以添加特定的MMOItems变量处理
            // 例如获取物品类型、ID等
            // 由于不直接依赖MMOItems，我们使用反射获取信息
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理MMOItems变量时出错: " + e.getMessage());
        }
        
        return "%" + "mmoitems" + "_" + variableName + "%";
    }
    
    /**
     * 处理ItemsAdder插件变量
     */
    private String processItemsAdderVariable(String variableName, ItemStack item) {
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (!pluginManager.isPluginEnabled("ItemsAdder") || item == null) {
            return "%" + "itemsadder" + "_" + variableName + "%";
        }
        
        try {
            // 尝试动态使用ItemsAdder API
            Plugin itemsAdderPlugin = pluginManager.getSupportedPlugin("ItemsAdder");
            if (itemsAdderPlugin == null) return "%" + "itemsadder" + "_" + variableName + "%";
            
            // 这里可以添加特定的ItemsAdder变量处理
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理ItemsAdder变量时出错: " + e.getMessage());
        }
        
        return "%" + "itemsadder" + "_" + variableName + "%";
    }
    
    /**
     * 处理MythicMobs插件变量
     */
    private String processMythicMobsVariable(String variableName, ItemStack item) {
        PluginManager pluginManager = plugin.getPluginManager();
        
        if (!pluginManager.isPluginEnabled("MythicMobs") || item == null) {
            return "%" + "mythicmobs" + "_" + variableName + "%";
        }
        
        try {
            // 尝试动态使用MythicMobs API
            Plugin mythicMobsPlugin = pluginManager.getSupportedPlugin("MythicMobs");
            if (mythicMobsPlugin == null) return "%" + "mythicmobs" + "_" + variableName + "%";
            
            // 这里可以添加特定的MythicMobs变量处理
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理MythicMobs变量时出错: " + e.getMessage());
        }
        
        return "%" + "mythicmobs" + "_" + variableName + "%";
    }
    
    /**
     * 动态尝试处理任何插件的变量
     */
    private String processDynamicPluginVariable(String pluginName, String variableName, Player player, ItemStack item) {
        // 先检查插件是否存在
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null || !targetPlugin.isEnabled()) {
            return "%" + pluginName + "_" + variableName + "%";
        }
        
        try {
            // 尝试通过反射调用插件的API
            // 这只是一个通用框架，具体实现需要根据各个插件的API结构调整
            Class<?> pluginClass = targetPlugin.getClass();
            
            // 尝试找到处理变量的方法
            try {
                Method getVariableMethod = findVariableMethod(pluginClass);
                if (getVariableMethod != null) {
                    return (String) getVariableMethod.invoke(targetPlugin, variableName, player);
                }
            } catch (Exception e) {
                // 忽略，继续尝试其他方法
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理插件 " + pluginName + " 的变量时出错: " + e.getMessage());
        }
        
        // 如果无法处理，保留原始变量格式
        return "%" + pluginName + "_" + variableName + "%";
    }
    
    /**
     * 尝试在插件类中查找处理变量的方法
     */
    private Method findVariableMethod(Class<?> pluginClass) {
        // 常见的变量处理方法名
        String[] methodNames = {
            "getVariable", "getPlaceholder", "getPlaceholderValue", "processVariable", "processPlaceholder"
        };
        
        for (String methodName : methodNames) {
            try {
                // 尝试不同的参数组合
                try {
                    return pluginClass.getMethod(methodName, String.class, Player.class);
                } catch (NoSuchMethodException e) {
                    // 继续尝试
                }
                
                try {
                    return pluginClass.getMethod(methodName, Player.class, String.class);
                } catch (NoSuchMethodException e) {
                    // 继续尝试
                }
                
                try {
                    return pluginClass.getMethod(methodName, String.class);
                } catch (NoSuchMethodException e) {
                    // 继续尝试
                }
            } catch (Exception e) {
                // 忽略异常，继续尝试下一个方法名
            }
        }
        
        return null;
    }
} 