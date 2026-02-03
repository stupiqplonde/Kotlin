package lesson17

import de.fabmax.kool.KoolApplication // подключение основной библиотеки Kool
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f      // Vec3f - 3D-вектор (x, y, z) координаты мира
import de.fabmax.kool.math.defaultRandomInstance
import de.fabmax.kool.scene.Scene     // Scene - сцена (мир) куда и будет добавлять объекты
import de.fabmax.kool.time.Time       // Time.deltaT - время между кадрами
import de.fabmax.kool.util.copy       // Цвет интерфейса
import de.fabmax.kool.modules.ksl.KslPbrShader // Шейдер PBR - в роли материалов у объектов
import de.fabmax.kool.modules.ui2.*   // Компоненты кнопок, текста, кол
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.defaultOrbitCamera

// Игровой мир - простая фигура Куб
// HUD с обновляемой информацией
// GameState - данные игрока (такие, как NBT - Данные профиля)
// UI - Читать данные игрока, и в случае их изменения - обновлять интерфейс
// Дерево компонентов UI

class GameState{
    val playerName = mutableStateOf("Player")
    val hp = mutableStateOf("100")
    val gold = mutableStateOf(0)
    val potionTicksLeft = mutableStateOf(0)
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
        }
    }
}