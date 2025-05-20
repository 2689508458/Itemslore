package org.Itemslore.itemslore.listeners;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.managers.LoreManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 物品事件监听器，处理物品相关事件
 */
public class ItemEventListener implements Listener {
    
    private final Itemslore plugin;
    private final LoreManager loreManager;
    
    public ItemEventListener(Itemslore plugin, LoreManager loreManager) {
        this.plugin = plugin;
        this.loreManager = loreManager;
    }

    /**
     * 监听物品捡起事件
     */
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        if (loreManager.shouldProcessItem(item)) {
            loreManager.addLoreToItem(item, player, "拾取");
        }
    }

    /**
     * 监听物品合成事件
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (loreManager.shouldProcessItem(item)) {
            loreManager.addLoreToItem(item, player, "合成");
        }
    }
} 