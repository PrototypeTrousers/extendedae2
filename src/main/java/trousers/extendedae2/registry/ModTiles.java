package trousers.extendedae2.registry;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import trousers.extendedae2.tile.misc.ExtendedInterfaceTileEntity;

import static trousers.extendedae2.ExtendedAE2.MODID;

public class ModTiles {
    public static TileEntityType<ExtendedInterfaceTileEntity> extendedInterfaceTileEntity;

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onTileEntityRegistry(
                final RegistryEvent.Register<TileEntityType<?>> tileEntityTypeRegister) {
            extendedInterfaceTileEntity = TileEntityType.Builder
                    .create(ExtendedInterfaceTileEntity::new, ModBlocks.extendedInterfaceBlock).build(null);
            extendedInterfaceTileEntity.setRegistryName(MODID, "extended_interface");
            tileEntityTypeRegister.getRegistry().register(extendedInterfaceTileEntity);
        }
    }
}
