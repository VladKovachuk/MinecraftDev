package com.example.client.render;

import com.example.block.entity.DryingTableBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;

/**
 * Renders items lying on top of the drying table.
 * Port of Psychedelicraft's TileEntityRendererDryingTable: deterministic scatter of
 * input items across the surface, the result item goes to the exact center and is
 * scaled up 1.5x.
 */
public class DryingTableBlockEntityRenderer implements BlockEntityRenderer<DryingTableBlockEntity> {

    // Top of the block: 6 pixels tall → 6/16 = 0.375
    private static final float SURFACE_Y = 5.0f / 16.0f;

    public DryingTableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(DryingTableBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        BlockPos pos = entity.getPos();
        // Same deterministic seed pattern as Psychedelicraft — item positions stay stable per block
        Random random = new Random(pos.getX() * 100L + pos.getY() * 10L + pos.getZ());
        MinecraftClient client = MinecraftClient.getInstance();

        for (int i = 0; i < entity.size(); i++) {
            ItemStack stack = entity.getStack(i);
            if (stack.isEmpty()) continue;

            boolean isResult = (i == DryingTableBlockEntity.SLOT_RESULT);

            float posX = isResult ? 0.5f : 0.35f + random.nextFloat() * 0.3f;
            float posZ = isResult ? 0.5f : 0.35f + random.nextFloat() * 0.3f;
            float rotation = random.nextFloat() * 360.0f;

            matrices.push();
            // Place onto the top surface; small per-slot Y offset to avoid z-fighting between stacked items
            matrices.translate(posX, SURFACE_Y + 0.002f + i / 500.0f, posZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            // Lay flat like a dropped item resting on a table
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));

            float scale = isResult ? 0.375f : 0.25f * 1.5f; // размер предмета на сушильном столе
            matrices.scale(scale, scale, scale);

            client.getItemRenderer().renderItem(
                    stack,
                    ModelTransformationMode.FIXED,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    vertexConsumers,
                    entity.getWorld(),
                    0
            );
            matrices.pop();
        }
    }
}
