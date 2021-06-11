package trousers.extendedae2.core.sync.packets;

import appeng.core.sync.BasePacket;
import appeng.core.sync.BasePacketHandler;
import net.minecraft.network.PacketBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EAEBasePacketHandler {
    private static final Map<Class<? extends EAEBasePacket>, PacketTypes> REVERSE_LOOKUP = new HashMap<>();
    
    public enum PacketTypes {
        EXTENDED_ME_INTERFACE_UPDATE(ExtendedInterfaceTerminalPacket.class, ExtendedInterfaceTerminalPacket::new);
    
        private final Function<PacketBuffer, EAEBasePacket> factory;
    
        PacketTypes(Class<? extends EAEBasePacket> packetClass, Function<PacketBuffer, EAEBasePacket> factory) {
            this.factory = factory;
        
            REVERSE_LOOKUP.put(packetClass, this);
        }
    
        public static PacketTypes getPacket(final int id) {
            return (values())[id];
        }
    
        public int getPacketId() {
            return ordinal();
        }
    
        static PacketTypes getID(final Class<? extends EAEBasePacket> c) {
            return REVERSE_LOOKUP.get(c);
        }
    
        public EAEBasePacket parsePacket(final PacketBuffer in) throws IllegalArgumentException {
            return this.factory.apply(in);
        }
    }
}

