package trousers.extendedae2.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.AELog;
import appeng.core.Api;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import com.google.common.base.Preconditions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Function;

import static trousers.extendedae2.ExtendedAE2.MODID;

public class EAEContainerTypeBuilder<C extends AEBaseContainer, I> {

    private final Class<I> hostInterface;

    private final ContainerFactory<C, I> factory;

    private Function<I, ITextComponent> containerTitleStrategy = this::getDefaultContainerTitle;

    @Nullable
    private SecurityPermissions requiredPermission;

    @Nullable
    private InitialDataSerializer<I> initialDataSerializer;

    @Nullable
    private InitialDataDeserializer<C, I> initialDataDeserializer;

    private ContainerType<C> containerType;

    private EAEContainerTypeBuilder(Class<I> hostInterface, TypedContainerFactory<C, I> typedFactory) {
        this.hostInterface = hostInterface;
        this.factory = (windowId, playerInv, accessObj) -> typedFactory.create(containerType, windowId, playerInv,
                accessObj);
    }

    private EAEContainerTypeBuilder(Class<I> hostInterface, ContainerFactory<C, I> factory) {
        this.hostInterface = hostInterface;
        this.factory = factory;
    }

    public static <C extends AEBaseContainer, I> EAEContainerTypeBuilder<C, I> create(ContainerFactory<C, I> factory,
            Class<I> hostInterface) {
        return new EAEContainerTypeBuilder<>(hostInterface, factory);
    }

    public static <C extends AEBaseContainer, I> EAEContainerTypeBuilder<C, I> create(
            TypedContainerFactory<C, I> factory,
            Class<I> hostInterface) {
        return new EAEContainerTypeBuilder<>(hostInterface, factory);
    }

    /**
     * Requires that the player has a certain permission on the tile to open the container.
     */
    public EAEContainerTypeBuilder<C, I> requirePermission(SecurityPermissions permission) {
        this.requiredPermission = permission;
        return this;
    }

    /**
     * Specifies a custom strategy for obtaining a custom container name.
     * <p>
     * The stratgy should return {@link StringTextComponent#EMPTY} if there's no custom name.
     */
    public EAEContainerTypeBuilder<C, I> withContainerTitle(Function<I, ITextComponent> containerTitleStrategy) {
        this.containerTitleStrategy = containerTitleStrategy;
        return this;
    }

    /**
     * Sets a serializer and deserializer for additional data that should be transmitted from server->client when the
     * container is being first opened.
     */
    public EAEContainerTypeBuilder<C, I> withInitialData(InitialDataSerializer<I> initialDataSerializer,
            InitialDataDeserializer<C, I> initialDataDeserializer) {
        this.initialDataSerializer = initialDataSerializer;
        this.initialDataDeserializer = initialDataDeserializer;
        return this;
    }

    /**
     * Opens a container that is based around a single tile entity. The tile entity's position is encoded in the packet
     * buffer.
     */
    private C fromNetwork(int windowId, PlayerInventory inv, PacketBuffer packetBuf) {
        I host = getHostFromLocator(inv.player, ContainerLocator.read(packetBuf));
        if (host != null) {
            C container = factory.create(windowId, inv, host);
            if (initialDataDeserializer != null) {
                initialDataDeserializer.deserializeInitialData(host, container, packetBuf);
            }
            return container;
        }
        return null;
    }

    private boolean open(PlayerEntity player, ContainerLocator locator) {
        if (!(player instanceof ServerPlayerEntity)) {
            // Cannot open containers on the client or for non-players
            // FIXME logging?
            return false;
        }

        I accessInterface = getHostFromLocator(player, locator);

        if (accessInterface == null) {
            return false;
        }

        if (!checkPermission(player, accessInterface)) {
            return false;
        }

        ITextComponent title = containerTitleStrategy.apply(accessInterface);

        INamedContainerProvider container = new SimpleNamedContainerProvider((wnd, p, pl) -> {
            C c = factory.create(wnd, p, accessInterface);
            // Set the original locator on the opened server-side container for it to more
            // easily remember how to re-open after being closed.
            c.setLocator(locator);
            return c;
        }, title);
        NetworkHooks.openGui((ServerPlayerEntity) player, container, buffer -> {
            locator.write(buffer);
            if (initialDataSerializer != null) {
                initialDataSerializer.serializeInitialData(accessInterface, buffer);
            }
        });

        return true;
    }

