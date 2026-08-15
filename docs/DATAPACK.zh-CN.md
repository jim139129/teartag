# TearTag 数据包制作指南

本文档对应 Minecraft `26.1.2` 与 TearTag `1.1`。TearTag 负责名牌渲染、命中验证、撕取进度和淘汰状态；外置数据包通过命令、攻击规则和事件函数控制队伍、回合及传送流程。

仓库中的 [`docs/datapack-example/tear_demo`](datapack-example/tear_demo) 是一个可直接安装的最小示例。

## 1. 数据包能控制什么

数据包可以：

- 使用 `/teartag` 启用或禁用参与者、修改文字、重置回合、分配攻击规则。
- 使用实体 `/tag` 和原版队伍控制谁能撕谁。
- 监听成功撕取、贴墙失败、恢复、淘汰等事件。
- 在淘汰事件中接管默认传送，并在延迟结束时执行自己的传送逻辑。
- 从 `teartag:event` storage 读取目标、攻击者、撕取次数和规则等上下文。

数据包不能绕过服务端射线、距离、冷却和贴墙概率判定。全局数值配置仍由 `config/teartag.toml` 控制。

## 2. 最小目录结构

Minecraft 26.1.2 的数据包格式为 `101.1`。一个包含攻击规则和事件回调的数据包可以使用以下结构：

```text
tear_demo/
├─ pack.mcmeta
└─ data/
   ├─ tear_demo/
   │  ├─ function/
   │  │  ├─ round/start.mcfunction
   │  │  └─ event/on_eliminate.mcfunction
   │  └─ teartag_attack_rules/
   │     └─ hunter_vs_runner.json
   └─ teartag/
      └─ tags/
         └─ function/
            └─ on_eliminate.json
```

注意目录名是单数 `function` 和 `tags/function`，不是旧版本使用的 `functions`。

`pack.mcmeta`：

```json
{
  "pack": {
    "description": "TearTag game controller",
    "min_format": [101, 1],
    "max_format": 101
  }
}
```

将包含 `pack.mcmeta` 的数据包目录放入 `<世界目录>/datapacks/`，然后执行 `/reload`。若使用 ZIP，`pack.mcmeta` 必须位于 ZIP 根目录，不能再套一层文件夹。修改攻击规则或函数后也使用 `/reload`；`/teartag config reload` 只重载 `config/teartag.toml`。

## 3. 回合控制命令

数据包函数可以直接执行以下命令：

| 命令 | 作用 |
| --- | --- |
| `/teartag enable <targets>` | 启用玩家。首次启用时记录当前显示名；安装 Trinkets 时可自动装备名牌物品。 |
| `/teartag disable <targets>` | 禁用玩家并停止渲染，保留文字、规则和进度记录。 |
| `/teartag text plain <targets> <text>` | 设置普通名牌文字。 |
| `/teartag text set <targets> <component>` | 设置完整 Minecraft 文本组件。 |
| `/teartag text reset <targets>` | 重新读取玩家当前显示名。 |
| `/teartag reset round <targets>` | 清除撕取数和淘汰状态，保留启用状态、文字和规则。 |
| `/teartag reset all <targets>` | 删除玩家的整个 TearTag 持久状态并停止渲染。 |
| `/teartag rule set <targets> <rule_id>` | 给被保护的目标分配一个已加载的攻击规则。 |
| `/teartag status [target]` | 查看状态、进度、规则和文字。 |
| `/teartag event claim` | 仅在同步执行的 `on_eliminate` 回调内接管默认淘汰传送。 |

所有参与攻击的玩家都必须处于已启用、未淘汰状态。攻击规则分配给“被攻击的目标”，不是攻击者。例如：

```mcfunction
teartag enable @a[tag=mygame.active]
teartag reset round @a[tag=mygame.active]
teartag rule set @a[tag=mygame.runner] mygame:hunter_vs_runner
```

## 4. 攻击规则

规则文件位于：

```text
data/<命名空间>/teartag_attack_rules/<路径>.json
```

文件路径就是规则 ID：

```text
data/mygame/teartag_attack_rules/hunter_vs_runner.json
→ mygame:hunter_vs_runner

data/mygame/teartag_attack_rules/round/final.json
→ mygame:round/final
```

完整格式：

