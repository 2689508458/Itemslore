# ItemsLore - 智能物品信息增强插件

## 📝 插件介绍

ItemsLore是一款功能强大的Minecraft Bukkit/Spigot服务器插件，可以自动为物品添加丰富多彩的Lore信息。通过该插件，玩家可以轻松查看物品的耐久度、获取时间、获取者和来源等信息，增强游戏体验。

## ✨ 主要特性

- **自动添加Lore**: 当玩家获取物品时自动添加各种实用信息
- **耐久度显示**: 显示物品当前耐久和最大耐久，并使用动态颜色直观展示
- **时间记录**: 记录物品获取的具体时间
- **来源追踪**: 记录物品的来源（例如合成、挖掘、击杀等）
- **随机属性**: 根据物品类型随机生成特殊属性描述
- **模板系统**: 通过灵活的模板系统自定义不同类型物品的Lore格式
- **权限控制**: 详细的权限设置，确保功能安全可控
- **完整的命令**: 提供丰富的命令和Tab补全支持

## 🔧 插件配置

该插件提供了高度可定制的配置选项：

- 自定义Lore模板，支持不同物品类型使用不同模板
- 完全可自定义的分隔线样式和信息格式
- 随机Lore池可根据物品类型进行配置
- 支持自定义符号和颜色

## 📋 安装方法

1. 从[发布页面](https://github.com/YourUsername/Itemslore/releases)下载最新版本的插件JAR文件
2. 将下载的JAR文件放入服务器的`plugins`文件夹中
3. 重启服务器或使用插件管理器加载插件
4. 插件将自动创建配置文件，您可以根据需要进行自定义

## 📚 使用方法

### 基本命令

- `/itemslore reload` - 重新加载插件配置
- `/itemslore clear` - 清除手中物品的所有Lore
- `/itemslore random` - 为手中物品添加随机Lore
- `/itemslore template list` - 查看所有可用模板
- `/itemslore template info <模板名>` - 查看指定模板的详细信息
- `/itemslore template apply <模板名>` - 将指定模板应用到手中物品
- `/itemslore help` - 显示帮助信息

### 权限节点

- `itemslore.use` - 允许使用基本命令（默认所有玩家）
- `itemslore.reload` - 允许重载插件配置（默认OP）
- `itemslore.clear` - 允许清除物品Lore
- `itemslore.random` - 允许应用随机Lore
- `itemslore.template` - 允许使用模板相关命令

## 🔌 与其他插件兼容

ItemsLore通过预设的变量前缀设计，避免与其他插件产生变量冲突。同时支持以下插件的集成：

- PlaceholderAPI - 支持使用各种变量
- Vault - 支持经济相关信息
- MMOItems - 支持MMOItems物品属性
- Multiverse-Core - 支持多世界信息

## ⚙️ 配置文件示例

```yaml
# 自定义Lore模板示例
templates:
  weapon:
    enabled: true
    item-types:
      - "SWORD"
      - "BOW"
    content:
      - "%ilore_top_separator%"
      - ""
      - "&c&l⚔ 武器属性"
      - "%ilore_durability%"
      - ""
      - "&8[ &c随机武器特性 &8]"
      - "%ilore_random_lore%"
      - ""
      - "%ilore_info_separator%"
      - ""
      - "%ilore_time%"
      - "%ilore_player%"
      - "%ilore_source%"
      - ""
      - "%ilore_bottom_separator%"
```

## 🌟 展示

当装备一个剑时，可能会显示如下信息：

```
⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤

⚔ 武器属性
❖ 耐久: 1561/1562

[ 随机武器特性 ]
⚔ 锋利: 锋利无比，可轻易切断钢铁

· · · · · · · · · · · · · · · · · · · ·

◈ 获取时间: 2023-05-20 18:49
✦ 获取者: xiaoyue
✧ 来源: 模板应用

⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤
```

## 📝 更新日志

### 版本 1.0.0
- 初始版本发布
- 添加基本物品信息功能
- 实现模板系统
- 添加随机Lore生成功能

## 📞 获取支持

如果您在使用过程中遇到任何问题，可以通过以下方式联系我们：

- 在GitHub上提交[Issue](https://github.com/YourUsername/Itemslore/issues)
- 加入我们的QQ群: 123456789

## 📜 许可证

本项目采用 [MIT 许可证](LICENSE) 发布。

---

感谢使用 ItemsLore 插件！希望它能为您的服务器增添乐趣和便利。 