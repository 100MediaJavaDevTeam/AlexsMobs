package com.github.alexthe666.alexsmobs.message;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageKangarooEat {

    public int kangaroo;
    public ItemStack stack;

    public MessageKangarooEat(int kangaroo, ItemStack stack) {
        this.kangaroo = kangaroo;
        this.stack = stack;
    }

    public MessageKangarooEat() {
    }

    public static MessageKangarooEat read(PacketBuffer buf) {
        return new MessageKangarooEat(buf.readInt(), buf.readItem());
    }

    public static void write(MessageKangarooEat message, PacketBuffer buf) {
        buf.writeInt(message.kangaroo);
        buf.writeItem(message.stack);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageKangarooEat message, Supplier<NetworkEvent.Context> context) {
            context.get().setPacketHandled(true);
            PlayerEntity player = context.get().getSender();
            if(context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT){
                player = AlexsMobs.PROXY.getClientSidePlayer();
            }

            if (player != null) {
                if (player.level != null) {
                    Entity entity = player.level.getEntity(message.kangaroo);
                    if(entity instanceof EntityKangaroo && ((EntityKangaroo) entity).kangarooInventory != null){
                        EntityKangaroo kangaroo = (EntityKangaroo)entity;
                        for (int i = 0; i < 7; i++) {
                            double d2 = kangaroo.getRandom().nextGaussian() * 0.02D;
                            double d0 = kangaroo.getRandom().nextGaussian() * 0.02D;
                            double d1 = kangaroo.getRandom().nextGaussian() * 0.02D;
                            entity.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, message.stack), entity.getX() + (double) (kangaroo.getRandom().nextFloat() * entity.getBbWidth()) - (double) entity.getBbWidth() * 0.5F, entity.getY() + entity.getBbHeight() * 0.5F + (double) (kangaroo.getRandom().nextFloat() * entity.getBbHeight() * 0.5F), entity.getZ() + (double) (kangaroo.getRandom().nextFloat() * entity.getBbWidth()) - (double) entity.getBbWidth() * 0.5F, d0, d1, d2);
                        }
                    }
                }
            }
        }
    }
}