package com.example.worldgen;

import com.example.ExampleMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class TobaccoWorldGen {

    public static final RegistryKey<PlacedFeature> WILD_TOBACCO_PATCH =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                    Identifier.of(ExampleMod.MOD_ID, "wild_tobacco_patch"));

    public static void register() {
        // Warm/temperate biomes only; no cold biomes and no deserts
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        BiomeKeys.PLAINS,
                        BiomeKeys.SUNFLOWER_PLAINS,
                        BiomeKeys.SAVANNA,
                        BiomeKeys.SAVANNA_PLATEAU,
                        BiomeKeys.WINDSWEPT_SAVANNA,
                        BiomeKeys.JUNGLE,
                        BiomeKeys.SPARSE_JUNGLE,
                        BiomeKeys.BAMBOO_JUNGLE,
                        BiomeKeys.SWAMP,
                        BiomeKeys.MANGROVE_SWAMP,
                        BiomeKeys.MEADOW
                ),
                GenerationStep.Feature.VEGETAL_DECORATION,
                WILD_TOBACCO_PATCH
        );
    }
}
