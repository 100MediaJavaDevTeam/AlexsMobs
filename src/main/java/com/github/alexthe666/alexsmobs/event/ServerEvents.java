package com.github.alexthe666.alexsmobs.event;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.effect.EffectClinging;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.ItemFalconryGlove;
import com.github.alexthe666.alexsmobs.message.MessageSwingArm;
import com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.misc.EmeraldsForItemsTrade;
import com.github.alexthe666.alexsmobs.misc.ItemsForEmeraldsTrade;
import com.github.alexthe666.alexsmobs.world.BeachedCachalotWhaleSpawner;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.NonTamedTargetGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.*;
import net.minecraft.loot.conditions.RandomChance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import org.antlr.v4.runtime.misc.Triple;

import java.util.*;

@Mod.EventBusSubscriber(modid = AlexsMobs.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    private static final UUID SAND_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF28E");
    private static final UUID SNEAK_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF28F");
    private static final AttributeModifier SAND_SPEED_BONUS = new AttributeModifier(SAND_SPEED_MODIFIER, "roadrunner speed bonus", 0.1F, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier SNEAK_SPEED_BONUS = new AttributeModifier(SNEAK_SPEED_MODIFIER, "frontier cap speed bonus", 0.1F, AttributeModifier.Operation.ADDITION);
    private static final Map<ServerWorld, BeachedCachalotWhaleSpawner> BEACHED_CACHALOT_WHALE_SPAWNER_MAP = new HashMap<ServerWorld, BeachedCachalotWhaleSpawner>();
    public static List<Triple<ServerPlayerEntity, ServerWorld, BlockPos>> teleportPlayers = new ArrayList<>();
    @SubscribeEvent
    public static void onServerTick(TickEvent.WorldTickEvent tick) {
        if (!tick.world.isClientSide && tick.world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) tick.world;
            if (BEACHED_CACHALOT_WHALE_SPAWNER_MAP.get(serverWorld) == null) {
                BEACHED_CACHALOT_WHALE_SPAWNER_MAP.put(serverWorld, new BeachedCachalotWhaleSpawner(serverWorld));
            }
            BeachedCachalotWhaleSpawner spawner = BEACHED_CACHALOT_WHALE_SPAWNER_MAP.get(serverWorld);
            spawner.tick();
        }
        if (!tick.world.isClientSide && tick.world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) tick.world;
            if (BEACHED_CACHALOT_WHALE_SPAWNER_MAP.get(serverWorld) == null) {
                BEACHED_CACHALOT_WHALE_SPAWNER_MAP.put(serverWorld, new BeachedCachalotWhaleSpawner(serverWorld));
            }
            BeachedCachalotWhaleSpawner spawner = BEACHED_CACHALOT_WHALE_SPAWNER_MAP.get(serverWorld);
            spawner.tick();
        }
        if(!tick.world.isClientSide && tick.world instanceof ServerWorld){
            for(Triple trip : teleportPlayers){
                ServerPlayerEntity player = (ServerPlayerEntity) trip.a;
                ServerWorld endpointWorld = (ServerWorld) trip.b;
                BlockPos endpoint = (BlockPos) trip.c;
                player.teleportTo(endpointWorld, endpoint.getX() + 0.5D, endpoint.getY() + 0.5D, endpoint.getZ() + 0.5D, player.yRot, player.xRot);
            }
            teleportPlayers.clear();
        }
    }

    protected static BlockRayTraceResult rayTrace(World worldIn, PlayerEntity player, RayTraceContext.FluidMode fluidMode) {
        float f = player.xRot;
        float f1 = player.yRot;
        Vector3d vector3d = player.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -MathHelper.cos(-f * ((float) Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float) Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
        Vector3d vector3d1 = vector3d.add((double) f6 * d0, (double) f5 * d0, (double) f7 * d0);
        return worldIn.clip(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
    }


    @SubscribeEvent
    public static void onItemUseLast(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().getItem() == Items.CHORUS_FRUIT && new Random().nextInt(3) == 0 && event.getEntityLiving().hasEffect(AMEffectRegistry.ENDER_FLU)) {
            event.getEntityLiving().removeEffect(AMEffectRegistry.ENDER_FLU);
        }
    }

    @SubscribeEvent
    public static void onEntityResize(EntityEvent.Size event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity entity = (PlayerEntity) event.getEntity();
            try {
                Map<Effect, EffectInstance> potions = entity.getActiveEffectsMap();
                if (event.getEntity().level != null && potions != null && !potions.isEmpty() && potions.containsKey(AMEffectRegistry.CLINGING)) {
                    if (EffectClinging.isUpsideDown(entity)) {
                        float minus = event.getOldSize().height - event.getOldEyeHeight();
                        event.setNewEyeHeight(minus);
                    }
                }
            } catch (Exception e) {
            }
        }

    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (AMConfig.giveBookOnStartup) {
            CompoundNBT playerData = event.getPlayer().getPersistentData();
            CompoundNBT data = playerData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
            if (data != null && !data.getBoolean("alexsmobs_has_book")) {
                ItemHandlerHelper.giveItemToPlayer(event.getPlayer(), new ItemStack(AMItemRegistry.ANIMAL_DICTIONARY));
                data.putBoolean("alexsmobs_has_book", true);
                playerData.put(PlayerEntity.PERSISTED_NBT_TAG, data);
            }
        }
    }

    @SubscribeEvent
    public void onProjectileHit(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityRayTraceResult && ((EntityRayTraceResult) event.getRayTraceResult()).getEntity() instanceof EntityEmu && !event.getEntity().level.isClientSide) {
            EntityEmu emu = ((EntityEmu) ((EntityRayTraceResult) event.getRayTraceResult()).getEntity());
            if (event.getEntity() instanceof AbstractArrowEntity) {
                //fixes soft crash with vanilla
                ((AbstractArrowEntity) event.getEntity()).setPierceLevel((byte) 0);
            }
            if ((emu.getAnimation() == EntityEmu.ANIMATION_DODGE_RIGHT || emu.getAnimation() == EntityEmu.ANIMATION_DODGE_LEFT) && emu.getAnimationTick() < 7) {
                event.setCanceled(true);
            }
            if (emu.getAnimation() != EntityEmu.ANIMATION_DODGE_RIGHT && emu.getAnimation() != EntityEmu.ANIMATION_DODGE_LEFT) {
                boolean left = true;
                Vector3d arrowPos = event.getEntity().position();
                Vector3d rightVector = emu.getLookAngle().yRot(0.5F * (float) Math.PI).add(emu.position());
                Vector3d leftVector = emu.getLookAngle().yRot(-0.5F * (float) Math.PI).add(emu.position());
                if (arrowPos.distanceTo(rightVector) < arrowPos.distanceTo(leftVector)) {
                    left = false;
                } else if (arrowPos.distanceTo(rightVector) > arrowPos.distanceTo(leftVector)) {
                    left = true;
                } else {
                    left = emu.getRandom().nextBoolean();
                }
                Vector3d vector3d2 = event.getEntity().getDeltaMovement().yRot((float) ((left ? -0.5F : 0.5F) * Math.PI)).normalize();
                emu.setAnimation(left ? EntityEmu.ANIMATION_DODGE_LEFT : EntityEmu.ANIMATION_DODGE_RIGHT);
                emu.hasImpulse = true;
                if (!emu.horizontalCollision) {
                    emu.move(MoverType.SELF, new Vector3d(vector3d2.x() * 0.25F, 0.1F, vector3d2.z() * 0.25F));
                }
                if (!event.getEntity().level.isClientSide) {
                    ServerPlayerEntity serverPlayerEntity = null;
                    if (event.getEntity() instanceof ArrowEntity) {
                        Entity thrower = ((ArrowEntity) event.getEntity()).getOwner();
                        if (thrower instanceof ServerPlayerEntity) {
                            serverPlayerEntity = (ServerPlayerEntity) thrower;
                        }
                    }
                    if (event.getEntity() instanceof ThrowableEntity) {
                        Entity thrower = ((ThrowableEntity) event.getEntity()).getOwner();
                        if (thrower instanceof ServerPlayerEntity) {
                            serverPlayerEntity = (ServerPlayerEntity) thrower;
                        }
                    }
                    if (serverPlayerEntity != null) {
                        AMAdvancementTriggerRegistry.EMU_DODGE.trigger(serverPlayerEntity);
                    }
                }
                emu.setDeltaMovement(emu.getDeltaMovement().add(vector3d2.x() * 0.5F, 0.32F, vector3d2.z() * 0.5F));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityDespawnAttempt(LivingSpawnEvent.AllowDespawn event){
        if(event.getEntityLiving().hasEffect(AMEffectRegistry.DEBILITATING_STING) && event.getEntityLiving().getEffect(AMEffectRegistry.DEBILITATING_STING) != null && event.getEntityLiving().getEffect(AMEffectRegistry.DEBILITATING_STING).getAmplifier() > 0){
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onTradeSetup(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.FISHERMAN) {
            VillagerTrades.ITrade ambergrisTrade = new EmeraldsForItemsTrade(AMItemRegistry.AMBERGRIS, 20, 3, 4);
            List l = event.getTrades().get(2);
            l.add(ambergrisTrade);
            event.getTrades().put(2, l);
        }
    }

    @SubscribeEvent
    public void onWanderingTradeSetup(WandererTradesEvent event) {
        if (AMConfig.wanderingTraderOffers) {
            List<VillagerTrades.ITrade> genericTrades = event.getGenericTrades();
            List<VillagerTrades.ITrade> rareTrades = event.getRareTrades();
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.ANIMAL_DICTIONARY, 4, 1, 2, 1));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.ACACIA_BLOSSOM, 3, 2, 2, 1));
            if (AMConfig.cockroachSpawnWeight > 0) {
                genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.COCKROACH_OOTHECA, 2, 1, 2, 1));
            }
            if (AMConfig.blobfishSpawnWeight > 0) {
                genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.BLOBFISH_BUCKET, 4, 1, 3, 1));
            }
            if (AMConfig.crocodileSpawnWeight > 0) {
                genericTrades.add(new ItemsForEmeraldsTrade(AMBlockRegistry.CROCODILE_EGG.asItem(), 6, 1, 2, 1));
            }
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.BEAR_FUR, 1, 1, 2, 1));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.CROCODILE_SCUTE, 5, 1, 2, 1));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.MOSQUITO_LARVA, 1, 3, 5, 1));
            rareTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.SOMBRERO, 20, 1, 1, 1));
            rareTrades.add(new ItemsForEmeraldsTrade(AMBlockRegistry.BANANA_PEEL, 1, 2, 1, 1));
            rareTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.BLOOD_SAC, 5, 2, 3, 1));
        }
    }

    @SubscribeEvent
    public void onLootLevelEvent(LootingLevelEvent event) {
        DamageSource src = event.getDamageSource();
        if (src != null) {
            Entity dmgSrc = src.getEntity();
            if (dmgSrc != null && dmgSrc instanceof EntitySnowLeopard) {
                event.setLootingLevel(event.getLootingLevel() + 2);
            }
        }

    }

    @SubscribeEvent
    public static void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        ItemFalconryGlove.onLeftClick(event.getPlayer(), event.getPlayer().getOffhandItem());
        ItemFalconryGlove.onLeftClick(event.getPlayer(), event.getPlayer().getMainHandItem());
        if (event.getWorld().isClientSide) {
            AlexsMobs.sendMSGToServer(new MessageSwingArm());
        }
    }

    @SubscribeEvent
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().getItem() == Items.WHEAT && event.getPlayer().getVehicle() instanceof EntityElephant) {
            if (((EntityElephant) event.getPlayer().getVehicle()).triggerCharge(event.getItemStack())) {
                event.getPlayer().swing(event.getHand());
                if (!event.getPlayer().isCreative()) {
                    event.getItemStack().shrink(1);
                }
            }
        }
        if (event.getItemStack().getItem() == Items.GLASS_BOTTLE && AMConfig.lavaBottleEnabled) {
            RayTraceResult raytraceresult = rayTrace(event.getWorld(), event.getPlayer(), RayTraceContext.FluidMode.SOURCE_ONLY);
            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
                BlockPos blockpos = ((BlockRayTraceResult) raytraceresult).getBlockPos();
                if (event.getWorld().mayInteract(event.getPlayer(), blockpos)) {
                    if (event.getWorld().getFluidState(blockpos).is(FluidTags.LAVA)) {
                        event.getWorld().playSound(event.getPlayer(), event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ(), SoundEvents.BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                        event.getPlayer().awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
                        event.getPlayer().setSecondsOnFire(6);
                        if (!event.getPlayer().addItem(new ItemStack(AMItemRegistry.LAVA_BOTTLE))) {
                            event.getPlayer().spawnAtLocation(new ItemStack(AMItemRegistry.LAVA_BOTTLE));
                        }
                        event.getPlayer().swing(event.getHand());
                        if (!event.getPlayer().isCreative()) {
                            event.getItemStack().shrink(1);
                        }
                    }
                }
            }
        }

    }

    @SubscribeEvent
    public void onInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof LivingEntity && !(event.getTarget() instanceof PlayerEntity) && !(event.getTarget() instanceof EntityEndergrade) && ((LivingEntity) event.getTarget()).hasEffect(AMEffectRegistry.ENDER_FLU)) {
            if (event.getItemStack().getItem() == Items.CHORUS_FRUIT) {
                if (!event.getPlayer().isCreative()) {
                    event.getItemStack().shrink(1);
                }
                event.getTarget().playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.5F + event.getPlayer().getRandom().nextFloat());
                if (event.getPlayer().getRandom().nextFloat() < 0.4F) {
                    ((LivingEntity) event.getTarget()).removeEffect(AMEffectRegistry.ENDER_FLU);
                    Items.CHORUS_FRUIT.finishUsingItem(event.getItemStack().copy(), event.getWorld(), ((LivingEntity) event.getTarget()));
                }
                event.setCanceled(true);
                event.setCancellationResult(ActionResultType.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getEntity() instanceof WanderingTraderEntity && AMConfig.elephantTraderSpawnChance > 0) {
            Random rand = new Random();
            Biome biome = event.getWorld().getBiome(event.getEntity().blockPosition());
            if (rand.nextFloat() <= AMConfig.elephantTraderSpawnChance && (!AMConfig.limitElephantTraderBiomes || biome.getBaseTemperature() >= 1.0F)) {
                WanderingTraderEntity traderEntity = (WanderingTraderEntity) event.getEntity();
                EntityElephant elephant = AMEntityRegistry.ELEPHANT.create(traderEntity.level);
                elephant.copyPosition(traderEntity);
                if (elephant.canSpawnWithTraderHere()) {
                    elephant.setTrader(true);
                    elephant.setChested(true);
                    if (!event.getWorld().isClientSide()) {
                        traderEntity.level.addFreshEntity(elephant);
                        traderEntity.startRiding(elephant, true);
                    }
                    elephant.addElephantLoot(null, rand.nextInt());
                }
            }
        }
        try {
            if (event.getEntity() != null && event.getEntity() instanceof SpiderEntity && AMConfig.spidersAttackFlies) {
                SpiderEntity spider = (SpiderEntity) event.getEntity();
                spider.targetSelector.addGoal(4, new NearestAttackableTargetGoal(spider, EntityFly.class, 1, true, false, null));
            }
            if (event.getEntity() != null && event.getEntity() instanceof WolfEntity && AMConfig.wolvesAttackMoose) {
                WolfEntity wolf = (WolfEntity) event.getEntity();
                wolf.targetSelector.addGoal(6, new NonTamedTargetGoal(wolf, EntityMoose.class, false, null));
            }
            if (event.getEntity() != null && event.getEntity() instanceof PolarBearEntity && AMConfig.polarBearsAttackSeals) {
                PolarBearEntity bear = (PolarBearEntity) event.getEntity();
                bear.targetSelector.addGoal(6, new NearestAttackableTargetGoal(bear, EntitySeal.class, 15, true, true, null));
            }
            if (event.getEntity() != null && event.getEntity() instanceof CreeperEntity) {
                CreeperEntity creeper = (CreeperEntity) event.getEntity();
                creeper.targetSelector.addGoal(3, new AvoidEntityGoal<>(creeper, EntitySnowLeopard.class, 6.0F, 1.0D, 1.2D));
                creeper.targetSelector.addGoal(3, new AvoidEntityGoal<>(creeper, EntityTiger.class, 6.0F, 1.0D, 1.2D));
            }
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("Tried to add unique behaviors to vanilla mobs and encountered an error");
        }
    }

    @SubscribeEvent
    public void onPlayerAttackEntityEvent(AttackEntityEvent event) {
        if (event.getPlayer().getItemBySlot(EquipmentSlotType.HEAD).getItem() == AMItemRegistry.MOOSE_HEADGEAR && event.getTarget() instanceof LivingEntity) {
            float f1 = 2;
            ((LivingEntity) event.getTarget()).knockback(f1 * 0.5F, MathHelper.sin(event.getPlayer().yRot * ((float) Math.PI / 180F)), -MathHelper.cos(event.getPlayer().yRot * ((float) Math.PI / 180F)));
        }
        if(event.getPlayer().hasEffect(AMEffectRegistry.TIGERS_BLESSING) && event.getTarget() instanceof LivingEntity && !event.getTarget().isAlliedTo(event.getPlayer()) && !(event.getTarget() instanceof EntityTiger)){
            AxisAlignedBB bb = new AxisAlignedBB(event.getPlayer().getX() - 32, event.getPlayer().getY() - 32, event.getPlayer().getZ() - 32, event.getPlayer().getZ() + 32, event.getPlayer().getY() + 32, event.getPlayer().getZ() + 32);
            List<EntityTiger> tigers = event.getPlayer().level.getEntitiesOfClass(EntityTiger.class, bb, EntityPredicates.ENTITY_STILL_ALIVE);
            for(EntityTiger tiger : tigers){
                if(!tiger.isBaby()){
                    tiger.setTarget((LivingEntity)event.getTarget());
                }
            }

        }
    }

    @SubscribeEvent
    public void onLivingDamageEvent(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof PlayerEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
            if (event.getAmount() > 0 && attacker.hasEffect(AMEffectRegistry.SOULSTEAL) && attacker.getEffect(AMEffectRegistry.SOULSTEAL) != null) {
                int level = attacker.getEffect(AMEffectRegistry.SOULSTEAL).getAmplifier() + 1;
                Random rand = new Random();
                if (attacker.getHealth() < attacker.getMaxHealth() && rand.nextFloat() < (0.25F + (level * 0.25F))) {
                    attacker.heal(Math.min(event.getAmount() / 2F * level, 2 + 2 * level));
                }
            }
        }
        if (event.getEntityLiving() instanceof PlayerEntity && event.getSource().getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            if(attacker instanceof EntityMimicOctopus && ((EntityMimicOctopus) attacker).isOwnedBy(player)){
                event.setCanceled(true);
                return;
            }
            if (player.getItemBySlot(EquipmentSlotType.HEAD).getItem() == AMItemRegistry.SPIKED_TURTLE_SHELL) {
                float f1 = 1F;
                if (attacker.distanceTo(player) < attacker.getBbWidth() + player.getBbWidth() + 0.5F) {
                    attacker.hurt(DamageSource.thorns(player), 1F);
                    attacker.knockback(f1 * 0.5F, MathHelper.sin((attacker.yRot + 180) * ((float) Math.PI / 180F)), -MathHelper.cos((attacker.yRot + 180) * ((float) Math.PI / 180F)));
                }
            }
        }
        if (!event.getEntityLiving().getItemBySlot(EquipmentSlotType.LEGS).isEmpty() && event.getEntityLiving().getItemBySlot(EquipmentSlotType.LEGS).getItem() == AMItemRegistry.EMU_LEGGINGS) {
            if (event.getSource().isProjectile() && event.getEntityLiving().getRandom().nextFloat() < AMConfig.emuPantsDodgeChance) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onStructureGetSpawnLists(StructureSpawnListGatherEvent event) {
        if (AMConfig.mimicubeSpawnInEndCity && AMConfig.mimicubeSpawnWeight > 0) {
            if (event.getStructure() == Structure.END_CITY) {
                event.addEntitySpawn(EntityClassification.MONSTER, new MobSpawnInfo.Spawners(AMEntityRegistry.MIMICUBE, AMConfig.mimicubeSpawnWeight, 1, 3));
            }
        }
        if (AMConfig.soulVultureSpawnOnFossil && AMConfig.soulVultureSpawnWeight > 0) {
            if (event.getStructure() == Structure.NETHER_FOSSIL) {
                event.addEntitySpawn(EntityClassification.MONSTER, new MobSpawnInfo.Spawners(AMEntityRegistry.SOUL_VULTURE, AMConfig.soulVultureSpawnWeight, 2, 3));
            }
        }
    }

    @SubscribeEvent
    public void onLivingSetTargetEvent(LivingSetAttackTargetEvent event) {
        if (event.getTarget() != null && event.getEntityLiving() instanceof MobEntity) {
            if (event.getEntityLiving().getMobType() == CreatureAttribute.ARTHROPOD) {
                if (event.getTarget().hasEffect(AMEffectRegistry.BUG_PHEROMONES) && event.getEntityLiving().getLastHurtByMob() != event.getTarget()) {
                    ((MobEntity) event.getEntityLiving()).setTarget(null);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingUpdateEvent(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            if (event.getEntityLiving().getEyeHeight() < event.getEntityLiving().getBbHeight() * 0.5D) {
                event.getEntityLiving().refreshDimensions();
            }
            ModifiableAttributeInstance modifiableattributeinstance = event.getEntityLiving().getAttribute(Attributes.MOVEMENT_SPEED);
            if (event.getEntityLiving().getItemBySlot(EquipmentSlotType.FEET).getItem() == AMItemRegistry.ROADDRUNNER_BOOTS || modifiableattributeinstance.hasModifier(SAND_SPEED_BONUS)) {
                boolean sand = event.getEntityLiving().level.getBlockState(getDownPos(event.getEntityLiving().blockPosition(), event.getEntityLiving().level)).getBlock().is(BlockTags.SAND);
                if (sand && !modifiableattributeinstance.hasModifier(SAND_SPEED_BONUS)) {
                    modifiableattributeinstance.addPermanentModifier(SAND_SPEED_BONUS);
                }
                if (event.getEntityLiving().tickCount % 25 == 0 && (event.getEntityLiving().getItemBySlot(EquipmentSlotType.FEET).getItem() != AMItemRegistry.ROADDRUNNER_BOOTS || !sand) && modifiableattributeinstance.hasModifier(SAND_SPEED_BONUS)) {
                    modifiableattributeinstance.removeModifier(SAND_SPEED_BONUS);
                }
            }
            if (event.getEntityLiving().getItemBySlot(EquipmentSlotType.HEAD).getItem() == AMItemRegistry.FRONTIER_CAP || modifiableattributeinstance.hasModifier(SNEAK_SPEED_BONUS)) {
                if (event.getEntityLiving().isShiftKeyDown() && !modifiableattributeinstance.hasModifier(SNEAK_SPEED_BONUS)) {
                    modifiableattributeinstance.addPermanentModifier(SNEAK_SPEED_BONUS);
                }
                if ((!event.getEntityLiving().isShiftKeyDown() || event.getEntityLiving().getItemBySlot(EquipmentSlotType.HEAD).getItem() != AMItemRegistry.FRONTIER_CAP) && modifiableattributeinstance.hasModifier(SNEAK_SPEED_BONUS)) {
                    modifiableattributeinstance.removeModifier(SNEAK_SPEED_BONUS);
                }
            }
            if (event.getEntityLiving().getItemBySlot(EquipmentSlotType.HEAD).getItem() == AMItemRegistry.SPIKED_TURTLE_SHELL) {
                if (!event.getEntityLiving().isEyeInFluid(FluidTags.WATER)) {
                    event.getEntityLiving().addEffect(new EffectInstance(Effects.WATER_BREATHING, 210, 0, false, false, true));
                }
            }
        }

        if (event.getEntityLiving().getItemBySlot(EquipmentSlotType.LEGS).getItem() == AMItemRegistry.CENTIPEDE_LEGGINGS) {
            if (event.getEntityLiving().horizontalCollision && !event.getEntityLiving().isInWater()) {
                event.getEntityLiving().fallDistance = 0.0F;
                Vector3d motion = event.getEntityLiving().getDeltaMovement();
                double d0 = MathHelper.clamp(motion.x, -0.15F, 0.15F);
                double d1 = MathHelper.clamp(motion.z, -0.15F, 0.15F);
                double d2 = 0.1D;
                if (d2 < 0.0D && !event.getEntityLiving().getFeetBlockState().isScaffolding(event.getEntityLiving()) && event.getEntityLiving().isSuppressingSlidingDownLadder()) {
                    d2 = 0.0D;
                }
                motion = new Vector3d(d0, d2, d1);
                event.getEntityLiving().setDeltaMovement(motion);
            }


        }
    }

    private BlockPos getDownPos(BlockPos entered, IWorld world) {
        int i = 0;
        while (world.isEmptyBlock(entered) && i < 3) {
            entered = entered.below();
            i++;
        }
        return entered;
    }

    @SubscribeEvent
    public void onFOVUpdate(FOVUpdateEvent event){
        if(event.getEntity().hasEffect(AMEffectRegistry.FEAR)){
            event.setNewfov(1.0F);
        }
    }

    @SubscribeEvent
    public void onEntityLeaveWorld(EntityLeaveWorldEvent event){
        if(event.getEntity() instanceof ItemEntity && ((ItemEntity) event.getEntity()).getItem().getItem() == AMItemRegistry.MYSTERIOUS_WORM && AMConfig.voidWormSummonable){
            String dim = event.getEntity().level.dimension().location().toString();
            if(AMConfig.voidWormSpawnDimensions.contains(dim) && event.getEntity().getY() < -10){
                EntityVoidWorm worm = AMEntityRegistry.VOID_WORM.create(event.getWorld());
                worm.setPos(event.getEntity().getX(), 0, event.getEntity().getZ());
                worm.setSegmentCount(25 + new Random().nextInt(15));
                worm.xRot = -90.0F;
                worm.updatePostSummon = true;
                if(!event.getWorld().isClientSide){
                    if(((ItemEntity) event.getEntity()).getThrower() != null){
                        UUID uuid = ((ItemEntity) event.getEntity()).getThrower();
                        if(event.getWorld().getPlayerByUUID(uuid) instanceof ServerPlayerEntity){
                            AMAdvancementTriggerRegistry.VOID_WORM_SUMMON.trigger((ServerPlayerEntity)event.getWorld().getPlayerByUUID(uuid));
                        }
                    }
                    event.getWorld().addFreshEntity(worm);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if(!event.getEntityLiving().getUseItem().isEmpty() && event.getSource() != null && event.getSource().getEntity() != null){
            if(event.getEntityLiving().getUseItem().getItem() == AMItemRegistry.SHIELD_OF_THE_DEEP){
               Entity attacker = event.getSource().getEntity();
               if(attacker instanceof LivingEntity){
                   boolean flag = false;
                   if(attacker.distanceTo(event.getEntityLiving()) <= 4 && !((LivingEntity)attacker).hasEffect(AMEffectRegistry.EXSANGUINATION)){
                       ((LivingEntity) attacker).addEffect(new EffectInstance(AMEffectRegistry.EXSANGUINATION, 60, 2));
                       flag = true;
                   }
                   if(event.getEntityLiving().isInWaterOrBubble()){
                       event.getEntityLiving().setAirSupply(Math.min(event.getEntityLiving().getMaxAirSupply(), event.getEntityLiving().getAirSupply() + 150));
                       flag = true;
                   }
                   if(flag){
                       event.getEntityLiving().getUseItem().hurtAndBreak(1, event.getEntityLiving(), (playerIn) -> {
                           playerIn.broadcastBreakEvent(event.getEntityLiving().getUsedItemHand());
                       });
                   }
               }
            }
        }
    }

    @SubscribeEvent
    public void onChestGenerated(LootTableLoadEvent event) {
        if (event.getName().equals(LootTables.JUNGLE_TEMPLE)) {
            LootEntry.Builder item = ItemLootEntry.lootTableItem(AMItemRegistry.ANCIENT_DART).setQuality(40).setWeight(1);
            LootPool.Builder builder = new LootPool.Builder().name("am_dart").add(item).when(RandomChance.randomChance(1f)).setRolls(new RandomValueRange(0, 1)).bonusRolls(0, 1);
            event.getTable().addPool(builder.build());
        }
        if (event.getName().equals(LootTables.JUNGLE_TEMPLE_DISPENSER)) {
            LootEntry.Builder item = ItemLootEntry.lootTableItem(AMItemRegistry.ANCIENT_DART).setQuality(20).setWeight(3);
            LootPool.Builder builder = new LootPool.Builder().name("am_dart_dispenser").add(item).when(RandomChance.randomChance(1f)).setRolls(new RandomValueRange(0, 2)).bonusRolls(0, 1);
            event.getTable().addPool(builder.build());
        }
    }

}
