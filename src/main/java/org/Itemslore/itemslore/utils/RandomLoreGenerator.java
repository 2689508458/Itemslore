package org.Itemslore.itemslore.utils;

import org.Itemslore.itemslore.Itemslore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机Lore生成器
 * 负责处理和生成随机属性lore
 */
public class RandomLoreGenerator {
    private final Itemslore plugin;
    private final ColorManager colorManager;
    private final VariableProcessor variableProcessor;
    private final Random random = new Random();
    
    public RandomLoreGenerator(Itemslore plugin, ColorManager colorManager, VariableProcessor variableProcessor) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.variableProcessor = variableProcessor;
    }
    
    /**
     * 获取随机Lore
     * @param item 物品
     * @param player 玩家
     * @return 随机Lore列表
     */
    public List<String> getRandomLores(ItemStack item, Player player) {
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
        
        // 先添加不计入随机数量的固定Lore (这些总是会出现)
        randomLores.addAll(fixedLores);
        
        // 如果没有可用的随机选择对象，直接返回
        if (randomLorePool.isEmpty() && countedFixedLores.isEmpty()) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("没有可用的随机Lore池和计入数量的固定Lore，只返回固定Lore: " + fixedLores.size() + "个");
            }
            return randomLores;
        }
        
        // 获取随机Lore的数量设置
        int minAmount = plugin.getConfig().getInt("lore.random-lore.amount.min", 1);
        int maxAmount = plugin.getConfig().getInt("lore.random-lore.amount.max", 3);
        
        // 确保最小值不大于最大值
        minAmount = Math.min(minAmount, maxAmount);
        
        // 如果固定Lore已经超过或等于最大数量，且计入随机数量，则不需要再选择随机Lore
        if (countedFixedLores.size() >= maxAmount && fixedCountAsRandom) {
            // 记录日志
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("固定Lore数量(" + countedFixedLores.size() + 
                                      ")已达到或超过最大随机数量(" + maxAmount + ")，不再生成随机Lore");
            }
            return randomLores;
        }
        
        // 计算实际可用的最小和最大数量（考虑已有的固定Lore）
        int effectiveMin = fixedCountAsRandom ? Math.max(0, minAmount - countedFixedLores.size()) : minAmount;
        int effectiveMax = fixedCountAsRandom ? Math.max(0, maxAmount - countedFixedLores.size()) : maxAmount;
        
        // 确保有效最小值不超过有效最大值
        effectiveMin = Math.min(effectiveMin, effectiveMax);
        
        // 计算应该生成的随机Lore数量
        int targetAmount = (effectiveMin >= effectiveMax) ? 
                           effectiveMin : 
                           (random.nextInt(effectiveMax - effectiveMin + 1) + effectiveMin);
        
        // 记录原始目标数量
        int originalTarget = targetAmount;
        
        // 限制目标数量不超过可用的随机Lore池大小
        targetAmount = Math.min(targetAmount, randomLorePool.size());
        
        // 记录日志
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("随机Lore目标数量: " + originalTarget + 
                                  " (调整后: " + targetAmount + 
                                  ", 固定计数: " + countedFixedLores.size() + 
                                  ", 池大小: " + randomLorePool.size() + ")");
        }
        
        // 跟踪已选择的唯一Lore的类型
        List<String> selectedUniqueTypes = new ArrayList<>();
        List<String> selectedLores = new ArrayList<>();
        
        // 最多尝试选择次数，避免死循环
        int maxAttempts = randomLorePool.size() * 2;
        int attempts = 0;
        
        // 随机选择Lore，直到达到目标数量或尝试次数用完
        while (selectedLores.size() < targetAmount && attempts < maxAttempts && !randomLorePool.isEmpty()) {
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
                    targetAmount++;
                    
                    // 记录日志
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("发现唯一性Lore[" + uniqueCategory + "]不计入数量，增加目标数量至: " + targetAmount);
                    }
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
        
        // 确保满足最小数量要求（如果配置了不计入随机数量的选项可能导致数量不足）
        int totalRandomCount = countedFixedLores.size() + selectedLores.size();
        int originalMin = plugin.getConfig().getInt("lore.random-lore.amount.min", 1);
        
        if (totalRandomCount < originalMin && randomLorePool.size() > selectedLores.size()) {
            // 记录警告
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("随机Lore数量(" + totalRandomCount + ")未达到最小值(" + originalMin + 
                                         ")，尝试添加更多Lore...");
            }
            
            // 尝试添加更多随机Lore以满足最小要求
            int needed = originalMin - totalRandomCount;
            for (int i = 0; i < Math.min(needed, randomLorePool.size()); i++) {
                if (!randomLorePool.isEmpty()) {
                    // 简单添加第一个可用的Lore
                    List<Object> entry = randomLorePool.remove(0);
                    String loreText = String.valueOf(entry.get(0));
                    
                    // 处理变量和颜色
                    String processedLore = variableProcessor.parseAllVariables(loreText, player, item);
                    String coloredLore = colorManager.colorize(processedLore);
                    
                    randomLores.add(coloredLore);
                }
            }
        }
        
        // 记录最终日志，便于调试
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("最终生成Lore统计:");
            plugin.getLogger().info("- 固定Lore(不计数): " + fixedLores.size() + "个");
            plugin.getLogger().info("- 固定Lore(计数): " + countedFixedLores.size() + "个");
            plugin.getLogger().info("- 随机选择Lore: " + selectedLores.size() + "个");
            plugin.getLogger().info("- 总计Lore数量: " + randomLores.size() + "个");
            
            if (fixedCountAsRandom) {
                plugin.getLogger().info("- 固定Lore计入随机数量: 是");
            } else {
                plugin.getLogger().info("- 固定Lore计入随机数量: 否");
            }
            
            if (uniqueCountAsRandom) {
                plugin.getLogger().info("- 唯一Lore计入随机数量: 是");
            } else {
                plugin.getLogger().info("- 唯一Lore计入随机数量: 否");
            }
        }
        
        return randomLores;
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