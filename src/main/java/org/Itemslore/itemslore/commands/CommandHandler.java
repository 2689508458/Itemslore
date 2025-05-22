package org.Itemslore.itemslore.commands;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.managers.ConfigManager;
import org.Itemslore.itemslore.managers.LoreManager;
import org.Itemslore.itemslore.utils.ColorManager;
import org.Itemslore.itemslore.utils.ItemTypeChecker;
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
    private final ItemTypeChecker itemTypeChecker;
    
    public CommandHandler(Itemslore plugin, ColorManager colorManager, LoreManager loreManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.colorManager = colorManager;
        this.loreManager = loreManager;
        this.configManager = configManager;
        this.itemTypeChecker = new ItemTypeChecker();
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
                
            case "mode":
                return handleModeCommand(sender, args);
                
            case "give":
                return handleGiveCommand(sender, args);
            
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
                    (type.equals("WEAPON") && itemTypeChecker.isWeaponItem(item.getType())) ||
                    (type.equals("TOOL") && itemTypeChecker.isToolItem(item.getType())) ||
                    (type.equals("ARMOR") && itemTypeChecker.isArmorItem(item.getType()))) {
                    matchType = true;
                    break;
                }
            }
            
            if (!matchType) {
                player.sendMessage(ChatColor.RED + "该模板不适用于你手中的物品类型！");
                return true;
            }
        }
        
        // 强制覆盖模式下，清除现有lore
        if (plugin.getConfig().getString("existing-lore.mode", "APPEND").equalsIgnoreCase("OVERWRITE")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(new ArrayList<>());
                item.setItemMeta(meta);
            }
        }
        
        // 添加新的lore
        boolean success = loreManager.addLoreToItem(item, player, "模板应用");
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "已将 '" + templateName + "' 模板应用到手中物品！");
        } else {
            // 如果添加失败，可能是因为物品已有lore且模式为IGNORE
            String mode = plugin.getConfig().getString("existing-lore.mode", "APPEND");
            if (mode.equalsIgnoreCase("IGNORE") && item.getItemMeta().hasLore()) {
                player.sendMessage(ChatColor.YELLOW + "物品已有Lore，当前模式为忽略(IGNORE)，未应用模板。");
            } else {
                player.sendMessage(ChatColor.YELLOW + "物品可能已经有插件添加的Lore，未应用模板。");
            }
        }
        
        return true;
    }
    
    /**
     * 处理模式命令
     * 用于设置和查看已有Lore的处理模式
     */
    private boolean handleModeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemslore.mode")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }
        
        // 如果只有一个参数，显示当前模式
        if (args.length == 1) {
            String currentMode = plugin.getConfig().getString("existing-lore.mode", "APPEND");
            sender.sendMessage(ChatColor.GOLD + "=== 当前Lore处理模式 ===");
            sender.sendMessage(ChatColor.YELLOW + "模式: " + ChatColor.WHITE + currentMode);
            sender.sendMessage(ChatColor.YELLOW + "- APPEND: " + ChatColor.WHITE + "在已有Lore后添加新的Lore");
            sender.sendMessage(ChatColor.YELLOW + "- OVERWRITE: " + ChatColor.WHITE + "覆盖已有Lore");
            sender.sendMessage(ChatColor.YELLOW + "- IGNORE: " + ChatColor.WHITE + "忽略已有Lore的物品");
            sender.sendMessage(ChatColor.YELLOW + "使用 /itemslore mode <APPEND|OVERWRITE|IGNORE> 来更改模式");
            return true;
        }
        
        // 如果有第二个参数，尝试设置模式
        String newMode = args[1].toUpperCase();
        if (!newMode.equals("APPEND") && !newMode.equals("OVERWRITE") && !newMode.equals("IGNORE")) {
            sender.sendMessage(ChatColor.RED + "无效的模式值！有效值: APPEND, OVERWRITE, IGNORE");
            return true;
        }
        
        // 更新配置
        plugin.getConfig().set("existing-lore.mode", newMode);
        configManager.saveConfig();
        
        sender.sendMessage(ChatColor.GREEN + "已将Lore处理模式设置为: " + newMode);
        
        // 给出模式的说明
        switch (newMode) {
            case "APPEND":
                sender.sendMessage(ChatColor.YELLOW + "这将在已有Lore后添加新的Lore");
                break;
            case "OVERWRITE":
                sender.sendMessage(ChatColor.YELLOW + "这将覆盖物品的已有Lore");
                break;
            case "IGNORE":
                sender.sendMessage(ChatColor.YELLOW + "这将忽略已有Lore的物品，不进行处理");
                break;
        }
        
        return true;
    }
    
    /**
     * 处理give命令，给指定玩家的手持物品添加随机Lore
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemslore.give")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /itemslore give <玩家名> [来源]");
            return true;
        }
        
        // 获取目标玩家
        String playerName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(playerName);
        
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(ChatColor.RED + "找不到在线玩家: " + playerName);
            return true;
        }
        
        // 获取玩家手持物品
        ItemStack item = targetPlayer.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "玩家 " + playerName + " 手上没有持有物品！");
            return true;
        }
        
        // 检查物品是否符合处理条件
        if (!loreManager.shouldProcessItem(item)) {
            sender.sendMessage(ChatColor.RED + "该物品不符合处理条件！请检查配置的item-types设置。");
            return true;
        }
        
        // 确定来源信息
        String source = "命令赠予";
        if (args.length >= 3) {
            // 如果提供了自定义来源，使用自定义来源
            source = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        }
        
        // 添加Lore
        boolean success = loreManager.addLoreToItem(item, targetPlayer, source);
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "已成功为玩家 " + playerName + " 的手持物品添加Lore！");
            targetPlayer.sendMessage(ChatColor.GREEN + "你的手持物品已被赋予了神秘的属性！");
        } else {
            String mode = plugin.getConfig().getString("existing-lore.mode", "APPEND");
            if (mode.equalsIgnoreCase("IGNORE") && item.getItemMeta().hasLore()) {
                sender.sendMessage(ChatColor.YELLOW + "物品已有Lore，当前模式为忽略(IGNORE)，未应用Lore。");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "物品可能已经有插件添加的Lore，未应用Lore。");
            }
        }
        
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
        sender.sendMessage(ChatColor.YELLOW + "/itemslore mode " + ChatColor.WHITE + "- 查看当前Lore处理模式");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore mode <APPEND|OVERWRITE|IGNORE> " + ChatColor.WHITE + "- 设置Lore处理模式");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore give <玩家名> " + ChatColor.WHITE + "- 给指定玩家的手持物品添加随机Lore");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore help " + ChatColor.WHITE + "- 显示此帮助信息");
    }
} 