package rocks.cleanstone.game.world.chunk.data.block;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import rocks.cleanstone.game.block.Block;
import rocks.cleanstone.game.block.CachingImmutableBlockProvider;
import rocks.cleanstone.game.block.state.CachingBlockStateProvider;
import rocks.cleanstone.game.material.MaterialRegistry;
import rocks.cleanstone.game.material.SimpleMaterialRegistry;
import rocks.cleanstone.game.world.chunk.ArrayBlockDataTable;
import rocks.cleanstone.game.world.chunk.BlockDataTable;
import rocks.cleanstone.game.world.chunk.data.block.vanilla.DirectPalette;
import rocks.cleanstone.game.world.chunk.data.block.vanilla.SimpleVanillaBlockDataStorageFactory;
import rocks.cleanstone.game.world.chunk.data.block.vanilla.VanillaBlockDataCodec;
import rocks.cleanstone.game.world.chunk.data.block.vanilla.VanillaBlockDataStorage;
import rocks.cleanstone.net.minecraft.protocol.v1_13.ProtocolBlockStateMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaBlockDataStorageTest {

    private Random random;
    private DirectPalette directPalette;
    private VanillaBlockDataStorage storage;
    private SimpleVanillaBlockDataStorageFactory simpleVanillaBlockDataStorageFactory;

    @BeforeEach
    void createStorageByTable() {
        CachingBlockStateProvider blockStateProvider = new CachingBlockStateProvider();
        CachingImmutableBlockProvider immutableBlockProvider = new CachingImmutableBlockProvider();
        simpleVanillaBlockDataStorageFactory = new SimpleVanillaBlockDataStorageFactory();

        random = new Random(1);
        directPalette = new DirectPalette(new ProtocolBlockStateMapping(), 14);
        MaterialRegistry materialRegistry = new SimpleMaterialRegistry();

        BlockDataTable blockDataTable = new ArrayBlockDataTable(true);
        for (int i = 0; i < 20; i++) {
            Block randomBlock = immutableBlockProvider.of(
                    new ArrayList<>(materialRegistry.getBlockTypes())
                            .get(random.nextInt(materialRegistry.getBlockTypes().size())));
            blockDataTable.setBlock(random.nextInt(16), random.nextInt(256), random.nextInt(16), randomBlock);
        }
        storage = new VanillaBlockDataStorage(blockDataTable, directPalette, true);
    }

    @Disabled
    @Test
    void testSerializationAndTable() {
        VanillaBlockDataCodec codec = new VanillaBlockDataCodec(simpleVanillaBlockDataStorageFactory, directPalette, true);
        ByteBuf serialized = codec.serialize(storage);
        VanillaBlockDataStorage deserialized;
        try {
            deserialized = codec.deserialize(serialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals(storage.constructTable(), deserialized.constructTable());
        serialized.release();
    }
}