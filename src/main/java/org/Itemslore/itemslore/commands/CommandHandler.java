package org.Itemslore.itemslore.commands;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.managers.ConfigManager;
import org.Itemslore.itemslore.managers.LoreManager;
import org.Itemslore.itemslore.utils.ColorManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 命令处理器，处理插件的命令
 */
public class CommandHandler implements CommandExecutor {
    
    private final Itemslore plugin;
    private final ColorManager colorManager;
    private final LoreManager loreManager;
    private final ConfigManager configManager;
    
    public CommandHandler(Itemslore plugin, ColorManager colorManager, LoreManager loreManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.loreManager = loreManager;
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReloadCommand(sender);
            
            case "clear":
                return handleClearCommand(sender, args);
            
            case "random":
                return handleRandomCommand(sender, args);
            
            case "template":
                return handleTemplateCommand(sender, args);
            
            case "help":
                showHelp(sender);
                return true;
            
            default:
                showHelp(sender);
                return true;
        }
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("itemslore.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }
        
        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "ItemsLore配置已重新加载！");
        return true;
    }
    
    /**
     * 处理清除lore命令
     */
    private boolean handleClearCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }
        
        if (!sender.hasPermission("itemslore.clear")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || !item.hasItemMeta()) {
            sender.sendMessage(ChatColor.RED + "你手上没有持有物品或物品没有Meta数据！");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        meta.setLore(new ArrayList<>());
        item.setItemMeta(meta);
        
        sender.sendMessage(ChatColor.GREEN + "已清除手中物品的所有Lore！");
        return true;
    }
    
    /**
     * 处理随机lore命令
     */
    private boolean handleRandomCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }
        
        if (!sender.hasPermission("itemslore.random")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "你手上没有持有物品！");
            return true;
        }
        
        if (!plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "随机Lore功能未启用！请在配置中启用。");
            return true;
        }
        
        // 添加随机lore到物品
        loreManager.addLoreToItem(item, player, "命令");
        
        sender.sendMessage(ChatColor.GREEN + "已为手中物品添加随机Lore！");
        return true;
    }
    
    /**
     * 处理模板命令
     */
    private boolean handleTemplateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /itemslore template <list|info|apply> [模板名]");
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleTemplateListCommand(sender);
                
            case "info":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /itemslore template info <模板名>");
                    return true;
                }
                return handleTemplateInfoCommand(sender, args[2]);
                
            case "apply":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
                    return true;
                }
                
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /itemslore template apply <模板名>");
                    return true;
                }
                
                return handleTemplateApplyCommand((Player) sender, args[2]);
                
            default:
                sender.sendMessage(ChatColor.RED + "未知的子命令: " + subCommand);
                sender.sendMessage(ChatColor.RED + "可用子命令: list, info, apply");
                return true;
        }
    }
    
    /**
     * 处理模板列表命令
     */
    private boolean handleTemplateListCommand(CommandSender sender) {
        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("lore.templates");
        if (templatesSection == null) {
            sender.sendMessage(ChatColor.RED + "找不到任何模板配置！");
            return true;
        }
        
        Set<String> templateNames = templatesSection.getKeys(false);
        if (templateNames.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "没有配置任何模板！");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== 可用的Lore模板 ===");
        
        for (String templateName : templateNames) {
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(templateName);
            if (templateSection != null && templateSection.getBoolean("enabled", true)) {
                List<String> itemTypes = templateSection.getStringList("item-types");
                String itemTypesStr = itemTypes.isEmpty() ? "所有物品" : String.join(", ", itemTypes);
                
                sender.sendMessage(ChatColor.YELLOW + " - " + templateName + ChatColor.GRAY + " (适用于: " + itemTypesStr + ")");
            }
        }
        
        sender.sendMessage(ChatColor.YELLOW + "使用 /itemslore template info <模板名> 查看模板详情");
        sender.sendMessage(ChatColor.YELLOW + "使用 /itemslore template apply <模板名> 应用模板到手持物品");
        
        return true;
    }
    
    /**
     * 处理模板信息命令
     */
    private boolean handleTemplateInfoCommand(CommandSender sender, String templateName) {
        ConfigurationSection templateSection = plugin.getConfig().getConfigurationSection("lore.templates." + templateName);
        if (templateSection == null) {
            sender.sendMessage(ChatColor.RED + "找不到名为 '" + templateName + "' 的模板！");
            return true;
        }
        
        boolean enabled = templateSection.getBoolean("enabled", true);
        List<String> itemTypes = templateSection.getStringList("item-types");
        List<String> content = templateSection.getStringList("content");
        
        sender.sendMessage(ChatColor.GOLD + "=== 模板信息: " + templateName + " ===");
        sender.sendMessage(ChatColor.YELLOW + "状态: " + (enabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        
        String itemTypesStr = itemTypes.isEmpty() ? "所有物品" : String.join(", ", itemTypes);
        sender.sendMessage(ChatColor.YELLOW + "适用物品: " + ChatColor.WHITE + itemTypesStr);
        
        sender.sendMessage(ChatColor.YELLOW + "模板内容:");
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            sender.sendMessage(ChatColor.GRAY + " " + (i + 1) + ". " + colorManager.colorize(line));
        }
        
        return true;
    }
    
    /**
     * 处理模板应用命令
     */
    private boolean handleTemplateApplyCommand(Player player, String templateName) {
        ConfigurationSection templateSection = plugin.getConfig().getConfigurationSection("lore.templates." + templateName);
        if (templateSection == null) {
            player.sendMessage(ChatColor.RED + "找不到名为 '" + templateName + "' 的模板！");
            return true;
        }
        
        if (!templateSection.getBoolean("enabled", true)) {
            player.sendMessage(ChatColor.RED + "该模板已被禁用！");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "你手上没有持有物品！");
            return true;
        }
        
        // 检查物品类型是否符合模板要求
        List<String> itemTypes = templateSection.getStringList("item-types");
        if (!itemTypes.isEmpty()) {
            String materialName = item.getType().name().toUpperCase();
            boolean matchType = false;
            
            for (String type : itemTypes) {
                if (materialName.contains(type) || 
                    (type.equals("WEAPON") && loreManager.isWeaponItem(item.getType())) ||
                    (type.equals("TOOL") && loreManager.isToolItem(item.getType())) ||
                    (type.equals("ARMOR") && loreManager.isArmorItem(item.getType()))) {
                    matchType = true;
                    break;
                }
            }
            
            if (!matchType) {
                player.sendMessage(ChatColor.RED + "该模板不适用于你手中的物品类型！");
                return true;
            }
        }
        
        // 清除现有lore
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(new ArrayList<>());
            item.setItemMeta(meta);
        }
        
        // 添加新的lore
        loreManager.addLoreToItem(item, player, "模板应用");
        
        player.sendMessage(ChatColor.GREEN + "已将 '" + templateName + "' 模板应用到手中物品！");
        return true;
    }
    
    /**
     * 显示帮助信息
     * @param sender 命令发送者
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== ItemsLore 命令帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore reload " + ChatColor.WHITE + "- 重新加载配置");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore clear " + ChatColor.WHITE + "- 清除手中物品的所有Lore");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore random " + ChatColor.WHITE + "- 为手中物品添加随机Lore");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore template list " + ChatColor.WHITE + "- 查看所有可用模板");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore template info <模板名> " + ChatColor.WHITE + "- 查看模板详情");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore template apply <模板名> " + ChatColor.WHITE + "- 应用模板到手持物品");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore help " + ChatColor.WHITE + "- 显示此帮助信息");
    }
} 