```json
{
  "allow_self": false,
  "any_of": [
    {
      "attacker_requires": ["mygame.hunter", "mygame.active"],
      "attacker_forbids": ["mygame.stunned"],
      "target_requires": ["mygame.runner", "mygame.active"],
      "target_forbids": ["mygame.immune"],
      "team": "different"
    }
  ],
  "participant_damage": "deny",
  "damage_rules": [
    {
      "attacker_requires": ["mygame.team_a"],
      "target_requires": ["mygame.team_b"],
      "policy": "allow"
    }
  ]
}
```

### 4.1 字段语义

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `allow_self` | `false` | 是否允许攻击者与目标为同一玩家。正常客户端不会瞄准自己，此项主要供 Java API 等扩展使用。 |
| `any_of` | 一个空条件分支 | OR 条件列表，只要任意分支匹配即允许尝试撕取。 |
| `attacker_requires` | `[]` | 攻击者必须拥有列表中的全部实体标签。 |
| `attacker_forbids` | `[]` | 攻击者拥有其中任意标签时，该分支失败。 |
| `target_requires` | `[]` | 目标必须拥有列表中的全部实体标签。 |
| `target_forbids` | `[]` | 目标拥有其中任意标签时，该分支失败。 |
| `team` | `"any"` | 队伍关系：`any`、`same` 或 `different`。 |
| `participant_damage` | `"deny"` | 该规则目标的普通玩家伤害默认策略：`deny` 或 `allow`。 |
| `damage_rules` | `[]` | 按文件顺序匹配的普通伤害覆盖规则；首个匹配项的 `policy` 生效。条件字段与 `any_of` 相同。 |

`any_of` 只决定“攻击者能否撕下该目标的名牌”，`damage_rules` 只决定“该攻击方向的普通玩家伤害是否保留”，两者相互独立。省略 `participant_damage` 和 `damage_rules` 时，保持旧行为：已启用且未淘汰的玩家之间不造成普通伤害。

例如，可以给 B 使用下面的目标规则，让 A 攻击 B 时保留普通伤害；给 A 使用没有 `damage_rules` 的规则，则 B 攻击 A 仍不造成普通伤害：

```json
{
  "any_of": [
    {
      "attacker_requires": ["game.team_a"],
      "team": "different"
    }
  ],
  "damage_rules": [
    {
      "attacker_requires": ["game.team_a"],
      "policy": "allow"
    }
  ]
}
```

`damage_rules` 的匹配顺序是从上到下；建议先写更具体的标签/队伍条件，再写兜底条件。伤害策略只影响 TearTag 主动拦截的玩家对玩家伤害，不会绕过 Minecraft 或其他模组的其他伤害检查。

这里的标签是原版实体标签，通过 `/tag` 管理：

```mcfunction
tag PlayerA add mygame.hunter
tag PlayerA add mygame.active
```

它们不是计分板 objective。需要按分数控制规则时，应先用数据包函数把分数状态转换成实体标签，再让规则读取这些标签。

`any_of` 中不同对象之间是 OR；同一个对象中的所有 `requires`、`forbids` 和 `team` 条件是 AND。例如可以添加第二个分支允许管理员攻击：

```json
{
  "allow_self": false,
  "any_of": [
    {
      "attacker_requires": ["mygame.hunter"],
      "target_requires": ["mygame.runner"]
    },
    {
      "attacker_requires": ["mygame.admin"]
    }
  ]
}
```

特殊情况：

- 省略 `any_of` 相当于允许任意已启用参与者攻击，但仍默认禁止自己攻击自己。
- 明确写成 `"any_of": []` 会拒绝所有攻击，可用作保护规则。
- `same` 只在双方属于同一个非空原版队伍时匹配。
- `different` 是 `same` 的反面，因此双方都没有队伍、只有一方有队伍时也会匹配。若要求严格阵营关系，请同时使用实体标签。
- 内置规则 `teartag:any_participant` 始终存在，允许任意两个不同的有效参与者互相撕取。
- 玩家记录引用的规则在 `/reload` 后不存在时会拒绝所有攻击，不会自动退回默认规则。

## 5. 注册事件函数

TearTag 通过固定的函数标签调用数据包。以 `on_eliminate` 为例：

```text
data/teartag/tags/function/on_eliminate.json
```

```json
{
  "replace": false,
  "values": [
    "mygame:event/on_eliminate"
  ]
}
```

对应函数位于：

```text
data/mygame/function/event/on_eliminate.mcfunction
```

`replace` 应保持为 `false`，这样多个数据包可以共同监听事件。若回调之间存在严格顺序要求，建议只在标签中注册一个入口函数，再由入口函数按顺序调用其他函数。

