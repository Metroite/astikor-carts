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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.item.Item;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolderRegistry;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CommonInitializer implements Initializer {
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
                    LogicalSidedProvider.WORKQUEUE.<ThreadTaskExecutor<Runnable>>get(EffectiveSide.get())
                        .tell(() -> ObjectHolderRegistry.removeHandler(this));
                }
            }
        });
        mod.modBus().<FMLCommonSetupEvent>addListener(e -> {
            CapabilityManager.INSTANCE.register(AstikorWorld.class, new Capability.IStorage<AstikorWorld>() {
                @Nullable
                @Override
                public INBT writeNBT(final Capability<AstikorWorld> capability, final AstikorWorld instance, final Direction side) {
                    return null;
                }

                @Override
                public void readNBT(final Capability<AstikorWorld> capability, final AstikorWorld instance, final Direction side, final INBT nbt) {
                }
            }, SimpleAstikorWorld::new);
            e.enqueueWork(() -> {
                GlobalEntityTypeAttributes.put(AstikorCarts.EntityTypes.POSTILION.get(), LivingEntity.createLivingAttributes().build()); // TODO: remove in 1.17
            });
        });
        /*mod.modBus().<EntityAttributeCreationEvent>addListener(e -> {
            e.put(AstikorCarts.EntityTypes.POSTILION.get(), LivingEntity.registerAttributes().create()); // TODO: add in 1.17
        });*/
        mod.bus().<AttachCapabilitiesEvent<World>, World>addGenericListener(World.class, e ->
            e.addCapability(new ResourceLocation(AstikorCarts.ID, "astikor"), AstikorWorld.createProvider(SimpleAstikorWorld::new))
        );
        GoalAdder.mobGoal(MobEntity.class)
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
