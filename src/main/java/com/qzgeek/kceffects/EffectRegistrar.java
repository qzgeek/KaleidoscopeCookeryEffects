package com.qzgeek.kceffects;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 尝试注册自定义 MobEffect 到服务端注册表
 * 若成功，效果可在效果栏显示（配合资源包 lang）
 */
public class EffectRegistrar {

    /** 注册自定义效果，返回 true 表示成功 */
    public static boolean registerCustomEffects(JavaPlugin plugin) {
        try {
            boolean anySuccess = false;
            for (KcEffect kc : KcEffect.values()) {
                String[] parts = kc.key().split(":");
                Identifier id = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0x88AA66) {};
                try {
                    Registry.register(BuiltInRegistries.MOB_EFFECT, id, effect);
                    anySuccess = true;
                    plugin.getLogger().info("已注册自定义效果: " + kc.key());
                } catch (Exception e) {
                    // 注册表已冻结时静默跳过（事件驱动模式）
                }
            }
            return anySuccess;
        } catch (Throwable t) {
            plugin.getLogger().warning("自定义效果注册失败: " + t.getMessage());
            return false;
        }
    }
}
