package de.overchargehearts.event;

import de.overchargehearts.registry.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModEvents {

    private static final String KEY_EXTRA_HEALTH = "overcharge_extra_health";
    private static final String KEY_LAST_COMBAT_TICK = "overcharge_last_combat";
    private static final String KEY_REGEN_PROGRESS = "overcharge_regen_progress";

    // 20 HP = 10 Overcharge-Herzen
    public static final int MAX_EXTRA_HEALTH = 20;
    public static final int COMBAT_COOLDOWN_TICKS = 15 * 20; // 15 Sekunden
    public static final int REGEN_INTERVAL_TICKS = 100;      // 1 HP pro 100 Ticks

    // ======= NBT Helper =======

    public static int getExtraOverchargeHealth(Player player) {
        return player.getPersistentData().getInt(KEY_EXTRA_HEALTH);
    }

    public static void setExtraOverchargeHealth(Player player, int value) {
        int clamped = Math.max(0, Math.min(MAX_EXTRA_HEALTH, value));
        player.getPersistentData().putInt(KEY_EXTRA_HEALTH, clamped);
    }

    public static void markCombat(Player player) {
        player.getPersistentData().putLong(KEY_LAST_COMBAT_TICK, player.level().getGameTime());
        player.getPersistentData().putInt(KEY_REGEN_PROGRESS, 0);
    }

    public static void resetRegenState(Player player) {
        player.getPersistentData().putInt(KEY_REGEN_PROGRESS, 0);
    }

    // ======= Drops =======

@SubscribeEvent
public static void onLivingDrops(LivingDropsEvent event) {
    LivingEntity entity = event.getEntity();
    Level level = entity.level();

    if (level.isClientSide) return;
    if (entity instanceof Player) return;

    double chance = 0.0D;

    // Bosse: 100 %
    if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
        chance = 1.0D;
    }
    // Mini-Bosse: 50 %
    else if (entity instanceof Warden || entity instanceof ElderGuardian) {
        chance = 0.5D;
    } else {
        ResourceKey<Level> dim = level.dimension();

        if (dim == Level.NETHER) {
            // Nether-Mobs: 4 %
            chance = 0.04D;
        } else if (dim == Level.OVERWORLD) {
            // Overworld: nur aggressive Mobs, 2 %
            if (isAllowedOverworldMob(entity)) {
                chance = 0.02D;
            }
        }
    }

    if (chance <= 0.0D) return;

    if (level.random.nextDouble() < chance) {
        ItemStack drop = new ItemStack(ModItems.CRYSTALLISED_HEART_SHARD.get());
        event.getDrops().add(new ItemEntity(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                drop
        ));
    }
}


    private static boolean isAllowedOverworldMob(LivingEntity entity) {
        EntityType<?> type = entity.getType();

        // nur aggressive Mobs in der Overworld
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.CREEPER
                || type == EntityType.ENDERMAN
                || type == EntityType.WITCH
                || type == EntityType.PHANTOM;
        // Wolf & Polar Bear wurden entfernt, da sie neutral sind.
    }

    // ======= Combat-Tracking =======

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        Level level = event.getEntity().level();

        if (level.isClientSide) return;

        if (source.getEntity() instanceof Player player) {
            markCombat(player);
        }

        if (event.getEntity() instanceof Player player2) {
            markCombat(player2);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide) return;

        if (event.getEntity() instanceof Player player) {
            markCombat(player);
        }
    }

    // ======= Regeneration der Overcharge-Herzen (Absorption) =======

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();

        if (event.phase != TickEvent.Phase.END) return;
        if (level.isClientSide) return;

        int extraCap = getExtraOverchargeHealth(player);
        if (extraCap <= 0) return; // keine Overcharge-Kapazität

        long gameTime = level.getGameTime();
        long lastCombat = player.getPersistentData().getLong(KEY_LAST_COMBAT_TICK);
        long ticksSinceCombat = gameTime - lastCombat;

        if (ticksSinceCombat < COMBAT_COOLDOWN_TICKS) {
            // Noch im Kampf-Timeout
            resetRegenState(player);
            return;
        }

        int progress = player.getPersistentData().getInt(KEY_REGEN_PROGRESS);
        progress++;

        if (progress >= REGEN_INTERVAL_TICKS) {
            progress = 0;

            float currentAbs = player.getAbsorptionAmount();
            float cap = (float) extraCap;

            // Nur die Overcharge-Leiste (Absorption) regenerieren
            if (currentAbs < cap) {
                currentAbs += 1.0F; // +1 HP = +0,5 Herz goldene Leiste
                if (currentAbs > cap) {
                    currentAbs = cap;
                }
                player.setAbsorptionAmount(currentAbs);
            }
        }

        player.getPersistentData().putInt(KEY_REGEN_PROGRESS, progress);
    }

    // ======= Golden Apples: keine zusätzlichen Absorptions-Herzen =======

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity living = event.getEntity();
        Level level = living.level();
        if (level.isClientSide) return;

        if (!(living instanceof Player player)) {
            return;
        }

        ItemStack used = event.getItem();
        if (!used.is(Items.GOLDEN_APPLE) && !used.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return;
        }

        // Maximale Overcharge-Kapazität (in HP)
        int extraCap = getExtraOverchargeHealth(player);
        float cap = extraCap > 0 ? (float) extraCap : 0.0F;

        if (cap <= 0.0F) {
            // Spieler hat keine Overcharge-Kapazität -> Goldäpfel sollen KEINE gelben Herzen erzeugen
            player.setAbsorptionAmount(0.0F);
        } else {
            // Spieler hat Overcharge-Kapazität -> Goldäpfel dürfen NICHT mehr Absorption geben als Overcharge
            float current = player.getAbsorptionAmount();
            if (current > cap) {
                player.setAbsorptionAmount(cap);
            }
            // Wenn current <= cap, lassen wir es unverändert:
            // Goldäpfel erhöhen dann effektiv NICHT die Overcharge-Herzen.
        }
    }

    // ======= Tod / Respawn =======

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        if (event.isWasDeath()) {
            // Bei Tod: komplette Overcharge-Mechanik zurücksetzen
            clone.getPersistentData().remove(KEY_EXTRA_HEALTH);
            clone.getPersistentData().remove(KEY_LAST_COMBAT_TICK);
            clone.getPersistentData().remove(KEY_REGEN_PROGRESS);

            clone.setAbsorptionAmount(0.0F);
        } else {
            // z.B. Dimensionenwechsel: Daten übernehmen
            int extra = original.getPersistentData().getInt(KEY_EXTRA_HEALTH);
            long lastCombat = original.getPersistentData().getLong(KEY_LAST_COMBAT_TICK);
            int progress = original.getPersistentData().getInt(KEY_REGEN_PROGRESS);

            clone.getPersistentData().putInt(KEY_EXTRA_HEALTH, extra);
            clone.getPersistentData().putLong(KEY_LAST_COMBAT_TICK, lastCombat);
            clone.getPersistentData().putInt(KEY_REGEN_PROGRESS, progress);

            // Aktuelle Overcharge-Herzen (Absorption) übernehmen
            clone.setAbsorptionAmount(original.getAbsorptionAmount());
        }
    }
}
