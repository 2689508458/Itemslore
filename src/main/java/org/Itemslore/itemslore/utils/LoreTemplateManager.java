package org.Itemslore.itemslore.utils;

import org.Itemslore.itemslore.Itemslore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lore模板管理器
 * 负责处理和应用Lore模板
 */
public class LoreTemplateManager {
    private final Itemslore plugin;
    private final ColorManager colorManager;
    private final VariableProcessor variableProcessor;
    private final ItemTypeChecker itemTypeChecker;
    
    public LoreTemplateManager(Itemslore plugin, ColorManager colorManager, VariableProcessor variableProcessor) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.variableProcessor = variableProcessor;
        this.itemTypeChecker = new ItemTypeChecker();
    }
    
    /**
     * 从模板生成物品lore内容
     * @param item 物品
     * @param player 玩家
     * @param source 来源
     * @param randomLores 随机Lore列表
     * @return 生成的lore列表
     */
    public List<String> generateLoreFromTemplate(ItemStack item, Player player, String source, List<String> randomLores) {
        List<String> lore = new ArrayList<>();
        
        // 获取合适的模板
        String templateName = findSuitableTemplate(item);
        ConfigurationSection templateSection = plugin.getConfig().getConfigurationSection("lore.templates." + templateName);
        
        // 如果没有找到模板，使用默认模板
        if (templateSection == null) {
            templateSection = plugin.getConfig().getConfigurationSection("lore.templates.default");
            // 如果还是没有，则使用传统格式
            if (templateSection == null) {
                return generateLegacyLore(item, player, source, randomLores);
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
                if (plugin.getConfig().getBoolean("lore.random-lore.enabled", true) && !randomLores.isEmpty()) {
                    // 将第一个随机lore替换变量
                    String firstLore = randomLores.remove(0); // 移除第一个并返回它
                    lore.add(colorManager.colorize(processedLine.replace("%ilore_random_lore%", firstLore)));
                    
                    // 添加剩余的随机lore作为单独的行
                    for (String randomLore : randomLores) {
                        lore.add(colorManager.colorize(randomLore));
                    }
                    
                    continue; // 跳过下面的添加，因为已经在这里处理了
                } else {
                    // 如果随机lore未启用或为空，删除该行
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
    public String findSuitableTemplate(ItemStack item) {
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
                if ((itemTypes.contains("WEAPON") && itemTypeChecker.isWeaponItem(item.getType())) ||
                    (itemTypes.contains("TOOL") && itemTypeChecker.isToolItem(item.getType())) ||
                    (itemTypes.contains("ARMOR") && itemTypeChecker.isArmorItem(item.getType()))) {
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
    private List<String> generateLegacyLore(ItemStack item, Player player, String source, List<String> randomLores) {
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
        if (!randomLores.isEmpty()) {
            lore.add("");
            lore.addAll(randomLores);
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
} 