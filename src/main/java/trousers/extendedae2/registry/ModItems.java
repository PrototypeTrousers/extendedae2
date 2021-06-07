package trousers.extendedae2.registry;

import appeng.items.parts.PartItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import trousers.extendedae2.parts.reporting.ExtendedInterfaceTerminalPart;

import static trousers.extendedae2.ExtendedAE2.MODID;
import static trousers.extendedae2.registry.ModBlocks.extendedInterfaceBlock;

public class ModItems {

    public static Item itemExtendedInterface;  // this holds the unique instance of your block
    public static Item itemExtendedInterfaceTerminal;

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
            Item.Properties itemSimpleProperties = new Item.Properties().maxStackSize(64).group(ItemGroup.MISC);  // which inventory tab?
            itemExtendedInterface = new BlockItem(extendedInterfaceBlock, itemSimpleProperties);
            itemExtendedInterface.setRegistryName(extendedInterfaceBlock.getRegistryName());
            itemRegistryEvent.getRegistry().register(itemExtendedInterface);
            
            itemExtendedInterfaceTerminal = new PartItem<>(itemSimpleProperties, ExtendedInterfaceTerminalPart::new);
            itemExtendedInterfaceTerminal.setRegistryName(MODID, "extended_interface_terminal");
            itemRegistryEvent.getRegistry().register(itemExtendedInterfaceTerminal);
    
        }
    }
}
