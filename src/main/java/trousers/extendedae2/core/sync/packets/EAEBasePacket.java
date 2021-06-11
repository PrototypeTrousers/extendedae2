package trousers.extendedae2.core.sync.packets;

import appeng.api.features.AEFeature;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.sync.network.INetworkInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import org.apache.commons.lang3.tuple.Pair;
import trousers.extendedae2.core.sync.network.EAENetworkHandler;

public class EAEBasePacket {
    
    public static final int MAX_STRING_LENGTH = 32767;
    
    private PacketBuffer p;
    
    public void serverPacketData(final INetworkInfo manager, final PlayerEntity player) {
        throw new UnsupportedOperationException(
                "This packet ( " + this.getPacketID() + " does not implement a server side handler.");
    }
    
    public final int getPacketID() {
        return EAEBasePacketHandler.PacketTypes.getID(this.getClass()).ordinal();
    }
    
    public void clientPacketData(final INetworkInfo network, final PlayerEntity player) {
        throw new UnsupportedOperationException(
                "This packet ( " + this.getPacketID() + " does not implement a client side handler.");
    }
    
    protected void configureWrite(final PacketBuffer data) {
        data.capacity(data.readableBytes());
        this.p = data;
    }
    
    public IPacket<?> toPacket(NetworkDirection direction) {
        if (this.p.array().length > 2 * 1024 * 1024) // 2k walking room :)
        {
            throw new IllegalArgumentException(
                    "Sorry AE2 made a " + this.p.array().length + " byte packet by accident!");
        }
        
        if (AEConfig.instance().isFeatureEnabled(AEFeature.PACKET_LOGGING)) {
            AELog.info(this.getClass().getName() + " : " + p.readableBytes());
        }
        
        return direction.buildPacket(Pair.of(p, 0), EAENetworkHandler.instance().getChannel()).getThis();
    }
}
