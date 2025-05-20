package org.Itemslore.itemslore;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsloreCommand implements CommandExecutor, TabCompleter {

    private final Itemslore plugin;
    private final List<String> subCommands = Arrays.asList("reload", "help");

    public ItemsloreCommand(Itemslore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("itemslore.reload")) {
                    sender.sendMessage(ChatColor.RED + "您没有权限执行此命令！");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ItemsLore配置已重新加载！");
                return true;
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "未知命令，请使用 /itemslore help 查看帮助。");
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "==========[ ItemsLore 命令帮助 ]==========");
        sender.sendMessage(ChatColor.YELLOW + "/itemslore help " + ChatColor.WHITE + "- 显示帮助信息");
        if (sender.hasPermission("itemslore.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/itemslore reload " + ChatColor.WHITE + "- 重新加载配置文件");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(subCmd -> sender.hasPermission("itemslore." + subCmd) || subCmd.equals("help"))
                    .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
} 