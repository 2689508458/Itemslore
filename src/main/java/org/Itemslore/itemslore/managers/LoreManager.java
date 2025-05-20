package org.Itemslore.itemslore.managers;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.utils.ColorManager;
import org.Itemslore.itemslore.utils.VariableProcessor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Lore管理器，负责生成和处理物品的Lore
 */
public class LoreManager {
    private final Itemslore plugin;
    private final ColorManager colorManager;
    private final VariableProcessor variableProcessor;
    private final Random random = new Random();
    
    public LoreManager(Itemslore plugin, ColorManager colorManager, VariableProcessor variableProcessor) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.variableProcessor = variableProcessor;
    }
    
    /**
     * 为物品添加Lore
     * @param item 物品
     * @param player 玩家
     * @param source 来源
     * @return 是否成功添加
     */
    public boolean addLoreToItem(ItemStack item, Player player, String source) {
        if (item == null || player == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (lore == null) lore = new ArrayList<>();
        
        // 检查是否已经有lore，避免重复添加
        String durabilityPrefix = colorManager.colorize("&8❖ &7耐久");
        
        boolean hasLore = false;
        for (String line : lore) {
            if (line.contains(durabilityPrefix)) {
                hasLore = true;
                break;
            }
        }
        
        if (hasLore) return false;
        
        // 生成新的lore
        List<String> newLore = generateLoreFromTemplate(item, player, source);
        lore.addAll(newLore);
        
        // 设置新的lore
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return true;
    }
    
    /**
     * 从模板生成物品lore内容
     * @param item 物品
     * @param player 玩家
     * @param source 来源
     * @return 生成的lore列表
     */
    private List<String> generateLoreFromTemplate(ItemStack item, Player player, String source) {
        List<String> lore = new ArrayList<>();
        
        // 获取合适的模板
        String templateName = findSuitableTemplate(item);
        ConfigurationSection templateSection = plugin.getConfig().getConfigurationSection("lore.templates." + templateName);
        
        // 如果没有找到模板，使用默认模板
        if (templateSection == null) {
            templateSection = plugin.getConfig().getConfigurationSection("lore.templates.default");
            // 如果还是没有，则使用传统格式
            if (templateSection == null) {
                return generateLegacyLore(item, player, source);
            }
        }
        
        // 获取模板内容
        List<String> templateContent = templateSection.getStringList("content");
        
        // 处理模板中的变量
        for (String line : templateContent) {
            String processedLine = line;
            
            // 处理顶部分隔线
            if (processedLine.contains("%ilore_top_separator%")) {
                String separator = plugin.getConfig().getString("lore.basic-settings.top-separator", "&8&m⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤");
                processedLine = processedLine.replace("%ilore_top_separator%", separator);
            }
            
            // 处理底部分隔线
            if (processedLine.contains("%ilore_bottom_separator%")) {
                String separator = plugin.getConfig().getString("lore.basic-settings.bottom-separator", "&8&m⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤");
                processedLine = processedLine.replace("%ilore_bottom_separator%", separator);
            }
            
            // 处理信息分隔线
            if (processedLine.contains("%ilore_info_separator%")) {
                String separator = plugin.getConfig().getString("lore.basic-settings.info-separator", "&8&m· · · · · · · · · · · · · · · · · · · ·");
                processedLine = processedLine.replace("%ilore_info_separator%", separator);
            }
            
            // 处理耐久度
            if (processedLine.contains("%ilore_durability%") && item.getItemMeta() instanceof Damageable) {
                Damageable damageable = (Damageable) item.getItemMeta();
                int maxDurability = item.getType().getMaxDurability();
                int currentDurability = maxDurability - damageable.getDamage();
                
                // 计算耐久度百分比
                int durabilityPercentage = (maxDurability > 0) ? (currentDurability * 100 / maxDurability) : 100;
                
                String durabilityText = plugin.getConfig().getString("lore.durability-format", "&8❖ &7耐久: &c%ilore_current_low%&e%ilore_current_medium%&a%ilore_current_high%&8/&f%ilore_max%");
                
                // 根据耐久度百分比处理不同颜色段
                String currentLow = "";
                String currentMedium = "";
                String currentHigh = "";
                
                if (durabilityPercentage <= 20) {
                    currentLow = String.valueOf(currentDurability);
                } else if (durabilityPercentage <= 50) {
                    currentMedium = String.valueOf(currentDurability);
                } else {
                    currentHigh = String.valueOf(currentDurability);
                }
                
                durabilityText = durabilityText
                        .replace("%ilore_current_low%", currentLow)
                        .replace("%ilore_current_medium%", currentMedium)
                        .replace("%ilore_current_high%", currentHigh)
                        .replace("%ilore_max%", String.valueOf(maxDurability));
                
                processedLine = processedLine.replace("%ilore_durability%", durabilityText);
            } else if (processedLine.contains("%ilore_durability%")) {
                // 如果物品没有耐久度，则删除该行
                continue;
            }
            
            // 处理获取时间
            if (processedLine.contains("%ilore_time%") && plugin.getConfig().getBoolean("lore.show-time", true)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        plugin.getConfig().getString("lore.time-format", "yyyy-MM-dd HH:mm:ss"));
                String timeText = plugin.getConfig().getString("lore.time-prefix", "&7获取时间：&f%ilore_time%");
                timeText = timeText.replace("%ilore_time%", dateFormat.format(new Date()));
                
                processedLine = processedLine.replace("%ilore_time%", timeText);
            } else if (processedLine.contains("%ilore_time%")) {
                // 如果不显示时间，则删除该行
                continue;
            }
            
            // 处理玩家名称
            if (processedLine.contains("%ilore_player%") && plugin.getConfig().getBoolean("lore.show-player", true)) {
                String playerText = plugin.getConfig().getString("lore.player-prefix", "&7获取人：&f%ilore_player%");
                playerText = playerText.replace("%ilore_player%", player.getName());
                
                processedLine = processedLine.replace("%ilore_player%", playerText);
            } else if (processedLine.contains("%ilore_player%")) {
                // 如果不显示玩家，则删除该行
                continue;
            }
            
            // 处理来源
            if (processedLine.contains("%ilore_source%") && plugin.getConfig().getBoolean("lore.show-source", true)) {
                String sourceText = plugin.getConfig().getString("lore.source-prefix", "&7来源：&f%ilore_source%");
                sourceText = sourceText.replace("%ilore_source%", source);
                
                processedLine = processedLine.replace("%ilore_source%", sourceText);
            } else if (processedLine.contains("%ilore_source%")) {
                // 如果不显示来源，则删除该行
                continue;
            }
            
            // 处理随机lore
            if (processedLine.contains("%ilore_random_lore%")) {
                if (plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
                    // 添加随机lore
                    List<String> randomLores = getRandomLores(item, player);
                    
                    // 判断如果没有随机lore，跳过该行
                    if (randomLores.isEmpty()) {
                        continue;
                    }
                    
                    // 将所有随机lore添加到列表中，先添加当前行的替代，后续lore单独添加
                    if (!randomLores.isEmpty()) {
                        lore.add(colorManager.colorize(processedLine.replace("%ilore_random_lore%", randomLores.get(0))));
                        // 添加剩余的随机lore
                        for (int i = 1; i < randomLores.size(); i++) {
                            lore.add(colorManager.colorize(randomLores.get(i)));
                        }
                    }
                    continue; // 跳过下面的添加，因为已经在这里处理了
                } else {
                    // 如果随机lore未启用，删除该行
                    continue;
                }
            }
            
            // 处理变量
            processedLine = variableProcessor.parseAllVariables(processedLine, player, item);
            
            // 处理颜色代码
            processedLine = colorManager.colorize(processedLine);
            
            // 添加到lore列表
            lore.add(processedLine);
        }
        
        // 添加自定义lore
        if (plugin.getConfig().getBoolean("lore.custom-lore.enabled", false)) {
            addCustomLoreToList(lore, item, player, source);
        }
        
        return lore;
    }
    
    /**
     * 获取适合物品的模板名称
     * @param item 物品
     * @return 模板名称
     */
    private String findSuitableTemplate(ItemStack item) {
        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("lore.templates");
        if (templatesSection == null) {
            return "default";
        }
        
        String materialName = item.getType().name().toUpperCase();
        
        // 检查每个模板是否适用
        for (String templateName : templatesSection.getKeys(false)) {
            // 跳过默认模板
            if (templateName.equals("default")) continue;
            
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(templateName);
            if (templateSection != null && templateSection.getBoolean("enabled", true)) {
                List<String> itemTypes = templateSection.getStringList("item-types");
                
                // 如果物品类型列表为空，表示适用于所有类型
                if (itemTypes.isEmpty()) {
                    return templateName;
                }
                
                // 检查物品类型是否匹配
                for (String type : itemTypes) {
                    if (materialName.contains(type)) {
                        return templateName;
                    }
                }
                
                // 检查特殊类型
                if ((itemTypes.contains("WEAPON") && isWeaponItem(item.getType())) ||
                    (itemTypes.contains("TOOL") && isToolItem(item.getType())) ||
                    (itemTypes.contains("ARMOR") && isArmorItem(item.getType()))) {
                    return templateName;
                }
            }
        }
        
        // 如果没有找到匹配的模板，返回默认模板
        return "default";
    }
    
    /**
     * 使用传统方式生成lore
     * 用于兼容旧版本配置
     */
    private List<String> generateLegacyLore(ItemStack item, Player player, String source) {
        List<String> lore = new ArrayList<>();
        
        // 获取配置
        boolean showDurability = plugin.getConfig().getBoolean("lore.show-durability", true);
        boolean showTime = plugin.getConfig().getBoolean("lore.show-time", true);
        boolean showPlayer = plugin.getConfig().getBoolean("lore.show-player", true);
        boolean showSource = plugin.getConfig().getBoolean("lore.show-source", true);
        
        // 添加顶部分割线
        String topSeparator = plugin.getConfig().getString("lore.basic-settings.top-separator", "&8&m⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤");
        lore.add(colorManager.colorize(topSeparator));
        lore.add("");
        
        // 添加耐久度
        if (showDurability && item.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) item.getItemMeta();
            int maxDurability = item.getType().getMaxDurability();
            int currentDurability = maxDurability - damageable.getDamage();
            
            // 计算耐久度百分比
            int durabilityPercentage = (maxDurability > 0) ? (currentDurability * 100 / maxDurability) : 100;
            
            // 处理耐久度显示
            String durabilityText = plugin.getConfig().getString("lore.durability-format", "&8❖ &7耐久: &c%ilore_current_low%&e%ilore_current_medium%&a%ilore_current_high%&8/&f%ilore_max%");
                
            // 根据耐久度百分比处理不同颜色段
            String currentLow = "";
            String currentMedium = "";
            String currentHigh = "";
            
            if (durabilityPercentage <= 20) {
                currentLow = String.valueOf(currentDurability);
            } else if (durabilityPercentage <= 50) {
                currentMedium = String.valueOf(currentDurability);
            } else {
                currentHigh = String.valueOf(currentDurability);
            }
            
            durabilityText = durabilityText
                    .replace("%ilore_current_low%", currentLow)
                    .replace("%ilore_current_medium%", currentMedium)
                    .replace("%ilore_current_high%", currentHigh)
                    .replace("%ilore_max%", String.valueOf(maxDurability));
            
            durabilityText = colorManager.colorize(durabilityText);
            
            lore.add(durabilityText);
            lore.add("");
        }
        
        // 添加随机Lore
        if (plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            List<String> randomLores = getRandomLores(item, player);
            if (!randomLores.isEmpty()) {
                lore.add("");
                lore.addAll(randomLores);
            }
        }
        
        // 添加信息分隔线
        String infoSeparator = plugin.getConfig().getString("lore.basic-settings.info-separator", "&8&m· · · · · · · · · · · · · · · · · · · ·");
        lore.add("");
        lore.add(colorManager.colorize(infoSeparator));
        lore.add("");
        
        // 添加获取时间
        if (showTime) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    plugin.getConfig().getString("lore.time-format", "yyyy-MM-dd HH:mm:ss"));
            String timeText = plugin.getConfig().getString("lore.time-prefix", "&7获取时间：&f%ilore_time%");
            timeText = colorManager.colorize(timeText
                    .replace("%ilore_time%", dateFormat.format(new Date())));
            
            lore.add(timeText);
        }
        
        // 添加玩家名称
        if (showPlayer) {
            String playerText = plugin.getConfig().getString("lore.player-prefix", "&7获取人：&f%ilore_player%");
            playerText = colorManager.colorize(playerText
                    .replace("%ilore_player%", player.getName()));
            
            lore.add(playerText);
        }
        
        // 添加来源
        if (showSource) {
            String sourceText = plugin.getConfig().getString("lore.source-prefix", "&7来源：&f%ilore_source%");
            sourceText = colorManager.colorize(sourceText
                    .replace("%ilore_source%", source));
            
            lore.add(sourceText);
        }
        
        // 添加底部分割线
        lore.add("");
        String bottomSeparator = plugin.getConfig().getString("lore.basic-settings.bottom-separator", "&8&m⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤");
        lore.add(colorManager.colorize(bottomSeparator));
        
        return lore;
    }
    
    /**
     * 获取随机lore
     * @param item 物品
     * @param player 玩家
     * @return 随机lore列表
     */
    private List<String> getRandomLores(ItemStack item, Player player) {
        List<String> randomLores = new ArrayList<>();
        
        if (!plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            return randomLores;
        }
        
        // 获取随机lore数量
        int minAmount = plugin.getConfig().getInt("lore.random-lore.amount.min", 1);
        int maxAmount = plugin.getConfig().getInt("lore.random-lore.amount.max", 3);
        
        // 确保范围有效
        if (minAmount <= 0) minAmount = 1;
        if (maxAmount < minAmount) maxAmount = minAmount;
        
        // 随机生成数量
        int amount = (minAmount == maxAmount) ? minAmount : minAmount + random.nextInt(maxAmount - minAmount + 1);
        
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
                randomLore = randomLore.replace("%ilore_player_name%", player.getName())
                        .replace("%ilore_material_name%", item.getType().toString());
                
                // 处理所有插件变量
                randomLore = variableProcessor.parseAllVariables(randomLore, player, item);
                
                // 处理颜色代码
                randomLore = colorManager.colorize(randomLore);
                
                randomLores.add(randomLore);
            }
        }
        
        return randomLores;
    }
    
    /**
     * 添加自定义lore到列表中
     * @param lore 当前lore列表
     * @param item 物品
     * @param player 玩家
     * @param source 来源
     */
    private void addCustomLoreToList(List<String> lore, ItemStack item, Player player, String source) {
        if (!plugin.getConfig().getBoolean("lore.custom-lore.enabled", false)) {
            return;
        }
        
        List<String> customLores = plugin.getConfig().getStringList("lore.custom-lore.lines");
        if (customLores.isEmpty()) {
            return;
        }
        
        // 添加空行分隔
        if (plugin.getConfig().getBoolean("lore.basic-settings.show-empty-lines", true)) {
            lore.add("");
        }
        
        // 添加每一行自定义lore
        for (String line : customLores) {
            // 处理变量
            line = variableProcessor.parseAllVariables(line, player, item);
            
            // 处理颜色代码
            line = colorManager.colorize(line);
            
            lore.add(line);
        }
    }
    
    /**
     * 检查物品是否应该处理
     * @param item 物品
     * @return 是否应该处理
     */
    public boolean shouldProcessItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        
        List<String> itemTypes = plugin.getConfig().getStringList("item-types");
        
        // 如果包含ALL，则处理所有物品
        if (itemTypes.contains("ALL")) return true;
        
        Material material = item.getType();
        String materialName = material.toString();
        
        // 检查物品材质名称是否在配置的物品类型列表中
        for (String type : itemTypes) {
            if (materialName.contains(type)) return true;
        }
        
        // 检查特殊类型
        if (itemTypes.contains("DAMAGEABLE") && material.getMaxDurability() > 0) return true;
        if (itemTypes.contains("TOOL") && isToolItem(material)) return true;
        if (itemTypes.contains("ARMOR") && isArmorItem(material)) return true;
        if (itemTypes.contains("WEAPON") && isWeaponItem(material)) return true;
        
        return false;
    }
    
    /**
     * 检查物品是否为工具类
     * @param material 物品材质
     * @return 是否为工具类
     */
    public boolean isToolItem(Material material) {
        String name = material.toString();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || 
               name.endsWith("_SHOVEL") || name.endsWith("_HOE") || 
               name.endsWith("_SHEARS") || name.equals("FLINT_AND_STEEL");
    }
    
    /**
     * 检查物品是否为盔甲类
     * @param material 物品材质
     * @return 是否为盔甲类
     */
    public boolean isArmorItem(Material material) {
        String name = material.toString();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || 
               name.equals("SHIELD") || name.equals("ELYTRA");
    }
    
    /**
     * 检查物品是否为武器类
     * @param material 物品材质
     * @return 是否为武器类
     */
    public boolean isWeaponItem(Material material) {
        String name = material.toString();
        return name.endsWith("_SWORD") || name.equals("BOW") || 
               name.equals("CROSSBOW") || name.equals("TRIDENT");
    }
} 