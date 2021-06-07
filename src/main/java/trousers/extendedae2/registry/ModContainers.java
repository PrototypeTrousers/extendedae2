package trousers.extendedae2.registry;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import trousers.extendedae2.container.implementations.ExtendedInterfaceContainer;
import trousers.extendedae2.container.implementations.ExtendedInterfaceTerminalContainer;

public class ModContainers {
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onContainersRegistry(
                final RegistryEvent.Register<ContainerType<?>> containerTypeRegisterEvent) {
            final IForgeRegistry<ContainerType<?>> registry = containerTypeRegisterEvent.getRegistry();
            registry.register(ExtendedInterfaceContainer.TYPE);
            registry.register(ExtendedInterfaceTerminalContainer.TYPE);
        }
    }
}
