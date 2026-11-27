package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import com.johnn.realtimecobblemon.moves.ZMoves;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Optional integration check against the live item tags and RTC move registry. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class FrontierPracticeAudit {
    @SubscribeEvent
    public static void check(ServerStartedEvent event) {
        if (!Boolean.getBoolean("rumble.auditArenaRosters")) return;
        var charizard = FrontierRoster.species(ResourceLocation.parse("cobblemon:charizard"));
        var draft = new ArrayList<FrontierRoster.Pick>();
        for (int i = 0; i < FrontierRoster.TEAM_SIZE; i++) {
            var pick = FrontierBotDraft.pick(draft, FrontierRoster.initialPick(charizard));
            if (pick == null) throw new IllegalStateException("Frontier practice draft could not fill a slot");
            draft.add(pick);
        }
        if (FrontierRoster.create(draft).size() != FrontierRoster.TEAM_SIZE)
            throw new IllegalStateException("Frontier practice draft did not produce a legal team");

        var ogerpon = FrontierRoster.species(ResourceLocation.parse("cobblemon:ogerpon"));
        var held = FrontierRoster.heldItems();
        var fireStone = ResourceLocation.parse("cobblemon:fire_stone");
        var evolutionPick = new FrontierRoster.Pick(charizard.getResourceIdentifier(), fireStone, List.of("flamethrower", "", "", ""));
        if (held.contains(fireStone) || !FrontierRoster.NONE.equals(FrontierRoster.withoutUnsupportedHeld(evolutionPick).held())
                || FrontierRoster.validate(List.of(evolutionPick, FrontierRoster.EMPTY, FrontierRoster.EMPTY), true) != FrontierRoster.Issue.HELD_ITEM)
            throw new IllegalStateException("Frontier must filter evolution-only items and clear them from legacy presets");
        if (held.stream().anyMatch(id -> id.getNamespace().equals("mega_showdown"))) {
            var fireCrystal = ResourceLocation.parse("mega_showdown:firium_z");
            var zPick = new FrontierRoster.Pick(charizard.getResourceIdentifier(), fireCrystal, List.of("airslash", "flamethrower", "", ""));
            var zUser = FrontierRoster.create(List.of(zPick, FrontierRoster.EMPTY, FrontierRoster.EMPTY)).getFirst();
            var choice = ZMoves.choose(zUser);
            if (!held.contains(fireCrystal) || choice == null || !"fire".equals(choice.base().type())
                    || held.contains(ResourceLocation.parse("mega_showdown:mimikium_z")))
                throw new IllegalStateException("Frontier Z-Crystal filter or RTC Z-Move selection is invalid");
        }
        if (ogerpon != null && held.stream().anyMatch(id -> id.getNamespace().equals("mega_showdown"))) {
            var base = FrontierRoster.initialPick(ogerpon);
            for (var mask : List.of("wellspring_mask", "hearthflame_mask", "cornerstone_mask")) {
                var id = ResourceLocation.fromNamespaceAndPath("mega_showdown", mask);
                if (!held.contains(id) || FrontierRoster.heldGroup(id) != FrontierRoster.HeldGroup.FORM)
                    throw new IllegalStateException("Missing Frontier form item: " + id);
                var mon = FrontierRoster.create(List.of(new FrontierRoster.Pick(base.species(), id, base.moves()),
                        FrontierRoster.EMPTY, FrontierRoster.EMPTY)).getFirst();
                if (mon.getForm() == ogerpon.getStandardForm())
                    throw new IllegalStateException("Frontier held item failed to change Ogerpon's form: " + id);
            }
        }
        org.slf4j.LoggerFactory.getLogger(FrontierPracticeAudit.class)
                .info("[FrontierPracticeAudit] Draft and held items verified ({} available items)", held.size());
    }
}
