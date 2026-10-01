# 玄剑·战争 BattledXuanjian

类《战地》/《三角洲行动》大战场玩法小游戏插件：**随机分队、据点占领、兵力消耗、攻防胜负判定、阵营显示与隐藏 ID**。

- 目标服务端：**Paper 26.2+**（Java 25）
- 指令前缀：`/bx`
- 权限节点：`battledxuanjian.admin`（管理员）、`battledxuanjian.player`（玩家，默认可用）
- 可选依赖：**ProtocolLib**（隐藏 Tab 列表中的敌方 ID）、**PlaceholderAPI**（变量扩展）

> 需求依据：`玄剑·战争_BattledXuanjian_插件开发需求说明书.md`

---

## 1. 快速开始

```
/bx pos1                 # 站到据点一角，设置选区点 1
/bx pos2                 # 站到对角的另一角，设置选区点 2
/bx point create A       # 用当前选区创建据点 A
/bx spawn C              # 站在 C 军（守方）出发点执行
/bx spawn M              # 站在 M 军（攻方）出发点执行
/bx kithand M            # 手持装满装备的潜影盒，设置 M 军装备包
/bx kithand C            # 同上，设置 C 军装备包
/bx lobby set            # 设置等待区大厅坐标（可选）

/bx join                 # 玩家加入等待队列
/bx team join M          # 玩家预选阵营（最终按人数平衡调整）
/bx start                # 管理员开局，进入倒计时
```

游戏进行中：`/bx status` 查看状态，`/bx stop` 强制结束并完整重置。

## 2. 指令一览

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `/bx join` / `/bx leave` | 加入 / 离开战局 | `battledxuanjian.player` |
| `/bx team join <C/M>` | 选择 C 军（守）或 M 军（攻） | `battledxuanjian.player` |
| `/bx status` | 查看阶段、人数、兵力、剩余时间、据点 | `battledxuanjian.player` |
| `/bx stats [reset]` | 查看 / 清空累计统计 | 玩家 / 管理员 |
| `/bx start` | 开始一局（人数不足会拒绝） | `battledxuanjian.admin` |
| `/bx stop` | 强制结束并重置 | `battledxuanjian.admin` |
| `/bx reload` | 重载 config.yml / points.yml / items.yml | `battledxuanjian.admin` |
| `/bx pos1` `/bx pos2` | 设置据点长方体选区对角点 | `battledxuanjian.admin` |
| `/bx point create/remove/list [名称]` | 据点管理 | `battledxuanjian.admin` |
| `/bx spawn <C/M> [攻/守]` | 设置阵营出发点 | `battledxuanjian.admin` |
| `/bx kithand <C/M>` | 用手持潜影盒内容设置阵营装备包 | `battledxuanjian.admin` |
| `/bx set reinforcements <数量>` | 设置攻方公共兵力 | `battledxuanjian.admin` |
| `/bx set time <分钟>` | 设置时间限制（0 = 无限制） | `battledxuanjian.admin` |
| `/bx lobby set` | 设置等待区大厅坐标 | `battledxuanjian.admin` |
| `/bx selftest` | 运行期自检（排障 / CI 使用） | `battledxuanjian.admin` |

## 3. 玩法规则（与需求说明书一致）

### 3.1 流程
等待（`/bx join`，可预选阵营）→ `/bx start` 检查人数并倒计时（全服 Title 每秒刷新）→ 阵营分配（尊重预选、`|C-M| ≤ 1`、未预选随机均匀）→ 传送、清背包、发放装备包 → 战斗 → 胜负判定 → 全服宣告 + 战报 + 完整重置（可连续开局）。

### 3.2 据点占领状态机
| 状态 | 进度 | BossBar 颜色 |
| --- | --- | --- |
| C 军控制 | 100% | 蓝 |
| M 军控制 | 100% | 红 |
| 无主 | 0% | 白 |
| 争夺中 | 0~100% | 随争夺方 |

- **脱离阶段**：据点内只有敌方玩家时，进度从 100% 下降，归零 → 全服宣告“据点 A 已脱离 X 军掌控！”→ 据点变为无主。
- **占领阶段**：无主据点内只有某方玩家时，进度从 0% 上升，满 100% → 全服宣告“X 军占领了据点 A！”。
- **速度公式**：`速率 = 100 / base-time × (1 + (在场人数 - 1) × speed-per-player)`；默认单人 15 秒，2 人 10 秒。
- **冻结**：双方同时在场或无人时进度不变；控制方回归可把中间进度重新推到 100%。
- 每个据点一条 BossBar，标题形如 `据点 A | C军控制 100%`。

### 3.3 兵力与重生
- 攻方 M 军：公共兵力（默认 50），**重生时扣 1 点**；归零立即判定守方胜利，攻方玩家进入旁观模式。
- 守方 C 军：默认无限兵力。
- 死亡不掉落装备；`respawn.delay` 秒后自动重生，重生点优先阵营出发点，若离死亡点不足 `respawn.min-distance` 格则在死亡点周围随机寻找安全落点（避开岩浆、仙人掌、营火等）。

### 3.4 显示与隐藏
- 玩家 ID 前显示阵营头衔（`§9[C] 玩家名` / `§c[M] 玩家名`）。
- 头顶名字：原版 Scoreboard Team + `NAME_TAG_VISIBILITY = FOR_OWN_TEAM`，敌方看不到。
- Tab 列表：**需要 ProtocolLib**，插件拦截 `PLAYER_INFO` 包把敌方条目显示名替换为遮挡文本（默认 `§k`），同阵营可见；未安装/不兼容时自动降级并在控制台告警（需求 §6 明确该能力依赖 ProtocolLib）。

  > ⚠️ 上游现状：ProtocolLib 最新正式版 **5.4.0（2025-08）仅支持到 Minecraft 1.21.8，尚不支持 26.x**。因此本插件在 26.2 服务端上会走“降级 + 控制台告警”分支（CI 已断言该分支行为正常）；一旦 ProtocolLib 适配 26.x，只需把 jar 放入 `plugins/` 即可自动启用，无需改动代码。
