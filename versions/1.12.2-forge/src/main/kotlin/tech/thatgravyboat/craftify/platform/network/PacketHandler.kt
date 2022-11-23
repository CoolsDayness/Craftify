package tech.thatgravyboat.craftify.platform.network

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload

@Suppress("UNNECESSARY_SAFE_CALL")
object PacketHandler {

    fun sendPacket(id: String, data: (ByteBuf) -> Unit) {
        val buf = Unpooled.buffer()
        data(buf)
        Minecraft.getMinecraft().connection?.sendPacket(CPacketCustomPayload(id, PacketBuffer(buf)))
    }

    fun getServerAddress(): String {
        return Minecraft.getMinecraft().currentServerData?.serverIP ?: ""
    }

    fun isOnLanServer(): Boolean {
        return Minecraft.getMinecraft().currentServerData?.isOnLAN ?: false
    }
}