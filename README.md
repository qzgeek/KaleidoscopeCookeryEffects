# Kaleidoscope Cookery Effects (KcEffects)

森罗物语：厨房（Kaleidoscope Cookery）Buff 移植插件。

基于模组源码 ([KaleidoscopeMods/KaleidoscopeCookery](https://github.com/KaleidoscopeMods/KaleidoscopeCookery)) 完整还原 12 种自定义状态效果，使用事件驱动 + BossBar 展示替代原模组的 MobEffect 系统。

## 支持环境

| 项目 | 版本 |
|------|------|
| 服务端类型 | **Folia**（使用 EntityScheduler / GlobalRegionScheduler，Paper 未经测试） |
| Minecraft | **1.21 ~ 最新版** |
| CraftEngine | **26.7.4**（Community 版） |
| KaleidoscopeCookeryPlugin | **1.1.2+** |
| Java | **21+** |

## 12 种自定义效果

| 效果 | 作用 | 来源（示例） |
|------|------|-------------|
| 生机 | 击杀成年生物生成幼体（僵尸5%→小村民） | 大麦茶 8min |
| 活力 | 奔跑不消耗饥饿值 | 青椒炒肉/鱼香肉丝/大骨汤 |
| 保鲜 | 食用后移除食物中的所有有害药水效果 | 部分食物自带 |
| 硫磺 | 附近幻翼失去攻击目标 | 恐惧浓汤 8min |
| 芥末 | 附近苦力怕主动回避 | 河豚汤 10min / 刺身类 |
| 寒带疾行 | 雪/冰/细雪上加速（frosted_ice 等 7 种冷方块） | 萝卜羊肉汤 5min |
| 胀气 | 潜行键弹射上升约 2.5 格（一次潜行一次加速） | 罗宋汤/黏液饭/仰望星空派 |
| 饱腹代偿 | 受伤减免（100% 上限 64）+ 消耗食物条 | 佛跳墙/东坡肉/盖饭 |
| 温暖 | 热源 5×5×3 范围内每秒回 1 血（下界 25% 回 0.5） | 小鸡炖蘑菇/牛肉面/热干面 |
| 即时熔炼 | 挖矿直接得熔炼产物（支持等级） | 铁观音 2min |
| 弹射闪避 | 被弹射物命中时传送+扣时长 | 碧螺春 2min |
| 迟滞 | 攻击目标附加 5s 缓慢 II | 樱吹雪 6min |

## 效果展示

自定义效果通过 **BossBar** 展示：效果名 + 剩余时间 + 进度条。正面效果绿色、负面效果（胀气）红色。

## 覆盖范围

- **手持食用**：74 种食物/茶（`PlayerItemConsumeEvent`）
- **放置食用**：所有可放置菜品家具（`FurnitureInteractEvent`）
- 效果持久化：下线保存、上线恢复
- 死亡清除（与原版药水一致）
- Folia 兼容：所有实体操作走 `EntityScheduler`

## 安装

1. 将 `KcEffects-2.0.0.jar` 放入 `plugins/` 目录
2. 确保已安装 `CraftEngine`（26.7.4）和 `KaleidoscopeCookeryPlugin`（1.1.2+）
3. 重启服务器

## 构建

```bash
# 修改 build.sh 中的 jar 路径指向你的服务器
bash build.sh
```

## 与模组原版的对照

本插件参照模组源码 `KaleidoscopeMods/KaleidoscopeCookery` 实现：

- `FlatulenceEffect` + `FlatulenceEvent` → 胀气
- `WarmthEffect` → 温暖
- `VigorEffect` → 活力
- `SulfurEffect` → 硫磺
- `InstantSmeltingEffect` → 即时熔炼
- `HinderEvent` → 迟滞
- `PreservationEvent` → 保鲜
- `ProjectileDodgeEvent` → 弹射闪避
- `SatiatedShieldEvent` → 饱腹代偿
- `VitalityEvent` → 生机
- `TundraStrider`（属性实现）→ 寒带疾行
- `Mustard`（属性实现）→ 芥末

## License

MIT
