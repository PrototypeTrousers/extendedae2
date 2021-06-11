package trousers.extendedae2.core.sync.packets;

import appeng.core.sync.BasePacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import trousers.extendedae2.client.gui.implementations.ExtendedInterfaceTerminalScreen;

public class ExtendedInterfaceTerminalPacket extends EAEBasePacket {
    private final CompoundNBT in;
    private final boolean fullUpdate;
    
    public ExtendedInterfaceTerminalPacket(PacketBuffer stream) {
        this.fullUpdate = stream.readBoolean();
        this.in = stream.readCompoundTag();
    }
    
    public ExtendedInterfaceTerminalPacket(boolean fullUpdate, CompoundNBT din) {
        this.fullUpdate = false;
        this.in = null;
        PacketBuffer data = new PacketBuffer(Unpooled.buffer(2048));
        data.writeInt(this.getPacketID());
        data.writeBoolean(fullUpdate);
        data.writeCompoundTag(din);
        this.configureWrite(data);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientPacketData(final INetworkInfo network, final PlayerEntity player) {
        final Screen gs = Minecraft.getInstance().currentScreen;
        
        if (gs instanceof ExtendedInterfaceTerminalScreen) {
            ((ExtendedInterfaceTerminalScreen) gs).postUpdate(fullUpdate, this.in);
        }
    }
}
