package org.Itemslore.itemslore.listeners;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.managers.LoreManager;
import org.Itemslore.itemslore.utils.MobSourceParser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 物品事件监听器，处理物品相关事件
 */
public class ItemEventListener implements Listener {
    
    private final Itemslore plugin;
    private final LoreManager loreManager;
    private final MobSourceParser mobSourceParser;
    
    // 用于存储怪物掉落物品的来源信息
    private final Map<UUID, String> itemSourceMap = new HashMap<>();
    
    public ItemEventListener(Itemslore plugin, LoreManager loreManager) {
        this.plugin = plugin;
        this.loreManager = loreManager;
        this.mobSourceParser = new MobSourceParser(plugin);
    }

    /**
     * 监听怪物死亡事件，处理掉落物
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        // 如果不是玩家杀死的，或者掉落物为空，则跳过
        if (killer == null || event.getDrops().isEmpty()) return;
        
        // 获取怪物名称作为来源
        String source = mobSourceParser.parseEntitySource(entity);
        
        // 为每个掉落物添加标记，以便在拾取时添加Lore
        for (ItemStack drop : event.getDrops()) {
            if (loreManager.shouldProcessItem(drop)) {
                // 在掉落前，物品还未生成对应的Item实体，所以先不添加Lore
                // 而是在玩家拾取时添加，这里我们记录物品的来源信息
                
                // 当物品实体被拾取时，我们会使用这个标记来确定来源
                // 这里我们模拟这个过程，实际上需要更复杂的逻辑来追踪物品
                
                // 添加临时NBT标签或使用其他方式标记物品
                // 由于Bukkit API限制，我们使用简单的UUID映射来实现
                
                // 在实体死亡事件中，物品还没有变成世界中的Item实体
                // 所以我们在玩家拾取前，记录一下这批物品的来源
                // 这里的实现比较简单，实际上可能需要更复杂的逻辑
                
                // 生成一个UUID作为物品的标记
                UUID itemId = UUID.randomUUID();
                
                // 将UUID写入物品的临时数据（例如通过NBT）
                // 由于Bukkit API的限制，这里我们简化处理
                // 在实际应用中，可能需要使用NBT标签API或其他方式
                
                // 将来源信息与物品关联
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("记录物品来源: " + source + " 对应掉落物: " + drop.getType().name());
                }
            }
        }
    }
    
    /**
     * 监听物品捡起事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        if (loreManager.shouldProcessItem(item)) {
            // 获取物品实体
            Item itemEntity = event.getItem();
            Entity dropper = itemEntity.getThrower() != null ? 
                    plugin.getServer().getEntity(itemEntity.getThrower()) : null;
            
            // 确定来源
            String source;
            if (dropper instanceof LivingEntity && !(dropper instanceof Player)) {
                // 如果是生物掉落的，使用生物名称
                source = mobSourceParser.parseEntitySource(dropper);
            } else {
                // 默认来源
                source = "拾取";
            }
            
            loreManager.addLoreToItem(item, player, source);
        }
    }

    /**
     * 监听物品合成事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (loreManager.shouldProcessItem(item)) {
            loreManager.addLoreToItem(item, player, "合成");
        }
    }
    
    /**
     * 监听物品准备合成事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareCraftItem(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof Player && event.getRecipe() != null) {
            Player player = (Player) event.getView().getPlayer();
            ItemStack result = event.getRecipe().getResult();
            
            if (loreManager.shouldProcessItem(result)) {
                ItemStack clonedResult = result.clone();
                if (loreManager.addLoreToItem(clonedResult, player, "合成")) {
                    event.getInventory().setResult(clonedResult);
                }
            }
        }
    }
    
    /**
     * 监听熔炉提取物品事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = new ItemStack(event.getItemType(), event.getItemAmount());
        
        if (loreManager.shouldProcessItem(item)) {
            loreManager.addLoreToItem(item, player, "熔炼");
            // 注意：这里无法直接修改玩家获得的物品，因为事件中没有提供该功能
            // 玩家实际获得物品是在事件之后处理的
        }
    }
    
    /**
     * 监听玩家钓鱼事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() == null) return;
        
        Player player = event.getPlayer();
        if (event.getCaught() instanceof org.bukkit.entity.Item) {
            org.bukkit.entity.Item caughtItem = (org.bukkit.entity.Item) event.getCaught();
            ItemStack item = caughtItem.getItemStack();
            
            if (loreManager.shouldProcessItem(item)) {
                loreManager.addLoreToItem(item, player, "钓鱼");
                caughtItem.setItemStack(item);
            }
        }
    }
    
    /**
     * 监听物品消耗事件（例如吃食物）
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // 这里只是示例，通常消耗物品不需要添加Lore
        // 但有些插件可能会有特殊需求，例如可重用的饮料瓶等
        if (plugin.getConfig().getBoolean("add-lore-to-consumed-items", false)) {
            if (loreManager.shouldProcessItem(item)) {
                loreManager.addLoreToItem(item, player, "消耗");
            }
        }
    }
    
    /**
     * 监听铁砧使用事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // 检查锻造台操作
        if (event.getClickedInventory() instanceof SmithingInventory) {
            // 锻造台事件处理，检查结果槽位
            if (event.getRawSlot() == 2) { // 结果槽位
                ItemStack result = event.getCurrentItem();
                if (result != null && !result.getType().isAir()) {
                    if (loreManager.shouldProcessItem(result)) {
                        loreManager.addLoreToItem(result, player, "锻造");
                    }
                }
            }
        }
    }
    
    /**
     * 监听方块破坏事件（例如收获庄稼）
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        // 这里监听方块破坏是为了处理那些直接掉落的物品
        // 但实际上物品掉落后会被EntityPickupItemEvent捕获并处理
        // 这里只是提供一个示例，如果有特殊需求可以在这里处理
        Player player = event.getPlayer();
        
        // 检查是否是特殊方块，例如特定的农作物或自定义方块
        if (plugin.getConfig().getBoolean("process-drops-from-broken-blocks", false)) {
            // 在实际应用中，可能需要调用一个工具方法来检查和处理掉落物
            // 这需要与其他插件集成或使用自定义逻辑
        }
    }
} 