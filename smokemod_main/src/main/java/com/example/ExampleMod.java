package com.example;

import com.example.block.DryingTableBlock;
import com.example.block.JarBlock;
import com.example.block.TobaccoCropBlock;
import com.example.block.CannabisCropBlock;
import com.example.worldgen.CannabisPatchFeature;
import com.example.worldgen.CannabisWorldGen;
import com.example.block.entity.DryingTableBlockEntity;
import com.example.block.entity.JarBlockEntity;
import com.example.screen.DryingTableScreenHandler;
import com.example.screen.JarScreenHandler;
import com.example.effects.EffectManager;
import com.example.item.CigaretteItem;
import com.example.item.FilteredCigaretteItem;
import com.example.item.JointItem;
import com.example.item.CuredJointItem;
import com.example.nicotine.NicotineManager;
import com.example.worldgen.TobaccoPatchFeature;
import com.example.worldgen.TobaccoWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.shape.VoxelShapes;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "smokemod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Звук затяжки сигареты
	public static final Identifier SOUND_CIGARETTE_ID = Identifier.of(MOD_ID, "sound_cigarette");
	public static final SoundEvent SOUND_CIGARETTE = SoundEvent.of(SOUND_CIGARETTE_ID);

	// Звук выдоха после использования сигареты
	public static final Identifier SOUND_EXHALATION_ID = Identifier.of(MOD_ID, "exhalation50");
	public static final SoundEvent SOUND_EXHALATION = SoundEvent.of(SOUND_EXHALATION_ID);

	// Предмет сигареты
	// maxDamage(100) = максимальная прочность 100 (5 использований по 20% каждое)
	public static final Item CIGARETTE = new CigaretteItem(new Item.Settings().maxDamage(100));

	// Сигарета с фильтром — без эффекта серости
	public static final Item FILTERED_CIGARETTE = new FilteredCigaretteItem(new Item.Settings().maxDamage(100));

	// Предмет джоинта (аналог сигареты, своя модель и текстура)
	public static final Item JOINT = new JointItem(new Item.Settings().maxDamage(100));
	public static final Item CURED_JOINT = new CuredJointItem(new Item.Settings().maxDamage(100));

	// Блок культуры табака (5 стадий, стадии 3-4 двублочные)
	public static final TobaccoCropBlock TOBACCO_CROP = new TobaccoCropBlock(
			AbstractBlock.Settings.copy(Blocks.WHEAT));

	public static final CannabisCropBlock CANNABIS_CROP = new CannabisCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT));
	public static final Item CANNABIS_SEEDS = new AliasedBlockItem(CANNABIS_CROP, new Item.Settings());
	public static final Item CANNABIS_BUD = new Item(new Item.Settings());
	public static final Item GREEN_CANNABIS_LEAF = new Item(new Item.Settings());
	public static final Item DRIED_CANNABIS_BUD = new Item(new Item.Settings());
	public static final Item DRIED_CANNABIS_LEAF = new Item(new Item.Settings());
	public static final Item CURED_CANNABIS_BUD = new Item(new Item.Settings());

	// Стол для сушки
	public static final DryingTableBlock DRYING_TABLE = new DryingTableBlock(
			AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).nonOpaque());
	public static final BlockItem DRYING_TABLE_ITEM = new BlockItem(DRYING_TABLE, new Item.Settings());

	public static final JarBlock JAR = new JarBlock(
			AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque(),
			VoxelShapes.union(
					Block.createCuboidShape(5.47, 0.0, 5.47, 10.53, 6.325, 10.53),
					Block.createCuboidShape(6.41875, 6.325, 6.41875, 9.58125, 7.59, 9.58125),
					Block.createCuboidShape(6.1025, 7.59, 6.1025, 9.8975, 8.2225, 9.8975)),
			JarScreenHandler.SMALL_CAPACITY);
	public static final BlockItem JAR_ITEM = new BlockItem(JAR, new Item.Settings());

	// Большая банка — занимает почти весь блок
	public static final JarBlock JAR_LARGE = new JarBlock(
			AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque(),
			VoxelShapes.union(
					Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 2.0, 14.0),
					Block.createCuboidShape(1.75, 2.0, 1.75, 14.25, 4.0, 14.25),
					Block.createCuboidShape(1.5, 4.0, 1.5, 14.5, 10.0, 14.5),
					Block.createCuboidShape(1.75, 10.0, 1.75, 14.25, 12.0, 14.25),
					Block.createCuboidShape(3.0, 12.0, 3.0, 13.0, 15.0, 13.0),
					Block.createCuboidShape(2.75, 15.0, 2.75, 13.25, 16.0, 13.25)),
			JarScreenHandler.LARGE_CAPACITY);
	public static final BlockItem JAR_LARGE_ITEM = new BlockItem(JAR_LARGE, new Item.Settings());

	// BlockEntity + ScreenHandler для стола сушки
	public static BlockEntityType<DryingTableBlockEntity> DRYING_TABLE_BLOCK_ENTITY;
	public static ScreenHandlerType<DryingTableScreenHandler> DRYING_TABLE_SCREEN_HANDLER;
	public static BlockEntityType<JarBlockEntity> JAR_BLOCK_ENTITY;
	public static ScreenHandlerType<JarScreenHandler> JAR_SCREEN_HANDLER;
	public static ScreenHandlerType<JarScreenHandler> JAR_LARGE_SCREEN_HANDLER;

	// Зелёный лист табака — выпадает со спелого растения (стадия 4)
	public static final Item GREEN_TOBACCO_LEAF = new Item(new Item.Settings());

	// Высушенный лист табака — результат сушки в drying_table
	public static final Item DRIED_TOBACCO_LEAF = new Item(new Item.Settings());

	// Нарезанный табак — получается из высушенных листьев
	public static final Item CHOPPED_TOBACCO = new Item(new Item.Settings());

	// Семена табака — при использовании сажают TOBACCO_CROP на грядке
	public static final Item TOBACCO_SEEDS = new AliasedBlockItem(TOBACCO_CROP, new Item.Settings());

	// Кастомная частица дыма для сигареты
	public static final DefaultParticleType CIGARETTE_CLOUD = FabricParticleTypes.simple();

	// Кастомная вкладка в креативе
	public static final ItemGroup SMOKEMOD_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(CIGARETTE))
			.displayName(Text.translatable("itemGroup.smokemod.main"))
			.entries((context, entries) -> {
				entries.add(CIGARETTE);

				ItemStack litCigarette = new ItemStack(CIGARETTE);
				litCigarette.getOrCreateNbt().putBoolean("lit", true);
				entries.add(litCigarette);

				entries.add(FILTERED_CIGARETTE);

				ItemStack litFilteredCigarette = new ItemStack(FILTERED_CIGARETTE);
				litFilteredCigarette.getOrCreateNbt().putBoolean("lit", true);
				entries.add(litFilteredCigarette);

				entries.add(JOINT);

				ItemStack litJoint = new ItemStack(JOINT);
				litJoint.getOrCreateNbt().putBoolean("lit", true);
				entries.add(litJoint);
				entries.add(CURED_JOINT);
				ItemStack litCuredJoint = new ItemStack(CURED_JOINT);
				litCuredJoint.getOrCreateNbt().putBoolean("lit", true);
				entries.add(litCuredJoint);

				entries.add(TOBACCO_SEEDS);
				entries.add(CANNABIS_SEEDS);
				entries.add(CANNABIS_BUD);
				entries.add(GREEN_CANNABIS_LEAF);
				entries.add(DRIED_CANNABIS_BUD);
				entries.add(CURED_CANNABIS_BUD);
				entries.add(DRIED_CANNABIS_LEAF);
				entries.add(GREEN_TOBACCO_LEAF);
				entries.add(DRIED_TOBACCO_LEAF);
				entries.add(CHOPPED_TOBACCO);
				entries.add(DRYING_TABLE_ITEM);
				entries.add(JAR_ITEM);
				entries.add(JAR_LARGE_ITEM);
			})
			.build();

	@Override
	public void onInitialize() {
        com.example.block.entity.JarCuringClock.register();
		// Регистрация звуков в реестре
		Registry.register(Registries.SOUND_EVENT, SOUND_CIGARETTE_ID, SOUND_CIGARETTE);
		Registry.register(Registries.SOUND_EVENT, SOUND_EXHALATION_ID, SOUND_EXHALATION);

		// Регистрация кастомной частицы
		Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "cigarette_cloud"), CIGARETTE_CLOUD);

		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "cigarette"), CIGARETTE);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "filtered_cigarette"), FILTERED_CIGARETTE);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "joint"), JOINT);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "cured_joint"), CURED_JOINT);
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "tobacco_crop"), TOBACCO_CROP);
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "drying_table"), DRYING_TABLE);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "drying_table"), DRYING_TABLE_ITEM);
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "jar"), JAR);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "jar"), JAR_ITEM);
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "jar_large"), JAR_LARGE);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "jar_large"), JAR_LARGE_ITEM);

		JAR_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
				Identifier.of(MOD_ID, "jar"),
				BlockEntityType.Builder.create(JarBlockEntity::new, JAR, JAR_LARGE).build(null));
		JAR_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
				Identifier.of(MOD_ID, "jar"),
				new ScreenHandlerType<>((syncId, inventory) ->
						new JarScreenHandler(syncId, inventory, JarScreenHandler.SMALL_CAPACITY),
						net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));
		JAR_LARGE_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
				Identifier.of(MOD_ID, "jar_large"),
				new ScreenHandlerType<>((syncId, inventory) ->
						new JarScreenHandler(syncId, inventory, JarScreenHandler.LARGE_CAPACITY),
						net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

		DRYING_TABLE_BLOCK_ENTITY = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				Identifier.of(MOD_ID, "drying_table"),
				BlockEntityType.Builder.create(DryingTableBlockEntity::new, DRYING_TABLE).build(null));

		DRYING_TABLE_SCREEN_HANDLER = Registry.register(
				Registries.SCREEN_HANDLER,
				Identifier.of(MOD_ID, "drying_table"),
				new ScreenHandlerType<>(DryingTableScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "tobacco_seeds"), TOBACCO_SEEDS);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "green_tobacco_leaf"), GREEN_TOBACCO_LEAF);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "dried_tobacco_leaf"), DRIED_TOBACCO_LEAF);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "chopped_tobacco"), CHOPPED_TOBACCO);
		Registry.register(Registries.FEATURE, Identifier.of(MOD_ID, "tobacco_patch"),
				new TobaccoPatchFeature(DefaultFeatureConfig.CODEC));

		// Дикий табак спавнится в тёплых/умеренных биомах
		TobaccoWorldGen.register();
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "cannabis_crop"), CANNABIS_CROP);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "cannabis_seeds"), CANNABIS_SEEDS);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "cannabis_bud"), CANNABIS_BUD);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "green_cannabis_leaf"), GREEN_CANNABIS_LEAF);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "dried_cannabis_bud"), DRIED_CANNABIS_BUD);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "cured_cannabis_bud"), CURED_CANNABIS_BUD);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "dried_cannabis_leaf"), DRIED_CANNABIS_LEAF);
		Registry.register(Registries.FEATURE, Identifier.of(MOD_ID, "cannabis_patch"), new CannabisPatchFeature(DefaultFeatureConfig.CODEC));
		CannabisWorldGen.register();

		// Grass seed chances: tobacco 1.5%, cannabis 0.5%.
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (id.equals(new Identifier("minecraft", "blocks/grass"))
					|| id.equals(new Identifier("minecraft", "blocks/tall_grass"))) {
				tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1.0f))
						.with(ItemEntry.builder(CANNABIS_SEEDS))
						.conditionally(RandomChanceLootCondition.builder(0.005f))
						.build());
				tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1.0f))
						.with(ItemEntry.builder(TOBACCO_SEEDS))
						.conditionally(RandomChanceLootCondition.builder(0.015f))
						.build());
			}
		});

		// Регистрируем вкладку в креативе
		Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), SMOKEMOD_GROUP);

		// Система никотина (лёгких)
		NicotineManager.register();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			NicotineManager.onPlayerJoin(handler.player);
		});

		// Система эффектов
		EffectManager.register();

		// При смерти игрока — мгновенно сбрасываем все визуальные эффекты
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
			if (killedEntity instanceof ServerPlayerEntity deadPlayer) {
				EffectManager.clearEffects(deadPlayer);
			}
		});

		// Регистрация системы продолжительного выдоха
		com.example.item.ExhalationManager.register();

		LOGGER.info("Smoke Mod initialized"); 
	}
}
