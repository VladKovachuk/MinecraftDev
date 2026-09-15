# Карта важных файлов

Все пути ниже указаны относительно `smokemod_main/`.

## Точки входа

| Файл | Назначение |
|---|---|
| `src/main/java/com/example/ExampleMod.java` | Регистрация блоков, предметов, звуков, частиц, BlockEntity/ScreenHandler, worldgen, лута травы и серверных систем. |
| `src/client/java/com/example/ExampleModClient.java` | Render layers, GUI и renderer стола, model predicates `lit`, HUD и приём S2C-пакетов. |

## Предметы, никотин и эффекты

| Файл | Назначение |
|---|---|
| `src/main/java/com/example/item/CigaretteItem.java` | Общая механика сигареты/джоинта и параметры серости. |
| `src/main/java/com/example/item/FilteredCigaretteItem.java` | Сигарета с фильтром без серости. |
| `src/main/java/com/example/item/JointItem.java` | Подкласс общей сигаретной механики. |
| `src/main/java/com/example/item/ExhalationManager.java` | Продолжительный выдох дыма. |
| `src/main/java/com/example/nicotine/NicotineManager.java` | Никотин, убывание, урон и синхронизация. |
| `src/main/java/com/example/nicotine/NicotineState.java` | Сохранение никотина в данных мира. |
| `src/main/java/com/example/effects/EffectManager.java` | Серверное состояние визуальных эффектов и пакеты `effect_sync`/`effect_clear`. |
| `src/client/java/com/example/shader/ShaderManager.java` | Загрузка, плавное обновление и рендер серости. |

## Табак и worldgen

| Файл | Назначение |
|---|---|
| `src/main/java/com/example/block/TobaccoCropBlock.java` | Возраст 0–4, LOWER/UPPER, рост, удобрение и совместное разрушение половин. |
| `src/main/java/com/example/worldgen/TobaccoPatchFeature.java` | Размещение зрелых двухблочных кустов группой. |
| `src/main/java/com/example/worldgen/TobaccoWorldGen.java` | Подключение placed feature к разрешённым биомам. |
| `src/main/resources/data/smokemod/worldgen/configured_feature/wild_tobacco_patch.json` | Конфигурация кастомной feature. |
| `src/main/resources/data/smokemod/worldgen/placed_feature/wild_tobacco_patch.json` | Частота и placement modifiers. |
| `src/main/resources/data/smokemod/loot_tables/blocks/tobacco_crop.json` | Дроп культуры. |

## Сушильный стол

| Файл | Назначение |
|---|---|
| `src/main/java/com/example/block/DryingTableBlock.java` | BlockEntity, соединения сторон, форма, GUI, дроп инвентаря и ticker. |
| `src/main/java/com/example/block/entity/DryingTableBlockEntity.java` | Инвентарь, NBT, прогресс и превращение зелёных листьев в сушёные. `DRYING_TICKS` задаёт время. |
| `src/main/java/com/example/screen/DryingTableScreenHandler.java` | Девять рабочих слотов, координаты инвентаря, PropertyDelegate и безопасный Shift-click по одному предмету. |
| `src/client/java/com/example/screen/DryingTableScreen.java` | Масштабирование GUI 891×1001 до 176×200 и белый процент прогресса. |
| `src/client/java/com/example/client/render/DryingTableBlockEntityRenderer.java` | Отображение листьев на поверхности; `SURFACE_Y` задаёт высоту. |
| `src/main/resources/assets/smokemod/blockstates/drying_table.json` | Multipart-модель соединяемых столов. |

## Ключевые ресурсы

| Путь | Содержимое |
|---|---|
| `src/main/resources/assets/smokemod/models/item/` | Модели сигарет, листьев, семян, табака и фильтра. |
| `src/main/resources/assets/smokemod/textures/item/` | Текстуры предметов; `cigarette_filter.png` пока имеет модель, но предмет не зарегистрирован. |
| `src/main/resources/assets/smokemod/shaders/post/desaturation.json` | Цепочка пост-эффекта. |
| `src/main/resources/assets/minecraft/shaders/program/smokemod_desaturation.*` | Программа и fragment shader. |
| `src/main/resources/assets/smokemod/lang/en_us.json` | Английские отображаемые имена. |
| `src/main/resources/fabric.mod.json` | Метаданные и entrypoints. |

## Каннабис

- src/main/java/com/example/block/CannabisCropBlock.java — рост и разрушение трёхблочной культуры.
- src/main/java/com/example/worldgen/CannabisPatchFeature.java — генерация зрелых растений.
- src/main/java/com/example/worldgen/CannabisWorldGen.java — подключение к биомам.
- src/main/resources/assets/smokemod/blockstates/cannabis_crop.json — соответствие стадий текстурам.
- src/main/resources/data/smokemod/loot_tables/blocks/cannabis_crop.json — семена, соцветие и зелёный лист.
- src/main/resources/assets/smokemod/models/block/seedling.json — общая модель первой стадии табака и каннабиса.

