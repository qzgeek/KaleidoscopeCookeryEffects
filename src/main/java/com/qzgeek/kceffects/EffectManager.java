package com.qzgeek.kceffects;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 效果状态管理：跟踪玩家当前生效的自定义效果及剩余时间。
 */
public class EffectManager {
    public static class ActiveEffect {
        public final long expiry;      // 到期时间戳（ms）
        public final long totalTicks;  // 总时长（tick）
        public final int level;        // 等级（0 = I 级）

        public ActiveEffect(long expiry, long totalTicks, int level) {
            this.expiry = expiry;
            this.totalTicks = totalTicks;
            this.level = level;
        }
    }

    /** player UUID -> (effect -> 效果状态) */
    private final Map<UUID, Map<KcEffect, ActiveEffect>> activeEffects = new ConcurrentHashMap<>();

    /** 应用效果：同类覆盖（重置时长），不与原版药水叠加 */
    public void apply(Player player, KcEffect effect, int durationTicks, int level) {
        Map<KcEffect, ActiveEffect> map = activeEffects.computeIfAbsent(
                player.getUniqueId(), k -> new ConcurrentHashMap<>());
        long newExpiry = System.currentTimeMillis() + durationTicks * 50L;
        map.put(effect, new ActiveEffect(newExpiry, durationTicks, level));
    }

    public boolean has(Player player, KcEffect effect) {
        Map<KcEffect, ActiveEffect> map = activeEffects.get(player.getUniqueId());
        if (map == null) return false;
        ActiveEffect ae = map.get(effect);
        if (ae == null) return false;
        if (ae.expiry <= System.currentTimeMillis()) {
            map.remove(effect);
            if (map.isEmpty()) activeEffects.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /** 获取所有未过期的效果 */
    public Map<KcEffect, ActiveEffect> getActive(Player player) {
        Map<KcEffect, ActiveEffect> map = activeEffects.get(player.getUniqueId());
        if (map == null) return Map.of();
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(e -> e.getValue().expiry <= now);
        if (map.isEmpty()) activeEffects.remove(player.getUniqueId());
        return map;
    }

    public void remove(Player player) {
        activeEffects.remove(player.getUniqueId());
    }

    /** 全局清理过期效果 */
    public void cleanup() {
        long now = System.currentTimeMillis();
        activeEffects.entrySet().removeIf(e -> {
            e.getValue().entrySet().removeIf(ee -> ee.getValue().expiry <= now);
            return e.getValue().isEmpty();
        });
    }

    /** 扣除效果剩余 tick，返回 false 表示效果已消失 */
    public boolean consumeTicks(Player player, KcEffect effect, long consumeTicks) {
        return consumeMs(player, effect, consumeTicks * 50L);
    }

    /** 扣除效果剩余毫秒，返回 false 表示效果已消失 */
    public boolean consumeMs(Player player, KcEffect effect, long consumeMs) {
        Map<KcEffect, ActiveEffect> map = activeEffects.get(player.getUniqueId());
        if (map == null) return false;
        ActiveEffect ae = map.get(effect);
        if (ae == null) return false;
        long newExpiry = ae.expiry - consumeMs;
        if (newExpiry <= System.currentTimeMillis()) {
            map.remove(effect);
            if (map.isEmpty()) activeEffects.remove(player.getUniqueId());
            return false;
        }
        map.put(effect, new ActiveEffect(newExpiry, ae.totalTicks, ae.level));
        return true;
    }

    // ---- 持久化 ----

    /** 获取可持久化的效果快照（effect名 -> 剩余tick） */
    public Map<String, Long> snapshot(Player player) {
        Map<String, Long> snap = new java.util.LinkedHashMap<>();
        Map<KcEffect, ActiveEffect> map = activeEffects.get(player.getUniqueId());
        if (map == null) return snap;
        long now = System.currentTimeMillis();
        for (Map.Entry<KcEffect, ActiveEffect> entry : map.entrySet()) {
            long remainMs = entry.getValue().expiry - now;
            if (remainMs > 0) snap.put(entry.getKey().name(), remainMs / 50L);
        }
        return snap;
    }

    /** 从持久化数据恢复效果 */
    public void restore(Player player, Map<String, Long> data) {
        if (data == null || data.isEmpty()) return;
        Map<KcEffect, ActiveEffect> map = activeEffects.computeIfAbsent(
                player.getUniqueId(), k -> new ConcurrentHashMap<>());
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            try {
                KcEffect kc = KcEffect.valueOf(entry.getKey());
                long remainTicks = entry.getValue();
                if (remainTicks <= 0) continue;
                map.put(kc, new ActiveEffect(
                        System.currentTimeMillis() + remainTicks * 50L, remainTicks, 0));
            } catch (Exception ignored) {
            }
        }
    }
}
