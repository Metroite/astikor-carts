package de.mennomax.astikorcarts;

import de.mennomax.astikorcarts.client.ClientInitializer;
import de.mennomax.astikorcarts.entity.SupplyCartEntity;
import de.mennomax.astikorcarts.entity.AnimalCartEntity;
import de.mennomax.astikorcarts.entity.PlowEntity;
import de.mennomax.astikorcarts.entity.PostilionEntity;
import de.mennomax.astikorcarts.inventory.container.PlowContainer;
import de.mennomax.astikorcarts.item.CartItem;
import de.mennomax.astikorcarts.network.NetBuilder;
import de.mennomax.astikorcarts.network.serverbound.ActionKeyMessage;
import de.mennomax.astikorcarts.network.serverbound.OpenSupplyCartMessage;
import de.mennomax.astikorcarts.network.serverbound.ToggleSlowMessage;
import de.mennomax.astikorcarts.network.clientbound.UpdateDrawnMessage;
import de.mennomax.astikorcarts.server.ServerInitializer;
import de.mennomax.astikorcarts.util.DefRegister;
import de.mennomax.astikorcarts.util.RegObject;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.stats.StatFormatter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

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

        private static final DefRegister.Forge<Item> R = REG.of(ForgeRegistries.ITEMS);

        public static final RegObject<Item, Item> WHEEL, SUPPLY_CART, PLOW, ANIMAL_CART;

        static {
            WHEEL = R.make("wheel", () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));
            final Supplier<Item> cart = () -> new CartItem(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION));
            SUPPLY_CART = R.make("supply_cart", cart);
            PLOW = R.make("plow", cart);
            ANIMAL_CART = R.make("animal_cart", cart);
        }
    }

    public static final class EntityTypes {
        private EntityTypes() {
        }

        private static final DefRegister.Forge<EntityType<?>> R = REG.of(ForgeRegistries.ENTITIES);

        public static final RegObject<EntityType<?>, EntityType<SupplyCartEntity>> SUPPLY_CART;
        public static final RegObject<EntityType<?>, EntityType<PlowEntity>> PLOW;
        public static final RegObject<EntityType<?>, EntityType<AnimalCartEntity>> ANIMAL_CART;
        public static final RegObject<EntityType<?>, EntityType<PostilionEntity>> POSTILION;

        static {
            SUPPLY_CART = R.make("supply_cart", () -> EntityType.Builder.of(SupplyCartEntity::new, MobCategory.MISC)
                .sized(1.5F, 1.4F)
                .build(ID + ":supply_cart"));
            PLOW = R.make("plow", () -> EntityType.Builder.of(PlowEntity::new, MobCategory.MISC)
                .sized(1.3F, 1.4F)
                .build(ID + ":plow"));
            ANIMAL_CART = R.make("animal_cart", () -> EntityType.Builder.of(AnimalCartEntity::new, MobCategory.MISC)
                .sized(1.3F, 1.4F)
                .build(ID + ":animal_cart"));
            POSTILION = R.make("postilion", () -> EntityType.Builder.of(PostilionEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .noSummon()
                .noSave()
                .build(ID + ":postilion"));
        }
    }

    public static final class SoundEvents {
        private SoundEvents() {
        }

        private static final DefRegister.Forge<SoundEvent> R = REG.of(ForgeRegistries.SOUND_EVENTS);

        public static final RegObject<SoundEvent, SoundEvent> CART_ATTACHED = R.make("entity.cart.attach", SoundEvent::new);
        public static final RegObject<SoundEvent, SoundEvent> CART_DETACHED = R.make("entity.cart.detach", SoundEvent::new);
        public static final RegObject<SoundEvent, SoundEvent> CART_PLACED = R.make("entity.cart.place", SoundEvent::new);
    }

    public static final class Stats {
        private Stats() {
        }

        private static final DefRegister.Vanilla<ResourceLocation, StatFormatter> R = REG.of(Registry.CUSTOM_STAT, net.minecraft.stats.Stats.CUSTOM::get, rl -> StatFormatter.DEFAULT);

        public static final ResourceLocation CART_ONE_CM = R.make("cart_one_cm", rl -> rl, rl -> StatFormatter.DISTANCE);
    }

    public static final class ContainerTypes {
        private ContainerTypes() {
        }

        private static final DefRegister.Forge<MenuType<?>> R = REG.of(ForgeRegistries.CONTAINERS);

        public static final RegObject<MenuType<?>, MenuType<PlowContainer>> PLOW_CART = R.make("plow", () -> IForgeContainerType.create(PlowContainer::new));
    }

    public AstikorCarts() {
        final Initializer.Context ctx = new InitContext();
        DistExecutor.runForDist(() -> ClientInitializer::new, () -> ServerInitializer::new).init(ctx);
        REG.registerAll(ctx.modBus(), Items.R, EntityTypes.R, SoundEvents.R, ContainerTypes.R, Stats.R);
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
