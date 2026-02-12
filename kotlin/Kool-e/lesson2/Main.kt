package lesson2

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
import kotlin.reflect.typeOf


// урок 2 - hotbar инвентарь
enum class ItemType{
    WEAPON,
    ARMOR,
    POTION
}

data class Item(
    val id: String,
    val name: String,
    val type: ItemType,
    val maxStack: Int
)

data class ItemStack(
    val item: Item,
    val count: Int
)

class gameState{
    val playerId = mutableStateOf("Player")
    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)
    val poisonTicksLeft = mutableStateOf(0)
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

fun putInToSlot(
    slots: List<ItemStack?>,
    slotIndex: Int,
    item: Item,
    addCount: Int
): List<ItemStack?>{
    val newSlots = slots.toMutableList()
    val current = newSlots[slotIndex]

    if (current == null){
        val count = minOf(addCount, item.maxStack)
        newSlots[slotIndex] = ItemStack(item, count)
        return newSlots
    }
    // если слот не пуст - стакаем в него предметы только если это тот же предмет и не превышен максимум предметов
    if (current.item.id == item.id && item.maxStack > 1){
        val freeSpace = item.maxStack - current.count
        val toAdd = minOf(addCount, freeSpace)
        newSlots[slotIndex] = ItemStack(item, current.count + toAdd)
        return newSlots
    }
    if (current.item.id == "wood_sword"){
        val notSwordPut = 1
    }

    // если предмет другой (или максимальное число стака = 1) - не кладем данный предмет
    // на данной реализации будет надпись "неудача"
    return newSlots
}

fun useSelected(
    slots: List<ItemStack?>,
    slotIndex: Int
): Pair<List<ItemStack?>, ItemStack?>{
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

fun main() = KoolApplication{
    val game = gameState()

    addScene {
        defaultOrbitCamera()

        addColorMesh {
            generate { cube{ colored()} }

            shader = KslPbrShader{
                color {
                    vertexColor()
                }
                metallic ( 0.7f )
                roughness ( 0.6f )
            }
            onUpdate{
                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
            }
        }
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }

        var poisonTimerSec = 0f

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
    }

    addScene {
        setupUiScene(ClearColorLoad)

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(20.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.5f), 20.dp))
                .padding(12.dp)

            Column {
                Text ("Player: ${game.playerId.use()}"){}
                Text ("HP: ${game.hp.use()} Gold: ${game.gold.use()}"){}
                Text ("Potion: ${game.poisonTicksLeft.use()}"){}

                modifier.padding(10.dp)
                Row {
                    modifier.margin(top = 6.dp)

                    val slots = game.hotbar.use()
                    // выводим инфу
                    val selected = game.selectedSlot.use()

                    for (i in 0 until 9){
                        // рисуем слоты
                        val isSelected = (i == selected)
                        // Box - контейнер
                        Box {
                            modifier
                                .size(50.dp, 50.dp)
                                .margin(end = 10.dp)
                                .background(
                                    RoundRectBackground(
                                        if (isSelected){ Color (0.2f, 0.6f, 1f, 0.8f)} else{ Color(0f, 0f, 0f, 0.8f)
                                        },
                                        8.dp
                                    )
                                )
                                .onClick{
                                    game.selectedSlot.value = i
                                }
                            val stack = slots[i]
                            // назначение слота = имя предмета + кол-во предметов
                            if (stack == null){
                                Text (""){}
                            }else{
                                Column {
                                    modifier.padding(6.dp)
                                    Text (stack.item.name){
                                        modifier.font(sizes.smallText)
                                    }

                                    Text ("x${stack.count}"){
                                        modifier.font(sizes.smallText)
                                    }
                                }
                            }
                        }
                    }
                }

                Row {
                    modifier.margin(top = 6.dp)

                    Button ("Наложить эффект") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                val idx = game.selectedSlot.value
                                val updated = putInToSlot(game.hotbar.value, idx, HEALING_POTION, 1)
                                game.hotbar.value = updated
                            }
                    }
                    Button ("Дать деревянный меч") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                val idx = game.selectedSlot.value
                                val updated = putInToSlot(game.hotbar.value, idx, WOOD_SWORD, 1)
                                game.hotbar.value = updated
                            }
                    }
                    Button ("Использовать предмет") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                val idx = game.selectedSlot.value
                                val (updatedSlots, used) = useSelected(game.hotbar.value, idx)
                                game.hotbar.value = updatedSlots

                                if (used != null && used.item.type == ItemType.POTION){
                                    game.hp.value = (game.hp.value + 20).coerceAtMost(100)
                                }
                            }
                    }
                }

                Row {
                    modifier.margin(top = 6.dp)

                    Button("Poison +5") {
                        modifier.onClick {
                            game.poisonTicksLeft.value = game.poisonTicksLeft.value + 5
                        }
                    }
                }
            }
        }
    }
}