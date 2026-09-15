# Архитектура Smoke Mod

## Технологии

| Слой | Технология |
|---|---|
| Игра | Minecraft Java Edition 1.20.1 |
| Загрузчик/API | Fabric Loader + Fabric API |
| Язык | Java 17 |
| Сборка | Gradle, Fabric Loom, split source sets `main`/`client` |
| Состояние мира | Minecraft `PersistentState` и NBT BlockEntity |

## Основные модули

```text
smokemod_main/src/main/java/com/example/
├── ExampleMod.java
├── block/
│   ├── TobaccoCropBlock.java
│   ├── DryingTableBlock.java
│   └── entity/DryingTableBlockEntity.java
├── screen/DryingTableScreenHandler.java
├── worldgen/
│   ├── TobaccoPatchFeature.java
│   └── TobaccoWorldGen.java
├── item/
│   ├── CigaretteItem.java
│   ├── FilteredCigaretteItem.java
│   ├── JointItem.java
│   └── ExhalationManager.java
├── nicotine/{NicotineManager,NicotineState}.java
└── effects/EffectManager.java

smokemod_main/src/client/java/com/example/
├── ExampleModClient.java
├── screen/DryingTableScreen.java
├── client/render/DryingTableBlockEntityRenderer.java
├── shader/ShaderManager.java
├── particle/CigaretteCloudParticle.java
└── mixin/client/{GameRendererMixin,PostEffectProcessorAccessor}.java
```

## Поток курения и эффектов

```text
CigaretteItem.finishUsing
├── меняет NBT lit и прочность
├── NicotineManager.addPuff → PersistentState → nicotine_sync → HUD
├── ExhalationManager → частицы дыма
└── EffectManager.addEffect(DESATURATION, параметры предмета)
    └── effect_sync → ShaderManager target/current interpolation
        └── GameRendererMixin → PostEffectProcessor.render
```

`FilteredCigaretteItem` переопределяет дозу и максимум серости в `0.0f`, сохраняя общую механику родительского класса.

## Поток выращивания

```text
tobacco_seeds (AliasedBlockItem)
→ TobaccoCropBlock age 0..4
→ при переходе 2→3 создаётся UPPER
→ зрелая культура age=4
→ loot table: семена + green_tobacco_leaf
```

`TobaccoPatchFeature` напрямую размещает зрелые LOWER/UPPER растения на разрешённой поверхности. `TobaccoWorldGen` подключает placed feature только к выбранным тёплым/умеренным биомам.

## Поток сушки

```text
ПКМ DryingTableBlock
→ DryingTableBlockEntity.createMenu
→ DryingTableScreenHandler (9 рабочих слотов + инвентарь игрока)
→ DryingTableScreen

server ticker
→ если есть green_tobacco_leaf: progress += 1/1200 каждый тик
→ при 100% каждый зелёный лист в слотах 1..9 заменяется dried_tobacco_leaf
→ NBT + PropertyDelegate + BlockEntity update packet
→ GUI получает процент, renderer получает содержимое слотов
```

BlockEntity содержит 10 слотов для совместимости с ранее созданной структурой, но слот 0 не отображается и не участвует в текущем рецепте. Рабочие слоты 1–9 имеют лимит один предмет.

## Ресурсы сушильного стола

- `blockstates/drying_table.json` — multipart по состояниям соседей.
- `models/block/drying_table_net.json` — постоянная сетка.
- `models/block/drying_table_balk.json` — поворачиваемая боковая балка.
- `textures/gui/container/drying_table.png` — GUI 891×1001, в коде масштабируется до 176×200.
- `DryingTableScreenHandler` хранит координаты кликабельных слотов; `DryingTableScreen` отвечает только за фон и процент.

## Шейдеры Minecraft 1.20.1

`PostEffectProcessor` собирает путь программы через одноаргументный `Identifier`, поэтому программа и GLSL размещены в `assets/minecraft/shaders/program/` с уникальным префиксом `smokemod_`. JSON цепочки может оставаться в namespace `smokemod`.

В JSON следует объявлять только реально используемые GLSL uniform-переменные: компилятор удаляет неиспользуемые uniform, после чего загрузчик падает с `Uniform 'X' does not exist`.

## Каннабис (2026-09-07)

- Зарегистрированы `cannabis_seeds`, `cannabis_bud`, `green_cannabis_leaf`; все три предмета доступны во вкладке Smoke Mod, используют пользовательские текстуры и имеют английские/русские названия.
- `CannabisCropBlock`: `age=0..5`, `part=0..2`. Посадка семян на пашню, естественный рост и костная мука по одной стадии; переход к большей высоте требует свободного места.
- Стадии снизу вверх: `stage0`; `stage1`; `stage2`; `stage2 + mid0`; `stage3 + mid1`; `stage4 + mid2 + top` (все текстуры имеют префикс `cannabis_crop_`).
- Дроп растения: одно семя на любой стадии; зрелое дополнительно даёт один `cannabis_bud` и один `green_cannabis_leaf`. При разрушении любой части игроком удаляется весь куст; в креативе добыча не выпадает.
- Шанс семян из обычной/высокой травы — 0,5% (табак — 1,5%).
- `CannabisPatchFeature` и `CannabisWorldGen` создают зрелые трёхблочные растения в тех же тёплых/умеренных биомах, что табак. Rarity filter: 150 против 50 у табака, то есть попытки генерации групп втрое реже; 8 попыток в радиусе 4 блоков.
- Зрелые растения сохраняются на естественной почве, используемой генерацией. Новая генерация появляется в новых чанках.
- Ресурсы: `blockstates/cannabis_crop.json`, `models/block/cannabis_crop_*.json`, три item-модели, `loot_tables/blocks/cannabis_crop.json`, configured/placed feature `wild_cannabis_patch.json`.
