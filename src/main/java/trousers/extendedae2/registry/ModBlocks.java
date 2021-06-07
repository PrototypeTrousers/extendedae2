package trousers.extendedae2.registry;

import appeng.block.AEBaseTileBlock;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import trousers.extendedae2.block.misc.ExtendedInterfaceBlock;
import trousers.extendedae2.tile.misc.ExtendedInterfaceTileEntity;

import static trousers.extendedae2.ExtendedAE2.MODID;

public class ModBlocks {
    public static Block extendedInterfaceBlock;  // this holds the unique instance of your block

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            extendedInterfaceBlock = new ExtendedInterfaceBlock();
            extendedInterfaceBlock.setRegistryName(MODID, "extended_interface");
            blockRegistryEvent.getRegistry().register(extendedInterfaceBlock);
            AEBaseTileBlock<ExtendedInterfaceTileEntity> baseTileBlock = (AEBaseTileBlock<ExtendedInterfaceTileEntity>) extendedInterfaceBlock;
            baseTileBlock.setTileEntity(ExtendedInterfaceTileEntity.class, ExtendedInterfaceTileEntity::new);
        }
    }
}
