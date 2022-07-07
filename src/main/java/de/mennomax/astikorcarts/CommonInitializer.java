package de.mennomax.astikorcarts;

import com.google.common.collect.ImmutableMap;
import de.mennomax.astikorcarts.config.AstikorCartsConfig;
import de.mennomax.astikorcarts.entity.PostilionEntity;
import de.mennomax.astikorcarts.entity.ai.goal.PullCartGoal;
import de.mennomax.astikorcarts.entity.ai.goal.RideCartGoal;
import de.mennomax.astikorcarts.util.GoalAdder;
import de.mennomax.astikorcarts.util.RegObject;
import de.mennomax.astikorcarts.world.AstikorWorld;
import de.mennomax.astikorcarts.world.SimpleAstikorWorld;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.fmllegacy.LogicalSidedProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolderRegistry;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = AstikorCarts.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonInitializer implements Initializer {
    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(AstikorWorld.class);
    }

    @SubscribeEvent
    public void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(AstikorCarts.EntityTypes.POSTILION.get(), LivingEntity.createLivingAttributes().build());
    }

    @Override
    public void init(final Context mod) {
        final ModContainer container = mod.context().getActiveContainer();
        ObjectHolderRegistry.addHandler(new Consumer<Predicate<ResourceLocation>>() {
            boolean run = true;

            @Override
            public void accept(final Predicate<ResourceLocation> filter) {
                if (this.run && filter.test(ForgeRegistries.ENTITIES.getRegistryName())) {
                    container.addConfig(new ModConfig(ModConfig.Type.COMMON, AstikorCartsConfig.spec(), container));
                    this.run = false;
                    LogicalSidedProvider.WORKQUEUE.<BlockableEventLoop<Runnable>>get(EffectiveSide.get())
                        .tell(() -> ObjectHolderRegistry.removeHandler(this));
                }
            }
        });
        /*mod.modBus().<EntityAttributeCreationEvent>addListener(e -> {
            e.put(AstikorCarts.EntityTypes.POSTILION.get(), LivingEntity.registerAttributes().create()); // TODO: add in 1.17
        });*/
        mod.bus().<AttachCapabilitiesEvent<Level>, Level>addGenericListener(Level.class, e ->
            e.addCapability(new ResourceLocation(AstikorCarts.ID, "astikor"), AstikorWorld.createProvider(SimpleAstikorWorld::new))
        );
        GoalAdder.mobGoal(Mob.class)
            .add(1, PullCartGoal::new)
            .add(1, RideCartGoal::new)
            .build()
            .register(mod.bus());
        mod.bus().<PlayerInteractEvent.EntityInteract>addListener(e -> {
            final Entity rider = e.getTarget().getControllingPassenger();
            if (rider instanceof PostilionEntity) {
                rider.stopRiding();
            }
        });
        mod.bus().<TickEvent.WorldTickEvent>addListener(e -> {
            if (e.phase == TickEvent.Phase.END) {
                AstikorWorld.get(e.world).ifPresent(AstikorWorld::tick);
            }
        });
        mod.bus().addGenericListener(Item.class, this.remap(ImmutableMap.<String, RegObject<Item, ? extends Item>>builder()
            .put("cargo_cart", AstikorCarts.Items.SUPPLY_CART)
            .put("plow_cart", AstikorCarts.Items.PLOW)
            .put("mob_cart", AstikorCarts.Items.ANIMAL_CART)
            .build()
        ));
        mod.bus().addGenericListener(EntityType.class, this.remap(ImmutableMap.<String, RegObject<EntityType<?>, ? extends EntityType<?>>>builder()
            .put("cargo_cart", AstikorCarts.EntityTypes.SUPPLY_CART)
            .put("plow_cart", AstikorCarts.EntityTypes.PLOW)
            .put("mob_cart", AstikorCarts.EntityTypes.ANIMAL_CART)
            .build()
        ));
    }

    private <T extends IForgeRegistryEntry<T>> Consumer<RegistryEvent.MissingMappings<T>> remap(final Map<String, RegObject<T, ? extends T>> objects) {
        return e -> {
            for (final RegistryEvent.MissingMappings.Mapping<T> mapping : e.getAllMappings()) {
                if (AstikorCarts.ID.equals(mapping.key.getNamespace())) {
                    final RegObject<T, ? extends T> target = objects.get(mapping.key.getPath());
                    if (target != null) {
                        mapping.remap(target.get());
                    }
                }
            }
        };
    }
}