### 5.1 可用事件

| 函数标签 | 触发时机 | 攻击者 | `wall_attempt` |
| --- | --- | --- | --- |
| `#teartag:on_enable` | 玩家状态被启用后。 | 无 | `false` |
| `#teartag:on_disable` | 玩家状态被禁用后。 | 无 | `false` |
| `#teartag:on_tear_success` | 撕取数增加后、淘汰判定前。达到上限的一击会先触发此事件，再触发 `on_eliminate`。 | 有 | 按命中类型 |
| `#teartag:on_participant_damage` | 已启用且未淘汰的玩家对玩家伤害即将结算前；无论最终允许还是拒绝都会触发。 | 有 | `false` |
| `#teartag:on_wall_attempt_failed` | 贴墙正面尝试未通过概率判定后。撕取数不增加。 | 有 | `true` |
| `#teartag:on_recover` | 一层撕取进度恢复并同步后。 | 无 | `false` |
| `#teartag:on_eliminate` | 达到所需撕取数、玩家进入淘汰状态后。 | 有 | 按最后一击 |
| `#teartag:before_default_teleport` | 淘汰延迟结束后、默认传送与旁观模式切换前。 | 无 | `false` |
| `#teartag:on_reset` | `reset round` 或 `reset all` 后。 | 无 | `false` |

### 5.1.1 用伤害事件添加自定义效果

`on_participant_damage` 不限定效果内容。比如一个数据包可以让同队互相攻击时给攻击者失明，而跨队攻击是否掉血仍完全由 `damage_rules` 决定：

```text
data/teartag/tags/function/on_participant_damage.json
```

```json
{
  "replace": false,
  "values": ["mygame:event/on_participant_damage"]
}
```

```mcfunction
# 目标与攻击者都带 game.team_a 时，攻击者获得 2 秒失明。
execute if entity @s[tag=game.team_a] as @a[tag=teartag.event_attacker,tag=game.team_a] run effect give @s minecraft:blindness 2 0 true
```

同一个入口函数还可以复制 `teartag:event` 到自有 storage、增加计分、播放声音或触发其他函数。事件会在伤害结算前同步执行；不要在回调结束后直接读取 `teartag:event`，异步逻辑必须先复制数据。

`reset all` 在触发 `on_reset` 前已经删除状态，因此该事件不会包含下面列出的状态相关 storage 字段。`reset round` 仍会包含这些字段。

## 6. 回调执行上下文

每个事件函数均同步执行，并具有以下上下文：

- `@s` 是事件目标玩家。
- 执行维度和坐标是目标玩家当前所在维度与位置。
- 目标玩家临时拥有实体标签 `teartag.event_target`。
- 存在攻击者时，攻击者临时拥有实体标签 `teartag.event_attacker`。
- `teartag:event` command storage 保存当前事件数据。

攻击者可以这样选择：

```mcfunction
execute as @a[tag=teartag.event_attacker,limit=1] run function mygame:event/as_attacker
```

`teartag.event_target` 和 `teartag.event_attacker` 是 TearTag 保留标签，不要在自己的逻辑中长期添加这两个标签。若事件发生前玩家已经错误地拥有保留标签，TearTag 会保留原状态，因此选择器可能不再唯一。

## 7. `teartag:event` storage

在事件函数内可以执行：

```mcfunction
data get storage teartag:event
```

字段如下：

| 字段 | NBT 类型 | 出现条件 | 说明 |
| --- | --- | --- | --- |
| `event` | string | 始终 | 事件名，例如 `on_tear_success`。 |
| `target_uuid` | string | 始终 | 目标 UUID 的带连字符字符串。 |
| `target_name` | string | 始终 | 目标 GameProfile 名称。 |
| `wall_attempt` | byte/boolean | 始终 | 是否为贴墙正面穿过身体的尝试。 |
| `tear_count` | int | 目标状态存在 | 当前撕取数。成功事件中已经包含本次增加。 |
| `required_tears` | int | 目标状态存在 | 当前配置要求的淘汰撕取数。 |
| `rule` | string | 目标状态存在 | 目标当前规则 ID。 |
| `text` | component NBT | 目标状态存在 | 名牌的序列化 Minecraft 文本组件。 |
| `attacker_uuid` | string | 存在攻击者 | 攻击者 UUID。 |
| `attacker_name` | string | 存在攻击者 | 攻击者 GameProfile 名称。 |
| `damage_amount` | double | `on_participant_damage` | Minecraft 本次伤害事件的原始数值。 |
| `damage_allowed` | byte/boolean | `on_participant_damage` | Java 回调处理后的当前伤害许可结果。数据包回调适合施加效果或记录状态；要改变许可结果，应使用 `damage_rules` 或 Java `DamageDecision` 回调。 |

