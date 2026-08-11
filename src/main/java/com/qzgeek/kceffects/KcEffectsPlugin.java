package com.qzgeek.kceffects;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 森罗物语 Buff 移植插件 — 重写版 2.0
 *
 * 基于模组源码 (KaleidoscopeMods/KaleidoscopeCookery) 完整还原 12 种自定义效果。
 * Folia 兼容：所有实体操作通过 EntityScheduler 调度。
 */
public class KcEffectsPlugin extends JavaPlugin implements Listener {

    private EffectManager effectManager;
    private int tickCounter;
    private Map<Material, Material> furnaceMap; // 熔炉配方缓存

    // ---- 胀气会话标记：一次潜行会话仅触发一次，避免服务器取消潜行→客户端重按循环 ----
    private final Map<UUID, Boolean> bloatingTriggered = new ConcurrentHashMap<>();

    // ---- BossBar 展示 ----
    private final Map<UUID, Map<KcEffect, org.bukkit.boss.BossBar>> bossBars = new ConcurrentHashMap<>();

    // ==================== 生命周期 ====================

    @Override
    public void onEnable() {
        effectManager = new EffectManager();
        buildFurnaceMap();
        EffectRegistrar.registerCustomEffects(this);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> tick(), 1, 1);
        getLogger().info("森罗物语 Buff 插件 v2.0 已启用（" + FoodEffects.MAP.size()
                + " 种食物/茶效果，模组原版逻辑还原）");
    }

    @Override
    public void onDisable() {
        if (effectManager != null) effectManager.cleanup();
        for (Map<KcEffect, org.bukkit.boss.BossBar> bars : bossBars.values()) {
            for (org.bukkit.boss.BossBar bar : bars.values()) bar.removeAll();
        }
        bossBars.clear();
        getLogger().info("森罗物语 Buff 插件已卸载");
    }

    /** 构建熔炉配方缓存（用于即时熔炼效果） */
    private void buildFurnaceMap() {
        furnaceMap = new HashMap<>();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof FurnaceRecipe fr) {
                Material input = fr.getInput().getType();
                Material output = fr.getResult().getType();
                furnaceMap.putIfAbsent(input, output); // 优先保留第一个配方
            }
        }
        getLogger().info("熔炉配方缓存: " + furnaceMap.size() + " 条");
    }

    // ==================== 每 tick 调度 ====================

    private void tick() {
        tickCounter++;
        effectManager.cleanup();

        for (Player player : Bukkit.getOnlinePlayers()) {
            updateBossBars(player);

            // === 活力：奔跑时每 tick 重置疲劳值（模组: isDurationEffectTick=true） ===
            if (effectManager.has(player, KcEffect.VIGOR) && player.isSprinting()) {
                scheduleEntity(player, p -> p.setExhaustion(0f));
            }

            // === 硫磺：每 5 tick 主动扫描附近幻翼并清除目标（模组: duration%5==0） ===
            if (tickCounter % 5 == 0 && effectManager.has(player, KcEffect.SULFUR)) {
                scheduleEntity(player, p -> {
                    Location pl = p.getLocation();
                    BoundingBox box = new BoundingBox(
                            pl.getX() - 8, pl.getY() - 16, pl.getZ() - 8,
                            pl.getX() + 8, pl.getY() + 16, pl.getZ() + 8);
                    for (Entity e : p.getWorld().getNearbyEntities(box, e -> e instanceof Phantom)) {
                        Phantom ph = (Phantom) e;
                        if (p.equals(ph.getTarget())) ph.setTarget(null);
                    }
                });
            }

            // === 温暖：每秒检查热源回血（模组: duration%20==0） ===
            if (tickCounter % 20 == 0 && effectManager.has(player, KcEffect.WARMTH)) {
                scheduleEntity(player, p -> {
                    if (p.getHealth() >= p.getMaxHealth()) return;
                    if (hasHeatSourceNearby(p)) {
                        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1.0));
                    } else if (p.getWorld().getEnvironment() == World.Environment.NETHER) {
                        if (Math.random() < 0.25) {
                            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 0.5));
                        }
                    }
                });
            }

            // === 寒带疾行：每 2 秒更新移速，效果结束时恢复 0.2（模组: BaseEffect无tick，但通过属性实现） ===
            if (tickCounter % 40 == 0) {
                boolean hasColdStride = effectManager.has(player, KcEffect.COLD_STRIDE);
                scheduleEntity(player, p -> {
                    if (hasColdStride) {
                        Block below = p.getLocation().add(0, -0.5, 0).getBlock();
                        p.setWalkSpeed(isColdBlock(below.getType()) ? 0.28f : 0.2f);
                    } else {
                        p.setWalkSpeed(0.2f);
                    }
                });
            }
        }
    }

    /** 检查周围 5×5×3 是否有热源（含点燃状态检测，匹配 mod WarmthEffect） */
    private boolean hasHeatSourceNearby(Player player) {
        Location loc = player.getLocation();
        World w = player.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block b = w.getBlockAt(bx + dx, by + dy, bz + dz);
                    Material m = b.getType();
                    // 直接热源
                    if (m == Material.FIRE || m == Material.SOUL_FIRE
                            || m == Material.LAVA || m == Material.MAGMA_BLOCK) {
                        return true;
                    }
                    // 营火（需点燃）
                    if ((m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE)
                            && b.getBlockData() instanceof org.bukkit.block.data.Lightable la
                            && la.isLit()) {
                        return true;
                    }
                    // 熔炉/烟熏炉/高炉（需点燃；匹配 BlockStateProperties.LIT）
                    if ((m == Material.FURNACE || m == Material.BLAST_FURNACE
                            || m == Material.SMOKER)
                            && b.getBlockData() instanceof org.bukkit.block.data.Lightable la
                            && la.isLit()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 匹配模组 tundra_strider_speed_blocks 标签 */
    private boolean isColdBlock(Material mat) {
        return mat == Material.SNOW_BLOCK || mat == Material.SNOW
                || mat == Material.ICE || mat == Material.PACKED_ICE
                || mat == Material.BLUE_ICE || mat == Material.FROSTED_ICE
                || mat == Material.POWDER_SNOW;
    }

    // ==================== 食用事件 ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = getItemId(item);
        if (itemId == null) return;

        String spec = FoodEffects.MAP.get(itemId);
        if (spec == null) return;

        // 黑暗料理：33% 概率失明+中毒
        if (spec.equals("DARK_CUISINE")) {
            if (Math.random() < 0.33) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            }
            return;
        }
        // 谜之炒菜：15% 概率随机效果
        if (spec.equals("SUSPICIOUS")) {
            if (Math.random() < 0.15) {
                PotionEffectType[] types = {
                        PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST,
                        PotionEffectType.HASTE, PotionEffectType.LUCK,
                        PotionEffectType.SLOWNESS, PotionEffectType.NAUSEA
                };
                PotionEffectType type = types[(int) (Math.random() * types.length)];
                int dur = type == PotionEffectType.NAUSEA ? 400 : 1200;
                player.addPotionEffect(new PotionEffect(type, dur, 0));
            }
            return;
        }

        // 常规效果：A|B|C 管道分隔
        for (String effectSpec : spec.split("\\|")) {
            applyEffect(player, effectSpec);
        }
    }

    // ===== 放置菜肴食用：通过 CraftEngine FurnitureInteractEvent 捕获 =====
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnitureEat(FurnitureInteractEvent event) {
        if (event.isCancelled()) return;
        Player player = event.player();
        // 通过家具持久化数据获取菜品物品 ID
        Optional<Item> sourceItem = event.furniture().persistentData.item();
        if (sourceItem.isEmpty()) return;

        Item item = sourceItem.get();
        Optional<Key> customId = item.customId();
        if (customId.isEmpty()) return;

        String itemId = customId.get().toString();
        String spec = FoodEffects.MAP.get(itemId);
        if (spec == null) return;

        // 检查是否已有效果（同类覆盖重置时长），防止连续吃多口时反复弹 ActionBar
        boolean alreadyHas = effectManager.has(player, getEffectFromSpec(spec));

        for (String effectSpec : spec.split("\\|")) {
            applyEffectFurniture(player, effectSpec);
        }

        if (!alreadyHas) {
            player.sendActionBar("§a✦ 享用美食获得效果");
        }
    }

    /** 从效果规格中提取第一个自定义效果（用于检测是否已持有） */
    private KcEffect getEffectFromSpec(String spec) {
        for (String s : spec.split("\\|")) {
            String[] parts = s.split(":");
            if (!parts[0].equals("VANILLA")) {
                try { return KcEffect.valueOf(parts[0]); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** 家具食用专用：静默应用效果（ActionBar 由调用方控制） */
    private void applyEffectFurniture(Player player, String spec) {
        try {
            String[] parts = spec.split(":");
            if (parts[0].equals("VANILLA")) {
                PotionEffectType type = PotionEffectType.getByName(parts[2].toUpperCase());
                if (type == null) return;
                int ticks = Integer.parseInt(parts[3]);
                int level = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
                player.addPotionEffect(new PotionEffect(type, ticks, level));
            } else {
                KcEffect effect = KcEffect.valueOf(parts[0]);
                int ticks = Integer.parseInt(parts[1]);
                int level = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                effectManager.apply(player, effect, ticks, level);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyEffect(Player player, String spec) {
        try {
            String[] parts = spec.split(":");
            if (parts[0].equals("VANILLA")) {
                // VANILLA:minecraft:effect:tick[:level]
                PotionEffectType type = PotionEffectType.getByName(parts[2].toUpperCase());
                if (type == null) return;
                int ticks = Integer.parseInt(parts[3]);
                int level = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
                player.addPotionEffect(new PotionEffect(type, ticks, level));
            } else {
                // KcEffect:TICK[:level]
                KcEffect effect = KcEffect.valueOf(parts[0]);
                int ticks = Integer.parseInt(parts[1]);
                int level = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                effectManager.apply(player, effect, ticks, level);
                String dur = formatDuration(ticks);
                player.sendActionBar("§a✦ 获得效果：§f" + effect.displayName() + " §7(" + dur + ")");
            }
        } catch (Exception ignored) {
        }
    }

    private String formatDuration(int ticks) {
        int sec = ticks / 20;
        if (sec >= 60) return (sec / 60) + "分" + (sec % 60 > 0 ? (sec % 60) + "秒" : "");
        return sec + "秒";
    }

    // ==================== 保鲜：有害效果移除（匹配 mod PreservationEvent） ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreservation(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!effectManager.has(player, KcEffect.PRESERVATION)) return;

        ItemStack stack = event.getItem();
        if (!stack.getType().isEdible()) return;

        // 模拟模组：遍历食物的 effects，移除 HARMFUL 类的效果
        // Bukkit 无法直接读取 food effects，使用启发式方法
        for (PotionEffect pe : player.getActivePotionEffects()) {
            PotionEffectType t = pe.getType();
            if (t == PotionEffectType.POISON || t == PotionEffectType.HUNGER
                    || t == PotionEffectType.NAUSEA || t == PotionEffectType.WEAKNESS
                    || t == PotionEffectType.BLINDNESS
                    || t == PotionEffectType.WITHER || t == PotionEffectType.BAD_OMEN
                    || t == PotionEffectType.UNLUCK || t == PotionEffectType.DARKNESS
                    || t == PotionEffectType.MINING_FATIGUE || t == PotionEffectType.SLOWNESS) {
                player.removePotionEffect(t);
            }
        }
    }

    // ==================== 饱腹代偿（匹配 mod SatiatedShieldEvent） ====================

    // 默认配置值（与模组 GeneralConfig 默认值一致）
    private static final double DAMAGE_REDUCTION_PERCENT = 1.0;
    private static final double MAX_DAMAGE_REDUCTION = 64.0;
    private static final double MIN_DAMAGE = 0.0;
    private static final double EXHAUSTION_PER_DAMAGE = 2.0;
    private static final double WEAKNESS_MULTIPLIER = 2.0;
    private static final int MIN_FOOD_LEVEL = 4;

    @EventHandler(priority = EventPriority.HIGH)
    public void onSatiatedShield(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!effectManager.has(player, KcEffect.SATIETY)) return;
        if (event.isCancelled()) return;

        // 饱腹代偿失效条件：饥饿效果 + food 等级不足
        if (player.hasPotionEffect(PotionEffectType.HUNGER)) return;
        if (player.getFoodLevel() < MIN_FOOD_LEVEL) return;

        float original = (float) event.getDamage();
        // 1. 计算减免量（原伤害 × 减免百分比，上限 MAX_DAMAGE_REDUCTION）
        float reduced = Math.min((float) (original * DAMAGE_REDUCTION_PERCENT), (float) MAX_DAMAGE_REDUCTION);
        float finalDamage = original - reduced;
        // 2. 不低于 MIN_DAMAGE
        if (original > MIN_DAMAGE) {
            finalDamage = Math.max(finalDamage, (float) MIN_DAMAGE);
            reduced = original - finalDamage;
        }

        // 3. 计算疲劳消耗
        float exhaustion = reduced * (float) EXHAUSTION_PER_DAMAGE;

        // 4. 弱点伤害类型双倍消耗
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean weak = cause == EntityDamageEvent.DamageCause.WITHER
                || cause == EntityDamageEvent.DamageCause.SONIC_BOOM
                || cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.SUFFOCATION
                || cause == EntityDamageEvent.DamageCause.FREEZE
                || cause == EntityDamageEvent.DamageCause.LIGHTNING
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        if (weak) exhaustion *= WEAKNESS_MULTIPLIER;

        // 5. 应用最终伤害 + 疲劳
        event.setDamage(Math.max(0, finalDamage));
        final float fExhaustion = Math.max(0, exhaustion);
        scheduleEntity(player, p -> p.setExhaustion(p.getExhaustion() + fExhaustion));
    }

    // ==================== 弹射闪避（匹配 mod ProjectileDodgeEvent） ====================

    private static final int DODGE_COST_TICKS = 200; // 每次闪避消耗 10 秒

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Projectile)) return;
        if (!effectManager.has(player, KcEffect.PROJECTILE_DODGE)) return;

        event.setCancelled(true);

        // 随机传送（匹配 mod randomTeleport: range 3-16, maxAttempts 16）
        randomTeleport(player, 3, 16);

        // 消耗时间
        if (!effectManager.consumeTicks(player, KcEffect.PROJECTILE_DODGE, DODGE_COST_TICKS)) {
            player.sendActionBar("§c✦ 弹射闪避已耗尽");
        }
    }

    /** 模组 randomTeleport：多次尝试传送到安全位置，失败则原地。 */
    private void randomTeleport(Player player, double minRange, int maxAttempts) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        int minH = world.getMinHeight();
        int maxH = world.getMaxHeight();
        double range = 16.0; // 匹配模组最大 range

        for (int i = 0; i < maxAttempts; i++) {
            double tx = x + (Math.random() - 0.5) * range;
            double ty = Math.clamp(y + (Math.random() - 0.5) * range, minH, minH + maxH - 1);
            double tz = z + (Math.random() - 0.5) * range;

            Location target = new Location(world, tx, ty, tz);

            // 检查目标位置是否安全（非固体 + 下方有支撑）
            Block targetBlock = target.getBlock();
            Block belowBlock = target.clone().add(0, -1, 0).getBlock();

            if (!targetBlock.getType().isSolid()
                    && belowBlock.getType().isSolid()
                    && !belowBlock.getType().toString().contains("LAVA")) {
                // 安全的传送位置
                if (player.isInsideVehicle()) player.leaveVehicle();
                final Location safeLoc = target.clone();
                player.teleportAsync(safeLoc).thenAccept(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        world.playSound(safeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    }
                });
                return;
            }
        }
        // 失败 - 保持在原位（不做传送）
    }

    // ==================== 迟滞：攻击目标缓慢 II 5秒（匹配 mod HinderEvent） ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onHinder(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!effectManager.has(player, KcEffect.HASTEN)) return;
        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
        }
    }

    // ==================== 生机：击杀成年生物生成幼体（匹配 mod VitalityEvent） ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onVitality(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || !effectManager.has(killer, KcEffect.VITALITY)) return;

        Location loc = dead.getLocation();
        EntityType type = dead.getType();

        try {
            // AgeableMob 成年 → 生成同类型幼体
            if (dead instanceof Ageable && !((Ageable) dead).isAdult()) return;
            if (dead instanceof Ageable ageable) {
                Entity baby = dead.getWorld().spawnEntity(loc, type);
                if (baby instanceof Ageable babyAgeable) {
                    babyAgeable.setBaby();
                }
                // 僵尸额外：5% 概率生成小村民
                if (type == EntityType.ZOMBIE && Math.random() < 0.05) {
                    baby.remove();
                    Entity villager = dead.getWorld().spawnEntity(loc, EntityType.VILLAGER);
                    if (villager instanceof Ageable vAgeable) vAgeable.setBaby();
                }
                return;
            }
            // 非 Ageable 但有幼体形态？跳过
        } catch (Exception ignored) {
        }
    }

    // ==================== 芥末：苦力怕逃离 / 硫磺事件 ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (event.getEntity() instanceof Phantom && effectManager.has(player, KcEffect.SULFUR)) {
            event.setCancelled(true);
            event.getEntity().teleport(event.getEntity().getLocation().add(
                    Math.random() * 8 - 4, 3, Math.random() * 8 - 4));
        }
        if (event.getEntity() instanceof Creeper && effectManager.has(player, KcEffect.MUSTARD)) {
            event.setCancelled(true);
            event.getEntity().teleport(event.getEntity().getLocation().add(
                    Math.random() * 6 - 3, 0, Math.random() * 6 - 3));
        }
    }

    // ==================== 即时熔炼（匹配 mod InstantSmeltingEffect） ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInstantSmelt(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!effectManager.has(player, KcEffect.INSTANT_SMELT)) return;

        Material broken = event.getBlock().getType();
        Material smelted = furnaceMap.get(broken);
        if (smelted == null) return;

        // 获取效果等级：amplifier + 1 个物品可被熔炼
        EffectManager.ActiveEffect ae = effectManager.getActive(player).get(KcEffect.INSTANT_SMELT);
        int maxSmelt = (ae != null) ? ae.level + 1 : 1;

        // 先用原版掉落系统获取掉落物，然后替换
        // 简化方案：移除默认掉落，手动生成熔炼产物（最多 maxSmelt 个）
        event.setDropItems(false);
        for (int i = 0; i < maxSmelt; i++) {
            event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation(), new ItemStack(smelted));
        }
    }

    // ==================== 胀气：潜行弹射（匹配 mod FlatulenceEvent） ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onBloatingSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!effectManager.has(player, KcEffect.BLOATING)) return;

        if (event.isSneaking()) {
            // 本次潜行会话已触发过，跳过
            if (Boolean.TRUE.equals(bloatingTriggered.get(player.getUniqueId()))) return;
            bloatingTriggered.put(player.getUniqueId(), true);

            scheduleEntity(player, p -> {
                // 匹配模组：addDeltaMovement(0, 0.75, 0)
                Vector v = p.getVelocity();
                p.setVelocity(new Vector(v.getX(), 0.75, v.getZ()));
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.2f);
            });
        } else {
            // 玩家主动松开潜行 → 重置标记
            bloatingTriggered.remove(player.getUniqueId());
        }
    }

    // ==================== BossBar 展示 ====================

    private void updateBossBars(Player player) {
        UUID uid = player.getUniqueId();
        Map<KcEffect, EffectManager.ActiveEffect> active = effectManager.getActive(player);
        Map<KcEffect, org.bukkit.boss.BossBar> bars = bossBars.computeIfAbsent(uid,
                k -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();

        // 为每个活跃效果创建/更新 BossBar
        for (Map.Entry<KcEffect, EffectManager.ActiveEffect> entry : active.entrySet()) {
            KcEffect kc = entry.getKey();
            EffectManager.ActiveEffect ae = entry.getValue();
            org.bukkit.boss.BossBar bar = bars.get(kc);
            if (bar == null) {
                boolean negative = (kc == KcEffect.BLOATING);
                bar = Bukkit.createBossBar(
                        "§" + (negative ? "c" : "a") + "✦ " + kc.displayName(),
                        negative ? org.bukkit.boss.BarColor.RED : org.bukkit.boss.BarColor.GREEN,
                        org.bukkit.boss.BarStyle.SOLID);
                bar.addPlayer(player);
                bars.put(kc, bar);
            }
            long remainMs = Math.max(0, ae.expiry - now);
            double progress = ae.totalTicks > 0
                    ? (double) remainMs / (ae.totalTicks * 50L) : 0;
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            int remainSec = (int) (remainMs / 1000);
            String remain = formatDuration(remainSec * 20);
            String level = ae.level > 0 ? " " + toRoman(ae.level + 1) : "";
            boolean neg = (kc == KcEffect.BLOATING);
            bar.setTitle("§" + (neg ? "c" : "a") + "✦ " + kc.displayName() + level
                    + " ✦ §7" + remain);
        }

        // 移除过期条
        for (Iterator<Map.Entry<KcEffect, org.bukkit.boss.BossBar>> it = bars.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<KcEffect, org.bukkit.boss.BossBar> entry = it.next();
            if (!active.containsKey(entry.getKey())) {
                entry.getValue().removePlayer(player);
                entry.getValue().removeAll();
                it.remove();
            }
        }
        if (bars.isEmpty() && active.isEmpty()) bossBars.remove(uid);
    }

    private String toRoman(int n) {
        switch (n) {
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return "I";
        }
    }

    // ==================== 持久化 ====================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        saveEffects(event.getPlayer());
        Map<KcEffect, org.bukkit.boss.BossBar> bars = bossBars.remove(uid);
        if (bars != null) for (org.bukkit.boss.BossBar bar : bars.values()) bar.removeAll();
        effectManager.remove(event.getPlayer());
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uid = player.getUniqueId();
        Map<KcEffect, org.bukkit.boss.BossBar> bars = bossBars.remove(uid);
        if (bars != null) for (org.bukkit.boss.BossBar bar : bars.values()) bar.removeAll();
        effectManager.remove(player);
        bloatingTriggered.remove(uid);
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Map<String, Long> saved = loadEffects(event.getPlayer().getUniqueId());
        if (saved != null && !saved.isEmpty()) {
            effectManager.restore(event.getPlayer(), saved);
            deleteEffects(event.getPlayer().getUniqueId());
        }
    }

    private File getDataFile(UUID uuid) {
        File dir = new File(getDataFolder(), "effects");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, uuid + ".yml");
    }

    private void saveEffects(Player player) {
        try {
            Map<String, Long> snap = effectManager.snapshot(player);
            if (snap.isEmpty()) { deleteEffects(player.getUniqueId()); return; }
            File f = getDataFile(player.getUniqueId());
            StringBuilder sb = new StringBuilder("# 下线暂存效果\n");
            for (Map.Entry<String, Long> e : snap.entrySet())
                sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
            Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private Map<String, Long> loadEffects(UUID uuid) {
        File f = getDataFile(uuid);
        if (!f.exists()) return null;
        try {
            Map<String, Long> data = new LinkedHashMap<>();
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split(":", 2);
                if (parts.length == 2) data.put(parts[0].trim(), Long.parseLong(parts[1].trim()));
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteEffects(UUID uuid) {
        File f = getDataFile(uuid);
        if (f.exists()) f.delete();
    }

    // ==================== 工具方法 ====================

    /** Folia 兼容：在实体所属线程执行操作 */
    private void scheduleEntity(Player player, java.util.function.Consumer<Player> action) {
        if (player == null || !player.isOnline()) return;
        try {
            player.getScheduler().run(this, task -> {
                if (player.isOnline()) action.accept(player);
            }, null);
        } catch (Throwable ignored) {
            try { action.accept(player); } catch (Throwable ignored2) {}
        }
    }

    /** CraftEngine 物品 ID 识别 */
    private String getItemId(ItemStack stack) {
        try {
            ItemManager im = CraftEngine.instance().itemManager();
            Item item = im.wrap(stack);
            Optional<Key> customId = item.customId();
            if (customId.isPresent()) return customId.get().toString();
        } catch (Throwable ignored) {
        }
        return null;
    }
}
