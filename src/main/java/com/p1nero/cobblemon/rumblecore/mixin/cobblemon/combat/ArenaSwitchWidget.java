package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.SwitchActionResponse;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {BattleGUI.class, BattleSwitchPokemonSelection.class}, remap = false)
abstract class ArenaSwitchWidget {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/client/CobblemonClient;getBattle()Lcom/cobblemon/mod/common/client/battle/ClientBattle;"))
    private ClientBattle rumble$localRoster(CobblemonClient client, Operation<ClientBattle> original) {
        var roster = ArenaClient.buildingRoster();
        return roster == null ? original.call(client) : roster;
    }

    @Mixin(value = BattleSwitchPokemonSelection.class, remap = false)
    static abstract class Selection {
        @WrapOperation(method = "mousePrimaryClicked", at = @At(value = "INVOKE", target =
                "Lcom/cobblemon/mod/common/client/gui/battle/BattleGUI;selectAction(Lcom/cobblemon/mod/common/client/battle/SingleActionRequest;Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;)V"))
        private void rumble$pick(BattleGUI gui, SingleActionRequest request, ShowdownActionResponse response, Operation<Void> original) {
            if (!(response instanceof SwitchActionResponse swap) || !ArenaClient.choose(gui, swap.getNewPokemonId())) {
                original.call(gui, request, response);
            }
        }
    }
}
