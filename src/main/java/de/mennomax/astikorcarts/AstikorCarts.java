package de.mennomax.astikorcarts;

import de.mennomax.astikorcarts.client.ClientInitializer;
import de.mennomax.astikorcarts.entity.AnimalCartEntity;
import de.mennomax.astikorcarts.entity.PlowEntity;
import de.mennomax.astikorcarts.entity.PostilionEntity;
import de.mennomax.astikorcarts.entity.SupplyCartEntity;
import de.mennomax.astikorcarts.inventory.container.PlowContainer;
import de.mennomax.astikorcarts.item.CartItem;
import de.mennomax.astikorcarts.network.NetBuilder;
import de.mennomax.astikorcarts.network.clientbound.UpdateDrawnMessage;
import de.mennomax.astikorcarts.network.serverbound.ActionKeyMessage;
import de.mennomax.astikorcarts.network.serverbound.OpenSupplyCartMessage;
import de.mennomax.astikorcarts.network.serverbound.ToggleSlowMessage;
import de.mennomax.astikorcarts.server.ServerInitializer;
import de.mennomax.astikorcarts.util.DefRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@Mod(AstikorCarts.ID)
public final class AstikorCarts {
    public static final String ID = "astikorcarts";

    public static final SimpleChannel CHANNEL = new NetBuilder(new ResourceLocation(ID, "main"))
        .version(1).optionalServer().requiredClient()
        .serverbound(ActionKeyMessage::new).consumer(() -> ActionKeyMessage::handle)
        .serverbound(ToggleSlowMessage::new).consumer(() -> ToggleSlowMessage::handle)
        .clientbound(UpdateDrawnMessage::new).consumer(() -> new UpdateDrawnMessage.Handler())
        .serverbound(OpenSupplyCartMessage::new).consumer(() -> OpenSupplyCartMessage::handle)
        .build();

    private static final DefRegister REG = new DefRegister(ID);

    public static final class Items {
        private Items() {
        }

        private static final DeferredRegister<Item> R = DeferredRegister.create(ForgeRegistries.ITEMS, AstikorCarts.ID);

        public static final RegistryObject<Item> WHEEL, SUPPLY_CART, PLOW, ANIMAL_CART;

        static {
            WHEEL = R.register("wheel", () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));
            final Supplier<Item> cart = () -> new CartItem(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION));
            SUPPLY_CART = R.register("supply_cart", cart);
            PLOW = R.register("plow", cart);
            ANIMAL_CART = R.register("animal_cart", cart);
        }
    }

    public static final class EntityTypes {
        private EntityTypes() {
        }

        private static final DeferredRegister<EntityType<?>> R = DeferredRegister.create(ForgeRegistries.ENTITIES, AstikorCarts.ID);

        public static final RegistryObject<EntityType<SupplyCartEntity>> SUPPLY_CART;
        public static final RegistryObject<EntityType<PlowEntity>> PLOW;
        public static final RegistryObject<EntityType<AnimalCartEntity>> ANIMAL_CART;
        public static final RegistryObject<EntityType<PostilionEntity>> POSTILION;

        static {
            SUPPLY_CART = R.register("supply_cart", () -> EntityType.Builder.of(SupplyCartEntity::new, MobCategory.MISC)
                .sized(1.5F, 1.4F)
                .build(ID + ":supply_cart"));
            PLOW = R.register("plow", () -> EntityType.Builder.of(PlowEntity::new, MobCategory.MISC)
                .sized(1.3F, 1.4F)
                .build(ID + ":plow"));
            ANIMAL_CART = R.register("animal_cart", () -> EntityType.Builder.of(AnimalCartEntity::new, MobCategory.MISC)
                .sized(1.3F, 1.4F)
                .build(ID + ":animal_cart"));
            POSTILION = R.register("postilion", () -> EntityType.Builder.of(PostilionEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .noSummon()
                .noSave()
                .build(ID + ":postilion"));
        }
    }

    public static final class SoundEvents {
        private SoundEvents() {
        }

        private static final DeferredRegister<SoundEvent> R = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AstikorCarts.ID);

        public static final RegistryObject<SoundEvent> CART_ATTACHED = R.register("entity.cart.attach", () -> new SoundEvent(new ResourceLocation(AstikorCarts.ID, "entity.cart.attach")));
        public static final RegistryObject<SoundEvent> CART_DETACHED = R.register("entity.cart.detach", () -> new SoundEvent(new ResourceLocation(AstikorCarts.ID, "entity.cart.detach")));
        public static final RegistryObject<SoundEvent> CART_PLACED = R.register("entity.cart.place", () -> new SoundEvent(new ResourceLocation(AstikorCarts.ID, "entity.cart.place")));
    }

    public static final class Stats {
        private Stats() {
        }

//        private static final DeferredRegister<StatType<?>> R = DeferredRegister.create(ForgeRegistries.STAT_TYPES, AstikorCarts.ID);
//        public static final RegistryObject<StatType<ResourceLocation>> CART_ONE_CM = R.register("cart_one_cm", () -> new StatType<>(Registry.CUSTOM_STAT));
    }

    public static final class ContainerTypes {
        private ContainerTypes() {
        }

        private static final DeferredRegister<MenuType<?>> R = DeferredRegister.create(ForgeRegistries.CONTAINERS, AstikorCarts.ID);
        public static final RegistryObject<MenuType<PlowContainer>> PLOW_CART = R.register("plow", () -> IForgeMenuType.create((PlowContainer::new)));    }

    public AstikorCarts() {
        final Initializer.Context ctx = new InitContext();
        DistExecutor.safeRunForDist(() -> ClientInitializer::new, () -> ServerInitializer::new).init(ctx);
//        REG.registerAll(ctx.modBus(), Items.R, EntityTypes.R, SoundEvents.R, ContainerTypes.R, Stats.R);
    }

    private static class InitContext implements Initializer.Context {
        @Override
        public ModLoadingContext context() {
            return ModLoadingContext.get();
        }

        @Override
        public IEventBus bus() {
            return MinecraftForge.EVENT_BUS;
        }

        @Override
        public IEventBus modBus() {
            return FMLJavaModLoadingContext.get().getModEventBus();
        }
    }
}