条件示例：

```mcfunction
# 判断这次成功是否来自贴墙概率判定
execute if data storage teartag:event {wall_attempt:1b} run function mygame:event/wall_success

# 将事件复制到自己的 storage，供 schedule 或后续 tick 使用
data modify storage mygame:last_teartag_event current set from storage teartag:event {}
```

重要：回调结束后，TearTag 会恢复进入回调前的 `teartag:event` 内容和临时标签。不要让 `schedule function` 稍后直接读取 `teartag:event`；需要异步处理时，必须在当前回调内复制到自己的 storage 或计分板。上例的副本位于 `mygame:last_teartag_event current`。

## 8. 接管淘汰传送

默认流程是：达到撕取上限 → 进入淘汰状态并播放反馈 → 等待 `elimination.delay_ticks` → 触发 `before_default_teleport` → 传送到配置位置并切换旁观模式。

要接管默认传送，必须在同步执行的 `#teartag:on_eliminate` 回调中，以目标玩家 `@s` 执行：

```mcfunction
teartag event claim
```

该命令在其他事件、控制台、普通玩家命令或延迟调度函数中都会失败。接管只会阻止最后的默认传送和旁观模式切换，不会取消淘汰状态、粒子、标题或延迟计时。

推荐的延迟传送方案：

`on_eliminate.mcfunction`：

```mcfunction
teartag event claim
tag @s remove mygame.active
tag @s add mygame.eliminated
```

`before_default_teleport.mcfunction`：

```mcfunction
execute in minecraft:overworld run tp @s 0 80 0
gamemode spectator @s
```

无论是否已经 claim，`before_default_teleport` 都会在延迟结束时执行。若在 `on_eliminate` 中立即传送，也要确保后续的 `before_default_teleport` 回调不会重复处理。要让玩家进入下一轮，应由数据包适时执行 `/teartag reset round <targets>` 并恢复游戏模式、位置和业务标签。

## 9. 完整示例

仓库示例实现了以下逻辑：

- 拥有 `tear_demo.hunter` 和 `tear_demo.active` 的玩家只能撕拥有 `tear_demo.runner` 和 `tear_demo.active` 的目标。
- 猎人名牌使用拒绝所有攻击的规则。
- 每次成功撕取把事件复制到 `tear_demo:last_event`。
- 目标淘汰时接管默认流程、移除 active 标签，并在延迟结束后传送到主世界出生区域。

安装后执行：

```mcfunction
reload
tag Steve add tear_demo.active
tag Steve add tear_demo.hunter
tag Alex add tear_demo.active
tag Alex add tear_demo.runner
function tear_demo:round/start
```

将 `Steve` 和 `Alex` 替换成实际玩家名。验证命令：

```mcfunction
teartag status Steve
teartag status Alex
data get storage tear_demo:last_event current
```

## 10. 常见问题

### 规则文件已修改但没有生效

执行 `/reload`，并查看服务端日志中的 `Loaded ... teartag attack rules`。`/teartag config reload` 不会重载数据包。

### `/teartag rule set` 提示找不到规则

检查目录是否为 `teartag_attack_rules`、命名空间和嵌套路径是否与命令中的 ID 一致，并确认 JSON 可解析。

### 所有人都无法攻击

依次检查：

1. 攻击者和目标是否都执行过 `/teartag enable`。
2. 目标是否已淘汰。
3. 规则是否分配给目标。
4. `/tag <玩家> list` 中的标签是否满足 required/forbidden 条件。
5. 规则是否在最近一次 `/reload` 后仍存在。

### `team: "different"` 允许了没有队伍的玩家

这是预期语义。`different` 表示“不属于同一个非空队伍”。需要严格阵营时，同时要求 `mygame.hunter`、`mygame.runner` 等实体标签。

### 延迟函数读不到事件 storage

`teartag:event` 只保证在同步回调期间有效。先使用 `data modify storage <自己的命名空间>:... current set from storage teartag:event {}` 保存副本。

### claim 后玩家仍有淘汰特效

claim 只接管默认传送，不撤销淘汰。使用数据包事件控制后续流程，并在新回合开始时执行 `reset round`。
