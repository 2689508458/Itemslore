package org.Itemslore.itemslore.commands;

import org.Itemslore.itemslore.Itemslore;
import org.Itemslore.itemslore.utils.ColorManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 命令Tab补全处理器
 */
public class CommandTabCompleter implements TabCompleter {
    
    private final Itemslore plugin;
    private final ColorManager colorManager;
    
    private final List<String> MAIN_COMMANDS = Arrays.asList(
            "reload", "clear", "random", "help", "template", "mode", "give"
    );
    
    private final List<String> TEMPLATE_COMMANDS = Arrays.asList(
            "list", "info", "apply"
    );
    
    private final List<String> MODE_OPTIONS = Arrays.asList(
            "APPEND", "OVERWRITE", "IGNORE"
    );
    
    private final List<String> SOURCE_SUGGESTIONS = Arrays.asList(
            "击杀BOSS", "击杀自定义怪物", "任务奖励", "特殊合成", "商店购买", "活动奖励", "VIP礼包", "抽奖获得"
    );
    
    public CommandTabCompleter(Itemslore plugin, ColorManager colorManager) {
        this.plugin = plugin;
        this.colorManager = colorManager;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 主命令补全
            StringUtil.copyPartialMatches(args[0], MAIN_COMMANDS, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            // 子命令参数补全
            switch (args[0].toLowerCase()) {
                case "template":
                    // 补全template子命令
                    StringUtil.copyPartialMatches(args[1], TEMPLATE_COMMANDS, completions);
                    Collections.sort(completions);
                    return completions;
                case "mode":
                    // 补全mode选项
                    StringUtil.copyPartialMatches(args[1], MODE_OPTIONS, completions);
                    Collections.sort(completions);
                    return completions;
                case "give":
                    // 补全玩家名称
                    List<String> playerNames = plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[1], playerNames, completions);
                    Collections.sort(completions);
                    return completions;
                default:
                    return Collections.emptyList();
            }
        } else if (args.length == 3) {
            // 第三级参数补全
            if ("template".equalsIgnoreCase(args[0])) {
                if ("info".equalsIgnoreCase(args[1]) || "apply".equalsIgnoreCase(args[1])) {
                    // 获取所有模板名
                    List<String> templates = new ArrayList<>();
                    
                    // 添加配置文件中定义的所有模板
                    ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("lore.templates");
                    if (templatesSection != null) {
                        templatesSection.getKeys(false).forEach(templates::add);
                    }
                    
                    StringUtil.copyPartialMatches(args[2], templates, completions);
                    Collections.sort(completions);
                    return completions;
                }
            } else if ("give".equalsIgnoreCase(args[0])) {
                // 给指定玩家添加lore命令的来源建议
                StringUtil.copyPartialMatches(args[2], SOURCE_SUGGESTIONS, completions);
                Collections.sort(completions);
                return completions;
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 用于字符串部分匹配的工具类
     */
    public static class StringUtil {
        public static List<String> copyPartialMatches(String token, List<String> originals, List<String> collection) {
            for (String string : originals) {
                if (startsWithIgnoreCase(string, token)) {
                    collection.add(string);
                }
            }
            
            return collection;
        }
        
        public static boolean startsWithIgnoreCase(String string, String prefix) {
            if (string.length() < prefix.length()) {
                return false;
            }
            return string.regionMatches(true, 0, prefix, 0, prefix.length());
        }
    }
} 