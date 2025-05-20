package org.Itemslore.itemslore.utils;

import org.Itemslore.itemslore.Itemslore;
import org.bukkit.ChatColor;

/**
 * 颜色管理器，处理颜色相关功能
 */
public class ColorManager {
    private final Itemslore plugin;
    
    public ColorManager(Itemslore plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 将颜色代码转换为彩色文本
     * @param text 带颜色代码的文本
     * @return 转换后的彩色文本
     */
    public String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
} 