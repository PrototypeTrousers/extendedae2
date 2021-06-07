package trousers.extendedae2.container.implementations;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.RestrictedInputSlot;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public class ExtendedInterfaceContainer extends UpgradeableContainer {

    public static final ContainerType<ExtendedInterfaceContainer> TYPE = EAEContainerTypeBuilder
            .create(ExtendedInterfaceContainer::new, IInterfaceHost.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("extended_interface");

    @GuiSync(3)
    public YesNo bMode = YesNo.NO;

    @GuiSync(4)
    public YesNo iTermMode = YesNo.YES;

    public ExtendedInterfaceContainer(int id, PlayerInventory ip, IInterfaceHost te) {
        super(TYPE, id, ip, te.getInterfaceDuality().getHost());

        DualityInterface duality = te.getInterfaceDuality();

        for (int row = 0; row < 4; ++row) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                        duality.getPatterns(), x + row * 9), SlotSemantic.ENCODED_PATTERN);
            }
        }

        for (int x = 0; x < DualityInterface.NUMBER_OF_CONFIG_SLOTS; x++) {
            this.addSlot(new FakeSlot(duality.getConfig(), x), SlotSemantic.CONFIG);
        }

        for (int x = 0; x < DualityInterface.NUMBER_OF_STORAGE_SLOTS; x++) {
            this.addSlot(new AppEngSlot(duality.getStorage(), x), SlotSemantic.STORAGE);

        }
    }

    @Override
    protected void setupConfig() {
        this.setupUpgrades();
    }

    @Override
    public int availableUpgrades() {
        return 1;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        super.detectAndSendChanges();
    }

    @Override
    protected void loadSettingsFromHost(final IConfigManager cm) {
        this.setBlockingMode((YesNo) cm.getSetting(Settings.BLOCK));
        this.setInterfaceTerminalMode((YesNo) cm.getSetting(Settings.INTERFACE_TERMINAL));
    }

    public YesNo getBlockingMode() {
        return this.bMode;
    }

    private void setBlockingMode(final YesNo bMode) {
        this.bMode = bMode;
    }

    public YesNo getInterfaceTerminalMode() {
        return this.iTermMode;
    }

    private void setInterfaceTerminalMode(final YesNo iTermMode) {
        this.iTermMode = iTermMode;
    }
}
