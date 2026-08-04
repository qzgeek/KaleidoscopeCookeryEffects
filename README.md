# Kaleidoscope Effects

森罗物语（Kaleidoscope Cookery）Buff 移植插件。

依据官方 wiki（https://www.kaleidoscope.wiki）与 MC百科，为 74 种食物/茶实现食用后 Buff 效果。

## 实现方式

- **原版效果**（伤害吸收/急迫/抗火/水下呼吸/生命恢复等）：直接应用，显示在效果栏
- **模组自定义效果**（11 种）：事件驱动实现，BossBar 展示效果名 + 剩余时间进度

## 自定义效果

| 效果 | 作用 |
|------|------|
| 生机 | 持续恢复生命 |
| 活力 | 奔跑不消耗饥饿值 |
| 温暖 | 免疫冰冻/火焰伤害 |
| 饱腹代偿 | 饥饿值+饱和度缓慢恢复 |
| 芥末 | 苦力怕逃离 |
| 硫磺 | 幻翼逃离 |
| 胀气 | 跳跃增强（氮气加速） |
| 寒带疾行 | 雪地/冰面/细雪加速 |
| 即时熔炼 | 挖矿直接得熔炼产物 |
| 弹射闪避 | 免疫弹射物 |
| 保鲜 | 吃坏食物无负面 |

## 依赖

- CraftEngine（物品 ID 识别）
- Folia / Paper 26.1+

## 文件

```
jar/KcEffects-1.0.0.jar       # 编译产物
src/                          # 源码
```

## 环境

- 测试环境：Lophine 26.1.2 + CraftEngine 26.7.4 + KaleidoscopeCookeryPlugin 1.0.5
