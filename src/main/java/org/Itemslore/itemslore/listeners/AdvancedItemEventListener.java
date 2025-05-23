package org.Itemslore.itemslore.listeners;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.managers.LoreManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

/**
 * 高级物品事件监听器
 * 处理更多物品相关事件
 */
public class AdvancedItemEventListener implements Listener {
    
    private final Itemslore plugin;
    private final LoreManager loreManager;
    
    public AdvancedItemEventListener(Itemslore plugin, LoreManager loreManager) {
        this.plugin = plugin;
        this.loreManager = loreManager;
    }
    
    /**
     * 监听物品附魔事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        ItemStack item = event.getItem();
        
        if (loreManager.shouldProcessItem(item)) {
            loreManager.addLoreToItem(item, player, "附魔");
        }
    }
    
    /**
     * 监听物品修复事件（经验修补）
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemMend(PlayerItemMendEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (loreManager.shouldProcessItem(item)) {
            loreManager.addLoreToItem(item, player, "经验修补");
        }
    }
    
    /**
     * 监听物品破损事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemBreak(PlayerItemBreakEvent event) {
        // 这里只能记录而不能更改，因为物品已经破损
        if (plugin.getConfig().getBoolean("debug", false)) {
            Player player = event.getPlayer();
            ItemStack item = event.getBrokenItem();
            plugin.getLogger().info("玩家 " + player.getName() + " 的物品 " + item.getType().name() + " 已损坏");
        }
    }
    
    /**
     * 监听物品丢弃事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemDrop(PlayerDropItemEvent event) {
        // 这个事件通常不需要处理，因为丢弃的物品已经在物品栏中处理过
        // 如果有特殊需求，可以在这里添加处理逻辑
    }
    
    /**
     * 监听物品交换事件（主副手）
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        // 处理主手到副手的物品
        ItemStack mainHandItem = event.getMainHandItem();
        if (mainHandItem != null && loreManager.shouldProcessItem(mainHandItem)) {
            loreManager.addLoreToItem(mainHandItem, player, "物品交换");
        }
        
        // 处理副手到主手的物品
        ItemStack offHandItem = event.getOffHandItem();
        if (offHandItem != null && loreManager.shouldProcessItem(offHandItem)) {
            loreManager.addLoreToItem(offHandItem, player, "物品交换");
        }
    }
    
    /**
     * 监听物品交互事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 这个事件通常发生频率很高，所以需要谨慎处理
        // 如果有特殊需求，可以在这里添加处理逻辑
    }
    
    /**
     * 监听铁砧合成事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAnvilUse(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() != InventoryType.ANVIL) return;
        
        // 只有点击结果槽位才处理
        if (event.getRawSlot() != 2) return;
        
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;
        
        Player player = (Player) event.getWhoClicked();
        if (loreManager.shouldProcessItem(result)) {
            loreManager.addLoreToItem(result, player, "铁砧修复");
        }
    }
} 