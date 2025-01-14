package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.message.MessageMungusBiomeChange;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

public class EntityMungus extends AnimalEntity implements ITargetsDroppedItems, IShearable, net.minecraftforge.common.IForgeShearable {

    protected static final DataParameter<Optional<BlockPos>> TARGETED_BLOCK_POS = EntityDataManager.defineId(EntityMungus.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Boolean> ALT_ORDER_MUSHROOMS = EntityDataManager.defineId(EntityMungus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> REVERTING = EntityDataManager.defineId(EntityMungus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> MUSHROOM_COUNT = EntityDataManager.defineId(EntityMungus.class, DataSerializers.INT);
    private static final DataParameter<Integer> SACK_SWELL = EntityDataManager.defineId(EntityMungus.class, DataSerializers.INT);
    private static final DataParameter<Boolean> EXPLOSION_DISABLED = EntityDataManager.defineId(EntityMungus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<BlockState>> MUSHROOM_STATE = EntityDataManager.defineId(EntityMungus.class, DataSerializers.BLOCK_STATE);
    private static final int WIDTH_BITS = (int) Math.round(Math.log(16.0D) / Math.log(2.0D)) - 2;
    private static HashMap<String, String> MUSHROOM_TO_BIOME = new HashMap<>();
    private static HashMap<String, String> MUSHROOM_TO_BLOCK = new HashMap<>();
    private static boolean initBiomeData = false;
    public float prevSwellProgress = 0;
    public float swellProgress = 0;
    private int beamCounter = 0;
    private int mosquitoAttackCooldown = 0;
    private boolean hasExploded;

    protected EntityMungus(EntityType<? extends AnimalEntity> type, World worldIn) {
        super(type, worldIn);
        initBiomeData();
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 15D).add(Attributes.MOVEMENT_SPEED, 0.25F);
    }

    public static boolean canMungusSpawn(EntityType type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return worldIn.getBlockState(pos.below()).canOcclude();
    }

    public static BlockState getMushroomBlockstate(Item item) {
        if (item instanceof BlockItem) {
            if (MUSHROOM_TO_BIOME.containsKey(item.getRegistryName().toString())) {
                return ((BlockItem) item).getBlock().defaultBlockState();

            }
        }
        return null;
    }

    private static void initBiomeData() {
        if (!initBiomeData || MUSHROOM_TO_BIOME.isEmpty()) {
            initBiomeData = true;
            for (String str : AMConfig.mungusBiomeMatches) {
                String[] split = str.split("\\|");
                if (split.length >= 2) {
                    MUSHROOM_TO_BIOME.put(split[0], split[1]);
                    MUSHROOM_TO_BLOCK.put(split[0], split[2]);
                }
            }
        }

    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MUNGUS_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MUNGUS_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MUNGUS_HURT;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mungusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1, Ingredient.of(), false) {
            protected boolean shouldFollowItem(ItemStack stack) {
                return EntityMungus.this.shouldFollowMushroom(stack) || stack.getItem() == Items.MUSHROOM_STEW;
            }
        });
        this.goalSelector.addGoal(5, new AITargetMushrooms());
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new AnimalAIWanderRanged(this, 60, 1.0D, 14, 7));
        this.goalSelector.addGoal(8, new LookAtGoal(this, LivingEntity.class, 15.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false, 10));
    }

    public void baseTick() {
        super.baseTick();
        this.prevSwellProgress = swellProgress;
        if (this.isReverting() && AMConfig.mungusBiomeTransformationType == 2) {
            swellProgress += 0.5F;
            if (swellProgress >= 10) {
                explode();
                swellProgress = 0;
                this.entityData.set(REVERTING, false);
            }
        } else if (isAlive() && swellProgress > 0F) {
            swellProgress -= 1F;
        }
        if(entityData.get(EXPLOSION_DISABLED)){
            if(mosquitoAttackCooldown < 0){
                mosquitoAttackCooldown++;
            }
            if(mosquitoAttackCooldown > 200){
                mosquitoAttackCooldown = 0;
                entityData.set(EXPLOSION_DISABLED, false);
            }
        }
    }

