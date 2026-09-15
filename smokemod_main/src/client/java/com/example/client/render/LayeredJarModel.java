package com.example.client.render;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/** Keeps opaque contents out of the glass's camera-sorted translucent pass. */
public final class LayeredJarModel extends ForwardingBakedModel {
    private static final Identifier CONTENTS = new Identifier("smokemod", "block/jar_packed_buds");
    private static final Identifier SMALL_CONTENTS = new Identifier("smokemod", "item/cannabis_bud");
    private final RenderMaterial contentsMaterial;
    private final RenderMaterial glassMaterial;

    private LayeredJarModel(BakedModel original, Renderer renderer) {
        wrapped = original;
        contentsMaterial = renderer.materialFinder().blendMode(BlendMode.CUTOUT).find();
        glassMaterial = renderer.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
    }

    public static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register(
                ModelModifier.WRAP_PHASE, (model, context) -> {
                    Identifier id = context.id();
                    if (model == null || model instanceof LayeredJarModel
                            || !id.getNamespace().equals("smokemod")
                            || !isJarModel(id.getPath())) {
                        return model;
                    }
                    Renderer renderer = RendererAccess.INSTANCE.getRenderer();
                    return renderer == null ? model : new LayeredJarModel(model, renderer);
                }));
    }

    private static boolean isJarModel(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.equals("jar") || name.equals("jar_large")
                || name.equals("jar_empty") || name.equals("jar_large_empty");
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView view, BlockState state, BlockPos pos,
            Supplier<Random> randomSupplier, RenderContext context) {
        emit(state, randomSupplier, context, true);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        emit(null, randomSupplier, context, false);
    }

    private void emit(BlockState state, Supplier<Random> randomSupplier, RenderContext context,
            boolean block) {
        QuadEmitter emitter = context.getEmitter();
        for (int faceIndex = 0; faceIndex < 7; faceIndex++) {
            Direction face = faceIndex == 6 ? null : Direction.byId(faceIndex);
            for (BakedQuad quad : wrapped.getQuads(state, face, randomSupplier.get())) {
                Identifier texture = quad.getSprite().getContents().getId();
                RenderMaterial material = (CONTENTS.equals(texture) || SMALL_CONTENTS.equals(texture))
                        ? contentsMaterial : glassMaterial;
                emitter.fromVanilla(quad, material, block ? face : null).emit();
            }
        }
    }
}
