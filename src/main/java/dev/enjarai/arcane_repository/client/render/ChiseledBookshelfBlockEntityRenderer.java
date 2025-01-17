package dev.enjarai.arcane_repository.client.render;

import dev.enjarai.arcane_repository.item.custom.book.MysticalBookItem;
import dev.enjarai.arcane_repository.item.custom.page.type.ItemStorageTypePage;
import dev.enjarai.arcane_repository.util.BigStack;
import dev.enjarai.arcane_repository.util.ModifiedChiseledBookshelfBlockEntity;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

public class ChiseledBookshelfBlockEntityRenderer extends MildlyLessCringeBlockEntityRenderer<ChiseledBookshelfBlockEntity> {
    private final ItemCirclesRenderer circleRenderer = new ItemCirclesRenderer(true);

    public ChiseledBookshelfBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    final float totalTime = 2f;

    @Override
    protected void render(ChiseledBookshelfBlockEntity entity, float tickDelta, float frameDelta, long time, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var hitResult = MinecraftClient.getInstance().crosshairTarget;
        var mEntity = (ModifiedChiseledBookshelfBlockEntity) entity;

        if (hitResult instanceof BlockHitResult blockHitResult && entity.getPos().equals(blockHitResult.getBlockPos())) {

            var facing = entity.getCachedState().get(HorizontalFacingBlock.FACING);
            ChiseledBookshelfBlock.getHitPos(blockHitResult, facing).ifPresent(hitPos -> {


                var slot = ChiseledBookshelfBlock.getSlotForHitPos(hitPos);
                var bookStack = entity.getStack(slot);
                var book = bookStack.getItem();

                if (slot != mEntity.getLastSlot() || !blockHitResult.getBlockPos().equals(mEntity.getLastHitPos())) mEntity.setElapsed(0f);
                mEntity.setLastSlot(slot);
                mEntity.setLastHitPos(blockHitResult.getBlockPos());

                if (book instanceof MysticalBookItem bookItem && bookItem.getTypePage(bookStack) instanceof ItemStorageTypePage storagePage) {
                    var stacks = storagePage.getContents(bookStack).getAll();
                    if (stacks.isEmpty()) stacks = List.of(new BigStack(Items.AIR.getDefaultStack()));
                    var x = (slot % 3 / 3f + 1f / 6f) / 16f * 15f + 1f / 32f;
                    var y = slot / 3 / 2f + 1f / 4f;

                    matrices.push();

                    mEntity.setElapsed(mEntity.getElapsed() + frameDelta / 25);
                    if (mEntity.getElapsed() > totalTime) mEntity.setElapsed(totalTime);
                    float t = mEntity.getElapsed() / totalTime;

//                    t = (float) (1 - Math.pow(1 - t, 3)); // ease-out-cubic
//                    var scale = MathHelper.lerp(t, 0f, 0.01f);

                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - facing.asRotation()), 0.5f, 0f, 0.5f);
                    matrices.translate(1.0f - x, 1.0f - y, 0f);
//                    matrices.scale(scale, scale, scale);
                    matrices.scale(0.01f, 0.01f, 0.01f);
                    circleRenderer.render(matrices, 0, 0, 0, 0xF000F0, t, stacks);

                    matrices.pop();
                }
            });
        } else {
            mEntity.setElapsed(0f);
        }
    }
}
