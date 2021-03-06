package rocks.cleanstone.endpoint.minecraft.vanilla.net.login;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rocks.cleanstone.endpoint.minecraft.vanilla.net.packet.inbound.LoginStartPacket;
import rocks.cleanstone.net.event.InboundPacketEvent;

@Slf4j
@Component
public class LoginStartListener {
    private final LoginManager loginManager;

    @Autowired
    public LoginStartListener(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    @Async(value = "mcLoginExec")
    @EventListener
    public void onReceive(InboundPacketEvent<LoginStartPacket> event) {
        LoginStartPacket packet = event.getPacket();
        String playerName = packet.getPlayerName();
        loginManager.startLogin(event.getConnection(), playerName);
    }
}