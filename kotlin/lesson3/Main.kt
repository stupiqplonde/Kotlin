package lesson3

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
import kotlin.math.PI
import lesson2.ItemStack
import lesson2.ItemType
import lesson2.Item
import kotlin.reflect.typeOf



class GameState{
    val playerId = mutableStateOf("Player")
    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)
    val poisonTicksLeft = mutableStateOf(0)
    val regenTicksLeft = mutableStateOf(0)

    val dummyHp = mutableStateOf(50)

    val hotbar = mutableStateOf(
        List<ItemStack?>(9) {null}
        // список из 9 пустых ячеек
    )
    val selectedSlot = mutableStateOf(0)
}

val HEALING_POTION = Item(
    "potion_heal",
    "Healthiness potion",
    ItemType.POTION,
    12
)

val WOOD_SWORD = Item(
    "wood_sword",
    "Wood sword",
    ItemType.WEAPON,
    1
)

fun putIntoSlot(
    slots: List<ItemStack?>, // принимает кол-во текущих слотов
    slotIndex: Int, // индекс слота
    item: Item,
    addCount: Int
): Pair<List<ItemStack?>, Int>{
    // Pair передает:
    // 1 - новый измененный список слотов (но уже с положенным в него предметом)
    // 2 - число сколько предметов НЕ ВЛЕЗЛО В ЯЧЕЙКУ (остаток)

    val newSlots = slots.toMutableList()
    // копия списка для изменений

    val current = newSlots[slotIndex]
    // current - сохраняем информацию о том что сейчас лежит в слоте (null)

    if (current == null){
        val countToPlace = minOf(addCount, item.maxStack)
        // minOf(a, b) - берет минимум из 2 значений, т.е. округляет addCount до maxStack
        newSlots[slotIndex] = ItemStack(item, countToPlace)

        val leftOver = addCount - countToPlace
        // сколько еще предметов не влезло

        return Pair(newSlots, leftOver)
    }
    // если слот не пустой и предмет что в нем лежит совпадает по id с тем, который мы в него кладем
    // и если maxStack > 1
    if (current.item.id == item.id && item.maxStack > 1){
        val freeSpace = item.maxStack - current.count
        val toAdd = minOf(addCount, freeSpace)

        newSlots[slotIndex] = ItemStack(item, current.count + toAdd)

        val leftOver = addCount - toAdd
        return Pair(newSlots, addCount)

    }
    return Pair(newSlots, addCount)
    // если предмет ни один не стакается - ничего не меняется, возвращаем все как было
}

fun useSelected(
    slots: List<lesson2.ItemStack?>,
    slotIndex: Int
): Pair<List<lesson2.ItemStack?>, lesson2.ItemStack?>{
    // Pair - создает пару значений (новые слоты и что уже использовали)
    val newSlots = slots.toMutableList()
    val current = newSlots[slotIndex] ?: return Pair(newSlots, null)

    val newCount = current.count - 1

    if (newCount <= 0){
        // если слот после использования предмета стал пуст
        newSlots[slotIndex] = null
    }else{
        newSlots[slotIndex] = ItemStack(current.item, newCount)
        // если после использования предмета стак не закончился - обновляем стак
    }

    return Pair(newSlots, current)
}

fun main() = KoolApplication {
    val game = GameState()

    // 3D WORLD сцена
    addScene {
        defaultOrbitCamera()

        addColorMesh {
            generate { cube { colored() } }

            shader = KslPbrShader {
                color { vertexColor() }
                metallic(0.8f)
                roughness(0.2f)
            }

            onUpdate {
                transform.rotate(45f.deg * Time.deltaT, Vec3f.Z_AXIS)
            }
        }
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }

        var poisonTimerSec = 0f
        var regenTimerSec = 0f
        if (game.poisonTicksLeft.value > 0) {
            poisonTimerSec += Time.deltaT


            if (poisonTimerSec >= 1f) {
                poisonTimerSec = 0f
                game.poisonTicksLeft.value = game.poisonTicksLeft.value - 1
                game.hp.value = (game.hp.value - 2).coerceAtLeast(0)
            }
        } else {
            poisonTimerSec = 0f
            // если яда нет - таймер сбрасываем
        }

        if (game.regenTicksLeft.value > 0) {
            regenTimerSec += Time.deltaT
            if (regenTimerSec >= 1f) {
                poisonTimerSec = 0f
                game.regenTicksLeft.value -= 1
                game.hp.value = (game.hp.value + 1).coerceAtLeast(0)
            }
        } else {
            regenTimerSec = 0f

        }
    }

    addScene {
        setupUiScene(ClearColorLoad)
        // setupUiScene - явно указывает движку, что сцена у нас UI
        // ClearColorLoad - указывает интерфейсу отображаться поверх всех сцен
        // говорит наложить UI как слой поверх всех сцен и обновлять

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(16.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                Text("Player: ${game.playerId.use()}") {}
                Text("HP: ${game.hp.use()} Gold: ${game.gold.use()}") {
                    modifier.margin(bottom = sizes.gap)
                }
                Text("Potion: ${game.poisonTicksLeft.use()}") {}
                Text("Player: ${game.playerId.use()}") {}
            }
        }
    }
}

