package rocks.cleanstone.player;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.cleanstone.core.CleanstoneServer;
import rocks.cleanstone.game.Identity;
import rocks.cleanstone.game.chat.message.Text;
import rocks.cleanstone.game.entity.Entity;
import rocks.cleanstone.net.Connection;
import rocks.cleanstone.net.packet.Packet;
import rocks.cleanstone.player.data.LevelDBPlayerDataSource;
import rocks.cleanstone.player.data.PlayerDataSource;
import rocks.cleanstone.player.event.AsyncPlayerInitializationEvent;
import rocks.cleanstone.player.event.AsyncPlayerTerminationEvent;
import rocks.cleanstone.player.event.PlayerJoinEvent;
import rocks.cleanstone.player.event.PlayerQuitEvent;

@Slf4j
@Component
public class SimplePlayerManager implements PlayerManager {

    private final Collection<Player> onlinePlayers = Sets.newConcurrentHashSet();
    private final Collection<Player> terminatingPlayers = Sets.newConcurrentHashSet();
    private final Collection<Identity> playerIDs = Sets.newConcurrentHashSet();
    private final PlayerDataSource playerDataSource;

    public SimplePlayerManager() throws IOException {
        this.playerDataSource = new LevelDBPlayerDataSource(getPlayerDataFolder());
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return ImmutableSet.copyOf(onlinePlayers);
    }

    @Override
    @Nullable
    public Player getOnlinePlayer(Identity id) {
        return onlinePlayers.stream()
                .filter(player -> player.getID().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    @Nullable
    public Player getOnlinePlayer(Connection connection) {
        return onlinePlayers.stream()
                .filter(player -> player instanceof OnlinePlayer)
                .filter(player -> ((OnlinePlayer) player).getConnection() == connection)
                .findAny().orElse(null);
    }

    @Override
    @Nullable
    public Player getOnlinePlayer(String name) {
        return onlinePlayers.stream()
                .filter(player -> player.getID().getName().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

    @Nullable
    @Override
    public Player getOnlinePlayer(Entity entity) {
        return onlinePlayers.stream()
                .filter(player -> player.getEntity() == entity)
                .findAny().orElse(null);
    }

    @Override
    public Collection<Identity> getAllPlayerIDs() {
        return ImmutableSet.copyOf(playerIDs);
    }

    @Override
    public Identity getPlayerID(UUID uuid, String accountName) {
        return playerIDs.stream().filter(id -> id.getUUID().equals(uuid)).findFirst()
                .orElse(registerNewPlayerID(uuid, accountName));
    }

    @Override
    public PlayerDataSource getPlayerDataSource() {
        return playerDataSource;
    }

    private Identity registerNewPlayerID(UUID uuid, String accountName) {
        final Identity id = new SimplePlayerIdentity(uuid, accountName);
        playerIDs.add(id);
        return id;
    }

    @Override
    public boolean isPlayerOperator(Identity playerID) {
        final List<String> ops = CleanstoneServer.getInstance().getMinecraftConfig().getOps();
        return ops.contains(playerID.getName()) || ops.contains(playerID.getUUID().toString()); //TODO: Make this beauty <3
    }

    @Override
    public void broadcastPacket(Packet packet, Player... broadcastExemptions) {
        final Collection<Player> exemptions = Arrays.asList(broadcastExemptions);
        getOnlinePlayers().stream().filter(p -> !exemptions.contains(p))
                .forEach(onlinePlayer -> onlinePlayer.sendPacket(packet));
    }

    @Override
    public void initializePlayer(Player player) {
        log.info("Initializing player");
        Preconditions.checkState(onlinePlayers.add(player),
                "Cannot initialize already initialized player " + player);
        try {
            CleanstoneServer.publishEvent(new AsyncPlayerInitializationEvent(player), true);
        } catch (Exception e) {
            log.error("Error occurred during player initialization for " + player, e);
            player.kick(Text.of("Error occurred during player initialization"));
            return;
        }
        CleanstoneServer.publishEvent(new PlayerJoinEvent(player));
    }

    @Override
    public void terminatePlayer(Player player) {
        log.info("Terminating player");
        Preconditions.checkState(terminatingPlayers.add(player),
                "Cannot terminate already terminated / non-initialized player " + player);
        CleanstoneServer.publishEvent(new PlayerQuitEvent(player));
        CleanstoneServer.publishEvent(new AsyncPlayerTerminationEvent(player));
        onlinePlayers.remove(player);
        terminatingPlayers.remove(player);
    }

    @Override
    public boolean isTerminating(Player player) {
        return terminatingPlayers.contains(player);
    }

    private Path getPlayerDataFolder() {
        final Path dataFolder = Paths.get("data", "players");
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            log.error("Cannot create data folder (no permission?)", e);
        }
        return dataFolder;
    }

    @PreDestroy
    void destroy() {
        playerDataSource.close();
    }
}