    private I getHostFromLocator(PlayerEntity player, ContainerLocator locator) {
        if (locator.hasItemIndex()) {
            return getHostFromPlayerInventory(player, locator);
        }

        if (!locator.hasBlockPos()) {
            return null; // No block was clicked
        }

        TileEntity tileEntity = player.world.getTileEntity(locator.getBlockPos());

        // The tile entity itself can host a terminal (i.e. Chest!)
        if (hostInterface.isInstance(tileEntity)) {
            return hostInterface.cast(tileEntity);
        }

        if (!locator.hasSide()) {
            return null;
        }

        if (tileEntity instanceof IPartHost) {
            // But it could also be a part attached to the tile entity
            IPartHost partHost = (IPartHost) tileEntity;
            IPart part = partHost.getPart(locator.getSide());
            if (part == null) {
                return null;
            }

            if (hostInterface.isInstance(part)) {
                return hostInterface.cast(part);
            } else {
                AELog.debug("Trying to open a container @ %s for a %s, but the container requires %s", locator,
                        part.getClass(), hostInterface);
                return null;
            }
        } else {
            // FIXME: Logging? Dont know how to obtain the terminal host
            return null;
        }
    }

    private I getHostFromPlayerInventory(PlayerEntity player, ContainerLocator locator) {

        ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());

        if (it.isEmpty()) {
            AELog.debug("Cannot open container for player %s since they no longer hold the item in slot %d", player,
                    locator.hasItemIndex());
            return null;
        }

        if (it.getItem() instanceof IGuiItem) {
            IGuiItem guiItem = (IGuiItem) it.getItem();
            // Optionally contains the block the item was used on to open the container
            BlockPos blockPos = locator.hasBlockPos() ? locator.getBlockPos() : null;
            IGuiItemObject guiObject = guiItem.getGuiObject(it, locator.getItemIndex(), player.world, blockPos);
            if (hostInterface.isInstance(guiObject)) {
                return hostInterface.cast(guiObject);
            }
        }

        if (hostInterface.isAssignableFrom(WirelessTerminalGuiObject.class)) {
            final IWirelessTermHandler wh = Api.instance().registries().wireless().getWirelessTerminalHandler(it);
            if (wh != null) {
                return hostInterface.cast(new WirelessTerminalGuiObject(wh, it, player, locator.getItemIndex()));
            }
        }

        return null;
    }

    /**
     * Creates a container type that uses this helper as a factory and network deserializer.
     */
    public ContainerType<C> build(String id) {
        Preconditions.checkState(containerType == null, "build was already called");

        containerType = IForgeContainerType.create(this::fromNetwork);
        containerType.setRegistryName(MODID, id);
        ContainerOpener.addOpener(containerType, this::open);
        return containerType;
    }

    private boolean checkPermission(PlayerEntity player, Object accessInterface) {

        if (requiredPermission != null) {
            return Platform.checkPermissions(player, accessInterface, requiredPermission, true);
        }

        return true;

    }

    private ITextComponent getDefaultContainerTitle(I accessInterface) {
        if (accessInterface instanceof ICustomNameObject) {
            ICustomNameObject customNameObject = (ICustomNameObject) accessInterface;
            if (customNameObject.hasCustomInventoryName()) {
                return customNameObject.getCustomInventoryName();
            }
        }

        return StringTextComponent.EMPTY;
    }

    @FunctionalInterface
    public interface ContainerFactory<C, I> {
        C create(int windowId, PlayerInventory playerInv, I accessObj);
    }

    @FunctionalInterface
    public interface TypedContainerFactory<C extends Container, I> {
        C create(ContainerType<C> type, int windowId, PlayerInventory playerInv, I accessObj);
    }

    /**
     * Strategy used to serialize initial data for opening the container on the client-side into the packet that is sent
     * to the client.
     */
    @FunctionalInterface
    public interface InitialDataSerializer<I> {
        void serializeInitialData(I host, PacketBuffer buffer);
    }

    /**
     * Strategy used to deserialize initial data for opening the container on the client-side from the packet received
     * by the server.
     */
    @FunctionalInterface
    public interface InitialDataDeserializer<C, I> {
        void deserializeInitialData(I host, C container, PacketBuffer buffer);
    }

}