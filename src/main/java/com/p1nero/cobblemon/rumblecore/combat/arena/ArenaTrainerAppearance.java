package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.entity.GymLeaderEntity;
import com.nore.cobblebash.entity.GymTrainerEntity;
import com.p1nero.cobblemon.rumblecore.mixin.rct.TrainerAppearanceAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.level.Level;

/** Carries the source trainer's appearance on Cobblemon's already synchronized NPC aspects. */
public record ArenaTrainerAppearance(String aspect) {
    private static final String PREFIX = "rumble-simulator-visual/";

    public static ArenaTrainerAppearance capture(LivingEntity source) {
        if (source instanceof TrainerMob trainer) {
            return new ArenaTrainerAppearance(PREFIX + "road/" + trainer.getTrainerId());
        }
        if (source instanceof GymTrainerEntity trainer) {
            String kind = trainer instanceof GymLeaderEntity ? "leader" : "trainer";
            return new ArenaTrainerAppearance(PREFIX + kind + "/" + trainer.modelVariant() + "/" + trainer.textureVariant());
        }
        if (source instanceof Villager villager) {
            var data = villager.getVillagerData();
            return new ArenaTrainerAppearance(PREFIX + "villager/" + BuiltInRegistries.VILLAGER_TYPE.getKey(data.getType())
                    + "/" + BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.getProfession()) + "/" + data.getLevel());
        }
        return new ArenaTrainerAppearance("");
    }

    public void applyTo(NPCEntity npc) {
        if (aspect.isEmpty()) return;
        npc.getAppliedAspects().add(aspect);
        npc.updateAspects();
    }

    public static String getAspect(NPCEntity npc) {
        return npc.getAspects().stream().filter(aspect -> aspect.startsWith(PREFIX)).findFirst().orElse("");
    }

    public static LivingEntity createDisplay(Level level, String aspect) {
        if (!aspect.startsWith(PREFIX)) return null;
        String data = aspect.substring(PREFIX.length());
        if (data.startsWith("road/") && level.isClientSide()) {
            var trainer = TrainerMob.getEntityType().create(level);
            if (trainer != null) {
                // Only the detached render entity needs the ID. The public setter registers a server trainer.
                trainer.getEntityData().set(TrainerAppearanceAccess.rumble$trainerId(), data.substring("road/".length()));
            }
            return trainer;
        }
        String[] fields = data.split("/");
        try {
            if (fields.length == 3 && (fields[0].equals("leader") || fields[0].equals("trainer"))) {
                var type = fields[0].equals("leader") ? CobbleBash.GYM_LEADER.get() : CobbleBash.GYM_TRAINER.get();
                var trainer = type.create(level);
                if (trainer != null) trainer.setVisual(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
                return trainer;
            }
            if (fields.length == 4 && fields[0].equals("villager")) {
                var villager = EntityType.VILLAGER.create(level);
                if (villager != null) villager.setVillagerData(new VillagerData(
                        BuiltInRegistries.VILLAGER_TYPE.get(ResourceLocation.parse(fields[1])),
                        BuiltInRegistries.VILLAGER_PROFESSION.get(ResourceLocation.parse(fields[2])), Integer.parseInt(fields[3])));
                return villager;
            }
        } catch (IllegalArgumentException ignored) {
            // An invalid appearance falls back to the normal Cobblemon renderer.
        }
        return null;
    }
}
