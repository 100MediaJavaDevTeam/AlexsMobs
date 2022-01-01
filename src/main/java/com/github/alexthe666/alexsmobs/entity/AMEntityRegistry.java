package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.google.common.base.Predicates;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = AlexsMobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AMEntityRegistry {

    public static final EntityType<EntityGrizzlyBear> GRIZZLY_BEAR = registerEntity(EntityType.Builder.of(EntityGrizzlyBear::new, EntityClassification.CREATURE).sized(1.6F, 1.8F), "grizzly_bear");
    public static final EntityType<EntityRoadrunner> ROADRUNNER = registerEntity(EntityType.Builder.of(EntityRoadrunner::new, EntityClassification.CREATURE).sized(0.45F, 0.75F), "roadrunner");
    public static final EntityType<EntityBoneSerpent> BONE_SERPENT = registerEntity(EntityType.Builder.of(EntityBoneSerpent::new, EntityClassification.MONSTER).sized(1.2F, 1.15F).fireImmune(), "bone_serpent");
    public static final EntityType<EntityBoneSerpentPart> BONE_SERPENT_PART = registerEntity(EntityType.Builder.of(EntityBoneSerpentPart::new, EntityClassification.MONSTER).sized(1F, 1F).fireImmune(), "bone_serpent_part");
    public static final EntityType<EntityGazelle> GAZELLE = registerEntity(EntityType.Builder.of(EntityGazelle::new, EntityClassification.CREATURE).sized(0.85F, 1.25F), "gazelle");
    public static final EntityType<EntityCrocodile> CROCODILE = registerEntity(EntityType.Builder.of(EntityCrocodile::new, EntityClassification.WATER_CREATURE).sized(2.15F, 0.75F), "crocodile");
    public static final EntityType<EntityFly> FLY = registerEntity(EntityType.Builder.of(EntityFly::new, EntityClassification.AMBIENT).sized(0.35F, 0.35F), "fly");
    public static final EntityType<EntityHummingbird> HUMMINGBIRD = registerEntity(EntityType.Builder.of(EntityHummingbird::new, EntityClassification.CREATURE).sized(0.45F, 0.45F), "hummingbird");
    public static final EntityType<EntityOrca> ORCA = registerEntity(EntityType.Builder.of(EntityOrca::new, EntityClassification.WATER_CREATURE).sized(3.75F, 1.75F), "orca");
    public static final EntityType<EntitySunbird> SUNBIRD = registerEntity(EntityType.Builder.of(EntitySunbird::new, EntityClassification.CREATURE).sized(1.75F, 0.75F).fireImmune().setTrackingRange(10).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "sunbird");
    public static final EntityType<EntityGorilla> GORILLA = registerEntity(EntityType.Builder.of(EntityGorilla::new, EntityClassification.CREATURE).sized(1.15F, 1.35F), "gorilla");
    public static final EntityType<EntityCrimsonMosquito> CRIMSON_MOSQUITO = registerEntity(EntityType.Builder.of(EntityCrimsonMosquito::new, EntityClassification.MONSTER).sized(1.25F, 1.15F).fireImmune(), "crimson_mosquito");
    public static final EntityType<EntityMosquitoSpit> MOSQUITO_SPIT = registerEntity(EntityType.Builder.of(EntityMosquitoSpit::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityMosquitoSpit::new).fireImmune(), "mosquito_spit");
    public static final EntityType<EntityRattlesnake> RATTLESNAKE = registerEntity(EntityType.Builder.of(EntityRattlesnake::new, EntityClassification.CREATURE).sized(0.95F, 0.35F), "rattlesnake");
    public static final EntityType<EntityEndergrade> ENDERGRADE = registerEntity(EntityType.Builder.of(EntityEndergrade::new, EntityClassification.CREATURE).sized(0.95F, 0.85F), "endergrade");
    public static final EntityType<EntityHammerheadShark> HAMMERHEAD_SHARK = registerEntity(EntityType.Builder.of(EntityHammerheadShark::new, EntityClassification.WATER_CREATURE).sized(2.4F, 1.25F), "hammerhead_shark");
    public static final EntityType<EntitySharkToothArrow> SHARK_TOOTH_ARROW = registerEntity(EntityType.Builder.of(EntitySharkToothArrow::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntitySharkToothArrow::new), "shark_tooth_arrow");
    public static final EntityType<EntityLobster> LOBSTER = registerEntity(EntityType.Builder.of(EntityLobster::new, EntityClassification.WATER_AMBIENT).sized(0.7F, 0.4F), "lobster");
    public static final EntityType<EntityKomodoDragon> KOMODO_DRAGON = registerEntity(EntityType.Builder.of(EntityKomodoDragon::new, EntityClassification.CREATURE).sized(2.15F, 0.75F), "komodo_dragon");
    public static final EntityType<EntityCapuchinMonkey> CAPUCHIN_MONKEY = registerEntity(EntityType.Builder.of(EntityCapuchinMonkey::new, EntityClassification.CREATURE).sized(0.65F, 0.75F), "capuchin_monkey");
    public static final EntityType<EntityTossedItem> TOSSED_ITEM = registerEntity(EntityType.Builder.of(EntityTossedItem::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityTossedItem::new).fireImmune(), "tossed_item");
    public static final EntityType<EntityCentipedeHead> CENTIPEDE_HEAD = registerEntity(EntityType.Builder.of(EntityCentipedeHead::new, EntityClassification.CREATURE).sized(0.9F, 0.9F), "centipede_head");
    public static final EntityType<EntityCentipedeBody> CENTIPEDE_BODY = registerEntity(EntityType.Builder.of(EntityCentipedeBody::new, EntityClassification.CREATURE).sized(0.9F, 0.9F).fireImmune(), "centipede_body");
    public static final EntityType<EntityCentipedeTail> CENTIPEDE_TAIL = registerEntity(EntityType.Builder.of(EntityCentipedeTail::new, EntityClassification.CREATURE).sized(0.9F, 0.9F).fireImmune(), "centipede_tail");
    public static final EntityType<EntityWarpedToad> WARPED_TOAD = registerEntity(EntityType.Builder.of(EntityWarpedToad::new, EntityClassification.CREATURE).sized(0.9F, 1.4F).fireImmune().setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "warped_toad");
    public static final EntityType<EntityMoose> MOOSE = registerEntity(EntityType.Builder.of(EntityMoose::new, EntityClassification.CREATURE).sized(1.7F, 2.4F), "moose");
    public static final EntityType<EntityMimicube> MIMICUBE = registerEntity(EntityType.Builder.of(EntityMimicube::new, EntityClassification.MONSTER).sized(0.9F, 0.9F), "mimicube");
    public static final EntityType<EntityRaccoon> RACCOON = registerEntity(EntityType.Builder.of(EntityRaccoon::new, EntityClassification.CREATURE).sized(0.8F, 0.9F), "raccoon");
    public static final EntityType<EntityBlobfish> BLOBFISH = registerEntity(EntityType.Builder.of(EntityBlobfish::new, EntityClassification.WATER_AMBIENT).sized(0.7F, 0.45F), "blobfish");
    public static final EntityType<EntitySeal> SEAL = registerEntity(EntityType.Builder.of(EntitySeal::new, EntityClassification.CREATURE).sized(1.3F, 0.7F), "seal");
    public static final EntityType<EntityCockroach> COCKROACH = registerEntity(EntityType.Builder.of(EntityCockroach::new, EntityClassification.AMBIENT).sized(0.7F, 0.3F), "cockroach");
    public static final EntityType<EntityCockroachEgg> COCKROACH_EGG = registerEntity(EntityType.Builder.of(EntityCockroachEgg::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityCockroachEgg::new).fireImmune(), "cockroach_egg");
    public static final EntityType<EntityShoebill> SHOEBILL = registerEntity(EntityType.Builder.of(EntityShoebill::new, EntityClassification.CREATURE).sized(0.8F, 1.5F).setUpdateInterval(1), "shoebill");
    public static final EntityType<EntityElephant> ELEPHANT = registerEntity(EntityType.Builder.of(EntityElephant::new, EntityClassification.CREATURE).sized(2.1F, 2.5F).setUpdateInterval(1), "elephant");
    public static final EntityType<EntitySoulVulture> SOUL_VULTURE = registerEntity(EntityType.Builder.of(EntitySoulVulture::new, EntityClassification.MONSTER).sized(0.9F, 1.3F).setUpdateInterval(1).fireImmune(), "soul_vulture");
    public static final EntityType<EntitySnowLeopard> SNOW_LEOPARD = registerEntity(EntityType.Builder.of(EntitySnowLeopard::new, EntityClassification.CREATURE).sized(1.2F, 1.3F), "snow_leopard");
    public static final EntityType<EntitySpectre> SPECTRE = registerEntity(EntityType.Builder.of(EntitySpectre::new, EntityClassification.CREATURE).sized(3.15F, 0.8F).fireImmune().setTrackingRange(10).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "spectre");
    public static final EntityType<EntityCrow> CROW = registerEntity(EntityType.Builder.of(EntityCrow::new, EntityClassification.CREATURE).sized(0.45F, 0.45F), "crow");
    public static final EntityType<EntityAlligatorSnappingTurtle> ALLIGATOR_SNAPPING_TURTLE = registerEntity(EntityType.Builder.of(EntityAlligatorSnappingTurtle::new, EntityClassification.CREATURE).sized(1.25F, 0.65F), "alligator_snapping_turtle");
    public static final EntityType<EntityMungus> MUNGUS = registerEntity(EntityType.Builder.of(EntityMungus::new, EntityClassification.CREATURE).sized(0.75F, 1.45F), "mungus");
    public static final EntityType<EntityMantisShrimp> MANTIS_SHRIMP = registerEntity(EntityType.Builder.of(EntityMantisShrimp::new, EntityClassification.WATER_CREATURE).sized(1.25F, 1.2F), "mantis_shrimp");
    public static final EntityType<EntityGuster> GUSTER = registerEntity(EntityType.Builder.of(EntityGuster::new, EntityClassification.MONSTER).sized(1.42F, 2.35F).fireImmune(), "guster");
    public static final EntityType<EntitySandShot> SAND_SHOT = registerEntity(EntityType.Builder.of(EntitySandShot::new, EntityClassification.MISC).sized(0.95F, 0.65F).setCustomClientFactory(EntitySandShot::new).fireImmune(), "sand_shot");
    public static final EntityType<EntityGust> GUST = registerEntity(EntityType.Builder.of(EntityGust::new, EntityClassification.MISC).sized(0.8F, 0.8F).setCustomClientFactory(EntityGust::new).fireImmune(), "gust");
    public static final EntityType<EntityWarpedMosco> WARPED_MOSCO = registerEntity(EntityType.Builder.of(EntityWarpedMosco::new, EntityClassification.MONSTER).sized(1.99F, 3.25F).fireImmune(), "warped_mosco");
    public static final EntityType<EntityHemolymph> HEMOLYMPH = registerEntity(EntityType.Builder.of(EntityHemolymph::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityHemolymph::new).fireImmune(), "hemolymph");
    public static final EntityType<EntityStraddler> STRADDLER = registerEntity(EntityType.Builder.of(EntityStraddler::new, EntityClassification.MONSTER).sized(1.65F, 3F).fireImmune(), "straddler");
    public static final EntityType<EntityStradpole> STRADPOLE = registerEntity(EntityType.Builder.of(EntityStradpole::new, EntityClassification.WATER_AMBIENT).sized(0.5F, 0.5F).fireImmune(), "stradpole");
    public static final EntityType<EntityStraddleboard> STRADDLEBOARD = registerEntity(EntityType.Builder.of(EntityStraddleboard::new, EntityClassification.MISC).sized(1.5F, 0.35F).setCustomClientFactory(EntityStraddleboard::new).fireImmune(), "straddleboard");
    public static final EntityType<EntityEmu> EMU = registerEntity(EntityType.Builder.of(EntityEmu::new, EntityClassification.CREATURE).sized(1.1F, 1.8F), "emu");
    public static final EntityType<EntityEmuEgg> EMU_EGG = registerEntity(EntityType.Builder.of(EntityEmuEgg::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityEmuEgg::new).fireImmune(), "emu_egg");
    public static final EntityType<EntityPlatypus> PLATYPUS = registerEntity(EntityType.Builder.of(EntityPlatypus::new, EntityClassification.CREATURE).sized(0.8F, 0.5F), "platypus");
    public static final EntityType<EntityDropBear> DROPBEAR = registerEntity(EntityType.Builder.of(EntityDropBear::new, EntityClassification.MONSTER).sized(1.65F, 1.5F).fireImmune(), "dropbear");
    public static final EntityType<EntityTasmanianDevil> TASMANIAN_DEVIL = registerEntity(EntityType.Builder.of(EntityTasmanianDevil::new, EntityClassification.CREATURE).sized(0.7F, 0.8F), "tasmanian_devil");
    public static final EntityType<EntityKangaroo> KANGAROO = registerEntity(EntityType.Builder.of(EntityKangaroo::new, EntityClassification.CREATURE).sized(1.65F, 1.5F), "kangaroo");
    public static final EntityType<EntityCachalotWhale> CACHALOT_WHALE = registerEntity(EntityType.Builder.of(EntityCachalotWhale::new, EntityClassification.WATER_CREATURE).sized(9F, 4.0F), "cachalot_whale");
    public static final EntityType<EntityCachalotEcho> CACHALOT_ECHO = registerEntity(EntityType.Builder.of(EntityCachalotEcho::new, EntityClassification.MISC).sized(2F, 2F).setCustomClientFactory(EntityCachalotEcho::new).fireImmune(), "cachalot_echo");
    public static final EntityType<EntityLeafcutterAnt> LEAFCUTTER_ANT = registerEntity(EntityType.Builder.of(EntityLeafcutterAnt::new, EntityClassification.CREATURE).sized(0.8F, 0.5F), "leafcutter_ant");
    public static final EntityType<EntityEnderiophage> ENDERIOPHAGE = registerEntity(EntityType.Builder.of(EntityEnderiophage::new, EntityClassification.CREATURE).sized(0.85F, 1.95F).setUpdateInterval(1), "enderiophage");
    public static final EntityType<EntityEnderiophageRocket> ENDERIOPHAGE_ROCKET = registerEntity(EntityType.Builder.of(EntityEnderiophageRocket::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityEnderiophageRocket::new).fireImmune(), "enderiophage_rocket");
    public static final EntityType<EntityBaldEagle> BALD_EAGLE = registerEntity(EntityType.Builder.of(EntityBaldEagle::new, EntityClassification.CREATURE).sized(0.5F, 0.95F).setUpdateInterval(1).setTrackingRange(14), "bald_eagle");
    public static final EntityType<EntityTiger> TIGER = registerEntity(EntityType.Builder.of(EntityTiger::new, EntityClassification.CREATURE).sized(1.45F, 1.2F), "tiger");
    public static final EntityType<EntityTarantulaHawk> TARANTULA_HAWK = registerEntity(EntityType.Builder.of(EntityTarantulaHawk::new, EntityClassification.CREATURE).sized(1.2F, 0.9F), "tarantula_hawk");
    public static final EntityType<EntityVoidWorm> VOID_WORM = registerEntity(EntityType.Builder.of(EntityVoidWorm::new, EntityClassification.MONSTER).sized(3.4F, 3F).fireImmune().setTrackingRange(20).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "void_worm");
    public static final EntityType<EntityVoidWormPart> VOID_WORM_PART = registerEntity(EntityType.Builder.of(EntityVoidWormPart::new, EntityClassification.MONSTER).sized(1.2F, 1.35F).fireImmune().setTrackingRange(20).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "void_worm_part");
    public static final EntityType<EntityVoidWormShot> VOID_WORM_SHOT = registerEntity(EntityType.Builder.of(EntityVoidWormShot::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityVoidWormShot::new).fireImmune(), "void_worm_shot");
    public static final EntityType<EntityVoidPortal> VOID_PORTAL = registerEntity(EntityType.Builder.of(EntityVoidPortal::new, EntityClassification.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityVoidPortal::new).fireImmune(), "void_portal");
    public static final EntityType<EntityFrilledShark> FRILLED_SHARK = registerEntity(EntityType.Builder.of(EntityFrilledShark::new, EntityClassification.WATER_CREATURE).sized(1.3F, 0.4F), "frilled_shark");
    public static final EntityType<EntityMimicOctopus> MIMIC_OCTOPUS = registerEntity(EntityType.Builder.of(EntityMimicOctopus::new, EntityClassification.WATER_CREATURE).sized(0.9F, 0.6F), "mimic_octopus");
    public static final EntityType<EntitySeagull> SEAGULL = registerEntity(EntityType.Builder.of(EntitySeagull::new, EntityClassification.CREATURE).sized(0.45F, 0.45F), "seagull");

    private static final EntityType registerEntity(EntityType.Builder builder, String entityName) {
        ResourceLocation nameLoc = new ResourceLocation(AlexsMobs.MODID, entityName);
        return (EntityType) builder.build(entityName).setRegistryName(nameLoc);
    }

    static {
        EntitySpawnPlacementRegistry.register(GRIZZLY_BEAR, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(ROADRUNNER, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRoadrunner::canRoadrunnerSpawn);
        EntitySpawnPlacementRegistry.register(BONE_SERPENT, EntitySpawnPlacementRegistry.PlacementType.IN_LAVA, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBoneSerpent::canBoneSerpentSpawn);
        EntitySpawnPlacementRegistry.register(GAZELLE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(CROCODILE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCrocodile::canCrocodileSpawn);
        EntitySpawnPlacementRegistry.register(FLY, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFly::canFlySpawn);
        EntitySpawnPlacementRegistry.register(HUMMINGBIRD, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityHummingbird::canHummingbirdSpawn);
        EntitySpawnPlacementRegistry.register(ORCA, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityOrca::canOrcaSpawn);
        EntitySpawnPlacementRegistry.register(SUNBIRD, EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySunbird::canSunbirdSpawn);
        EntitySpawnPlacementRegistry.register(GORILLA, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityGorilla::canGorillaSpawn);
        EntitySpawnPlacementRegistry.register(CRIMSON_MOSQUITO, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCrimsonMosquito::canMosquitoSpawn);
        EntitySpawnPlacementRegistry.register(RATTLESNAKE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRattlesnake::canRattlesnakeSpawn);
        EntitySpawnPlacementRegistry.register(ENDERGRADE, EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEndergrade::canEndergradeSpawn);
        EntitySpawnPlacementRegistry.register(HAMMERHEAD_SHARK, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityHammerheadShark::canHammerheadSharkSpawn);
        EntitySpawnPlacementRegistry.register(LOBSTER, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityLobster::canLobsterSpawn);
        EntitySpawnPlacementRegistry.register(KOMODO_DRAGON, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityKomodoDragon::canKomodoDragonSpawn);
        EntitySpawnPlacementRegistry.register(CAPUCHIN_MONKEY, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityCapuchinMonkey::canCapuchinSpawn);
        EntitySpawnPlacementRegistry.register(CENTIPEDE_HEAD, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCentipedeHead::canCentipedeSpawn);
        EntitySpawnPlacementRegistry.register(WARPED_TOAD, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityWarpedToad::canWarpedToadSpawn);
        EntitySpawnPlacementRegistry.register(MOOSE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMoose::canMooseSpawn);
        EntitySpawnPlacementRegistry.register(MIMICUBE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobEntity::checkMobSpawnRules);
        EntitySpawnPlacementRegistry.register(RACCOON, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(BLOBFISH, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBlobfish::canBlobfishSpawn);
        EntitySpawnPlacementRegistry.register(SEAL, EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySeal::canSealSpawn);
        EntitySpawnPlacementRegistry.register(COCKROACH, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCockroach::canCockroachSpawn);
        EntitySpawnPlacementRegistry.register(SHOEBILL, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(ELEPHANT, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(SOUL_VULTURE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySoulVulture::canVultureSpawn);
        EntitySpawnPlacementRegistry.register(ALLIGATOR_SNAPPING_TURTLE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAlligatorSnappingTurtle::canTurtleSpawn);
        EntitySpawnPlacementRegistry.register(MUNGUS, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMungus::canMungusSpawn);
        EntitySpawnPlacementRegistry.register(MANTIS_SHRIMP, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMantisShrimp::canMantisShrimpSpawn);
        EntitySpawnPlacementRegistry.register(GUSTER, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGuster::canGusterSpawn);
        EntitySpawnPlacementRegistry.register(WARPED_MOSCO, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MonsterEntity::checkAnyLightMonsterSpawnRules);
        EntitySpawnPlacementRegistry.register(STRADDLER, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityStraddler::canStraddlerSpawn);
        EntitySpawnPlacementRegistry.register(STRADPOLE, EntitySpawnPlacementRegistry.PlacementType.IN_LAVA, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityStradpole::canStradpoleSpawn);
        EntitySpawnPlacementRegistry.register(EMU, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEmu::canEmuSpawn);
        EntitySpawnPlacementRegistry.register(PLATYPUS, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityPlatypus::canPlatypusSpawn);
        EntitySpawnPlacementRegistry.register(DROPBEAR, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MonsterEntity::checkAnyLightMonsterSpawnRules);
        EntitySpawnPlacementRegistry.register(TASMANIAN_DEVIL, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(KANGAROO, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityKangaroo::canKangarooSpawn);
        EntitySpawnPlacementRegistry.register(CACHALOT_WHALE, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCachalotWhale::canCachalotWhaleSpawn);
        EntitySpawnPlacementRegistry.register(LEAFCUTTER_ANT, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::checkAnimalSpawnRules);
        EntitySpawnPlacementRegistry.register(ENDERIOPHAGE, EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEnderiophage::canEnderiophageSpawn);
        EntitySpawnPlacementRegistry.register(BALD_EAGLE, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityBaldEagle::canEagleSpawn);
        EntitySpawnPlacementRegistry.register(TIGER, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTiger::canTigerSpawn);
        EntitySpawnPlacementRegistry.register(TARANTULA_HAWK, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTarantulaHawk::canTarantulaHawkSpawn);
        EntitySpawnPlacementRegistry.register(VOID_WORM, EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityVoidWorm::canVoidWormSpawn);
        EntitySpawnPlacementRegistry.register(FRILLED_SHARK, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFrilledShark::canFrilledSharkSpawn);
        EntitySpawnPlacementRegistry.register(MIMIC_OCTOPUS, EntitySpawnPlacementRegistry.PlacementType.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMimicOctopus::canMimicOctopusSpawn);
        EntitySpawnPlacementRegistry.register(SEAGULL, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySeagull::canSeagullSpawn);

    }

    @SubscribeEvent
    public static void registerEntities(final RegistryEvent.Register<EntityType<?>> event) {
        try {
            for (Field f : AMEntityRegistry.class.getDeclaredFields()) {
                Object obj = f.get(null);
                if (obj instanceof EntityType) {
                    event.getRegistry().register((EntityType) obj);
                } else if (obj instanceof EntityType[]) {
                    for (EntityType type : (EntityType[]) obj) {
                        event.getRegistry().register(type);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void initializeAttributes(EntityAttributeCreationEvent event) {
        event.put(GRIZZLY_BEAR, EntityGrizzlyBear.bakeAttributes().build());
        event.put(ROADRUNNER, EntityRoadrunner.bakeAttributes().build());
        event.put(BONE_SERPENT, EntityBoneSerpent.bakeAttributes().build());
        event.put(BONE_SERPENT_PART, EntityBoneSerpentPart.bakeAttributes().build());
        event.put(GAZELLE, EntityGazelle.bakeAttributes().build());
        event.put(CROCODILE, EntityCrocodile.bakeAttributes().build());
        event.put(FLY, EntityFly.bakeAttributes().build());
        event.put(HUMMINGBIRD, EntityHummingbird.bakeAttributes().build());
        event.put(ORCA, EntityOrca.bakeAttributes().build());
        event.put(SUNBIRD, EntitySunbird.bakeAttributes().build());
        event.put(GORILLA, EntityGorilla.bakeAttributes().build());
        event.put(CRIMSON_MOSQUITO, EntityCrimsonMosquito.bakeAttributes().build());
        event.put(RATTLESNAKE, EntityRattlesnake.bakeAttributes().build());
        event.put(ENDERGRADE, EntityEndergrade.bakeAttributes().build());
        event.put(HAMMERHEAD_SHARK, EntityHammerheadShark.bakeAttributes().build());
        event.put(LOBSTER, EntityLobster.bakeAttributes().build());
        event.put(KOMODO_DRAGON, EntityKomodoDragon.bakeAttributes().build());
        event.put(CAPUCHIN_MONKEY, EntityCapuchinMonkey.bakeAttributes().build());
        event.put(CENTIPEDE_HEAD, EntityCentipedeHead.bakeAttributes().build());
        event.put(CENTIPEDE_BODY, EntityCentipedeBody.bakeAttributes().build());
        event.put(CENTIPEDE_TAIL, EntityCentipedeTail.bakeAttributes().build());
        event.put(WARPED_TOAD, EntityWarpedToad.bakeAttributes().build());
        event.put(MOOSE, EntityMoose.bakeAttributes().build());
        event.put(MIMICUBE, EntityMimicube.bakeAttributes().build());
        event.put(RACCOON, EntityRaccoon.bakeAttributes().build());
        event.put(BLOBFISH, EntityBlobfish.bakeAttributes().build());
        event.put(SEAL, EntitySeal.bakeAttributes().build());
        event.put(COCKROACH, EntityCockroach.bakeAttributes().build());
        event.put(SHOEBILL, EntityShoebill.bakeAttributes().build());
        event.put(ELEPHANT, EntityElephant.bakeAttributes().build());
        event.put(SOUL_VULTURE, EntitySoulVulture.bakeAttributes().build());
        event.put(SNOW_LEOPARD, EntitySnowLeopard.bakeAttributes().build());
        event.put(SPECTRE, EntitySpectre.bakeAttributes().build());
        event.put(CROW, EntityCrow.bakeAttributes().build());
        event.put(ALLIGATOR_SNAPPING_TURTLE, EntityAlligatorSnappingTurtle.bakeAttributes().build());
        event.put(MUNGUS, EntityMungus.bakeAttributes().build());
        event.put(MANTIS_SHRIMP, EntityMantisShrimp.bakeAttributes().build());
        event.put(GUSTER, EntityGuster.bakeAttributes().build());
        event.put(WARPED_MOSCO, EntityWarpedMosco.bakeAttributes().build());
        event.put(STRADDLER, EntityStraddler.bakeAttributes().build());
        event.put(STRADPOLE, EntityStradpole.bakeAttributes().build());
        event.put(EMU, EntityEmu.bakeAttributes().build());
        event.put(PLATYPUS, EntityPlatypus.bakeAttributes().build());
        event.put(DROPBEAR, EntityDropBear.bakeAttributes().build());
        event.put(TASMANIAN_DEVIL, EntityTasmanianDevil.bakeAttributes().build());
        event.put(KANGAROO, EntityKangaroo.bakeAttributes().build());
        event.put(CACHALOT_WHALE, EntityCachalotWhale.bakeAttributes().build());
        event.put(LEAFCUTTER_ANT, EntityLeafcutterAnt.bakeAttributes().build());
        event.put(ENDERIOPHAGE, EntityEnderiophage.bakeAttributes().build());
        event.put(BALD_EAGLE, EntityBaldEagle.bakeAttributes().build());
        event.put(TIGER, EntityTiger.bakeAttributes().build());
        event.put(TARANTULA_HAWK, EntityTarantulaHawk.bakeAttributes().build());
        event.put(VOID_WORM, EntityVoidWorm.bakeAttributes().build());
        event.put(VOID_WORM_PART, EntityVoidWormPart.bakeAttributes().build());
        event.put(FRILLED_SHARK, EntityFrilledShark.bakeAttributes().build());
        event.put(MIMIC_OCTOPUS, EntityMimicOctopus.bakeAttributes().build());
        event.put(SEAGULL, EntitySeagull.bakeAttributes().build());
    }

    public static Predicate<LivingEntity> buildPredicateFromTag(ITag entityTag){
        if(entityTag == null){
            return Predicates.alwaysFalse();
        }else{
            return (com.google.common.base.Predicate<LivingEntity>) e -> e.isAlive() && e.getType().is(entityTag);
        }
    }

    public static Predicate<LivingEntity> buildPredicateFromTagTameable(ITag entityTag, LivingEntity owner){
        if(entityTag == null){
            return Predicates.alwaysFalse();
        }else{
            return (com.google.common.base.Predicate<LivingEntity>) e -> e.isAlive() && e.getType().is(entityTag) && !owner.isAlliedTo(e);
        }
    }

    public static boolean rollSpawn(int rolls, Random random, SpawnReason reason){
        if(reason == SpawnReason.SPAWNER){
            return true;
        }else{
            return rolls <= 0 || random.nextInt(rolls) == 0;
        }
    }

}
