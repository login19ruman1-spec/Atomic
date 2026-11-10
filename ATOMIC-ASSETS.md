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

## 3. Звуки ядерного взрыва

Для трёх дистанционных слоёв используются OGG Vorbis (не MP3/WAV), 44.1 или 48 kHz. Положи файлы с точными именами:

- `atomic-assets/sounds/blast/close.ogg` — вблизи эпицентра (до 50 блоков).
- `atomic-assets/sounds/blast/wave.ogg` — звук ударной волны (до 2000 блоков).
- `atomic-assets/sounds/blast/distant.ogg` — отдалённый гул (до 3000 блоков).

Они регистрируются как `beforethefall:blast_close`, `beforethefall:blast_wave` и `beforethefall:blast_distant`. Игрок слышит только один подходящий слой по расстоянию; звук отправляется в позицию слушателя, чтобы Minecraft не заглушал его из-за обычного затухания позиционного звука на больших расстояниях.

Подземный отдельный звук не используется и не регистрируется. Само нахождение под землёй не включает дополнительный звуковой слой.

Существующие дополнительные события зарядки/запуска/падения/радиоактивных последствий можно положить в `atomic-assets/sounds/nuke/charge.ogg`, `launch.ogg`, `blast.ogg`, `impact.ogg`, `fallout.ogg` соответственно; сборка зарегистрирует их при наличии файлов.

## 4. Сборка и проверка

1. Скопируй свои файлы в указанные папки.
2. Запусти `gradle --no-daemon clean test build` на Java 21.
3. Установи новый JAR из `build/libs/` и проверь Resource Pack на сервере.
4. Если заменил файлы с теми же именами, клиенту может понадобиться очистить кэш ресурс-пака или переподключиться.

Пустые папки в Git не сохраняются — загружай сами файлы или оставь рядом README.
