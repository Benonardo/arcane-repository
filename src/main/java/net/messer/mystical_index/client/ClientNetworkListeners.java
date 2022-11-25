package net.messer.mystical_index.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;

public class ClientNetworkListeners {
    public static final Identifier BLOCK_PARTICLES = MysticalIndex.id("block_particles");

    public static void registerListeners() {
        ClientPlayNetworking.registerGlobalReceiver(BLOCK_PARTICLES, (client, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();
            var effect = Registries.PARTICLE_TYPE.get(buf.readIdentifier());

            client.execute(() -> {
                Particles.spawnParticlesCoveringBlock(client.world, pos, (ParticleEffect) effect,
                        UniformIntProvider.create(3, 5), 0.0);
            });
        });
    }
}
