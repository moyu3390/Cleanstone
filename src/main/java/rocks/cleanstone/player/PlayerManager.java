package rocks.cleanstone.player;

import java.util.Collection;
import java.util.UUID;

import rocks.cleanstone.io.data.InGamePlayerDataRepository;

public interface PlayerManager {

    Collection<Player> getOnlinePlayers();

    Player getOnlinePlayer(PlayerID id);

    Collection<PlayerID> getAllPlayerIDs();

    InGamePlayerDataRepository getPlayerDataContainer(PlayerID id);

    PlayerID getPlayerID(UUID uuid, String accountName);

    void initializePlayer(Player player);

    void terminatePlayer(Player player);
}