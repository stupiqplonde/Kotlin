package lesson17

import de.fabmax.kool.KoolApplication // подключение основной библиотеки Kool
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f      // Vec3f - 3D-вектор (x, y, z) координаты мира
import de.fabmax.kool.math.deg        // deg - превращение числа в градусы (угол)
import de.fabmax.kool.scene.*    // Scene - сцена (мир) куда и будет добавлять объекты
import de.fabmax.kool.util.Time       // Time.deltaT - время между кадрами
import de.fabmax.kool.util.Color      // Цвет интерфейса
import de.fabmax.kool.modules.ksl.KslPbrShader // Шейдер PBR - в роли материалов у объектов
import de.fabmax.kool.modules.ui2.*   // Компоненты кнопок, текста, колонок, полей
import de.fabmax.kool.modules.ui2.UiModifier // модификаторы (padding, align ...)
import de.fabmax.kool.pipeline.ClearColorLoad // режим - "не отчищай экран от того что на нем загружено"
import de.fabmax.kool.pipeline.MipMapping

// Игровой мир - простая фигура Куб
// HUD с обновляемой информацией
// GameState - данные игрока (такие, как NBT - Данные профиля)
// UI - Читать данные игрока, и в случае их изменения - обновлять интерфейс
// Дерево компонентов UI

class GameState{
    val playerName = mutableStateOf("Player")
    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)
    val potionTicksLeft = mutableStateOf(0)
    // тик - это условная временная единица в игровом пространстве
    // в простом формате 1 тик = 1 секунде
}

fun main() = KoolApplication{
    // = KoolApplication - запуск движка при старте игры
    val game = GameState()

    addScene { // создание игровой сцены (мира)
        defaultOrbitCamera()
        // готовая камера, по умолчанию можно крутить мышкой вокруг объекта, управлять ею при событиях и тд

        // Добавляем на сцену куб
        addColorMesh { // Меш - модель (с цветом)
            generate { // генерация геометрии модели
                cube { // создание куба
                    colored() // создание раскраски куба по его вершинам
                }
            }

            shader = KslPbrShader {     // с помощью шейдер мы назначаем материал или цвет
                color { vertexColor() }
                metallic(0f)     // эффект металла на поверхности
                roughness(0.25f) // [РАФНЕС] - шероховатость (0 = глянец, 1 = матовость)
            }

            onUpdate {
                // onUpdate - вызывать тело {...} каждый кадр
                // TimeDeltaT - секунды между кадрами (важно использовать дельту, чтобы скорость,
                // урон, порядок действий были равными у всех игроков на разном FPS)

                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
                // rotate(угол, ось) - метод вращения объектов
                // 45f.deg - 45 градусов в секунду
                // Time.deltaT - формула подсчета "сколько секунд прошло"
                // Vec3f.X_AXIS - обозначение оси Х в трехмерном пространстве

            }
        }

        // Свет (без него не будет видна текстура)
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.DARK_MAGENTA, 5f)
        }

        // ЛОГИКА ПОДСЧЕТА УРОНА ОТ ЯДА
        var potionTimeSec = 0f
        // Счетчик действия яда на нас

        onUpdate{
            if (game.potionTicksLeft.value > 0){
                // .value - достаем текущее значение state
                potionTimeSec += Time.deltaT
                // накопление секунд
                if (potionTimeSec >= 1f){
                    // прошло больше или ровно 1 секунда тогда -> выполняем тик (накладываем урон от яда)
                    potionTimeSec = 0f
                    game.potionTicksLeft.value = game.potionTicksLeft.value - 1
                    // уменьшаем кол-во тиков действия яда

                    game.hp.value = (game.hp.value - 2).coerceAtLeast(0)
                }
            }else{
                potionTimeSec = 0f
            }
        }
    }

    // HUD
    addScene {
        setupUiScene(ClearColorLoad)
        // setupUiScene - прекращение сцены в UI сцену (то есть все параметры будут настроены под интерфейс)
        // ClearColorLoad - критично - не отчищать экран, оставлять картинку мира под ним UI
        addPanelSurface {
            // создание панели на UI экране - холста
            modifier
                .size(360.dp, 210.dp)
                // размер панели в ширину и высоту
                .align(AlignmentX.Start, AlignmentY.Top)
                // align - выравнивание панели на экране
                .padding(16.dp)
                // padding - отступ внутри
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))

            Column {
                // вертикальный контейнер
                // use() - "прочитать state и подписаться на него": если state изменится -> перерисовать UI

                Text("PlayerId: ${game.playerName.use()}"){} // Обновляем имя игрока, если оно изменится
                Text("HP: ${game.hp.use()}"){}
                Text("Gold: ${game.gold.use()}"){}
                Text("Potion Ticks: ${game.potionTicksLeft.use()}"){}
            }
        }
    }
}

