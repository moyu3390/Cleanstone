package rocks.cleanstone.net.netty.pipeline.inbound;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import rocks.cleanstone.core.CleanstoneServer;
import rocks.cleanstone.net.Connection;
import rocks.cleanstone.net.Networking;
import rocks.cleanstone.net.event.ConnectionClosedEvent;
import rocks.cleanstone.net.event.ConnectionOpenEvent;
import rocks.cleanstone.net.netty.NettyConnection;

@Slf4j
public class IdentificationHandler extends ChannelInboundHandlerAdapter {
    private final Collection<String> addressBlacklist;
    private final Networking networking;

    public IdentificationHandler(Networking networking) {
        this.networking = networking;
        this.addressBlacklist = networking.getClientAddressBlacklist();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        final InetAddress inetaddress = socketAddress.getAddress();
        final String ipAddress = inetaddress.getHostAddress();
        if (addressBlacklist.contains(ipAddress)) {
            ctx.close();
        }

        final Attribute<Connection> connectionKey = ctx.channel().attr(AttributeKey.valueOf("connection"));
        if (connectionKey.get() == null) {
            log.info("New connection from " + ipAddress);
            final Connection connection = new NettyConnection(ctx.channel(), inetaddress, networking.getProtocol()
                    .getDefaultClientLayer(), networking.getProtocol().getDefaultState());
            connectionKey.set(connection);
            if (CleanstoneServer.publishEvent(new ConnectionOpenEvent(connection, networking)).isCancelled()) {
                ctx.close();
                return;
            }
            ctx.channel().closeFuture().addListener((a) -> {
                log.info("Connection from " + ipAddress + " closed");
                CleanstoneServer.publishEvent(new ConnectionClosedEvent(connection, networking));
            });
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Error occurred while identifying incoming data: " + cause.getMessage());
        ctx.close();
    }
}