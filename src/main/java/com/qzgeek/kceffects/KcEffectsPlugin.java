package com.qzgeek.kceffects;

import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import java.io.File;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Creeper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 森罗物语 Buff 移植插件
 * 依据官方 wiki / MC百科：食物与茶食用后获得对应的 Buff
 */
public class KcEffectsPlugin extends JavaPlugin implements Listener {
    private EffectManager effectManager;
    private int tickCounter = 0;

    @Override
    public void onEnable() {
        effectManager = new EffectManager();
        // 尝试注册自定义效果到服务端注册表（效果栏显示）
        boolean registered = EffectRegistrar.registerCustomEffects(this);
        getServer().getPluginManager().registerEvents(this, this);
        // Folia 兼容：全局区域调度器，每 20 tick 执行一次效果逻辑
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> tick(), 20, 20);
        getLogger().info("森罗物语 Buff 插件已启用（" + FoodEffects.MAP.size() + " 种食物/茶效果"
                + (registered ? "，自定义效果已注册" : "，事件驱动模式") + "）");
    }

    @Override
    public void onDisable() {
        if (effectManager != null) effectManager.cleanup();
        for (Map<KcEffect, org.bukkit.boss.BossBar> bars : bossBars.values()) {
            for (org.bukkit.boss.BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        bossBars.clear();
        getLogger().info("森罗物语 Buff 插件已卸载");
    }

    // ===== 食用事件 =====
    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = getItemId(item);
        if (itemId == null) return;

        String spec = FoodEffects.MAP.get(itemId);
        if (spec == null) return;

        // 处理特殊效果
        if (spec.equals("DARK_CUISINE")) {
            if (Math.random() < 0.33) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            }
            return;
        }
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

        // 常规效果：A|B|C
        for (String effectSpec : spec.split("\\|")) {
            applyEffect(player, effectSpec);
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
                // action bar 提示效果获得
                String dur = formatDuration(ticks);
                player.sendActionBar("§a✦ 获得效果：§f" + effect.displayName() + " §7(" + dur + ")");
            }
        } catch (Exception ignored) {
        }
    }

    private String formatDuration(int ticks) {
        int sec = ticks / 20;
        if (sec >= 60) {
            return (sec / 60) + "分" + (sec % 60 > 0 ? (sec % 60) + "秒" : "");
        }
        return sec + "秒";
    }

    // ===== 效果逻辑 tick（每秒 20 tick 调用一次）=====
    private void tick() {
        tickCounter++;
        effectManager.cleanup();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 更新自定义效果的 BossBar 展示
            updateBossBars(player);
            // 活力：奔跑时将疲劳值重置为 0（每 5 tick）
            if (effectManager.has(player, KcEffect.VIGOR) && player.isSprinting() && tickCounter % 5 == 0) {
                scheduleEntity(player, p -> p.setExhaustion(0f));
            }
            // 寒带疾行：雪/冰/细雪上加速（每 2 秒更新，效果结束恢复 0.2）
            if (tickCounter % 2 == 0) {
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
            // 温暖：热源附近每秒恢复 1 点生命；下界 25% 概率恢复 0.5
            if (effectManager.has(player, KcEffect.WARMTH) && tickCounter % 2 == 0) {
                scheduleEntity(player, p -> {
                    if (p.getHealth() >= p.getMaxHealth()) return;
                    boolean inNether = p.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER;
                    if (inNether) {
                        if (Math.random() < 0.25) {
                            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 0.5));
                        }
                    } else if (hasHeatSourceNearby(p)) {
                        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1.0));
                    }
                });
            }
        }
    }

    /** 检查周围 5×5×3 范围内是否有热源 */
    private boolean hasHeatSourceNearby(Player player) {
        Location loc = player.getLocation();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block b = player.getWorld().getBlockAt(bx + dx, by + dy, bz + dz);
                    Material m = b.getType();
                    if (m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE
                            || m == Material.FIRE || m == Material.SOUL_FIRE
                            || m == Material.LAVA || m == Material.MAGMA_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Folia 兼容：实体操作必须在实体所属区域线程执行。
     * 使用 EntityScheduler 调度到正确线程；非 Folia 环境直接执行。
     */
    private void scheduleEntity(Player player, java.util.function.Consumer<Player> action) {
        if (player == null || !player.isOnline()) return;
        try {
            player.getScheduler().run(this, task -> {
                if (player.isOnline()) {
                    action.accept(player);
                }
            }, null);
        } catch (Throwable ignored) {
            try {
                action.accept(player);
            } catch (Throwable ignored2) {
            }
        }
    }

    private boolean isColdBlock(Material mat) {
        return mat == Material.SNOW_BLOCK || mat == Material.SNOW
                || mat == Material.ICE || mat == Material.PACKED_ICE
                || mat == Material.BLUE_ICE || mat == Material.POWDER_SNOW;
    }

    // ===== BossBar 展示自定义效果 =====
    private final Map<UUID, Map<KcEffect, org.bukkit.boss.BossBar>> bossBars = new java.util.concurrent.ConcurrentHashMap<>();

    private void updateBossBars(Player player) {
        UUID uid = player.getUniqueId();
        Map<KcEffect, EffectManager.ActiveEffect> active = effectManager.getActive(player);
        Map<KcEffect, org.bukkit.boss.BossBar> bars = bossBars.computeIfAbsent(uid, k -> new java.util.concurrent.ConcurrentHashMap<>());
        long now = System.currentTimeMillis();

        // 确保有效果的都有 BossBar
        for (Map.Entry<KcEffect, EffectManager.ActiveEffect> entry : active.entrySet()) {
            KcEffect kc = entry.getKey();
            EffectManager.ActiveEffect ae = entry.getValue();
            org.bukkit.boss.BossBar bar = bars.get(kc);
            if (bar == null) {
                // 负面效果红色，正面效果绿色
                boolean negative = kc == KcEffect.BLOATING;
                bar = Bukkit.createBossBar(
                        "§" + (negative ? "c" : "a") + "✦ " + kc.displayName(),
                        negative ? org.bukkit.boss.BarColor.RED : org.bukkit.boss.BarColor.GREEN,
                        org.bukkit.boss.BarStyle.SOLID);
                bar.addPlayer(player);
                bars.put(kc, bar);
            }
            // 更新标题（效果名 + 剩余时间）和进度
            long remainMs = Math.max(0, ae.expiry - now);
            double progress = (double) remainMs / (ae.totalTicks * 50L);
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            int remainSec = (int) (remainMs / 1000);
            String remain = formatDuration(remainSec * 20);
            String level = ae.level > 0 ? " " + toRoman(ae.level + 1) : "";
            boolean neg = kc == KcEffect.BLOATING;
            bar.setTitle("§" + (neg ? "c" : "a") + "✦ " + kc.displayName() + level + " ✦ §7" + remain);
        }

        // 移除已过期的 BossBar
        for (java.util.Iterator<Map.Entry<KcEffect, org.bukkit.boss.BossBar>> it = bars.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<KcEffect, org.bukkit.boss.BossBar> entry = it.next();
            if (!active.containsKey(entry.getKey())) {
                entry.getValue().removePlayer(player);
                entry.getValue().removeAll();
                it.remove();
            }
        }

        if (bars.isEmpty() && active.isEmpty()) {
            bossBars.remove(uid);
        }
    }

    private String toRoman(int n) {
        switch (n) {
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return "I";
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        // 保存效果到磁盘
        saveEffects(event.getPlayer());
        Map<KcEffect, org.bukkit.boss.BossBar> bars = bossBars.remove(uid);
        if (bars != null) {
            for (org.bukkit.boss.BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        effectManager.remove(event.getPlayer());
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // 恢复上次下线的效果
        java.util.Map<String, Long> saved = loadEffects(event.getPlayer().getUniqueId());
        if (saved != null && !saved.isEmpty()) {
            effectManager.restore(event.getPlayer(), saved);
            deleteEffects(event.getPlayer().getUniqueId());
        }
    }

    // ===== 效果持久化（下线暂存，上线恢复）=====
    private File getDataFile(UUID uuid) {
        File dir = new File(getDataFolder(), "effects");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, uuid + ".yml");
    }

    private void saveEffects(Player player) {
        try {
            java.util.Map<String, Long> snap = effectManager.snapshot(player);
            if (snap.isEmpty()) {
                deleteEffects(player.getUniqueId());
                return;
            }
            File f = getDataFile(player.getUniqueId());
            StringBuilder sb = new StringBuilder();
            sb.append("# 下线暂存效果\n");
            for (java.util.Map.Entry<String, Long> e : snap.entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
            }
            java.nio.file.Files.write(f.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private java.util.Map<String, Long> loadEffects(UUID uuid) {
        File f = getDataFile(uuid);
        if (!f.exists()) return null;
        try {
            java.util.Map<String, Long> data = new java.util.LinkedHashMap<>();
            for (String line : java.nio.file.Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split(":", 2);
                if (parts.length == 2) {
                    data.put(parts[0].trim(), Long.parseLong(parts[1].trim()));
                }
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

    // ===== 保鲜：食用坏食物无负面 =====
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBadConsume(PlayerItemConsumeEvent event) {
        if (!effectManager.has(event.getPlayer(), KcEffect.PRESERVATION)) return;
        Material type = event.getItem().getType();
        if (type == Material.ROTTEN_FLESH || type == Material.CHICKEN
                || type == Material.POISONOUS_POTATO || type == Material.PUFFERFISH
                || type == Material.SPIDER_EYE) {
            event.getPlayer().removePotionEffect(PotionEffectType.POISON);
            event.getPlayer().removePotionEffect(PotionEffectType.HUNGER);
        }
    }

    // ===== 硫磺：幻翼逃离 / 芥末：苦力怕逃离 =====
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

    // ===== 饱腹代偿：受伤害时降低伤害，消耗饱和度和饥饿值 =====
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!effectManager.has(player, KcEffect.SATIETY)) return;
        if (event.isCancelled()) return;
        // 弱点伤害：爆炸/闪电/凋零/音波/间接魔法/窒息/冻结 → 双倍疲劳消耗
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean weak = cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.LIGHTNING
                || cause == EntityDamageEvent.DamageCause.WITHER
                || cause == EntityDamageEvent.DamageCause.SONIC_BOOM
                || cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.SUFFOCATION
                || cause == EntityDamageEvent.DamageCause.FREEZE;
        // 最低食物等级 4 且不能处于饥饿效果
        if (player.getFoodLevel() < 4) return;
        if (player.hasPotionEffect(PotionEffectType.HUNGER)) return;
        double original = event.getDamage();
        double reduced = original * 0.6; // 减免 40%
        event.setDamage(reduced);
        // 疲劳消耗：减免的伤害转化为疲劳（弱点双倍）
        float exhaustion = (float) (original - reduced) * (weak ? 2.0f : 1.0f);
        scheduleEntity(player, p -> p.setExhaustion(p.getExhaustion() + exhaustion));
    }

    // ===== 弹射闪避：被弹射物命中时传送到附近安全位置，每次扣 10 秒 =====
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectile(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getDamager() instanceof Projectile
                && effectManager.has(player, KcEffect.PROJECTILE_DODGE)) {
            event.setCancelled(true);
            // 随机传送到附近安全位置（水平 3-6 格）
            Location loc = player.getLocation();
            double angle = Math.random() * Math.PI * 2;
            double dist = 3 + Math.random() * 3;
            Location target = loc.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
            Block ground = target.clone().add(0, -1, 0).getBlock();
            if (ground.getType().isSolid()) {
                player.teleport(target);
            }
            // 扣 10 秒剩余时间；不足则效果消失
            if (!effectManager.consumeTime(player, KcEffect.PROJECTILE_DODGE, 10000)) {
                player.sendActionBar("§c✦ 弹射闪避已耗尽");
            }
        }
    }

    // ===== 迟滞：对其他生物造成伤害时，给受击目标 5 秒 II 级缓慢 =====
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!effectManager.has(player, KcEffect.HASTEN)) return;
        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
        }
    }

    // ===== 生机：击杀成年年龄型生物时生成同类型幼体 =====
    @EventHandler(priority = EventPriority.HIGH)
    public void onKill(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        org.bukkit.entity.LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        if (!effectManager.has(killer, KcEffect.VITALITY)) return;
        // 生成同类型幼体（如击杀成年羊 → 小羊）
        try {
            if (dead instanceof org.bukkit.entity.Ageable ageable) {
                Location loc = dead.getLocation();
                org.bukkit.entity.LivingEntity baby = (org.bukkit.entity.LivingEntity) dead.getWorld().spawnEntity(loc, dead.getType());
                if (baby instanceof org.bukkit.entity.Ageable babyAgeable) {
                    babyAgeable.setBaby();
                }
                // 击杀成年僵尸 → 5% 概率生成小村民
                if (dead.getType() == org.bukkit.entity.EntityType.ZOMBIE && Math.random() < 0.05) {
                    baby.remove();
                    org.bukkit.entity.LivingEntity villager = (org.bukkit.entity.LivingEntity) dead.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.VILLAGER);
                    if (villager instanceof org.bukkit.entity.Ageable vAgeable) {
                        vAgeable.setBaby();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ===== 胀气：每次按下潜行键获得一次向上的弹射速度（约升高 2 格）=====
    @EventHandler(priority = EventPriority.HIGH)
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!effectManager.has(player, KcEffect.BLOATING)) return;
        scheduleEntity(player, p -> {
            // 0.56 垂直速度 ≈ 升高 2 格（MC 跳跃物理 h = v²/(2×0.08)）
            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.56, 0)));
            p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.2f);
        });
    }

    // ===== 即时熔炼：挖矿掉落熔炼产物 =====
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (!effectManager.has(event.getPlayer(), KcEffect.INSTANT_SMELT)) return;
        Material mat = event.getBlock().getType();
        Material smelted = getSmeltResult(mat);
        if (smelted == null) return;
        // 移除默认掉落，改为熔炼产物
        event.setDropItems(false);
        Location loc = event.getBlock().getLocation();
        event.getBlock().getWorld().dropItemNaturally(loc, new ItemStack(smelted));
    }

    private Material getSmeltResult(Material ore) {
        switch (ore) {
            case IRON_ORE: case DEEPSLATE_IRON_ORE: return Material.IRON_INGOT;
            case GOLD_ORE: case DEEPSLATE_GOLD_ORE: return Material.GOLD_INGOT;
            case COPPER_ORE: case DEEPSLATE_COPPER_ORE: return Material.COPPER_INGOT;
            case ANCIENT_DEBRIS: return Material.NETHERITE_SCRAP;
            case SAND: return Material.GLASS;
            default: return null;
        }
    }

    // ===== 胀气：跳跃提升（氮气加速）=====
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        if (!effectManager.has(event.getPlayer(), KcEffect.BLOATING)) return;
        if (event.getPlayer().isOnGround()) return;
        if (event.getTo().getY() > event.getFrom().getY()) {
            // 上升时略微增强
            event.getPlayer().setVelocity(event.getPlayer().getVelocity().add(
                    event.getPlayer().getVelocity().multiply(0.15)));
        }
    }

    // ===== CraftEngine 物品 ID 识别 =====
    private String getItemId(ItemStack stack) {
        try {
            ItemManager im = CraftEngine.instance().itemManager();
            Item item = im.wrap(stack);
            Optional<Key> customId = item.customId();
            if (customId.isPresent()) {
                return customId.get().toString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