    protected void tickDeath() {
        super.tickDeath();
        if (this.getMushroomCount() >= 5 && AMConfig.mungusBiomeTransformationType > 0 && !this.isBaby() && !this.entityData.get(EXPLOSION_DISABLED)) {
            this.swellProgress++;
            if (this.deathTime == 20 && !hasExploded) {
                hasExploded = true;
                explode();
            }
        }
    }

    private void explode() {
        for (int i = 0; i < 5; i++) {
            float r1 = 6F * (random.nextFloat() - 0.5F);
            float r2 = 2F * (random.nextFloat() - 0.5F);
            float r3 = 6F * (random.nextFloat() - 0.5F);
            this.level.addParticle(ParticleTypes.EXPLOSION, this.getX() + r1, this.getY() + 0.5F + r2, this.getZ() + r3, r1 * 4, r2 * 4, r3 * 4);
        }
        final int radius = 3;
        final int j = radius + level.random.nextInt(1);
        final int k = (radius + level.random.nextInt(1));
        final int l = radius + level.random.nextInt(1);
        final float f = (float) (j + k + l) * 0.333F + 0.5F;
        final float ff = f * f;
        final double ffDouble = ff;
        BlockPos center = this.blockPosition();
        BlockState transformState = Blocks.MYCELIUM.defaultBlockState();
        Biome biome = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(Biomes.MUSHROOM_FIELDS);
        ITag<Block> transformMatches = BlockTags.getAllTags().getTag(AMTagRegistry.MUNGUS_REPLACE_MUSHROOM);
        if (this.getMushroomState() != null) {
            String mushroomKey = this.getMushroomState().getBlock().getRegistryName().toString();
            if (MUSHROOM_TO_BLOCK.containsKey(mushroomKey)) {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(MUSHROOM_TO_BLOCK.get(mushroomKey)));
                if (block != null) {
                    transformState = block.defaultBlockState();
                    if (block == Blocks.WARPED_NYLIUM) {
                        transformMatches = BlockTags.getAllTags().getTag(AMTagRegistry.MUNGUS_REPLACE_NETHER);
                    }
                    if (block == Blocks.CRIMSON_NYLIUM) {
                        transformMatches = BlockTags.getAllTags().getTag(AMTagRegistry.MUNGUS_REPLACE_NETHER);
                    }
                }
            }
            if (getBiomeKeyFromShroom() != null) {
                biome = getBiomeKeyFromShroom();
            }
        }
        BlockState finalTransformState = transformState;
        ITag<Block> finalTransformReplace = transformMatches;

