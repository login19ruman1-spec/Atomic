# Atomic — как добавлять свои модели, текстуры и звуки

Все пользовательские файлы складывай в `src/main/resources/atomic-assets/`. После этого обычная сборка `gradle clean build` автоматически добавит их в Resource Pack внутри JAR.

## 1. PNG-текстуры

Клади PNG, сохраняя структуру:

- `atomic-assets/textures/item/my_item.png` → `beforethefall:textures/item/my_item.png`
- `atomic-assets/textures/gui/gas_mask_overlay.png` → `beforethefall:textures/gui/gas_mask_overlay.png`
- `atomic-assets/textures/block/my_block.png` → `beforethefall:textures/block/my_block.png`

Оверлей противогаза: `textures/gui/gas_mask_overlay.png`. Рекомендуется PNG с прозрачностью и размером 256×256 или 512×512. Он будет включён в ресурс-пак, но сам по себе не появится поверх камеры: для условного отображения при надетом противогазе нужен клиентский мод/клиентская часть. На чистом Paper сервер не может менять HUD-слой игрока.

## 2. Blockbench-модели

Исходники `.bbmodel` клади в `src/main/resources/blockbench/` (эта папка уже используется экспортёром проекта). В Blockbench перед сохранением встрои/импортируй PNG-текстуру в саму модель, чтобы у неё была embedded data-URL текстура. Экспортёр проекта читает встроенную текстуру из BBModel.

Если модель уже экспортирована в Minecraft JSON, положи её в `atomic-assets/models/item/my_model.json`, а PNG — в `atomic-assets/textures/item/my_model.png`. В JSON укажи текстуру namespace `beforethefall:item/my_model`.

## 3. Звуки взрыва

Используй OGG Vorbis (не MP3/WAV), 44.1 или 48 kHz. Положи файлы:

- `atomic-assets/sounds/nuke/charge.ogg`
- `atomic-assets/sounds/nuke/launch.ogg`
- `atomic-assets/sounds/nuke/blast.ogg`
- `atomic-assets/sounds/nuke/impact.ogg`
- `atomic-assets/sounds/nuke/fallout.ogg`

Сборка автоматически зарегистрирует те события, для которых найдены файлы:

- `beforethefall:nuke_charge`
- `beforethefall:nuke_launch`
- `beforethefall:nuke_blast`
- `beforethefall:nuke_impact`
- `beforethefall:nuke_fallout`

Это регистрация звуков в Resource Pack. Чтобы они реально проигрывались во время взрыва, код плагина должен вызвать соответствующие sound event ID; загрузка файлов сама по себе не добавляет вызовы воспроизведения.

## 4. Сборка и проверка

1. Скопируй свои файлы в указанные папки.
2. Запусти `gradle --no-daemon clean test build` на Java 21.
3. Установи новый JAR из `build/libs/` и проверь Resource Pack на сервере.
4. Если заменил файлы с теми же именами, клиенту может понадобиться очистить кэш ресурс-пака или переподключиться.

Пустые папки в Git не сохраняются — загружай сами файлы или оставь рядом README.
