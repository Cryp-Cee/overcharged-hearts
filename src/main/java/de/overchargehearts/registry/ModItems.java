package de.overchargehearts.registry;

import de.overchargehearts.OverchargeHearts;
import de.overchargehearts.item.OverchargedHeartItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OverchargeHearts.MOD_ID);

    // Crystallised Heart Shard
    public static final RegistryObject<Item> CRYSTALLISED_HEART_SHARD =
            ITEMS.register("crystallised_heart_shard",
                    () -> new Item(new Item.Properties()
                            .stacksTo(64)
                            .rarity(Rarity.RARE)));

    // Overcharged Heart (Konsum-Item, gibt +0.5 Herz)
    public static final RegistryObject<Item> OVERCHARGED_HEART =
            ITEMS.register("overcharged_heart",
                    () -> new OverchargedHeartItem(new Item.Properties()
                            .stacksTo(16)
                            .rarity(Rarity.EPIC)));
}
