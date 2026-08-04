package com.qzgeek.kceffects;

/**
 * 森罗物语自定义效果枚举
 * 对应模组原版的 11 个自定义 Buff（百科/官方 wiki 定义）
 */
public enum KcEffect {
    /** 生机：持续恢复生命 */
    VITALITY("kaleidoscopecookery:vitality", "生机"),
    /** 活力：奔跑时不消耗饥饿值 */
    VIGOR("kaleidoscopecookery:vigor", "活力"),
    /** 保鲜：食用腐肉/生鸡肉/毒马铃薯/河豚/蜘蛛眼不获得负面效果 */
    PRESERVATION("kaleidoscopecookery:preservation", "保鲜"),
    /** 硫磺：幻翼主动逃离玩家 */
    SULFUR("kaleidoscopecookery:sulfur", "硫磺"),
    /** 芥末：苦力怕主动逃离玩家 */
    MUSTARD("kaleidoscopecookery:mustard", "芥末"),
    /** 寒带疾行：雪地、冰面、细雪上快速通行 */
    COLD_STRIDE("kaleidoscopecookery:cold_stride", "寒带疾行"),
    /** 胀气：跳跃提升（氮气加速） */
    BLOATING("kaleidoscopecookery:bloating", "胀气"),
    /** 饱腹代偿：饥饿值缓慢恢复 */
    SATIETY("kaleidoscopecookery:satiety", "饱腹代偿"),
    /** 温暖：免疫冰冻伤害 */
    WARMTH("kaleidoscopecookery:warmth", "温暖"),
    /** 即时熔炼：挖掘矿物直接掉落熔炼产物 */
    INSTANT_SMELT("kaleidoscopecookery:instant_smelt", "即时熔炼"),
    /** 弹射闪避：免疫弹射物伤害 */
    PROJECTILE_DODGE("kaleidoscopecookery:projectile_dodge", "弹射闪避");

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
