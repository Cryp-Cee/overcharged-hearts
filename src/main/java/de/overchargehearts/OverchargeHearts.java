package de.overchargehearts;

import de.overchargehearts.event.ModEvents;
import de.overchargehearts.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OverchargeHearts.MOD_ID)
public class OverchargeHearts {

    public static final String MOD_ID = "overchargehearts";

    public OverchargeHearts() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registriere Items
        ModItems.ITEMS.register(modEventBus);

        // Registriere Events
        MinecraftForge.EVENT_BUS.register(ModEvents.class);
    }
}