- 聊天栏：`teams.chat-mode: GLOBAL`（全服可见、前缀区分）或 `TEAM_ONLY`（仅同阵营可见）。
- 友军伤害关闭（Scoreboard Team + `EntityDamageByEntityEvent` 双重保障）。
- 原版 `/team`、`/scoreboard` 指令修改队伍后，插件自动同步回自己的设置。

### 3.5 胜利条件
- **M 军（攻方）胜利**：占领全部据点。
- **C 军（守方）胜利**：攻方兵力归零，或时间限制到达。

## 4. 配置文件

`config.yml` 覆盖需求说明书 §4 的全部条目，并额外提供：

| 键 | 说明 |
| --- | --- |
| `game.auto-next-round` | 多局循环：结算后自动进入下一局倒计时 |
| `capture.update-interval-ticks` | 占领逻辑刷新间隔（默认 10 刻） |
| `teams.chat-mode` / `teams.tab-mask` / `teams.friendly-fire` / `teams.sync-vanilla-team-command` | 聊天可见性、Tab 遮挡文本、友军伤害、原版指令同步 |
| `kits.prevent-enemy-pickup` / `kits.prevent-drop` | 禁止拾取敌方装备 / 禁止丢弃本阵营装备 |
| `respawn.safe-search-attempts` | 安全落点搜索尝试次数 |
| `sidebar.*` / `broadcast.*` / `messages.*` | 侧边栏模板、宣告开关与音效、全部文本消息 |

数据文件（插件目录下）：

| 文件 | 内容 |
| --- | --- |
| `points.yml` | 据点选区与归属、阵营出发点 |
| `items.yml` | 阵营装备包（物品栏 / 护甲 / 副手） |
| `stats.yml` | 累计统计（可由 `stats.persist: false` 关闭） |

## 5. PlaceholderAPI 变量

`%bx_reinforcements%`、`%bx_reinforcements_raw%`、`%bx_time%`、`%bx_time_seconds%`、`%bx_phase%`、`%bx_phase_id%`、`%bx_players%`、`%bx_team%`、`%bx_team_id%`、`%bx_points_total%`、`%bx_points_c%`、`%bx_points_m%`、`%bx_winner%`、`%bx_point_<名称>%`、`%bx_point_<名称>_progress%`。

## 6. 构建与持续集成

**所有构建都在 GitHub Actions 中完成**（本地不执行构建）：

| Job | 内容 |
| --- | --- |
| `build` | JDK 25 + Gradle 9.8 → 编译、JUnit 5 单元测试、打包 jar、校验 jar 内容、上传产物 |
| `smoke` | 下载最新 Paper 26.2 服务端与 PlaceholderAPI 2.12.3 → 预置据点/出发点 → 实机加载插件 → 控制台执行 `/bx status`、`/bx start`、`/bx set reinforcements 30`、`/bx status`、`/bx selftest`、`/bx point list`、`/bx help` → 断言自检 `RESULT=PASS`、指令链路输出、`%bx_*%` 扩展注册、无插件异常堆栈 → 上传服务端日志 |

流水线：`.github/workflows/build.yml`；冒烟脚本：`ci/smoke-test.sh`（可用 `WORKDIR`、`MC_VERSION`、`JAR_DIR` 环境变量复用）。

`/bx selftest` 会在真实服务端中检查：配置解析、消息渲染、指令注册、世界与选区、Scoreboard/BossBar/侧边栏 API、占领状态机（15 秒脱离 / 再 15 秒占领 / 人数加速 / 双方冻结）、阵营分配平衡、兵力池、装备 PDC 标记、据点持久化、PlaceholderAPI 变量、可选依赖挂钩。

本地如需自行调试（需要 JDK 25）：

```bash
./gradlew build          # 编译 + 单元测试 + 打包
./gradlew test           # 仅单元测试
```

## 7. 验收对照（需求 §14）

| # | 验收项 | 实现位置 / 验证方式 |
| --- | --- | --- |
| 1 | `/bx start` 后倒计时、分队、传送、发装备 | `GameManager` + `KitManager` + `/bx selftest` |
| 2 | BossBar 正确显示，敌对进入 15 秒脱离、再 15 秒占领，人数影响速度 | `CapturePoint`（单元测试 `CapturePointTest`） |
| 3 | 攻方重生扣 1 兵力，归零守方胜利并旁观 | `RespawnManager` + `ReinforcementPool`（单元测试） |
| 4 | 攻方占领全部据点后获胜 | `PointManager.tick()` + `GameManager.endMatch` |
| 5 | 互相看不见头顶 ID 与 Tab 名字 | Scoreboard Team（头顶）+ ProtocolLib 钩子（Tab） |
| 6 | 友军伤害关闭 | `TeamManager` + `GameListener` |
| 7 | 侧边栏实时显示兵力、据点、时间 | `SidebarService` + `GameManager.sidebarLines()` |
| 8 | 关键参数可配置、可动态调整 | `config.yml` + `/bx set` + `/bx reload` |
| 9 | 结束后完整重置、可连续开局 | `GameManager.reset()` + `game.auto-next-round` |
