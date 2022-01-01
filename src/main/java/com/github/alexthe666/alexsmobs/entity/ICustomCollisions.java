package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.citadel.server.entity.collision.CitadelVoxelShapeSpliterator;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ReuseableStream;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ICustomCollisions {
    static Vector3d getAllowedMovementForEntity(Entity entity, Vector3d vec) {
        AxisAlignedBB axisalignedbb = entity.getBoundingBox();
        ISelectionContext iselectioncontext = ISelectionContext.of(entity);
        VoxelShape voxelshape = entity.level.getWorldBorder().getCollisionShape();
        Stream<VoxelShape> stream = VoxelShapes.joinIsNotEmpty(voxelshape, VoxelShapes.create(axisalignedbb.deflate(1.0E-7D)), IBooleanFunction.AND) ? Stream.empty() : Stream.of(voxelshape);
        Stream<VoxelShape> stream1 = entity.level.getEntityCollisions(entity, axisalignedbb.expandTowards(vec), (p_233561_0_) -> {
            return true;
        });
        ReuseableStream<VoxelShape> reuseablestream = new ReuseableStream(Stream.concat(stream1, stream));
        Vector3d vector3d = vec.lengthSqr() == 0.0D ? vec : ((ICustomCollisions)entity).collideBoundingBoxHeuristicallyPassable(entity, vec, axisalignedbb, entity.level, iselectioncontext, reuseablestream);
        boolean flag = vec.x != vector3d.x;
        boolean flag1 = vec.y != vector3d.y;
        boolean flag2 = vec.z != vector3d.z;
        boolean flag3 = entity.isOnGround() || flag1 && vec.y < 0.0D;
        if (entity.maxUpStep > 0.0F && flag3 && (flag || flag2)) {
            Vector3d vector3d1 = ((ICustomCollisions)entity).collideBoundingBoxHeuristicallyPassable(entity, new Vector3d(vec.x, (double)entity.maxUpStep, vec.z), axisalignedbb, entity.level, iselectioncontext, reuseablestream);
            Vector3d vector3d2 = ((ICustomCollisions)entity).collideBoundingBoxHeuristicallyPassable(entity, new Vector3d(0.0D, (double)entity.maxUpStep, 0.0D), axisalignedbb.expandTowards(vec.x, 0.0D, vec.z), entity.level, iselectioncontext, reuseablestream);
            if (vector3d2.y < (double)entity.maxUpStep) {
                Vector3d vector3d3 = ((ICustomCollisions)entity).collideBoundingBoxHeuristicallyPassable(entity, new Vector3d(vec.x, 0.0D, vec.z), axisalignedbb.move(vector3d2), entity.level, iselectioncontext, reuseablestream).add(vector3d2);
                if (Entity.getHorizontalDistanceSqr(vector3d3) > Entity.getHorizontalDistanceSqr(vector3d1)) {
                    vector3d1 = vector3d3;
                }
            }

            if (Entity.getHorizontalDistanceSqr(vector3d1) > Entity.getHorizontalDistanceSqr(vector3d)) {
                return vector3d1.add(((ICustomCollisions)entity).collideBoundingBoxHeuristicallyPassable(entity, new Vector3d(0.0D, -vector3d1.y + vec.y, 0.0D), axisalignedbb.move(vector3d1), entity.level, iselectioncontext, reuseablestream));
            }
        }

        return vector3d;
    }

    boolean canPassThrough(BlockPos var1, BlockState var2, VoxelShape var3);

    default Vector3d collideBoundingBoxHeuristicallyPassable(@Nullable Entity entity, Vector3d vec, AxisAlignedBB collisionBox, World world, ISelectionContext context, ReuseableStream<VoxelShape> potentialHits) {
        boolean flag = vec.x == 0.0D;
        boolean flag1 = vec.y == 0.0D;
        boolean flag2 = vec.z == 0.0D;
        if (flag && flag1 || flag && flag2 || flag1 && flag2) {
            return Entity.collideBoundingBox(vec, collisionBox, world, context, potentialHits);
        } else {
            ReuseableStream<VoxelShape> reuseablestream = new ReuseableStream(Stream.concat(potentialHits.getStream(), StreamSupport.stream(new CitadelVoxelShapeSpliterator(world, entity, collisionBox.expandTowards(vec)), false)));
            return Entity.collideBoundingBoxLegacy(vec, collisionBox, reuseablestream);
        }
    }
}