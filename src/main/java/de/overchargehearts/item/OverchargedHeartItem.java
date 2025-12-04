package de.overchargehearts.item;

import de.overchargehearts.event.ModEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class OverchargedHeartItem extends Item {

    public OverchargedHeartItem(Properties properties) {
        super(properties);
    }

    // Dauer des "Essens" in Ticks
    @Override
    public int getUseDuration(ItemStack stack) {
        return 32; // ~1,6 Sekunden
    }

    // Ess-Animation
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    // Rechtsklick startet das Essen
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // Wird aufgerufen, wenn der Spieler fertig ist mit Essen
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {

            int currentExtra = ModEvents.getExtraOverchargeHealth(player);
            if (currentExtra < ModEvents.MAX_EXTRA_HEALTH) {
                // +1 HP Kapazität = +0,5 Herz
                int newExtra = currentExtra + 1;
                ModEvents.setExtraOverchargeHealth(player, newExtra);

                // Overcharge-Leiste (Absorption) auf neue Kapazität auffüllen
                float newCap = (float) newExtra;
                if (player.getAbsorptionAmount() < newCap) {
                    player.setAbsorptionAmount(newCap);
                }

                // Regenerationszustand zurücksetzen (Timer neu starten)
                ModEvents.resetRegenState(player);
            }

            // Item verbrauchen
            stack.shrink(1);
        }

        return super.finishUsingItem(stack, level, entity);
    }
}
