package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.yajatkaul.mega_showdown.config.MegaShowdownConfig;
import com.github.yajatkaul.mega_showdown.gimmick.MegaGimmick;
import com.github.yajatkaul.mega_showdown.tag.MegaShowdownTags;
import com.github.yajatkaul.mega_showdown.utils.AccessoriesUtils;
import com.johnn.realtimecobblemon.moves.Looks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class MegaCombat {
    private MegaCombat() {}

    public static boolean available() { return ModList.get().isLoaded("mega_showdown"); }

    public static boolean isDevice(ItemStack stack) {
        return available() && (stack.is(MegaShowdownTags.Items.MEGA_BRACELET) || stack.is(MegaShowdownTags.Items.OMNI_RING));
    }

    public static boolean equipped(ServerPlayer player) {
        return available() && MegaShowdownConfig.mega
                && (isDevice(player.getMainHandItem()) || isDevice(player.getOffhandItem())
                || AccessoriesUtils.checkTagInAccessories(player, MegaShowdownTags.Items.MEGA_BRACELET)
                || AccessoriesUtils.checkTagInAccessories(player, MegaShowdownTags.Items.OMNI_RING));
    }

    public static boolean hasStone(Pokemon pokemon) {
        return available() && pokemon.heldItem().is(MegaShowdownTags.Items.MEGA_STONE) && canMega(pokemon);
    }

    public static boolean canMega(Pokemon pokemon) {
        if (!available() || !MegaShowdownConfig.mega || MegaGimmick.isMega(pokemon)) return false;
        // Mega Showdown assumes four occupied move slots for Rayquaza.
        if (pokemon.getSpecies().getName().equals("Rayquaza")) {
            return pokemon.getMoveSet().getMoves().stream().anyMatch(move -> move.getName().equals("dragonascent"));
        }
        return MegaGimmick.canMega(pokemon);
    }

    static void begin(ServerLevel level, PokemonEntity body) {
        RealtimeProtection.beginMega(body);
        Looks.mega(level, body, body.getLookAngle(), RealtimeProtection.MEGA_TICKS);
    }

    static boolean evolve(PokemonEntity body) {
        RealtimeProtection.endMega(body);
        var pokemon = body.getPokemon();
        if (!canMega(pokemon)) return false;
        var previousForm = pokemon.getForm();
        float hp = body.getHealth() / body.getMaxHealth();
        MegaGimmick.megaEvolve(pokemon);
        if (pokemon.getForm() != previousForm && MegaGimmick.isMega(pokemon)) {
            body.setHealth(Math.max(0.01F, hp * body.getMaxHealth()));
            pokemon.setCurrentHealth(Math.max(1, Math.round(hp * pokemon.getMaxHealth())));
            return true;
        }
        MegaGimmick.unmegaEvolve(pokemon);
        return false;
    }

    static float healing(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("cobblemon")) return 0;
        return switch (id.getPath()) {
            case "oran_berry" -> 10;
            case "sitrus_berry" -> -0.25F;
            default -> 0;
        };
    }

    static boolean heal(PokemonEntity body) {
        var pokemon = body.getPokemon();
        float amount = healing(pokemon.heldItem());
        if (amount == 0 || !body.isAlive() || body.getHealth() >= body.getMaxHealth()) return false;
        if (amount < 0) amount *= -body.getMaxHealth();
        pokemon.setHeldItem$common(ItemStack.EMPTY);
        body.heal(amount);
        pokemon.setCurrentHealth(Math.max(1, Math.round(body.getHealth() * pokemon.getMaxHealth() / body.getMaxHealth())));
        body.level().playSound(null, body.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.7F, 1.2F);
        return true;
    }
}
