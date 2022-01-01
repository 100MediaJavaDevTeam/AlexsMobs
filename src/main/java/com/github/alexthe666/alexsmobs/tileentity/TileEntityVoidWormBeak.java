package com.github.alexthe666.alexsmobs.tileentity;

import com.github.alexthe666.alexsmobs.block.BlockVoidWormBeak;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;

public class TileEntityVoidWormBeak extends TileEntity implements ITickableTileEntity {

    private float chompProgress;
    private float prevChompProgress;
    public int ticksExisted;


    public TileEntityVoidWormBeak() {
        super(AMTileEntityRegistry.VOID_WORM_BEAK);
    }

    @Override
    public void tick() {
        prevChompProgress = chompProgress;
        boolean powered = false;
        if(getBlockState().getBlock() instanceof BlockVoidWormBeak){
            powered = getBlockState().getValue(BlockVoidWormBeak.POWERED);
        }
        if(powered && chompProgress < 5F){
            chompProgress++;
        }
        if(!powered && chompProgress > 0F){
            chompProgress--;
        }
        if(chompProgress >= 5F && !level.isClientSide && ticksExisted % 5 == 0){
            float i = this.getBlockPos().getX() + 0.5F;
            float j = this.getBlockPos().getY() + 0.5F;
            float k = this.getBlockPos().getZ() + 0.5F;
            float d0 = 0.5F;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB((double) i - d0, (double) j - d0, (double) k - d0, (double) i + d0, (double) j + d0, (double) k + d0))) {
                entity.hurt(DamageSource.FALLING_BLOCK, 5);
            }
        }
        ticksExisted++;
    }

    public float getChompProgress(float partialTick){
        return prevChompProgress + (chompProgress - prevChompProgress) * partialTick;
    }
}
