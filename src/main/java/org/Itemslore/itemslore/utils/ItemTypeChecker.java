package org.Itemslore.itemslore.utils;

import org.bukkit.Material;

/**
 * 物品类型检查器
 * 用于识别和分类不同类型的物品
 */
public class ItemTypeChecker {
    
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
     * @return
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
    
    /**
     * 检查物品是否应该被处理
     * @param material 物品材质
     * @param itemTypes 配置的物品类型列表
     * @return 是否应该处理
     */
    public boolean shouldProcessItemType(Material material, java.util.List<String> itemTypes) {
        // 如果包含ALL，则处理所有物品
        if (itemTypes.contains("ALL")) return true;
        
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
} 