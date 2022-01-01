package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

public class EntityEmuEgg extends ProjectileItemEntity {

    public EntityEmuEgg(EntityType p_i50154_1_, World p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityEmuEgg(World worldIn, LivingEntity throwerIn) {
        super(AMEntityRegistry.EMU_EGG, throwerIn, worldIn);
    }

    public EntityEmuEgg(World worldIn, double x, double y, double z) {
        super(AMEntityRegistry.EMU_EGG, x, y, z, worldIn);
    }

    public EntityEmuEgg(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.EMU_EGG, world);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            double d0 = 0.08D;

            for (int i = 0; i < 8; ++i) {
                this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            if (this.random.nextInt(8) == 0) {
                int lvt_2_1_ = 1;
                if (this.random.nextInt(32) == 0) {
                    lvt_2_1_ = 4;
                }
                for (int lvt_3_1_ = 0; lvt_3_1_ < lvt_2_1_; ++lvt_3_1_) {
                    EntityEmu lvt_4_1_ = AMEntityRegistry.EMU.create(this.level);
                    if(this.random.nextInt(50) == 0){
                        lvt_4_1_.setVariant(2);
                    }else if(random.nextInt(3) == 0){
                        lvt_4_1_.setVariant(1);
                    }
                    lvt_4_1_.setAge(-24000);
                    lvt_4_1_.moveTo(this.getX(), this.getY(), this.getZ(), this.yRot, 0.0F);
                    this.level.addFreshEntity(lvt_4_1_);
                }
            }
            this.level.broadcastEntityEvent(this, (byte) 3);
            this.remove();
        }

    }

    protected Item getDefaultItem() {
        return AMItemRegistry.COCKROACH_OOTHECA;
    }
}
