package org.Itemslore.itemslore.managers;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.utils.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lore管理器，负责生成和处理物品的Lore
 * 通过组合其他专业工具类来减少代码复杂度
 */
public class LoreManager {
    private final Itemslore plugin;
    private final ColorManager colorManager;
    private final VariableProcessor variableProcessor;
    private final RandomLoreGenerator randomLoreGenerator;
    private final LoreTemplateManager loreTemplateManager;
    private final ItemTypeChecker itemTypeChecker;
    
    // 已有Lore处理模式
    public enum ExistingLoreMode {
        APPEND,     // 追加到现有Lore后面
        OVERWRITE,  // 覆盖现有Lore
        IGNORE      // 忽略已有Lore的物品
    }
    
    public LoreManager(Itemslore plugin, ColorManager colorManager, VariableProcessor variableProcessor) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.variableProcessor = variableProcessor;
        this.randomLoreGenerator = new RandomLoreGenerator(plugin, colorManager, variableProcessor);
        this.loreTemplateManager = new LoreTemplateManager(plugin, colorManager, variableProcessor);
        this.itemTypeChecker = new ItemTypeChecker();
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
        
        List<String> existingLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (existingLore == null) existingLore = new ArrayList<>();
        
        // 检查物品是否已被处理（通过检测关键字）
        if (hasPluginLore(existingLore)) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("物品已有插件Lore，跳过处理");
            }
            return false;
        }
        
        // 获取已有Lore处理模式
        ExistingLoreMode mode = getExistingLoreMode();
        
        // 如果已有Lore且模式为IGNORE，则跳过处理
        if (!existingLore.isEmpty() && mode == ExistingLoreMode.IGNORE) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("物品已有Lore且处理模式为IGNORE，跳过处理");
            }
            return false;
        }
        
        // 首先获取随机lore
        List<String> randomLores = randomLoreGenerator.getRandomLores(item, player);
        
        // 生成新的lore
        List<String> newLore = loreTemplateManager.generateLoreFromTemplate(item, player, source, randomLores);
        
        // 根据处理模式添加Lore
        List<String> finalLore;
        
        if (existingLore.isEmpty() || mode == ExistingLoreMode.OVERWRITE) {
            // 如果没有已有Lore或模式为覆盖，直接使用新的Lore
            finalLore = newLore;
                } else {
            // 模式为追加，将新Lore添加到已有Lore后面
            finalLore = new ArrayList<>(existingLore);
            
            // 如果配置了添加分隔线，则添加一个分隔线
            if (plugin.getConfig().getBoolean("existing-lore.add-separator", true)) {
                String separator = plugin.getConfig().getString("existing-lore.separator-style", 
                                                              "&8&m· · · · · · · · · · · · · · · · · · · ·");
                finalLore.add("");
                finalLore.add(colorManager.colorize(separator));
                finalLore.add("");
                } else {
                // 否则只添加一个空行
                finalLore.add("");
            }
            
            // 添加新的Lore
            finalLore.addAll(newLore);
        }
        
        // 设置新的lore
        meta.setLore(finalLore);
        item.setItemMeta(meta);
        
        return true;
    }
    
    /**
     * 获取已有Lore的处理模式
     * @return 处理模式枚举
     */
    private ExistingLoreMode getExistingLoreMode() {
        String modeStr = plugin.getConfig().getString("existing-lore.mode", "APPEND").toUpperCase();
        try {
            return ExistingLoreMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的existing-lore.mode配置值: " + modeStr + "，使用默认值APPEND");
            return ExistingLoreMode.APPEND;
        }
    }
    
    /**
     * 检查物品是否已经有插件生成的Lore
     * @param lore 物品Lore列表
     * @return 是否包含插件Lore
     */
    private boolean hasPluginLore(List<String> lore) {
        if (lore.isEmpty()) return false;
        
        List<String> detectionKeywords = plugin.getConfig().getStringList("existing-lore.detection-keywords");
        if (detectionKeywords.isEmpty()) {
            // 默认检测关键字
            detectionKeywords = new ArrayList<>();
            detectionKeywords.add("&8❖ &7耐久");
            detectionKeywords.add("&7获取时间");
            detectionKeywords.add("&7获取者");
            detectionKeywords.add("&7来源");
        }
        
        // 将检测关键字转换为颜色代码
        List<String> coloredKeywords = new ArrayList<>();
        for (String keyword : detectionKeywords) {
            coloredKeywords.add(colorManager.colorize(keyword));
        }
        
        // 检查是否包含任一关键字
        for (String line : lore) {
            for (String keyword : coloredKeywords) {
                if (line.contains(keyword)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查物品是否应该处理
     * @param item 物品
     * @return 是否应该处理
     */
    public boolean shouldProcessItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        
        List<String> itemTypes = plugin.getConfig().getStringList("item-types");
        return itemTypeChecker.shouldProcessItemType(item.getType(), itemTypes);
    }
} 