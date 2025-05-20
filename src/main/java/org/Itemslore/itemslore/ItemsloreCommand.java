package org.Itemslore.itemslore;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemsloreCommand implements CommandExecutor {

    private final Itemslore plugin;

    public ItemsloreCommand(Itemslore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("itemslore.reload")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ItemsLore配置已重新加载！");
                return true;
                
            case "clear":
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
                
            case "random":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
                    return true;
                }
                
                if (!sender.hasPermission("itemslore.random")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                
                player = (Player) sender;
                item = player.getInventory().getItemInMainHand();
                
                if (item == null || item.getType().isAir()) {
                    sender.sendMessage(ChatColor.RED + "你手上没有持有物品！");
                    return true;
                }
                
                // 添加随机lore到物品
                addRandomLoreToItem(player, item);
                
                sender.sendMessage(ChatColor.GREEN + "已为手中物品添加随机Lore！");
                return true;
                
            case "color":
            case "colours":
            case "colors":
                if (!sender.hasPermission("itemslore.colors")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                
                // 显示所有可用的颜色模板
                showColorTemplates(sender);
                return true;
                
            case "colortest":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /itemslore colortest <模板名>");
                    return true;
                }
                
                if (!sender.hasPermission("itemslore.colors")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                
                // 测试指定的颜色模板
                testColorTemplate(sender, args[1]);
                return true;
                
            case "coloritem":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
                    return true;
                }
                
                if (!sender.hasPermission("itemslore.colors")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /itemslore coloritem <模板名> [自定义文本]");
                    return true;
                }
                
                player = (Player) sender;
                item = player.getInventory().getItemInMainHand();
                
                if (item == null || item.getType().isAir()) {
                    sender.sendMessage(ChatColor.RED + "你手上没有持有物品！");
                    return true;
                }
                
                // 添加颜色样式lore到物品
                String templateName = args[1];
                String text = (args.length > 2) ? args[2] : "测试文本";
                addColorTemplateToItem(player, item, templateName, text);
                
                sender.sendMessage(ChatColor.GREEN + "已为手中物品添加颜色样式Lore！");
                return true;
                
            default:
                showHelp(sender);
                return true;
        }
    }
    
    /**
     * 显示所有可用的颜色模板
     * @param sender 命令发送者
     */
    private void showColorTemplates(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== 可用的颜色模板 ===");
        
        // 获取渐变颜色模板
        if (plugin.getConfig().isConfigurationSection("colors.templates.gradients")) {
            sender.sendMessage(ChatColor.YELLOW + "渐变颜色模板:");
            for (String key : plugin.getConfig().getConfigurationSection("colors.templates.gradients").getKeys(false)) {
                String value = plugin.getConfig().getString("colors.templates.gradients." + key);
                value = value.replace("%text%", key);
                sender.sendMessage(" - " + key + ": " + ChatColor.translateAlternateColorCodes('&', value));
            }
        }
        
        // 获取样式模板
        if (plugin.getConfig().isConfigurationSection("colors.templates.styles")) {
            sender.sendMessage(ChatColor.YELLOW + "样式模板:");
            for (String key : plugin.getConfig().getConfigurationSection("colors.templates.styles").getKeys(false)) {
                String value = plugin.getConfig().getString("colors.templates.styles." + key);
                value = value.replace("%text%", key);
                sender.sendMessage(" - " + key + ": " + ChatColor.translateAlternateColorCodes('&', value));
            }
        }
        
        sender.sendMessage(ChatColor.YELLOW + "使用: %color_模板名%文本");
        sender.sendMessage(ChatColor.YELLOW + "测试: /itemslore colortest <模板名>");
    }
    
    /**
     * 测试指定的颜色模板
     * @param sender 命令发送者
     * @param templateName 模板名称
     */
    private void testColorTemplate(CommandSender sender, String templateName) {
        // 尝试获取渐变模板
        String template = plugin.getConfig().getString("colors.templates.gradients." + templateName);
        
        // 尝试获取样式模板
        if (template == null) {
            template = plugin.getConfig().getString("colors.templates.styles." + templateName);
        }
        
        if (template == null) {
            sender.sendMessage(ChatColor.RED + "找不到名为 '" + templateName + "' 的颜色模板！");
            return;
        }
        
        // 显示测试文本
        String[] testTexts = {"物品名称", "常见描述", "珍稀属性", "史诗特效", "传说神器"};
        
        sender.sendMessage(ChatColor.GOLD + "=== 颜色模板测试: " + templateName + " ===");
        for (String text : testTexts) {
            String colored = template.replace("%text%", text);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', colored));
        }
        
        sender.sendMessage(ChatColor.YELLOW + "在lore中使用: %color_" + templateName + "%文本");
    }
    
    /**
     * 添加颜色样式lore到物品
     * @param player 玩家
     * @param item 物品
     * @param templateName 模板名称
     * @param text 文本内容
     */
    private void addColorTemplateToItem(Player player, ItemStack item, String templateName, String text) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (lore == null) lore = new ArrayList<>();
        
        // 准备示例文本
        String exampleText = "%color_" + templateName + "%" + text;
        
        try {
            // 创建临时ItemListener来使用颜色处理方法
            ItemListener tempListener = new ItemListener(plugin);
            
            // 使用反射调用私有方法processColorTemplates
            Method method = ItemListener.class.getDeclaredMethod("processColorTemplates", String.class);
            method.setAccessible(true);
            String processedText = (String) method.invoke(tempListener, exampleText);
            
            // 处理颜色代码
            processedText = ChatColor.translateAlternateColorCodes('&', processedText);
            
            lore.add(processedText);
            meta.setLore(lore);
            item.setItemMeta(meta);
            
        } catch (Exception e) {
            plugin.getLogger().warning("处理颜色模板时出错: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "处理颜色模板时出错！");
        }
    }
    
    /**
     * 为物品添加随机Lore
     * @param player 玩家
     * @param item 物品
     */
    private void addRandomLoreToItem(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("lore.random-lore.enabled", false)) {
            player.sendMessage(ChatColor.RED + "随机Lore功能未启用！请在配置中启用。");
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (lore == null) lore = new ArrayList<>();
        
        // 创建临时的ItemListener来复用添加随机lore的功能
        ItemListener tempListener = new ItemListener(plugin);
        
        // 使用反射调用私有方法 addRandomLoreToItem
        try {
            java.lang.reflect.Method method = ItemListener.class.getDeclaredMethod("addRandomLoreToItem", List.class, ItemStack.class, Player.class);
            method.setAccessible(true);
            method.invoke(tempListener, lore, item, player);
            
            // 设置更新的lore
            meta.setLore(lore);
            item.setItemMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().warning("添加随机Lore时出错: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "添加随机Lore时出错！");
        }
    }

    /**
     * 显示帮助信息
     * @param sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== ItemsLore 命令帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore reload " + ChatColor.WHITE + "- 重新加载配置");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore clear " + ChatColor.WHITE + "- 清除手中物品的所有Lore");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore random " + ChatColor.WHITE + "- 为手中物品添加随机Lore");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore colors " + ChatColor.WHITE + "- 查看所有可用的颜色模板");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore colortest <模板名> " + ChatColor.WHITE + "- 测试指定颜色模板");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore coloritem <模板名> [文本] " + ChatColor.WHITE + "- 为物品添加指定颜色样式的Lore");
    }
} 