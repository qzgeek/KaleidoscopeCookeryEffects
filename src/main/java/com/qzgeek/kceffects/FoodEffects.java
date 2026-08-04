package com.qzgeek.kceffects;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 食物/茶 → 效果映射表
 * 依据森罗物语官方 wiki（https://www.kaleidoscope.wiki）与 MC百科
 * 效果格式: EFFECT:时长tick[:等级]  或  VANILLA:效果id:时长tick[:等级]
 */
public final class FoodEffects {
    private FoodEffects() {}

    public static final Map<String, String> MAP = new LinkedHashMap<>();

    static {
        // ===== 简易食物 =====
        map("donkey_burger", "SATIETY:900");                 // 驴肉火烧 45s
        map("mantou", null);                                  // 馒头
        map("baozi", "VANILLA:minecraft:absorption:1600");   // 包子 80s 伤害吸收
        map("shengjian_mantou", "WARMTH:1600");              // 水煎包 80s 温暖
        map("samsa", "VANILLA:minecraft:haste:2000:1");      // 烤包子 100s 急迫II
        map("meat_pie", "WARMTH:1600");                      // 馅饼 80s 温暖
        map("dumpling", "WARMTH:1600");                      // 饺子 80s 温暖
        map("fried_egg", null);                              // 煎蛋
        map("qingtuan", null);                               // 青团
        map("zongzi", null);                                 // 粽子
        map("sticky_rice_cake", null);                       // 糯米糕
        map("sticky_candy", null);                           // 牛皮糖
        map("bamboo_tube_rice", null);                       // 竹筒饭

        // ===== 小盘炒菜（活力 90s / 保鲜 90s）=====
        map("stir_fried_pork_with_peppers", "VIGOR:1800");   // 青椒炒肉
        map("fish_flavored_shredded_pork", "VIGOR:1800");    // 鱼香肉丝
        map("sweet_and_sour_pork", "VIGOR:1800");            // 糖醋里脊
        map("scramble_egg_with_tomatoes", "VIGOR:1800");     // 番茄炒蛋
        map("braised_beef", "VIGOR:1800");                   // 红烧牛肉
        map("braised_beef_with_potatoes", "WARMTH:9600");    // 土豆炖牛肉 8min 温暖

        // ===== 米饭与盖饭 =====
        map("cooked_rice", null);                            // 米饭
        map("egg_fried_rice", null);                         // 蛋炒饭
        map("braised_beef_rice_bowl", "SATIETY:3600");       // 盖饭 3min 饱腹代偿
        map("stir_fried_pork_with_peppers_rice_bowl", "SATIETY:3600");
        map("fish_flavored_shredded_pork_rice_bowl", "SATIETY:3600");
        map("scramble_egg_with_tomatoes_rice_bowl", "SATIETY:3600");
        map("sweet_and_sour_pork_rice_bowl", "SATIETY:3600");

        // ===== 汤类 =====
        map("pork_bone_soup", "VIGOR:6000");                 // 大骨汤 5min 活力
        map("seafood_miso_soup", "VANILLA:minecraft:water_breathing:9600"); // 海鲜味噌汤 8min 水下呼吸
        map("fearsome_thick_soup", "SULFUR:9600");           // 恐惧浓汤 8min 硫磺
        map("chicken_and_mushroom_stew", "WARMTH:9600");     // 小鸡炖蘑菇 8min 温暖
        map("beef_meatball_soup", "WARMTH:9600");            // 牛丸汤 8min 温暖
        map("borscht", "BLOATING:3600");                     // 罗宋汤 3min 胀气
        map("pufferfish_soup", "MUSTARD:12000");             // 河豚汤 10min 芥末
        map("wild_mushroom_rabbit_soup", "VANILLA:minecraft:speed:9600"); // 野菌兔肉汤 8min 速度
        map("lamb_and_radish_soup", "COLD_STRIDE:6000");     // 萝卜羊肉汤 5min 寒带疾行
        map("laba_congee", null);                            // 腊八粥
        map("hot_dry_noodles", "WARMTH:3600");               // 热干面 3min 温暖
        map("dough_drop_soup", "WARMTH:1600");               // 疙瘩汤 80s 温暖
        map("four_joy_meatball_soup", "WARMTH:1600");        // 四喜丸子汤 80s 温暖

        // ===== 面类（3min 温暖）=====
        map("beef_noodle", "WARMTH:3600");                   // 牛肉面
        map("hui_noodle", "WARMTH:3600");                    // 羊肉烩面
        map("udon_noodle", "WARMTH:3600");                   // 乌冬面

        // ===== 瓦罐汤 =====
        map("brown_mushroom_pot_soup", "WARMTH:6000");       // 棕蘑菇瓦罐汤 5min 温暖
        map("red_mushroom_pot_soup", "WARMTH:6000");         // 红蘑菇瓦罐汤
        map("warped_fungus_pot_soup", "VANILLA:minecraft:fire_resistance:2400"); // 诡异菌 2min 抗火
        map("crimson_fungus_pot_soup", "VANILLA:minecraft:fire_resistance:2400"); // 绯红菌
        map("buddha_jumps_over_the_wall", "SATIETY:3600");   // 佛跳墙 3min 饱腹代偿

        // ===== 大盘菜 =====
        map("cold_roasted_meat", null);                      // 冷肉炙
        map("braised_pork_ribs", "WARMTH:1600");             // 红烧排骨 80s 温暖
        map("oil_splashed_fish", "WARMTH:1600");             // 油泼鱼 80s 温暖
        map("numbing_spicy_chicken", "VANILLA:minecraft:fire_resistance:2400"); // 椒麻鸡 2min 抗火
        map("spicy_chicken", "SATIETY:1600");                // 辣子鸡 80s 饱腹代偿
        map("spicy_blood_stew", "VANILLA:minecraft:fire_resistance:1600"); // 毛血旺 80s 抗火
        map("fried_spring_roll", "WARMTH:1600");             // 炸春卷 80s 温暖
        map("fried_caterpillar", "BLOATING:200");            // 油炸猪儿虫 10s 胀气
        map("spicy_rabbit_head", "WARMTH:1600");             // 麻辣兔头 80s 温暖
        map("stuffed_tiger_skin_pepper", "WARMTH:1600");     // 虎皮青椒酿肉 80s 温暖
        map("candied_potato", "WARMTH:1600");                // 拔丝土豆 80s 温暖
        map("stargazy_pie", "BLOATING:600|VANILLA:minecraft:unluck:3600"); // 仰望星空派 30s 胀气+3min 霉运
        map("crystal_lamb_chop", "VANILLA:minecraft:haste:6000:1"); // 水晶羊排 5min 急迫II
        map("blaze_lamb_chop", "VANILLA:minecraft:fire_resistance:1600"); // 烈焰羊排 80s 抗火
        map("frost_lamb_chop", "COLD_STRIDE:1600");          // 凛冬羊排 80s 寒带疾行
        map("slime_ball_meal", "BLOATING:700");              // 黏液饭 35s 胀气
        map("fondant_pie", "SATIETY:1600");                  // 翻糖派 80s 饱腹代偿
        map("fondant_spider_eye", "BLOATING:700");           // 翻糖蛛眼 35s 胀气
        map("pan_seared_knight_steak", "SATIETY:1800|WARMTH:800"); // 香煎骑士牛排 90s 饱腹代偿+40s 温暖
        map("yakitori", "SATIETY:1600");                     // 烧鸟串 80s 饱腹代偿
        map("chorus_fried_egg", "BLOATING:700");             // 荷包紫颂烧 35s 胀气
        map("dongpo_pork", "SATIETY:1600");                  // 东坡肉 80s 饱腹代偿
        map("golden_salad", "VANILLA:minecraft:resistance:2000|VANILLA:minecraft:regeneration:200"); // 黄金沙拉 100s 抗性提升+10s 生命恢复
        map("braised_fish", "VANILLA:minecraft:water_breathing:3600"); // 红烧鱼 3min 水下呼吸
        map("sweet_and_sour_ender_pearls", "BLOATING:700");  // 珍珠咕噜肉 35s 胀气
        map("cold_cut_ham_slices", null);                    // 冷切火腿片

        // ===== 刺身（5min 芥末）=====
        map("nether_style_sashimi", "MUSTARD:6000");
        map("cold_style_sashimi", "MUSTARD:6000");
        map("desert_style_sashimi", "MUSTARD:6000");
        map("tundra_style_sashimi", "MUSTARD:6000");
        map("end_style_sashimi", "MUSTARD:6000");

        // ===== 黑暗料理 / 谜之炒菜 =====
        map("dark_cuisine", "DARK_CUISINE:0");               // 33% 失明15s+中毒5s
        map("suspicious_stir_fry", "SUSPICIOUS:0");          // 15% 随机效果 60s

        // ===== 茶 =====
        map("barley_tea", "VITALITY:9600");                  // 大麦茶 8min 生机
        map("flower_tea", "VANILLA:minecraft:regeneration:400"); // 花茶 20s 生命恢复
        map("oolong", "VANILLA:minecraft:slow_falling:7200|VANILLA:minecraft:jump_boost:7200"); // 乌龙茶 6min 缓降+跳跃
        map("tieguanyin", "INSTANT_SMELT:2400");             // 铁观音 2min 即时熔炼
        map("biluochun", "PROJECTILE_DODGE:2400");           // 碧螺春 2min 弹射闪避
        map("sakura_fubuki", "HASTEN:7200");                // 樱吹雪 6min 迟滞
    }

    private static void map(String id, String effects) {
        if (effects != null) {
            MAP.put("kaleidoscopecookery:" + id, effects);
        }
    }
}
