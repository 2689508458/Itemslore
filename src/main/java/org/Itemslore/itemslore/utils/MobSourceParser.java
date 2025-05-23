package org.Itemslore.itemslore.utils;

import org.Itemslore.itemslore.Itemslore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 怪物来源解析器
 * 用于解析实体名称并转换为可读性更好的来源信息
 */
public class MobSourceParser {
    
    private final Itemslore plugin;
    private boolean mythicMobsEnabled = false;
    
    // 实体类型名称映射表
    private static final Map<String, String> ENTITY_NAME_MAP = new HashMap<>();
    
    static {
        // 初始化实体类型名称映射表
        ENTITY_NAME_MAP.put("ZOMBIE", "僵尸");
        ENTITY_NAME_MAP.put("SKELETON", "骷髅");
        ENTITY_NAME_MAP.put("CREEPER", "苦力怕");
        ENTITY_NAME_MAP.put("SPIDER", "蜘蛛");
        ENTITY_NAME_MAP.put("CAVE_SPIDER", "洞穴蜘蛛");
        ENTITY_NAME_MAP.put("ENDERMAN", "末影人");
        ENTITY_NAME_MAP.put("BLAZE", "烈焰人");
        ENTITY_NAME_MAP.put("GHAST", "恶魂");
        ENTITY_NAME_MAP.put("SLIME", "史莱姆");
        ENTITY_NAME_MAP.put("MAGMA_CUBE", "岩浆怪");
        ENTITY_NAME_MAP.put("WITCH", "女巫");
        ENTITY_NAME_MAP.put("GUARDIAN", "守卫者");
        ENTITY_NAME_MAP.put("ELDER_GUARDIAN", "远古守卫者");
        ENTITY_NAME_MAP.put("SHULKER", "潜影贝");
        ENTITY_NAME_MAP.put("ENDERMITE", "末影螨");
        ENTITY_NAME_MAP.put("SILVERFISH", "蠹虫");
        ENTITY_NAME_MAP.put("EVOKER", "唤魔者");
        ENTITY_NAME_MAP.put("VEX", "恼鬼");
        ENTITY_NAME_MAP.put("VINDICATOR", "卫道士");
        ENTITY_NAME_MAP.put("ILLUSIONER", "幻术师");
        ENTITY_NAME_MAP.put("RAVAGER", "劫掠兽");
        ENTITY_NAME_MAP.put("PHANTOM", "幻翼");
        ENTITY_NAME_MAP.put("DROWNED", "溺尸");
        ENTITY_NAME_MAP.put("PILLAGER", "掠夺者");
        ENTITY_NAME_MAP.put("WITHER_SKELETON", "凋灵骷髅");
        ENTITY_NAME_MAP.put("STRAY", "流浪者");
        ENTITY_NAME_MAP.put("HUSK", "尸壳");
        ENTITY_NAME_MAP.put("WITHER", "凋灵");
        ENTITY_NAME_MAP.put("ENDER_DRAGON", "末影龙");
        ENTITY_NAME_MAP.put("PIGLIN", "猪灵");
        ENTITY_NAME_MAP.put("PIGLIN_BRUTE", "猪灵蛮兵");
        ENTITY_NAME_MAP.put("ZOMBIFIED_PIGLIN", "僵尸猪灵");
        ENTITY_NAME_MAP.put("HOGLIN", "疣猪兽");
        ENTITY_NAME_MAP.put("ZOGLIN", "僵尸疣猪兽");
        ENTITY_NAME_MAP.put("STRIDER", "炽足兽");
        // 添加更多实体类型的映射
    }
    
    public MobSourceParser(Itemslore plugin) {
        this.plugin = plugin;
        // 检查MythicMobs插件是否已加载
        mythicMobsEnabled = plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs");
        
        if (mythicMobsEnabled) {
            plugin.getLogger().info("检测到MythicMobs插件，已启用自定义怪物名称支持");
        }
    }
    
    /**
     * 解析实体来源信息
     * @param entity 实体对象
     * @return 格式化的来源信息
     */
    public String parseEntitySource(Entity entity) {
        if (entity == null) {
            return "未知来源";
        }
        
        // 检查是否为MythicMobs实体
        if (mythicMobsEnabled) {
            String mythicName = getMythicMobName(entity);
            if (mythicName != null && !mythicName.isEmpty()) {
                return "击杀 " + mythicName;
            }
        }
        
        // 处理普通实体
        String entityType = entity.getType().name();
        
        // 如果实体有自定义名称，使用自定义名称
        if (entity instanceof LivingEntity && ((LivingEntity) entity).getCustomName() != null) {
            String customName = ((LivingEntity) entity).getCustomName();
            return "击杀 " + customName;
        }
        
        // 使用映射表中的中文名称
        String localizedName = ENTITY_NAME_MAP.getOrDefault(entityType, entityType);
        return "击杀 " + localizedName;
    }
    
    /**
     * 获取MythicMobs实体的自定义名称
     * @param entity 实体对象
     * @return MythicMobs自定义名称，如果不是MythicMobs实体则返回null
     */
    private String getMythicMobName(Entity entity) {
        if (!mythicMobsEnabled) return null;
        
        try {
            // 使用反射安全地调用MythicMobs API
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object instance = apiClass.getMethod("inst").invoke(null);
            Object mobManager = instance.getClass().getMethod("getMobManager").invoke(instance);
            
            // 检查实体是否为MythicMob
            boolean isMythicMob = (boolean) mobManager.getClass().getMethod("isActiveMob", Entity.class).invoke(mobManager, entity);
            
            if (isMythicMob) {
                // 获取MythicMob实例
                Object activeMob = mobManager.getClass().getMethod("getMythicMobInstance", Entity.class).invoke(mobManager, entity);
                
                // 获取MythicMob名称
                if (activeMob != null) {
                    String mobType = (String) activeMob.getClass().getMethod("getType").invoke(activeMob);
                    String displayName = (String) activeMob.getClass().getMethod("getDisplayName").invoke(activeMob);
                    
                    return displayName != null && !displayName.isEmpty() ? displayName : mobType;
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("获取MythicMobs名称时出错: " + e.getMessage());
            }
        }
        
        return null;
    }
} 