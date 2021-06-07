package trousers.extendedae2.helpers;

import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.AENetworkProxy;
import appeng.parts.automation.StackUpgradeInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import trousers.extendedae2.registry.ModItems;

import java.lang.reflect.Field;

public class ExtendedDualityInterface extends DualityInterface {

    public ExtendedDualityInterface(AENetworkProxy networkProxy, IInterfaceHost ih) {
        super(networkProxy, ih);
        Field patterns = ObfuscationReflectionHelper.findField(DualityInterface.class, "patterns");
        Field upgrades = ObfuscationReflectionHelper.findField(DualityInterface.class, "upgrades");
        try {
            patterns.setAccessible(true);
            patterns.set(this, new AppEngInternalInventory(this, 36, 1));
            upgrades.setAccessible(true);
            upgrades.set(this, new StackUpgradeInventory(ModItems.itemExtendedInterface.getDefaultInstance(), this, 4));

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

}