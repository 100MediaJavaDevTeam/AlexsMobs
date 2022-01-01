package com.github.alexthe666.alexsmobs.tileentity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.BlockCapsid;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.message.MessageUpdateCapsid;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndRodBlock;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public class TileEntityCapsid extends LockableTileEntity implements ITickableTileEntity, ISidedInventory {
    private static final int[] slotsTop = new int[]{0};
    public int ticksExisted;
    public float prevFloatUpProgress;
    public float floatUpProgress;
    public float prevYawSwitchProgress;
    public float yawSwitchProgress;
    public boolean vibrating = false;
    net.minecraftforge.common.util.LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
            net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN);
    private float yawTarget = 0;
    private int transformProg = 0;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);

    public TileEntityCapsid() {
        super(AMTileEntityRegistry.CAPSID);
    }

    @Override
    public void tick() {
        prevFloatUpProgress = floatUpProgress;
        prevYawSwitchProgress = yawSwitchProgress;
        ticksExisted++;
        vibrating = false;
        if (!this.getItem(0).isEmpty()) {
            TileEntity up = level.getBlockEntity(this.worldPosition.above());
            if (up instanceof IInventory) {
                if (floatUpProgress >= 1) {
                    LazyOptional<IItemHandler> handler = level.getBlockEntity(this.worldPosition.above()).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP);
                    if (handler.orElse(null) != null) {
                        if (ItemHandlerHelper.insertItem(handler.orElse(null), this.getItem(0), true).isEmpty()) {
                            ItemHandlerHelper.insertItem(handler.orElse(null), this.getItem(0).copy(), false);
                            this.setItem(0, ItemStack.EMPTY);
                        }
                    }
                    yawTarget = 0F;
                    yawSwitchProgress = 0F;
                } else {
                    if (up instanceof TileEntityCapsid) {
                        yawTarget = MathHelper.wrapDegrees(((TileEntityCapsid) up).getBlockAngle() - this.getBlockAngle());
                    }else{
                        yawTarget = 0F;
                    }
                    if(yawTarget < yawSwitchProgress){
                        yawSwitchProgress += yawTarget * 0.1F;
                    }else if(yawTarget > yawSwitchProgress){
                        yawSwitchProgress += yawTarget * 0.1F;
                    }
                    floatUpProgress += 0.05F;
                }
            } else {
                floatUpProgress = 0F;
            }
            if(this.getItem(0).getItem() == Items.ENDER_EYE && level.getBlockState(this.getBlockPos().below()).getBlock() == Blocks.END_ROD && level.getBlockState(this.getBlockPos().below()).getValue(EndRodBlock.FACING).getAxis() == Direction.Axis.Y){
                vibrating = true;
                if(transformProg > 20){
                    this.setItem(0, ItemStack.EMPTY);
                    this.level.destroyBlock(this.getBlockPos(), false);
                    this.level.destroyBlock(this.getBlockPos().below(), false);
                    EntityEnderiophage phage = AMEntityRegistry.ENDERIOPHAGE.create(level);
                    phage.setPos(this.getBlockPos().getX() + 0.5F, this.getBlockPos().getY() - 1.0F, this.getBlockPos().getZ() + 0.5F);
                    phage.setVariant(0);
                    if(!level.isClientSide){
                        level.addFreshEntity(phage);
                    }
                }
            }
            if(this.getItem(0).getItem() == AMItemRegistry.MOSQUITO_LARVA && level.getBlockState(this.getBlockPos().above()).getBlock() != this.getBlockState().getBlock()) {
                vibrating = true;
                if(transformProg > 60) {
                    ItemStack current = this.getItem(0).copy();
                    current.shrink(1);
                    if(!current.isEmpty()){
                        ItemEntity itemEntity = new ItemEntity(this.level, this.getBlockPos().getX() + 0.5F, this.getBlockPos().getY() + 0.5F, this.getBlockPos().getZ() + 0.5F, current);
                        if(!level.isClientSide){
                            level.addFreshEntity(itemEntity);
                        }
                    }
                    this.setItem(0, new ItemStack(AMItemRegistry.MYSTERIOUS_WORM));
                }
            }
            if(this.getItem(0).getItem().is(ItemTags.MUSIC_DISCS) && this.getItem(0).getItem() != AMItemRegistry.MUSIC_DISC_DAZE && level.getBlockState(this.getBlockPos().above()).getBlock() != this.getBlockState().getBlock()) {
                vibrating = true;
                if(transformProg > 120) {
                    ItemStack current = this.getItem(0).copy();
                    current.shrink(1);
                    if(!current.isEmpty()){
                        ItemEntity itemEntity = new ItemEntity(this.level, this.getBlockPos().getX() + 0.5F, this.getBlockPos().getY() + 0.5F, this.getBlockPos().getZ() + 0.5F, current);
                        if(!level.isClientSide){
                            level.addFreshEntity(itemEntity);
                        }
                    }
                    this.setItem(0, new ItemStack(AMItemRegistry.MUSIC_DISC_DAZE));
                }
            }
            } else {
            floatUpProgress = 0F;
        }
        if(!vibrating){
            transformProg = 0;
        }else{
            transformProg++;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        return new net.minecraft.util.math.AxisAlignedBB(worldPosition, worldPosition.offset(1, 2, 1));
    }

    @Override
    public int getContainerSize() {
        return this.stacks.size();
    }

    @Override
    public ItemStack getItem(int index) {
        return this.stacks.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (!this.stacks.get(index).isEmpty()) {
            ItemStack itemstack;

            if (this.stacks.get(index).getCount() <= count) {
                itemstack = this.stacks.get(index);
                this.stacks.set(index, ItemStack.EMPTY);
                return itemstack;
            } else {
                itemstack = this.stacks.get(index).split(count);

                if (this.stacks.get(index).isEmpty()) {
                    this.stacks.set(index, ItemStack.EMPTY);
                }

                return itemstack;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    public ItemStack getStackInSlotOnClosing(int index) {
        if (!this.stacks.get(index).isEmpty()) {
            ItemStack itemstack = this.stacks.get(index);
            this.stacks.set(index, itemstack);
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        boolean flag = !stack.isEmpty() && stack.sameItem(this.stacks.get(index)) && ItemStack.tagMatches(stack, this.stacks.get(index));
        this.stacks.set(index, stack);

        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.save(this.getUpdateTag());
        if (!level.isClientSide) {
            AlexsMobs.sendMSGToAll(new MessageUpdateCapsid(this.getBlockPos().asLong(), stacks.get(0)));
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT compound) {
        super.load(state, compound);
        this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, this.stacks);
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        super.save(compound);
        ItemStackHelper.saveAllItems(compound, this.stacks);
        return compound;
    }

    @Override
    public void startOpen(PlayerEntity player) {
    }

    @Override
    public void stopOpen(PlayerEntity player) {
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.stacks.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slotsTop;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return true;
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(worldPosition, -1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        load(this.getBlockState(), packet.getTag());
    }

    public CompoundNBT getUpdateTag() {
        return this.save(new CompoundNBT());
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack lvt_2_1_ = this.stacks.get(index);
        if (lvt_2_1_.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.stacks.set(index, ItemStack.EMPTY);
            return lvt_2_1_;
        }
    }

    @Override
    public ITextComponent getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("block.alexsmobs.capsid");
    }

    @Override
    protected Container createMenu(int id, PlayerInventory player) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public float getBlockAngle() {
        if (this.getBlockState().getBlock() instanceof BlockCapsid) {
            Direction dir = this.getBlockState().getValue(BlockCapsid.HORIZONTAL_FACING);
            return dir.toYRot();
        }
        return 0.0F;
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (!this.remove && facing != null && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == Direction.DOWN)
                return handlers[0].cast();
            else
                return handlers[1].cast();
        }
        return super.getCapability(capability, facing);
    }
}
