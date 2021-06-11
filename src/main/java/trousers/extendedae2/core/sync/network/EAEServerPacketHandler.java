package trousers.extendedae2.core.sync.network;

import appeng.core.AELog;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.network.IPacketHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import trousers.extendedae2.core.sync.packets.EAEBasePacket;
import trousers.extendedae2.core.sync.packets.EAEBasePacketHandler;

public class EAEServerPacketHandler extends EAEBasePacketHandler implements IPacketHandler {
    
    @Override
    public void onPacketData(final INetworkInfo manager, final INetHandler handler, final PacketBuffer packet,
                             final PlayerEntity player) {
        try {
            final int packetType = packet.readInt();
            final EAEBasePacket pack = EAEBasePacketHandler.PacketTypes.getPacket(packetType).parsePacket(packet);
            pack.serverPacketData(manager, player);
        } catch (final IllegalArgumentException e) {
            AELog.debug(e);
        }
    }
}

