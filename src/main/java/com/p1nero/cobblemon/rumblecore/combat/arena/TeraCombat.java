package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.yajatkaul.mega_showdown.config.MegaShowdownConfig;
import com.github.yajatkaul.mega_showdown.tag.MegaShowdownTags;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.EquipmentChecking;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Terastallization is a live RTC effect. Nothing is written to the stored Pokémon or its persistent aspects. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class TeraCombat {
    /** Mega Showdown already uses these synchronized aspects for the hat and particle layers. */
    public static final String NETWORK_ASPECT = "msd:tera_";
    public static final String BINDING = "key.cobblemon_rumble_core.tera";
    private static final ResourceLocation SOUND = ResourceLocation.parse("mega_showdown:terastallization");
    public static final ResourceLocation ORB = ResourceLocation.parse("mega_showdown:tera_orb");
    private record Active(Smash.Session session, String type) {}
    private static final Map<Pokemon, Active> ACTIVE = new IdentityHashMap<>();

    private TeraCombat() {}

    public static boolean available() { return MegaCombat.available() && MegaShowdownConfig.terastallization; }

    public static boolean isDevice(ItemStack stack) {
        return available() && (ORB.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                || stack.is(MegaShowdownTags.Items.TERA_ORB) || stack.is(MegaShowdownTags.Items.OMNI_RING));
    }

    public static boolean equipped(LivingEntity player) {
        if (!available()) return false;
        for (var stack : player.getHandSlots()) if (isDevice(stack)) return true;
        for (var stack : player.getArmorSlots()) if (isDevice(stack)) return true;
        var accessories = AccessoriesCapability.get(player);
        return accessories != null && accessories.isEquipped(TeraCombat::isDevice,
                EquipmentChecking.ACCESSORIES_ONLY);
    }

    static void use(ServerPlayer player) {
        if (RealtimeMoveWindup.locked(player)) return;
        var session = Smash.session(player);
        if (session == null || !session.pokemon.isAlive() || session.pokemon.getPokemon().isFainted() || !equipped(player)
                || PrologueTraining.restrict(player) || session.combat.lockedUntil > player.level().getGameTime()) return;
        var match = ArenaMatch.inArena(player);
        if (match != null) match.useTera(session);
        else if (canApply(session) && !GimmickCinematics.start(session, GimmickCinematics.Kind.TERASTALIZATION,
                () -> apply(session, true))) apply(session, true);
    }

    static boolean canApply(Smash.Session session) {
        var body = session.pokemon;
        var pokemon = body.getPokemon();
        return body.isAlive() && !pokemon.isFainted() && !ACTIVE.containsKey(pokemon) && pokemon.getTeraType() != null;
    }

    static boolean apply(Smash.Session session, boolean audiovisual) {
        var body = session.pokemon;
        if (!(body.level() instanceof ServerLevel level) || !body.isAlive()) return false;
        var pokemon = body.getPokemon();
        var type = pokemon.getTeraType();
        if (ACTIVE.containsKey(pokemon) || type == null) return false;
        var selected = type.showdownId().toLowerCase(Locale.ROOT);
        ACTIVE.put(pokemon, new Active(session, selected));
        sync(body, selected);
        if (audiovisual) {
            var sound = BuiltInRegistries.SOUND_EVENT.getOptional(SOUND).orElse(SoundEvents.AMETHYST_BLOCK_CHIME);
            level.playSound(null, body.blockPosition(), sound, SoundSource.PLAYERS, .8f, 1f);
            level.sendParticles(ParticleTypes.END_ROD, body.getX(), body.getY(0.55), body.getZ(), 38, .54, .45, .54, .08);
        }
        return true;
    }

    private static void sync(PokemonEntity body, String type) {
        Set<String> aspects = new HashSet<>(body.getPokemon().getAspects());
        if (type != null) aspects.add(NETWORK_ASPECT + type);
        body.getEntityData().set(PokemonEntity.getASPECTS(), Set.copyOf(aspects));
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PokemonEntity body) || body.level().isClientSide()) return;
        var active = ACTIVE.get(body.getPokemon());
        if (active == null || active.session().pokemon != body) return;
        if (Smash.session(active.session().player) != active.session() || !body.isAlive()) clear(active.session());
        else if (!body.getAspects().contains(NETWORK_ASPECT + active.type())) sync(body, active.type());
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity body) || event.getLevel().isClientSide()) return;
        var active = ACTIVE.get(body.getPokemon());
        if (active != null && active.session().pokemon == body) clear(active.session());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { ACTIVE.clear(); }

    public static void clear(Smash.Session session) {
        if (session == null) return;
        var body = session.pokemon;
        var active = ACTIVE.get(body.getPokemon());
        if (active != null && active.session() == session) {
            ACTIVE.remove(body.getPokemon());
            if (!body.isRemoved()) sync(body, null);
        }
    }

    public static List<String> defensiveTypes(Pokemon pokemon, List<String> original) {
        if (pokemon == null) return original;
        var active = ACTIVE.get(pokemon);
        return active == null || active.type().equals("stellar") ? original : List.of(active.type());
    }

    public static boolean hasDefensiveType(Pokemon pokemon, String type, boolean original) {
        var active = ACTIVE.get(pokemon);
        return active == null || active.type().equals("stellar") ? original : active.type().equals(type);
    }

    public static boolean stab(Pokemon pokemon, String type, boolean original) {
        var active = ACTIVE.get(pokemon);
        return original || active != null && active.type().equals(type);
    }

    public static float boost(Pokemon pokemon, String moveType) {
        var active = ACTIVE.get(pokemon);
        if (active == null || !active.type().equals(moveType)) return 1;
        // RTC already applies 1.5× STAB. Matching both original and Tera type raises that to 2×.
        return Smash.hasType(pokemon, moveType) ? 2f / 1.5f : 1f;
    }
}
