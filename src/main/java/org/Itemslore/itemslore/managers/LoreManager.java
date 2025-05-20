package org.Itemslore.itemslore.managers;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.utils.ColorManager;
import org.Itemslore.itemslore.utils.VariableProcessor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
                if (plugin.getConfig().getBoolean("lore.random-lore.enabled", true)) {
                    // 获取随机lore
                    List<String> randomLores = getRandomLores(item, player);
                    
                    // 判断如果没有随机lore，跳过该行
                    if (randomLores.isEmpty()) {
                        continue;
                    }
                    
                    // 将第一个随机lore替换变量
                    String firstLore = randomLores.remove(0); // 移除第一个并返回它
                    lore.add(colorManager.colorize(processedLine.replace("%ilore_random_lore%", firstLore)));
                    
                    // 添加剩余的随机lore作为单独的行
                    for (String randomLore : randomLores) {
                        lore.add(colorManager.colorize(randomLore));
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
     * 获取随机Lore
     * @param item 物品
     * @param player 玩家
     * @return 随机Lore列表
     */
    private List<String> getRandomLores(ItemStack item, Player player) {
        List<String> randomLores = new ArrayList<>();
        
        // 如果随机Lore功能禁用，则返回空列表
        if (!plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            return randomLores;
        }
        
        // 获取物品类型
        Material material = item.getType();
        String materialName = material.toString();
        
        // 检查随机Lore概率
        double chanceToGenerate = plugin.getConfig().getDouble("lore.random-lore.chances.global", 1.0);
        
        // 查找物品类型特定概率
        ConfigurationSection chancesSection = plugin.getConfig().getConfigurationSection("lore.random-lore.chances.types");
        if (chancesSection != null) {
            // 优先检查完整材质名称
            if (chancesSection.contains(materialName)) {
                chanceToGenerate = chancesSection.getDouble(materialName) / 100.0;
            } else {
                // 检查材质类别
                for (String key : chancesSection.getKeys(false)) {
                    if (materialName.contains(key) || 
                        (key.equals("WEAPON") && isWeaponItem(material)) ||
                        (key.equals("TOOL") && isToolItem(material)) ||
                        (key.equals("ARMOR") && isArmorItem(material))) {
                        
                        chanceToGenerate = chancesSection.getDouble(key) / 100.0;
                        break;
                    }
                }
            }
        }
        
        // 确保概率在0-1之间
        chanceToGenerate = Math.max(0.0, Math.min(1.0, chanceToGenerate));
        
        // 获取随机Lore配置部分
        ConfigurationSection poolsSection = plugin.getConfig().getConfigurationSection("lore.random-lore.pools");
        if (poolsSection == null) {
            return randomLores;
        }
        
        // 定义要加载的池列表
        List<String> poolsToLoad = new ArrayList<>();
        
        // 总是加载通用池
        if (poolsSection.contains("ALL")) {
            poolsToLoad.add("ALL");
        }
        
        // 根据物品类型加载对应的池
        for (String key : poolsSection.getKeys(false)) {
            if (key.equals("ALL")) continue; // 通用池已经处理过
            
            if (materialName.contains(key) || 
                (key.equals("WEAPON") && isWeaponItem(material)) ||
                (key.equals("TOOL") && isToolItem(material)) ||
                (key.equals("ARMOR") && isArmorItem(material))) {
                poolsToLoad.add(key);
            }
        }
        
        // 如果没有找到任何适用的池，只使用通用池
        if (poolsToLoad.isEmpty() && poolsSection.contains("ALL")) {
            poolsToLoad.add("ALL");
        }
        
        // 检查配置开关
        boolean fixedCountAsRandom = plugin.getConfig().getBoolean("lore.random-lore.fixed-count-as-random", false);
        boolean uniqueCountAsRandom = plugin.getConfig().getBoolean("lore.random-lore.unique-count-as-random", true);
        
        // 分离固定Lore和随机Lore
        List<String> fixedLores = new ArrayList<>(); // 不计入随机数量的固定Lore
        List<String> countedFixedLores = new ArrayList<>(); // 计入随机数量的固定Lore
        List<List<Object>> randomLorePool = new ArrayList<>();
        List<Double> poolWeights = new ArrayList<>();
        List<Boolean> isUnique = new ArrayList<>();
        double totalWeight = 0.0;
        
        // 从适用的池中加载Lore
        for (String poolName : poolsToLoad) {
            List<?> loreList = plugin.getConfig().getList("lore.random-lore.pools." + poolName);
            if (loreList != null) {
                for (Object loreObj : loreList) {
                    double weight = 0.5; // 默认权重为0.5 (50%)
                    String loreText;
                    boolean unique = false; // 默认非唯一
                    boolean fixed = false;  // 默认非固定
                    
                    if (loreObj instanceof List) {
                        // 格式：["文本", 权重, 唯一性标记(可选), 固定标记(可选)]
                        List<?> loreEntry = (List<?>) loreObj;
                        if (loreEntry.size() >= 4) {
                            loreText = String.valueOf(loreEntry.get(0));
                            
                            // 处理权重 - 将整数权重转换为0.1-1范围
                            try {
                                Object weightObj = loreEntry.get(1);
                                if (weightObj instanceof Number) {
                                    double rawWeight = ((Number) weightObj).doubleValue();
                                    if (rawWeight > 10) { // 如果是旧系统的1-100权重
                                        weight = Math.max(0.1, Math.min(1.0, rawWeight / 100.0));
                                    } else { // 如果是新系统的0.1-1权重
                                        weight = Math.max(0.1, Math.min(1.0, rawWeight));
                                    }
                                } else {
                                    weight = Double.parseDouble(String.valueOf(weightObj)) / 10.0;
                                    weight = Math.max(0.1, Math.min(1.0, weight));
                                }
                            } catch (Exception e) {
                                weight = 0.5; // 解析失败，使用默认权重
                            }
                            
                            unique = Boolean.parseBoolean(String.valueOf(loreEntry.get(2)));
                            fixed = Boolean.parseBoolean(String.valueOf(loreEntry.get(3)));
                        } else if (loreEntry.size() >= 3) {
                            loreText = String.valueOf(loreEntry.get(0));
                            
                            // 处理权重 - 将整数权重转换为0.1-1范围
                            try {
                                Object weightObj = loreEntry.get(1);
                                if (weightObj instanceof Number) {
                                    double rawWeight = ((Number) weightObj).doubleValue();
                                    if (rawWeight > 10) { // 如果是旧系统的1-100权重
                                        weight = Math.max(0.1, Math.min(1.0, rawWeight / 100.0));
                                    } else { // 如果是新系统的0.1-1权重
                                        weight = Math.max(0.1, Math.min(1.0, rawWeight));
                                    }
                                } else {
                                    weight = Double.parseDouble(String.valueOf(weightObj)) / 10.0;
                                    weight = Math.max(0.1, Math.min(1.0, weight));
                                }
                            } catch (Exception e) {
                                weight = 0.5; // 解析失败，使用默认权重
                            }
                            
                            unique = Boolean.parseBoolean(String.valueOf(loreEntry.get(2)));
                        } else if (loreEntry.size() >= 2) {
                            loreText = String.valueOf(loreEntry.get(0));
                            
                            // 处理权重 - 将整数权重转换为0.1-1范围
                            try {
                                Object weightObj = loreEntry.get(1);
                                if (weightObj instanceof Number) {
                                    double rawWeight = ((Number) weightObj).doubleValue();
                                    if (rawWeight > 10) { // 如果是旧系统的1-100权重
                                        weight = Math.max(0.1, Math.min(1.0, rawWeight / 100.0));
                                    } else { // 如果是新系统的0.1-1权重
                                        weight = Math.max(0.1, Math.min(1.0, rawWeight));
                                    }
                                } else {
                                    weight = Double.parseDouble(String.valueOf(weightObj)) / 10.0;
                                    weight = Math.max(0.1, Math.min(1.0, weight));
                                }
                            } catch (Exception e) {
                                weight = 0.5; // 解析失败，使用默认权重
                            }
                        } else if (loreEntry.size() == 1) {
                            loreText = String.valueOf(loreEntry.get(0));
                        } else {
                            continue; // 跳过无效条目
                        }
                    } else {
                        // 简单字符串格式
                        loreText = String.valueOf(loreObj);
                    }
                    
                    // 检查文本中是否包含特殊标记
                    if (loreText.contains("UNIQUE:")) {
                        unique = true;
                        loreText = loreText.replace("UNIQUE:", "");
                    }
                    
                    if (loreText.contains("FIXED:")) {
                        fixed = true;
                        loreText = loreText.replace("FIXED:", "");
                    }
                    
                    // 添加池来源标记，帮助调试
                    String debugText = loreText + "§r§8[" + poolName + "]";
                    
                    // 处理变量和占位符
                    String processedText = variableProcessor.parseAllVariables(debugText, player, item);
                    String coloredText = colorManager.colorize(processedText);
                    
                    // 移除调试标记用于显示
                    String finalText = coloredText.replaceAll("§r§8\\[[^\\]]+\\]$", "");
                    
                    if (fixed) {
                        // 固定Lore总是添加，不受概率影响
                        if (fixedCountAsRandom) {
                            // 计入随机数量的固定Lore
                            countedFixedLores.add(finalText);
                        } else {
                            // 不计入随机数量的固定Lore
                            fixedLores.add(finalText);
                        }
                    } else {
                        // 检查是否通过概率检测
                        if (random.nextDouble() <= chanceToGenerate) {
                            List<Object> entry = new ArrayList<>();
                            entry.add(loreText);
                            entry.add(weight);
                            
                            randomLorePool.add(entry);
                            poolWeights.add(weight);
                            isUnique.add(unique);
                            totalWeight += weight;
                        }
                    }
                }
            }
        }
        
        // 先添加不计入随机数量的固定Lore
        randomLores.addAll(fixedLores);
        
        // 如果没有可用的随机Lore池且没有计入数量的固定Lore，返回固定Lore列表
        if (randomLorePool.isEmpty() && countedFixedLores.isEmpty()) {
            return randomLores;
        }
        
        // 获取随机Lore的数量设置
        int minAmount = plugin.getConfig().getInt("lore.random-lore.amount.min", 1);
        int maxAmount = plugin.getConfig().getInt("lore.random-lore.amount.max", 3);
        
        // 确保最小值不大于最大值
        minAmount = Math.min(minAmount, maxAmount);
        
        // 计算应该生成的随机Lore数量
        int targetAmount = (minAmount >= maxAmount) ? minAmount : (random.nextInt(maxAmount - minAmount + 1) + minAmount);
        
        // 如果有计入数量的固定Lore，调整目标数量
        int adjustedTarget = targetAmount - countedFixedLores.size();
        adjustedTarget = Math.max(0, adjustedTarget); // 确保不会为负数
        
        // 限制目标数量不超过可用的随机Lore池大小
        adjustedTarget = Math.min(adjustedTarget, randomLorePool.size());
        
        // 跟踪已选择的唯一Lore的类型
        List<String> selectedUniqueTypes = new ArrayList<>();
        List<String> selectedLores = new ArrayList<>();
        
        // 最多尝试选择次数，避免死循环
        int maxAttempts = randomLorePool.size() * 2;
        int attempts = 0;
        
        // 随机选择Lore，直到达到调整后的目标数量或尝试次数用完
        while (selectedLores.size() < adjustedTarget && attempts < maxAttempts && !randomLorePool.isEmpty()) {
            attempts++;
            
            // 根据权重随机选择
            double randomValue = random.nextDouble() * totalWeight;
            double weightSum = 0.0;
            int selectedIndex = -1;
            
            for (int j = 0; j < poolWeights.size(); j++) {
                weightSum += poolWeights.get(j);
                if (randomValue <= weightSum) {
                    selectedIndex = j;
                    break;
                }
            }
            
            if (selectedIndex == -1 || selectedIndex >= randomLorePool.size()) {
                continue; // 无效索引，重试
            }
            
            String selectedLore = String.valueOf(randomLorePool.get(selectedIndex).get(0));
            boolean unique = isUnique.get(selectedIndex);
            
            // 检查唯一标记，避免选择多个相同类型的唯一性Lore
            if (unique) {
                // 确定唯一性Lore的类型/类别
                String uniqueCategory = determineUniqueCategory(selectedLore);
                
                // 检查是否已存在相同类别的唯一性Lore
                if (selectedUniqueTypes.contains(uniqueCategory)) {
                    // 已有相同类别的唯一性Lore，跳过这个
                    
                    // 更新总权重
                    totalWeight -= poolWeights.get(selectedIndex);
                    
                    // 移除已尝试的Lore
                    randomLorePool.remove(selectedIndex);
                    poolWeights.remove(selectedIndex);
                    isUnique.remove(selectedIndex);
                    
                    continue;
                }
                
                // 将该类别的唯一标记添加到列表
                selectedUniqueTypes.add(uniqueCategory);
                
                // 唯一性Lore计数特殊处理
                if (!uniqueCountAsRandom) {
                    // 如果唯一性Lore不计入随机数量，则增加目标数量
                    adjustedTarget++;
                }
                
                // 记录日志，便于调试
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("选择了唯一性Lore[" + uniqueCategory + "]: " + selectedLore);
                }
            }
            
            // 处理变量和占位符
            String processedLore = variableProcessor.parseAllVariables(selectedLore, player, item);
            String coloredLore = colorManager.colorize(processedLore);
            
            // 移除调试标记
            coloredLore = coloredLore.replaceAll("§r§8\\[[^\\]]+\\]$", "");
            
            selectedLores.add(coloredLore);
            
            // 更新总权重
            totalWeight -= poolWeights.get(selectedIndex);
            
            // 移除已选择的Lore，避免重复
            randomLorePool.remove(selectedIndex);
            poolWeights.remove(selectedIndex);
            isUnique.remove(selectedIndex);
        }
        
        // 先添加计入随机数量的固定Lore
        randomLores.addAll(countedFixedLores);
        
        // 再添加所有选定的随机Lore
        randomLores.addAll(selectedLores);
        
        // 记录日志，便于调试
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("生成的随机Lore总数: " + randomLores.size() + 
                                    " (固定不计数: " + fixedLores.size() + 
                                    ", 固定计数: " + countedFixedLores.size() + 
                                    ", 随机: " + selectedLores.size() + ")");
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
     * 确定唯一性Lore的类别
     * 根据Lore文本内容提取类别信息，用于区分不同种类的唯一性Lore
     * @param loreText Lore文本内容
     * @return 唯一性类别标识
     */
    private String determineUniqueCategory(String loreText) {
        // 尝试根据颜色代码和特殊符号后的第一个词来确定类别
        
        // 方法1：如果包含冒号，提取冒号前的关键字
        if (loreText.contains(":")) {
            String beforeColon = loreText.substring(0, loreText.indexOf(":")).trim();
            // 寻找最后一个空格，取最后一个词
            int lastSpaceIndex = beforeColon.lastIndexOf(" ");
            if (lastSpaceIndex != -1 && lastSpaceIndex < beforeColon.length() - 1) {
                return beforeColon.substring(lastSpaceIndex + 1);
            }
            return beforeColon; // 如果没有空格，返回整个冒号前的内容
        }
        
        // 方法2：按空格分割，找到第一个不是颜色代码的词
        String[] parts = loreText.split(" ");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.isEmpty() && !part.startsWith("&") && !part.startsWith("§")) {
                return part;
            }
        }
        
        // 如果无法确定特定类别，根据内容生成一个唯一标识
        // 使用hashCode可以为相同内容生成相同的标识
        return "UNIQUE_" + Math.abs(loreText.hashCode() % 1000);
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