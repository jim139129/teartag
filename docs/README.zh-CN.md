# TearTag 使用说明

TearTag 是面向 Minecraft 26.1.2 的 Fabric 撕名牌机制模组。服务端与所有客户端都必须安装。模组不会占用胸甲栏，也不会生成跟随玩家的交互实体或按玩家绘制资源包。

## 构建与安装

- Java 25
- Fabric Loader 0.19.3+
- Fabric API 0.155.2+26.1.2

TearTag 始终注册不可堆叠的 `teartag:nametag` 物品；可选安装 Trinkets Updated（`4.0.0-beta.3+26.1`）来提供独立的 `teartag/nametag` 饰品槽位。`/teartag enable` 可按配置自动填入该槽位。`config/teartag.toml` 的 `[trinkets]` 部分控制启用时自动装备、装备时启用以及卸下时禁用。未安装 Trinkets 时，TearTag 原有功能不受影响。

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
.\gradlew.bat build
```

生成文件位于 `build/libs/teartag-1.1.jar`。

## 主要命令

- `/teartag enable <targets>`：启用并在首次启用时记录当前显示名。
- `/teartag text plain <targets> <text>`：设置普通文字。
- `/teartag text set <targets> <json_component>`：设置完整 Minecraft 文本组件。
- `/teartag text reset <targets>`：重新取当前显示名。
- `/teartag reset round <targets>`：清进度和淘汰状态，保留文字与规则。
- `/teartag reset all <targets>`：删除该玩家的全部持久记录。
- `/teartag rule set <targets> <rule_id>`：切换数据包攻击规则。
- `/teartag config reload`：校验并热重载配置。

默认情况下，从背后直接瞄准名牌才会成功。仅当名牌后方紧贴碰撞方块时，才允许从正面让射线先穿过目标身体、再瞄准背后名牌；服务端按默认 25% 概率判定，失败会消耗独立冷却。该逻辑不能隔墙攻击。

完整的数据包目录、攻击规则字段、事件时序、`teartag:event` storage、淘汰接管流程和可安装示例请参阅 [`DATAPACK.zh-CN.md`](DATAPACK.zh-CN.md)。Java API 概览位于根目录英文 README。
