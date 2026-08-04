package com.qzgeek.kceffects;

/**
 * 森罗物语自定义效果枚举
 * 对应模组原版的 11 个自定义 Buff（百科/官方 wiki 定义）
 */
public enum KcEffect {
    /** 生机：击杀成年年龄型生物时，在死亡位置生成一只同类型幼体 */
    VITALITY("kaleidoscopecookery:vitality", "生机"),
    /** 活力：奔跑时将疲劳值重置为 0，奔跑不额外消耗饱和度和饥饿值 */
    VIGOR("kaleidoscopecookery:vigor", "活力"),
    /** 保鲜：进食结束后移除该食物自身配置的有害状态效果 */
    PRESERVATION("kaleidoscopecookery:preservation", "保鲜"),
    /** 硫磺：附近幻翼失去攻击目标 */
    SULFUR("kaleidoscopecookery:sulfur", "硫磺"),
    /** 芥末：附近的苦力怕主动回避 */
    MUSTARD("kaleidoscopecookery:mustard", "芥末"),
    /** 寒带疾行：雪/冰/细雪上加速，细雪不陷入 */
    COLD_STRIDE("kaleidoscopecookery:cold_stride", "寒带疾行"),
    /** 胀气：每次按下潜行键获得一次向上的弹射速度 */
    BLOATING("kaleidoscopecookery:bloating", "胀气"),
    /** 饱腹代偿：受伤害时降低伤害，消耗饱和度和饥饿值 */
    SATIETY("kaleidoscopecookery:satiety", "饱腹代偿"),
    /** 温暖：热源附近每秒恢复 1 点生命；下界 25% 概率恢复 0.5 */
    WARMTH("kaleidoscopecookery:warmth", "温暖"),
    /** 即时熔炼：破坏矿石将可烧炼掉落转化为熔炼产物 */
    INSTANT_SMELT("kaleidoscopecookery:instant_smelt", "即时熔炼"),
    /** 弹射闪避：被弹射物命中时传送到附近安全位置，每次扣 10 秒 */
    PROJECTILE_DODGE("kaleidoscopecookery:projectile_dodge", "弹射闪避"),
    /** 迟滞：对其他生物造成伤害时，给受击目标 5 秒 II 级缓慢 */
    HASTEN("kaleidoscopecookery:hasten", "迟滞");

    private final String key;
    private final String displayName;

    KcEffect(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }
}
