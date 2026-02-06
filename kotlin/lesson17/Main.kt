// ----------------------------
// ИМПОРТЫ (подключаем нужные классы/функции из Kool)
// ----------------------------

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)

import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.

import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами

import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"

import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf

// ----------------------------
// 1) GameState = "данные игрока" (то, что меняется во время игры)
// ----------------------------
class GameState {
    // mutableStateOf(...) - создаёт "состояние", за которым UI умеет следить
    // когда значение меняется -> UI автоматически перерисуется

    val playerId = mutableStateOf("player")
    // val - ссылка неизменяемая (мы не можем сделать playerId = что-то другое)
    // НО значение внутри state мы менять можем

    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)

    val poisonTicksLeft = mutableStateOf(0)
    // "тики" = условные шаги времени (как в Minecraft)
    // здесь 1 тик = 1 секунда (для простоты)
}

// ----------------------------
// 2) Точка входа: main()
// ----------------------------
fun main() = KoolApplication {
    // KoolApplication { ... } - запускаем движок и выполняем код внутри блока

    val game = GameState()
    // val game = ... - создаём объект состояния игры (один на всё приложение)

    // ------------------------------------------------------------
    // 3) СЦЕНА 1: WORLD (3D мир)
    // ------------------------------------------------------------
    addScene {
        // this внутри блока = Scene (сцена)

        defaultOrbitCamera()
        // defaultOrbitCamera() - готовая камера "крути вокруг центра мышкой"

        // 3D-куб
        addColorMesh {
            generate {
                cube {
                    colored()
                    // colored() - добавляет цвет на вершины куба (чтобы он был разноцветный)
                }
            }

            shader = KslPbrShader {
                // shader = ... - назначаем материал
                color { vertexColor() }
                // vertexColor() - берём цвет из вершин (из colored())
                metallic(0f)
                roughness(0.25f)
            }

            onUpdate {
                // onUpdate { ... } - вызывается каждый кадр
                // Time.deltaT - секунды между кадрами (важно для одинаковой скорости при разном FPS)

                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
                // rotate(угол, ось)
                // 45f.deg - 45 градусов в секунду
                // * Time.deltaT - "сколько прошло секунд"
                // Vec3f.X_AXIS - ось X
            }
        }

        // Свет (иначе PBR будет тёмным)
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }

        // ------------------------------------------------------------
        // 4) ЛОГИКА "ЯД ТИКАЕТ" (в мире, не в UI)
        // ------------------------------------------------------------

        var poisonTimerSec = 0f
        // var - переменная (её можно менять)
        // 0f - Float (число с плавающей точкой)

        onUpdate {
            // Этот onUpdate - на уровне сцены: тоже каждый кадр

            if (game.poisonTicksLeft.value > 0) {
                // .value - достаём текущее значение state

                poisonTimerSec += Time.deltaT
                // накапливаем секунды

                if (poisonTimerSec >= 1f) {
                    // прошло >= 1 секунды -> делаем "тик"

                    poisonTimerSec = 0f

                    game.poisonTicksLeft.value = game.poisonTicksLeft.value - 1
                    // уменьшаем количество тиков яда

                    game.hp.value = (game.hp.value - 2).coerceAtLeast(0)
                    // снимаем 2 HP, но не даём уйти ниже 0
                    // coerceAtLeast(0) = "не меньше 0"
                }
            } else {
                poisonTimerSec = 0f
                // если яда нет - таймер сбрасываем
            }
        }
    }

    // ------------------------------------------------------------
    // 5) СЦЕНА 2: HUD (UI поверх мира)
    // ------------------------------------------------------------
    addScene {
        // Это отдельная сцена, которая рисуется ПОСЛЕ мира
        // Значит она может быть "оверлеем"

        setupUiScene(ClearColorLoad)
        // setupUiScene(...) - превращает сцену в UI-сцену (камера/пайплайн под интерфейс)
        // ClearColorLoad - КРИТИЧНО: "не очищай экран", оставь картинку мира под UI
        // Именно это делает HUD настоящим оверлеем

        addPanelSurface {
            // addPanelSurface { ... } - создаём "панель" UI на экране

            modifier
                .size(360.dp, 210.dp)
                // size(w, h) - размер панели в dp (UI-единицы, не пиксели напрямую)

                .align(AlignmentX.Start, AlignmentY.Top)
                // align(...) - позиция панели на экране
                // AlignmentX.Start - слева
                // AlignmentY.Top - сверху  (ты просил Top вместо Start)

                .padding(16.dp)
                // padding - внутренний отступ панели

                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
            // background(...) - тут ждёт "рендерер фона"
            // RoundRectBackground(...) - фон-скруглённый прямоугольник
            // Color(r,g,b,a) - r,g,b,a от 0 до 1
            // 0.6f альфа = полупрозрачный

            // ---- Читаем state через .use() ----
            // use() - "прочитать state и подписаться": если state изменится, UI перерисуется

            Column {
                // Column - вертикальный контейнер (как список)

                Text("PlayerId: ${game.playerId.use()}") {
                    // Text("...") { ... } - текстовый UI-элемент
                    // ${ ... } - вставка значения в строку
                }

                Text("HP: ${game.hp.use()}") { }
                Text("Gold: ${game.gold.use()}") { }
                Text("Poison ticks: ${game.poisonTicksLeft.use()}") { }

                Row {
                    // Row - горизонтальный контейнер (кнопки в ряд)

                    modifier.padding(top = 10.dp)

                    Button("Damage -10") {
                        // Button("текст") { ... } - кнопка

                        modifier
                            .padding(end = 8.dp)
                            // небольшой отступ справа, чтобы кнопки не слипались

                            .onClick {
                                // onClick { ... } - что делать при клике
                                game.hp.value = (game.hp.value - 10).coerceAtLeast(0)
                            }
                    }

                    Button("Gold +5") {
                        modifier
                            .padding(end = 8.dp)
                            .onClick {
                                game.gold.value = game.gold.value + 5
                            }
                    }

                    Button("Poison +5") {
                        modifier.onClick {
                            game.poisonTicksLeft.value = game.poisonTicksLeft.value + 5
                        }
                    }
                }
            }
        }
    }

    addScene {
        setupUiScene(ClearColorLoad)

        addPanelSurface {
            val isDead = game.hp.use() == 0

            if (isDead) {
                modifier
                    .size(360.dp, 140.dp)
                    .align(AlignmentX.Center, AlignmentY.Center)
                    .background(RoundRectBackground(Color(255f, 0f, 0f, 1f), 20.dp))

                Column {
                    modifier
                        .align(AlignmentX.Center, AlignmentY.Top)

                    Text("YOU DIED") {
                        modifier
                            .align(AlignmentX.Center)
                            .padding(top = 40.dp)
                    }

                    Row {
                        modifier
                            .align(AlignmentX.Center)

                        Button("Restart") {
                            modifier
                                .size(100.dp, 40.dp)
                                .align(AlignmentX.Center, AlignmentY.Center)
                                .background(RoundRectBackground(Color(255f, 255f, 255f, 1f), 20.dp))
                                .onClick {
                                    game.hp.value = 100
                                    game.poisonTicksLeft.value = 0
                                }
                        }
                    }
                }

            }else {
                // Если не умерли - скрываем эту панель
                modifier
                    .size(
                    0.dp,
                    0.dp
                )
            }
        }
    }
}
