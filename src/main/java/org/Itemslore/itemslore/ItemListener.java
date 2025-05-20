package org.Itemslore.itemslore;

import org.Itemslore.itemslore.managers.PluginManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemListener implements Listener {

    private final Itemslore plugin;
    private final Pattern variablePattern = Pattern.compile("%([a-zA-Z0-9_]+)_([a-zA-Z0-9_]+)%");
    private final Random random = new Random();

    public ItemListener(Itemslore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        if (shouldProcessItem(item)) {
            addLoreToItem(item, player, "拾取");
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (shouldProcessItem(item)) {
            addLoreToItem(item, player, "合成");
        }
    }

    /**
     * 检查物品是否需要添加Lore
     */
    private boolean shouldProcessItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        // 获取配置中需要处理的物品类型
        List<String> itemTypes = plugin.getConfig().getStringList("item-types");
        
        // 检查物品类型
        if (itemTypes.contains("ALL")) return true;
        if (itemTypes.contains("DAMAGEABLE") && item.getItemMeta() instanceof Damageable) return true;
        
        String type = item.getType().toString();
        
        // 检查具体的物品类型
        if (itemTypes.contains(type)) return true;
        
        // 检查物品类别
        if (itemTypes.contains("TOOL") && isToolItem(item.getType())) return true;
        if (itemTypes.contains("ARMOR") && isArmorItem(item.getType())) return true;
        if (itemTypes.contains("WEAPON") && isWeaponItem(item.getType())) return true;
        
        return false;
    }

    /**
     * 为物品添加Lore
     */
    private void addLoreToItem(ItemStack item, Player player, String source) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (lore == null) lore = new ArrayList<>();
        
        // 检查是否已经有lore，避免重复添加
        String durabilityPrefix = ChatColor.translateAlternateColorCodes('&', 
                "&8❖ &7耐久");
        
        boolean hasLore = false;
        for (String line : lore) {
            if (line.contains(durabilityPrefix)) {
                hasLore = true;
                break;
            }
        }
        
        if (hasLore) return;
        
        // 获取配置
        boolean showDurability = plugin.getConfig().getBoolean("lore.show-durability", true);
        boolean showTime = plugin.getConfig().getBoolean("lore.show-time", true);
        boolean showPlayer = plugin.getConfig().getBoolean("lore.show-player", true);
        boolean showSource = plugin.getConfig().getBoolean("lore.show-source", true);
        
        // 添加顶部分割线
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m                         "));
        lore.add("");
        
        // 添加耐久度
        if (showDurability && meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int maxDurability = item.getType().getMaxDurability();
            int currentDurability = maxDurability - damageable.getDamage();
            
            // 计算耐久度百分比
            int durabilityPercentage = (maxDurability > 0) ? (currentDurability * 100 / maxDurability) : 100;
            
            // 获取动态耐久度颜色
            String durabilityColor = getDurabilityColor(durabilityPercentage);
            
            String durabilityText = plugin.getConfig().getString("lore.durability-format", "&7耐久度：&f%current%/%max%");
            durabilityText = durabilityText
                    .replace("%current%", String.valueOf(currentDurability))
                    .replace("%max%", String.valueOf(maxDurability))
                    .replace("%durability_color%", durabilityColor);
            
            durabilityText = ChatColor.translateAlternateColorCodes('&', durabilityText);
            
            lore.add(durabilityText);
            lore.add("");
        }
        
        // 添加获取时间
        if (showTime) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    plugin.getConfig().getString("lore.time-format", "yyyy-MM-dd HH:mm:ss"));
            String timeText = plugin.getConfig().getString("lore.time-prefix", "&7获取时间：&f%time%");
            timeText = ChatColor.translateAlternateColorCodes('&', timeText)
                    .replace("%time%", dateFormat.format(new Date()));
            
            lore.add(timeText);
        }
        
        // 添加玩家名称
        if (showPlayer) {
            String playerText = plugin.getConfig().getString("lore.player-prefix", "&7获取人：&f%player%");
            playerText = ChatColor.translateAlternateColorCodes('&', playerText)
                    .replace("%player%", player.getName());
            
            lore.add(playerText);
        }
        
        // 添加来源
        if (showSource) {
            String sourceText = plugin.getConfig().getString("lore.source-prefix", "&7来源：&f%source%");
            sourceText = ChatColor.translateAlternateColorCodes('&', sourceText)
                    .replace("%source%", source);
            
            lore.add(sourceText);
        }
        
        // 添加底部分割线
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m                         "));
        
        // 添加随机Lore
        if (plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            addRandomLoreToItem(lore, item, player);
        }
        
        // 添加自定义Lore
        if (plugin.getConfig().getBoolean("lore.custom-lore.enabled", false)) {
            List<String> customLoreLines = plugin.getConfig().getStringList("lore.custom-lore.lines");
            
            if (customLoreLines != null && !customLoreLines.isEmpty()) {
                for (String line : customLoreLines) {
                    // 处理基本变量
                    String processedLine = line
                            .replace("%player_name%", player.getName())
                            .replace("%player_world%", player.getWorld().getName())
                            .replace("%server_name%", Bukkit.getServer().getName())
                            .replace("%material_name%", item.getType().toString())
                            .replace("%source%", source);
                    
                    // 处理颜色模板变量
                    processedLine = processColorTemplates(processedLine);
                    
                    // 处理所有插件变量
                    processedLine = parseAllVariables(processedLine, player, item);
                    
                    // 处理颜色代码
                    processedLine = ChatColor.translateAlternateColorCodes('&', processedLine);
                    
                    lore.add(processedLine);
                }
            }
        }
        
        // 设置新的lore
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    /**
     * 获取根据耐久度百分比对应的颜色
     * @param percentage 耐久度百分比
     * @return 颜色代码
     */
    private String getDurabilityColor(int percentage) {
        // 如果动态耐久度颜色未启用，返回默认颜色
        if (!plugin.getConfig().getBoolean("colors.dynamic.durability.enabled", true)) {
            return "&f";
        }
        
        // 检查不同耐久度范围对应的颜色
        if (percentage <= 20) {
            return plugin.getConfig().getString("colors.dynamic.durability.ranges.0-20", "&c");
        } else if (percentage <= 50) {
            return plugin.getConfig().getString("colors.dynamic.durability.ranges.21-50", "&e");
        } else if (percentage <= 80) {
            return plugin.getConfig().getString("colors.dynamic.durability.ranges.51-80", "&a");
        } else {
            return plugin.getConfig().getString("colors.dynamic.durability.ranges.81-100", "&2");
        }
    }
    
    /**
     * 处理文本中的颜色模板变量
     * 支持格式: %color_模板名%
     */
    private String processColorTemplates(String text) {
        // 匹配颜色模板变量 %color_xxx%
        Pattern colorPattern = Pattern.compile("%color_([a-zA-Z0-9_]+)%([^%]*)");
        Matcher matcher = colorPattern.matcher(text);
        
        StringBuilder result = new StringBuilder(text);
        int offset = 0;
        
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String templateName = matcher.group(1);
            String textContent = matcher.group(2);
            
            // 尝试获取渐变模板
            String gradientTemplate = plugin.getConfig().getString("colors.templates.gradients." + templateName);
            
            // 尝试获取样式模板
            if (gradientTemplate == null) {
                gradientTemplate = plugin.getConfig().getString("colors.templates.styles." + templateName);
            }
            
            // 如果找到模板
            if (gradientTemplate != null) {
                String replacement = gradientTemplate.replace("%text%", textContent);
                
                // 替换文本
                int start = matcher.start() + offset;
                int end = matcher.end() + offset;
                result.replace(start, end, replacement);
                
                // 调整偏移
                offset += replacement.length() - fullMatch.length();
            }
        }
        
        return result.toString();
    }
    
    /**
     * 为物品添加随机Lore
     * @param lore 当前lore列表
     * @param item 物品
     * @param player 玩家
     */
    private void addRandomLoreToItem(List<String> lore, ItemStack item, Player player) {
        if (!plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            return;
        }
        
        // 获取随机lore数量
        int amount = plugin.getConfig().getInt("lore.random-lore.amount", 1);
        // 如果配置了范围（如：1-3），则随机生成在范围内的数量
        if (amount <= 0) amount = 1;
        
        if (amount > 0) {
            // 添加分隔空行
            lore.add("");
            
            // 收集所有可用的随机lore
            List<String> availableLores = new ArrayList<>();
            
            // 添加通用随机lore
            List<String> allLores = plugin.getConfig().getStringList("lore.random-lore.pools.ALL");
            if (allLores != null && !allLores.isEmpty()) {
                availableLores.addAll(allLores);
            }
            
            // 添加物品类型特定的随机lore
            String materialName = item.getType().toString();
            
            // 检查材料名称是否包含特定关键字，并添加对应的随机lore
            if (materialName.contains("DIAMOND")) {
                List<String> diamondLores = plugin.getConfig().getStringList("lore.random-lore.pools.DIAMOND");
                if (diamondLores != null && !diamondLores.isEmpty()) {
                    availableLores.addAll(diamondLores);
                }
            }
            
            if (materialName.contains("NETHERITE")) {
                List<String> netheriteLores = plugin.getConfig().getStringList("lore.random-lore.pools.NETHERITE");
                if (netheriteLores != null && !netheriteLores.isEmpty()) {
                    availableLores.addAll(netheriteLores);
                }
            }
            
            // 添加物品类别特定的随机lore
            if (isWeaponItem(item.getType())) {
                List<String> weaponLores = plugin.getConfig().getStringList("lore.random-lore.pools.WEAPON");
                if (weaponLores != null && !weaponLores.isEmpty()) {
                    availableLores.addAll(weaponLores);
                }
            } else if (isToolItem(item.getType())) {
                List<String> toolLores = plugin.getConfig().getStringList("lore.random-lore.pools.TOOL");
                if (toolLores != null && !toolLores.isEmpty()) {
                    availableLores.addAll(toolLores);
                }
            } else if (isArmorItem(item.getType())) {
                List<String> armorLores = plugin.getConfig().getStringList("lore.random-lore.pools.ARMOR");
                if (armorLores != null && !armorLores.isEmpty()) {
                    availableLores.addAll(armorLores);
                }
            }
            
            // 如果有可用的随机lore
            if (!availableLores.isEmpty()) {
                // 随机选择指定数量的lore
                Collections.shuffle(availableLores);
                int actualAmount = Math.min(amount, availableLores.size());
                
                for (int i = 0; i < actualAmount; i++) {
                    String randomLore = availableLores.get(i);
                    
                    // 处理变量
                    randomLore = randomLore.replace("%player_name%", player.getName())
                            .replace("%material_name%", item.getType().toString());
                    
                    // 处理颜色模板变量
                    randomLore = processColorTemplates(randomLore);
                    
                    // 处理所有插件变量
                    randomLore = parseAllVariables(randomLore, player, item);
                    
                    // 处理颜色代码
                    randomLore = ChatColor.translateAlternateColorCodes('&', randomLore);
                    
                    lore.add(randomLore);
                }
            }
        }
    }
    
    /**
     * 处理文本中的所有变量
     */
    private String parseAllVariables(String text, Player player, ItemStack item) {
        // 首先尝试使用PlaceholderAPI处理所有变量
        if (plugin.getPluginManager().isPlaceholderAPIEnabled() && player != null) {
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
     * 处理特定插件的变量
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
        if (!plugin.getPluginManager().isPluginEnabled("Multiverse-Core") || player == null) {
            return "%" + "mv" + "_" + variableName + "%";
        }
        
        try {
            Plugin multiversePlugin = plugin.getPluginManager().getSupportedPlugin("Multiverse-Core");
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
        if (!plugin.getPluginManager().isPluginEnabled("MultiWorld") || player == null) {
            return "%" + "mw" + "_" + variableName + "%";
        }
        
        try {
            Plugin multiWorldPlugin = plugin.getPluginManager().getSupportedPlugin("MultiWorld");
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
        if (!plugin.getPluginManager().isPluginEnabled("Vault") || player == null) {
            return "%" + "vault" + "_" + variableName + "%";
        }
        
        try {
            // 尝试获取Vault经济服务
            Plugin vaultPlugin = plugin.getPluginManager().getSupportedPlugin("Vault");
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
        if (!plugin.getPluginManager().isPluginEnabled("MMOItems") || item == null) {
            return "%" + "mmoitems" + "_" + variableName + "%";
        }
        
        try {
            // 尝试动态使用MMOItems API
            Plugin mmoItemsPlugin = plugin.getPluginManager().getSupportedPlugin("MMOItems");
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
        if (!plugin.getPluginManager().isPluginEnabled("ItemsAdder") || item == null) {
            return "%" + "itemsadder" + "_" + variableName + "%";
        }
        
        try {
            // 尝试动态使用ItemsAdder API
            Plugin itemsAdderPlugin = plugin.getPluginManager().getSupportedPlugin("ItemsAdder");
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
        if (!plugin.getPluginManager().isPluginEnabled("MythicMobs") || item == null) {
            return "%" + "mythicmobs" + "_" + variableName + "%";
        }
        
        try {
            // 尝试动态使用MythicMobs API
            Plugin mythicMobsPlugin = plugin.getPluginManager().getSupportedPlugin("MythicMobs");
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
    
    /**
     * 检查物品是否为工具
     */
    private boolean isToolItem(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || 
               name.endsWith("_HOE") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL") || 
               name.equals("FISHING_ROD");
    }

    /**
     * 检查物品是否为盔甲
     */
    private boolean isArmorItem(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || 
               name.equals("ELYTRA") || name.equals("SHIELD");
    }

    /**
     * 检查物品是否为武器
     */
    private boolean isWeaponItem(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW") || 
               name.equals("TRIDENT");
    }
} 