        if (AMConfig.mungusBiomeTransformationType == 2 && !level.isClientSide) {
            transformBiome(center, biome);
        }
        this.playSound(SoundEvents.GENERIC_EXPLODE, this.getSoundVolume(), this.getVoicePitch());
        if (!isReverting()) {
            BlockPos.betweenClosedStream(center.offset(-j, -k, -l), center.offset(j, k, l)).forEach(blockpos -> {
                if (blockpos.distSqr(center) <= ffDouble) {
                    if (level.random.nextFloat() > (float) blockpos.distSqr(center) / ff) {
                        if (level.getBlockState(blockpos).is(finalTransformReplace) && !level.getBlockState(blockpos.above()).canOcclude()) {
                            level.setBlockAndUpdate(blockpos, finalTransformState);
                        }
                        if (level.random.nextInt(4) == 0 && level.getBlockState(blockpos).getMaterial().isSolid() && level.getFluidState(blockpos.above()).isEmpty() && !level.getBlockState(blockpos.above()).canOcclude()) {
                            level.setBlockAndUpdate(blockpos.above(), this.getMushroomState().getBlockState());
                        }
                    }
                }
            });
        }
    }

    public void disableExplosion(){
        this.entityData.set(EXPLOSION_DISABLED, true);
    }

    private Biome getBiomeKeyFromShroom() {
        Registry<Biome> registry = this.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        BlockState state = this.getMushroomState();
        if (state == null) {
            return null;
        }
        String blockRegName = state.getBlock().getRegistryName().toString();
        String str = MUSHROOM_TO_BIOME.get(blockRegName);
        if (str != null) {
            return registry.getOptional(new ResourceLocation(str)).orElse(null);
        }
        return null;
    }

    private void transformBiome(BlockPos pos, Biome biome) {
        Chunk chunk = level.getChunkAt(pos);
        BiomeContainer container = chunk.getBiomes();
        if (this.entityData.get(REVERTING)) {
            int lvt_4_1_ = chunk.getPos().getMinBlockX() >> 2;
            int lvt_5_1_ = chunk.getPos().getMinBlockZ() >> 2;
            ChunkGenerator chunkgenerator = ((ServerWorld) level).getChunkSource().getGenerator();
            Biome b = null;
            for (int lvt_6_1_ = 0; lvt_6_1_ < container.biomes.length; ++lvt_6_1_) {
                int lvt_7_1_ = lvt_6_1_ & BiomeContainer.HORIZONTAL_MASK;
                int lvt_8_1_ = lvt_6_1_ >> WIDTH_BITS + WIDTH_BITS & BiomeContainer.VERTICAL_MASK;
                int lvt_9_1_ = lvt_6_1_ >> WIDTH_BITS & BiomeContainer.HORIZONTAL_MASK;
                b = chunkgenerator.getBiomeSource().getNoiseBiome(lvt_4_1_ + lvt_7_1_, lvt_8_1_, lvt_5_1_ + lvt_9_1_);
                container.biomes[lvt_6_1_] = b;
            }
            if (b != null && !level.isClientSide) {
                AlexsMobs.sendMSGToAll(new MessageMungusBiomeChange(this.getId(), pos.getX(), pos.getZ(), b.getRegistryName().toString()));
            }
        } else {
            if (biome == null) {
                return;
            }
            if (container != null && !level.isClientSide) {
                for (int i = 0; i < container.biomes.length; i++) {
                    container.biomes[i] = biome;
                }
                AlexsMobs.sendMSGToAll(new MessageMungusBiomeChange(this.getId(), pos.getX(), pos.getZ(), biome.getRegistryName().toString()));
            }
        }

    }

    private boolean shouldFollowMushroom(ItemStack stack) {
        BlockState state = getMushroomBlockstate(stack.getItem());
        if (state != null) {
            if (this.getMushroomCount() == 0) {
                return true;
            } else {
                return this.getMushroomState().getBlock() == state.getBlock();
            }
        }
        return false;
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        ActionResultType type = super.mobInteract(player, hand);
        if (itemstack.getItem() == Items.POISONOUS_POTATO && !this.isBaby()) {
            this.entityData.set(REVERTING, true);
            usePlayerItem(player, itemstack);
            return ActionResultType.SUCCESS;
        }
        if (shouldFollowMushroom(itemstack) && this.getMushroomCount() < 5) {
            this.entityData.set(REVERTING, false);
            BlockState state = getMushroomBlockstate(itemstack.getItem());
            this.playSound(SoundEvents.FUNGUS_PLACE, this.getSoundVolume(), this.getVoicePitch());
            if (this.getMushroomState() != null && state != null && state.getBlock() != this.getMushroomState().getBlock()) {
                this.setMushroomCount(0);
            }
            this.setMushroomState(state);
            this.usePlayerItem(player, itemstack);
            this.setMushroomCount(this.getMushroomCount() + 1);
            return ActionResultType.SUCCESS;
        } else {
            return type;
        }
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if (prev) {
            this.setBeamTarget(null);
            beamCounter = Math.min(beamCounter, -1200);
        }
        return prev;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MUSHROOM_STATE, Optional.empty());
        this.getEntityData().define(TARGETED_BLOCK_POS, Optional.empty());
        this.entityData.define(ALT_ORDER_MUSHROOMS, Boolean.valueOf(false));
        this.entityData.define(REVERTING, Boolean.valueOf(false));
        this.entityData.define(EXPLOSION_DISABLED, Boolean.valueOf(false));
        this.entityData.define(MUSHROOM_COUNT, 0);
        this.entityData.define(SACK_SWELL, 0);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        BlockState blockstate = this.getMushroomState();
        if (blockstate != null) {
            compound.put("MushroomState", NBTUtil.writeBlockState(blockstate));
        }
        compound.putInt("MushroomCount", this.getMushroomCount());
        compound.putInt("Sack", this.getSackSwell());
        compound.putInt("BeamCounter", this.beamCounter);
        compound.putBoolean("AltMush", this.entityData.get(ALT_ORDER_MUSHROOMS));
        if (this.getBeamTarget() != null) {
            compound.put("BeamTarget", NBTUtil.writeBlockPos(this.getBeamTarget()));
        }
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        BlockState blockstate = null;
        if (compound.contains("MushroomState", 10)) {
            blockstate = NBTUtil.readBlockState(compound.getCompound("MushroomState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        if (compound.contains("BeamTarget", 10)) {
            this.setBeamTarget(NBTUtil.readBlockPos(compound.getCompound("BeamTarget")));
        }
        this.setMushroomState(blockstate);
        this.setMushroomCount(compound.getInt("MushroomCount"));
        this.setSackSwell(compound.getInt("Sack"));
        this.beamCounter = compound.getInt("BeamCounter");
        this.entityData.set(ALT_ORDER_MUSHROOMS, compound.getBoolean("AltMush"));
    }

    public void aiStep() {
        super.aiStep();
        if (this.getBeamTarget() != null) {
            BlockPos t = this.getBeamTarget();
            if (isMushroomTarget(t) && this.canSeeMushroom(t)) {
                this.getLookControl().setLookAt(t.getX() + 0.5F, t.getY() + 0.15F, t.getZ() + 0.5F, 90.0F, 90.0F);
                this.getLookControl().tick();
                double d5 = 1.0F;
                double eyeHeight = this.getY() + 1.0F;
                if (beamCounter % 20 == 0) {
                    this.playSound(AMSoundRegistry.MUNGUS_LASER_LOOP, this.getVoicePitch(), this.getSoundVolume());
                }
                beamCounter++;

                double d0 = t.getX() + 0.5F - this.getX();
                double d1 = t.getY() + 0.5F - eyeHeight;
                double d2 = t.getZ() + 0.5F - this.getZ();
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d0 = d0 / d3;
                d1 = d1 / d3;
                d2 = d2 / d3;
                double d4 = this.random.nextDouble();
                while (d4 < d3 - 0.5F) {
                    d4 += 1.0D - d5 + this.random.nextDouble();
                    if (random.nextFloat() < 0.1F) {
                        float r1 = 0.3F * (random.nextFloat() - 0.5F);
                        float r2 = 0.3F * (random.nextFloat() - 0.5F);
                        float r3 = 0.3F * (random.nextFloat() - 0.5F);

                        this.level.addParticle(ParticleTypes.MYCELIUM, this.getX() + d0 * d4 + r1, eyeHeight + d1 * d4 + r2, this.getZ() + d2 * d4 + r3, r1 * 4, r2 * 4, r3 * 4);
                    }
                }
                if (beamCounter > 200) {
                    BlockState state = level.getBlockState(t);
                    if (state.getBlock() instanceof IGrowable) {
                        IGrowable igrowable = (IGrowable) state.getBlock();
                        boolean flag = false;
                        if (igrowable.isValidBonemealTarget(this.level, t, state, this.level.isClientSide)) {
                            for (int i = 0; i < 5; i++) {
                                float r1 = 3F * (random.nextFloat() - 0.5F);
                                float r2 = 2F * (random.nextFloat() - 0.5F);
                                float r3 = 3F * (random.nextFloat() - 0.5F);
                                this.level.addParticle(ParticleTypes.EXPLOSION, t.getX() + 0.5F + r1, t.getY() + 0.5F + r2, t.getZ() + 0.5F + r3, r1 * 4, r2 * 4, r3 * 4);
                            }
                            if (!this.level.isClientSide) {
                                this.level.levelEvent(2005, t, 0);
                                igrowable.performBonemeal((ServerWorld) this.level, this.level.random, t, state);
                                flag = level.getBlockState(t).getBlock() != state.getBlock();
                            }
                        }
                        if (!flag) {
                            int grown = 0;
                            int maxGrow = 2 + random.nextInt(3);
                            for (int i = 0; i < 15; i++) {
                                BlockPos pos = t.offset(random.nextInt(10) - 5, random.nextInt(4) - 2, random.nextInt(10) - 5);
                                if (grown < maxGrow) {
                                    if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).canOcclude()) {
                                        level.setBlockAndUpdate(pos, state);
                                        grown++;
                                    }
                                }
                            }
                        }
                        this.playSound(AMSoundRegistry.MUNGUS_LASER_END, this.getVoicePitch(), this.getSoundVolume());
                        if(flag){
                            this.playSound(AMSoundRegistry.MUNGUS_LASER_GROW, this.getVoicePitch(), this.getSoundVolume());
                        }
                        this.setBeamTarget(null);
                        beamCounter = -1200;
                        if (this.getMushroomCount() > 0) {
                            this.setMushroomCount(this.getMushroomCount() - 1);
                        }
                    }
                }
            } else {
                this.setBeamTarget(null);
            }
        }
        if (beamCounter < 0) {
            beamCounter++;
        }
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.MUSHROOM_STEW;
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.entityData.set(ALT_ORDER_MUSHROOMS, random.nextBoolean());
        this.setMushroomCount(random.nextInt(2));
        setMushroomState(random.nextBoolean() ? Blocks.BROWN_MUSHROOM.defaultBlockState() : Blocks.RED_MUSHROOM.defaultBlockState());
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public int getMushroomCount() {
        return this.entityData.get(MUSHROOM_COUNT).intValue();
    }

    public void setMushroomCount(int command) {
        this.entityData.set(MUSHROOM_COUNT, Integer.valueOf(command));
    }

    public int getSackSwell() {
        return this.entityData.get(SACK_SWELL).intValue();
    }

    public void setSackSwell(int command) {
        this.entityData.set(SACK_SWELL, Integer.valueOf(command));
    }

    @Nullable
    public BlockPos getBeamTarget() {
        return this.getEntityData().get(TARGETED_BLOCK_POS).orElse(null);
    }

    public void setBeamTarget(@Nullable BlockPos beamTarget) {
        this.getEntityData().set(TARGETED_BLOCK_POS, Optional.ofNullable(beamTarget));
    }

    public boolean isAltOrderMushroom() {
        return this.entityData.get(ALT_ORDER_MUSHROOMS).booleanValue();
    }

    @Nullable
    public BlockState getMushroomState() {
        return this.entityData.get(MUSHROOM_STATE).orElse(null);
    }

    public void setMushroomState(@Nullable BlockState state) {
        this.entityData.set(MUSHROOM_STATE, Optional.ofNullable(state));
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.MUNGUS.create(p_241840_1_);
    }

    public boolean isMushroomTarget(BlockPos pos) {
        if (this.getMushroomState() != null) {
            return level.getBlockState(pos).getBlock() == this.getMushroomState().getBlock();
        }
        return false;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return shouldFollowMushroom(stack) && this.getMushroomCount() < 5;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        if (shouldFollowMushroom(e.getItem())) {
            BlockState state = getMushroomBlockstate(e.getItem().getItem());
            if (this.getMushroomState() != null && state != null && state.getBlock() != this.getMushroomState().getBlock()) {
                this.setMushroomCount(0);
            }
            this.playSound(SoundEvents.FUNGUS_PLACE, this.getSoundVolume(), this.getVoicePitch());
            this.setMushroomState(state);
            this.setMushroomCount(this.getMushroomCount() + 1);
        }
    }

    private boolean canSeeMushroom(BlockPos destinationBlock) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
        Vector3d blockVec = net.minecraft.util.math.vector.Vector3d.atCenterOf(destinationBlock);
        BlockRayTraceResult result = this.level.clip(new RayTraceContext(Vector3d, blockVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }


    public boolean readyForShearing() {
        return this.isAlive() && this.getMushroomState() != null && this.getMushroomCount() > 0;
    }

    @Override
    public boolean isShearable(@javax.annotation.Nonnull ItemStack item, World world, BlockPos pos) {
        return readyForShearing();
    }

    @Override
    public void shear(SoundCategory category) {
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, category, 1.0F, 1.0F);
        if (!level.isClientSide() && this.getMushroomState() != null && this.getMushroomCount() > 0) {
            this.setMushroomCount(this.getMushroomCount() - 1);
            if (this.getMushroomCount() <= 0) {
                this.setMushroomState(null);
                this.setBeamTarget(null);
                beamCounter = Math.min(-1200, beamCounter);
            }

        }
    }

    @javax.annotation.Nonnull
    @Override
    public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable PlayerEntity player, @javax.annotation.Nonnull ItemStack item, World world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (!world.isClientSide() && this.getMushroomState() != null && this.getMushroomCount() > 0) {
            this.setMushroomCount(this.getMushroomCount() - 1);
            if (this.getMushroomCount() <= 0) {
                this.setMushroomState(null);
                this.setBeamTarget(null);
                beamCounter = Math.min(-1200, beamCounter);
            }

        }
        return java.util.Collections.emptyList();
    }

    public boolean isReverting() {
        return entityData.get(REVERTING);
    }

    public boolean isWarpedMoscoReady() {
        return this.getMushroomState() == Blocks.WARPED_FUNGUS.defaultBlockState() && this.getMushroomCount() >= 5;
    }


    class AITargetMushrooms extends Goal {
        private final int searchLength;
        protected BlockPos destinationBlock;
        protected int runDelay = 70;

        private AITargetMushrooms() {
            searchLength = 20;
        }

        public boolean canContinueToUse() {
            return destinationBlock != null && EntityMungus.this.isMushroomTarget(destinationBlock.mutable()) && isCloseToShroom(32);
        }

        public boolean isCloseToShroom(double dist) {
            return destinationBlock == null || EntityMungus.this.distanceToSqr(Vector3d.atCenterOf(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canUse() {
            if (EntityMungus.this.getBeamTarget() != null || EntityMungus.this.beamCounter < 0 || EntityMungus.this.getMushroomCount() <= 0) {
                return false;
            }
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 70 + EntityMungus.this.random.nextInt(150);
                return this.searchForDestination();
            }
        }

        public void start() {
        }

        public void tick() {
            if (this.destinationBlock == null || !EntityMungus.this.isMushroomTarget(this.destinationBlock) || EntityMungus.this.beamCounter < 0) {
                stop();
            } else {
                if (!EntityMungus.this.canSeeMushroom(this.destinationBlock)) {
                    EntityMungus.this.getNavigation().moveTo(this.destinationBlock.getX(), this.destinationBlock.getY(), this.destinationBlock.getZ(), 1D);
                } else {
                    EntityMungus.this.setBeamTarget(this.destinationBlock);
                    if(!EntityMungus.this.isInLove()){
                        EntityMungus.this.getNavigation().stop();
                    }
                }
            }
        }

        public void stop() {
            EntityMungus.this.setBeamTarget(null);
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            BlockPos lvt_3_1_ = EntityMungus.this.blockPosition();
            BlockPos.Mutable lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -5; lvt_5_1_ <= 5; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.setWithOffset(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isMushroom(EntityMungus.this.level, lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isMushroom(World world, BlockPos.Mutable lvt_4_1_) {
            return EntityMungus.this.isMushroomTarget(lvt_4_1_);
        }

    }
}