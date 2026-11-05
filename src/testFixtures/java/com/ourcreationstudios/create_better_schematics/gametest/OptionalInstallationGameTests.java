package com.ourcreationstudios.create_better_schematics.gametest;

import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ourcreationstudios.create_better_schematics.CreateBetterSchematics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

@GameTestHolder(CreateBetterSchematics.MODID)
@PrefixGameTestTemplate(false)
public final class OptionalInstallationGameTests {
    @GameTest(template = "empty")
    @SuppressWarnings("unchecked")
    public static void realPayloadNegotiationAcceptsPeersWithoutTheMod(GameTestHelper helper) throws Exception {
        var field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
        field.setAccessible(true);
        var registrations = (Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>>) field.get(null);
        int addonChannels = 0;
        for (var entry : registrations.entrySet()) {
            List<NegotiableNetworkComponent> installed = entry.getValue().values().stream()
                    .map(NegotiableNetworkComponent::new).toList();
            List<NegotiableNetworkComponent> absent = installed.stream()
                    .filter(channel -> !channel.id().getNamespace().equals(CreateBetterSchematics.MODID)).toList();
            addonChannels += installed.size() - absent.size();
            var clientOnly = NetworkComponentNegotiator.negotiate(absent, installed);
            var serverOnly = NetworkComponentNegotiator.negotiate(installed, absent);
            var both = NetworkComponentNegotiator.negotiate(installed, installed);
            helper.assertTrue(clientOnly.success() && serverOnly.success(),
                    "The actual " + entry.getKey() + " handshake must accept either peer without Better Schematics");
            helper.assertTrue(both.success() && both.components().size() == installed.size(),
                    "Installing both sides still negotiates every cannon channel");
        }
        helper.assertTrue(addonChannels == 5, "The test must exercise skip-picker and JEI material payloads");
        helper.assertTrue(BuiltInRegistries.STRUCTURE_PROCESSOR.keySet().stream()
                        .noneMatch(id -> id.getNamespace().equals(CreateBetterSchematics.MODID)),
                "Installing the mod must not add a processor registry entry required by the other side");
        Path states = Path.of("../build/shareable-fixtures/block-state-ids.txt");
        Files.createDirectories(states.getParent());
        Files.writeString(states, java.util.stream.IntStream.range(0, Block.BLOCK_STATE_REGISTRY.size())
                .mapToObj(id -> id + "=" + Block.BLOCK_STATE_REGISTRY.byId(id))
                .collect(java.util.stream.Collectors.joining("\n")));
        helper.succeed();
    }
}
