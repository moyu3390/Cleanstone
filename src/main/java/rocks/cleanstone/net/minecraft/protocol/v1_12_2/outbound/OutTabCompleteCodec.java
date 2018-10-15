package rocks.cleanstone.net.minecraft.protocol.v1_12_2.outbound;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.cleanstone.net.minecraft.packet.outbound.OutTabCompletePacket;
import rocks.cleanstone.net.protocol.OutboundPacketCodec;
import rocks.cleanstone.net.utils.ByteBufUtils;

@Slf4j
@Component
public class OutTabCompleteCodec implements OutboundPacketCodec<OutTabCompletePacket> {
    @Override
    public ByteBuf encode(ByteBuf byteBuf, OutTabCompletePacket packet) {

        ByteBufUtils.writeVarInt(byteBuf, packet.getMatches().size());

        final List<String> matches = packet.getMatches();

        for (final String match : matches) {
            try {
                ByteBufUtils.writeUTF8(byteBuf, match);
            } catch (IOException e) {
                log.error("Error while writing TabCompletePacket", e);
            }
        }

        return byteBuf;
    }
}
