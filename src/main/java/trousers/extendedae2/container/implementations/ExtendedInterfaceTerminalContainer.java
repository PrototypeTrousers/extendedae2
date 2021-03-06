package trousers.extendedae2.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.InventoryAction;
import appeng.items.misc.EncodedPatternItem;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.WrapperRangeItemHandler;
import appeng.util.inv.filter.IAEItemFilter;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.items.IItemHandler;
import trousers.extendedae2.core.sync.network.EAENetworkHandler;
import trousers.extendedae2.core.sync.packets.ExtendedInterfaceTerminalPacket;
import trousers.extendedae2.parts.reporting.ExtendedInterfaceTerminalPart;
import trousers.extendedae2.parts.reporting.misc.ExtendedInterfacePart;
import trousers.extendedae2.tile.misc.ExtendedInterfaceTileEntity;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ExtendedInterfaceTerminalContainer extends AEBaseContainer {

    public static final ContainerType<ExtendedInterfaceTerminalContainer> TYPE = EAEContainerTypeBuilder
            .create(ExtendedInterfaceTerminalContainer::new, ExtendedInterfaceTerminalPart.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("extended_interface_terminal");

    /**
     * this stuff is all server side..
     */

    // We use this serial number to uniquely identify all inventories we send to the client
    // It is used in packets sent by the client to interact with these inventories
    private static long inventorySerial = Long.MIN_VALUE;
    private final Map<IInterfaceHost, InvTracker> diList = new IdentityHashMap<>();
    private final Long2ObjectOpenHashMap<InvTracker> byId = new Long2ObjectOpenHashMap<>();

    public ExtendedInterfaceTerminalContainer(int id, final PlayerInventory ip, final ExtendedInterfaceTerminalPart anchor) {
        super(TYPE, id, ip, anchor);
        this.createPlayerInventorySlots(ip);
    }

    @Override
    public void detectAndSendChanges() {
        if (isClient()) {
            return;
        }

        super.detectAndSendChanges();

        IGrid grid = getGrid();

        VisitorState state = new VisitorState();
        if (grid != null) {
            visitInterfaceHosts(grid, ExtendedInterfaceTileEntity.class, state);
            visitInterfaceHosts(grid, ExtendedInterfacePart.class, state);
        }
    
        ExtendedInterfaceTerminalPacket packet;
        if (state.total != this.diList.size() || state.forceFullUpdate) {
            packet = this.createFullUpdate(grid);
        } else {
            packet = createIncrementalUpdate();
        }

        if (packet != null) {
            EAENetworkHandler.instance().sendTo(packet, (ServerPlayerEntity) this.getPlayerInventory().player);
        }
    }

    @Nullable
    private IGrid getGrid() {
        IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn != null && agn.isActive()) {
                return agn.getGrid();
            }
        }
        return null;
    }

    private static class VisitorState {
        // Total number of interface hosts founds
        int total;
        // Set to true if any visited machines were missing from diList, or had a different name
        boolean forceFullUpdate;
    }

    private <T extends IInterfaceHost & IGridHost> void visitInterfaceHosts(IGrid grid, Class<T> machineClass,
            VisitorState state) {
        for (final IGridNode gn : grid.getMachines(machineClass)) {
            if (gn.isActive()) {
                final IInterfaceHost ih = (IInterfaceHost) gn.getMachine();
                final DualityInterface dual = ih.getInterfaceDuality();
                if (dual.getConfigManager().getSetting(Settings.INTERFACE_TERMINAL) == YesNo.NO) {
                    continue;
                }

                final InvTracker t = this.diList.get(ih);
                if (t == null) {
                    state.forceFullUpdate = true;
                } else {
                    if (!t.name.equals(dual.getTermName())) {
                        state.forceFullUpdate = true;
                    }
                }

                state.total++;
            }
        }
    }

    @Override
    public void doAction(final ServerPlayerEntity player, final InventoryAction action, final int slot, final long id) {
        final InvTracker inv = this.byId.get(id);
        if (inv == null) {
            // Can occur if the client sent an interaction packet right before an inventory got removed
            return;
        }
        if (slot < 0 || slot >= inv.server.getSlots()) {
            // Client refers to an invalid slot. This should NOT happen
            AELog.warn("Client refers to invalid slot %d of inventory %s", slot, inv.name.getString());
            return;
        }

        final ItemStack is = inv.server.getStackInSlot(slot);
        final boolean hasItemInHand = !player.inventory.getItemStack().isEmpty();

        final InventoryAdaptor playerHand = new AdaptorItemHandler(new WrapperCursorItemHandler(player.inventory));

        final IItemHandler theSlot = new WrapperFilteredItemHandler(
                new WrapperRangeItemHandler(inv.server, slot, slot + 1), new PatternSlotFilter());
        final InventoryAdaptor interfaceSlot = new AdaptorItemHandler(theSlot);

        switch (action) {
            case PICKUP_OR_SET_DOWN:

                if (hasItemInHand) {
                    ItemStack inSlot = theSlot.getStackInSlot(0);
                    if (inSlot.isEmpty()) {
                        player.inventory.setItemStack(interfaceSlot.addItems(player.inventory.getItemStack()));
                    } else {
                        inSlot = inSlot.copy();
                        final ItemStack inHand = player.inventory.getItemStack().copy();

                        ItemHandlerUtil.setStackInSlot(theSlot, 0, ItemStack.EMPTY);
                        player.inventory.setItemStack(ItemStack.EMPTY);

                        player.inventory.setItemStack(interfaceSlot.addItems(inHand.copy()));

                        if (player.inventory.getItemStack().isEmpty()) {
                            player.inventory.setItemStack(inSlot);
                        } else {
                            player.inventory.setItemStack(inHand);
                            ItemHandlerUtil.setStackInSlot(theSlot, 0, inSlot);
                        }
                    }
                } else {
                    ItemHandlerUtil.setStackInSlot(theSlot, 0, playerHand.addItems(theSlot.getStackInSlot(0)));
                }

                break;
            case SPLIT_OR_PLACE_SINGLE:

                if (hasItemInHand) {
                    ItemStack extra = playerHand.removeItems(1, ItemStack.EMPTY, null);
                    if (!extra.isEmpty()) {
                        extra = interfaceSlot.addItems(extra);
                    }
                    if (!extra.isEmpty()) {
                        playerHand.addItems(extra);
                    }
                } else if (!is.isEmpty()) {
                    ItemStack extra = interfaceSlot.removeItems((is.getCount() + 1) / 2, ItemStack.EMPTY, null);
                    if (!extra.isEmpty()) {
                        extra = playerHand.addItems(extra);
                    }
                    if (!extra.isEmpty()) {
                        interfaceSlot.addItems(extra);
                    }
                }

                break;
            case SHIFT_CLICK:

                final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player);

                ItemHandlerUtil.setStackInSlot(theSlot, 0, playerInv.addItems(theSlot.getStackInSlot(0)));

                break;
            case MOVE_REGION:

                final InventoryAdaptor playerInvAd = InventoryAdaptor.getAdaptor(player);
                for (int x = 0; x < inv.server.getSlots(); x++) {
                    ItemHandlerUtil.setStackInSlot(inv.server, x,
                            playerInvAd.addItems(inv.server.getStackInSlot(x)));
                }

                break;
            case CREATIVE_DUPLICATE:

                if (player.abilities.isCreativeMode && !hasItemInHand) {
                    player.inventory.setItemStack(is.isEmpty() ? ItemStack.EMPTY : is.copy());
                }

                break;
            default:
                return;
        }

        this.updateHeld(player);
    }

    private ExtendedInterfaceTerminalPacket createFullUpdate(@Nullable IGrid grid) {
        this.byId.clear();
        this.diList.clear();

        if (grid == null) {
            return new ExtendedInterfaceTerminalPacket(true, new CompoundNBT());
        }

        for (final IGridNode gn : grid.getMachines(ExtendedInterfaceTileEntity.class)) {
            final IInterfaceHost ih = (IInterfaceHost) gn.getMachine();
            final DualityInterface dual = ih.getInterfaceDuality();
            if (gn.isActive() && dual.getConfigManager().getSetting(Settings.INTERFACE_TERMINAL) == YesNo.YES) {
                this.diList.put(ih, new InvTracker(dual, dual.getPatterns(), dual.getTermName()));
            }
        }

        for (final IGridNode gn : grid.getMachines(ExtendedInterfacePart.class)) {
            final IInterfaceHost ih = (IInterfaceHost) gn.getMachine();
            final DualityInterface dual = ih.getInterfaceDuality();
            if (gn.isActive() && dual.getConfigManager().getSetting(Settings.INTERFACE_TERMINAL) == YesNo.YES) {
                this.diList.put(ih, new InvTracker(dual, dual.getPatterns(), dual.getTermName()));
            }
        }

        CompoundNBT data = new CompoundNBT();
        for (final Map.Entry<IInterfaceHost, InvTracker> en : this.diList.entrySet()) {
            final InvTracker inv = en.getValue();
            this.byId.put(inv.serverId, inv);
            this.addItems(data, inv, 0, inv.server.getSlots());
        }
        return new ExtendedInterfaceTerminalPacket(true, data);
    }

    private ExtendedInterfaceTerminalPacket createIncrementalUpdate() {
        CompoundNBT data = null;
        for (final Map.Entry<IInterfaceHost, InvTracker> en : this.diList.entrySet()) {
            final InvTracker inv = en.getValue();
            for (int x = 0; x < inv.server.getSlots(); x++) {
                if (this.isDifferent(inv.server.getStackInSlot(x), inv.client.getStackInSlot(x))) {
                    if (data == null) {
                        data = new CompoundNBT();
                    }
                    this.addItems(data, inv, x, 1);
                }
            }
        }
        if (data != null) {
            return new ExtendedInterfaceTerminalPacket(false, data);
        }
        return null;
    }

    private boolean isDifferent(final ItemStack a, final ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return false;
        }

        if (a.isEmpty() || b.isEmpty()) {
            return true;
        }

        return !ItemStack.areItemStacksEqual(a, b);
    }

    private void addItems(final CompoundNBT data, final InvTracker inv, final int offset, final int length) {
        final String name = '=' + Long.toString(inv.serverId, Character.MAX_RADIX);
        final CompoundNBT tag = data.getCompound(name);

        if (tag.isEmpty()) {
            tag.putLong("sortBy", inv.sortBy);
            tag.putString("un", ITextComponent.Serializer.toJson(inv.name));
        }

        for (int x = 0; x < length; x++) {
            final CompoundNBT itemNBT = new CompoundNBT();

            final ItemStack is = inv.server.getStackInSlot(x + offset);

            // "update" client side.
            ItemHandlerUtil.setStackInSlot(inv.client, x + offset, is.isEmpty() ? ItemStack.EMPTY : is.copy());

            if (!is.isEmpty()) {
                is.write(itemNBT);
            }

            tag.put(Integer.toString(x + offset), itemNBT);
        }

        data.put(name, tag);
    }

    private static class InvTracker {

        private final long sortBy;
        private final long serverId = inventorySerial++;
        private final ITextComponent name;
        // This is used to track the inventory contents we sent to the client for change detection
        private final IItemHandler client;
        // This is a reference to the real inventory used by this machine
        private final IItemHandler server;

        public InvTracker(final DualityInterface dual, final IItemHandler patterns, final ITextComponent name) {
            this.server = patterns;
            this.client = new AppEngInternalInventory(null, this.server.getSlots());
            this.name = name;
            this.sortBy = dual.getSortValue();
        }
    }

    private static class PatternSlotFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(IItemHandler inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(IItemHandler inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem;
        }
    }
}

