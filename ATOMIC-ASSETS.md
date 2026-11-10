# Atomic — загрузка моделей, изображений и звуков

Все пользовательские ресурсы загружай в `src/main/resources/atomic-assets/`. После `gradle clean build` они попадут в создаваемый resource pack внутри JAR.

## Папки для загрузки

- `atomic-assets/photos/` — PNG-картинки/фото для текстур; сохраняй подпапки, например `photos/gui/radiation_warning.png`. В паке будут доступны как `beforethefall:textures/photos/gui/radiation_warning.png`.
- `atomic-assets/textures/` — игровые PNG-текстуры: `textures/item/`, `textures/block/`, `textures/gui/`.
- `atomic-assets/models/` — готовые Minecraft JSON-модели, например `models/item/custom_item.json`.
- `atomic-assets/sounds/blast/` — три звука ядерного взрыва: `close.ogg`, `wave.ogg`, `distant.ogg`.
- `atomic-assets/sounds/nuke/` — `charge.ogg`, `launch.ogg`, `blast.ogg`, `impact.ogg`, `fallout.ogg`.

Звуки должны быть OGG Vorbis. Звуковые события регистрируются автоматически, только если соответствующий файл существует. Отдельного подземного звука нет.

## Blockbench

Исходные `.bbmodel` клади в `src/main/resources/blockbench/`. В модели должна быть встроенная PNG-текстура. Проект автоматически экспортирует модели из этой папки. Дозиметр уже находится в `blockbench/dosimeter.bbmodel` и подключён к предмету CLOCK через CustomModelData 30.

## Дозиметр

Команда администратора: `/bft dosimeter`. Она выдаёт предмет дозиметра. Возьми его в руку и нажми ПКМ: он покажет текущий местный радиационный фон и накопленную дозу игрока. Значения местного фона — условные игровые единицы, это шкала игрового плагина, не реальные µSv/h.

## Противогазовый HUD

`textures/gui/gas_mask_overlay.png` можно положить в `atomic-assets/textures/gui/`. Сам по себе чистый Paper не может включить условный HUD-оверлей на клиенте; для его показа поверх экрана понадобится клиентский мод.

## Сборка

Используй Java 21 и `gradle --no-daemon clean test build`. После замены текстур/звуков игрокам может потребоваться обновить или очистить кэш resource pack.
