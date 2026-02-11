package lesson4

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
import kotlin.enums.enumEntries
import kotlin.reflect.typeOf



class GameState{
    val playerId = mutableStateOf("Player")
    val hp = mutableStateOf(100)
    val protect = mutableStateOf(0)
    val gold = mutableStateOf(0)
    val poisonTicksLeft = mutableStateOf(0)
    val regenTicksLeft = mutableStateOf(0)

    val dummyHp = mutableStateOf(50)

    val hotbar = mutableStateOf(
        List<ItemStack?>(9) {null}
        // список из 9 пустых ячеек
    )
    val selectedSlot = mutableStateOf(0)

    val eventLog = mutableStateOf<List<String>>(emptyList())
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

// наша игра будет состоять из связки
// Event System -> Quest System -> HUD log + progress
// почему это надо
// сейчас кнопки напрямую меняют состояние hp, hotbar, dummyHP
// если бы мы остановились при написании игры на этой системе то:
// 1. кнопка удар напрямую бы вычитала hp у моба
// 2. квесты нпс и тд не знали бы что удар по мобу произошел
// 3. система сохранений не знала бы что шаг произошел и его надо зафиксировать
// события решают проблему: кнопка/логика публикует "произошло Х", а другие системы (npc, log, quest
// подписаны и в зависимости от внутренней логики - реагируют на эти события

// система событий
// создаем интерфейс чтобы все наши события имели playerId
sealed interface GameEvent{
    val playerId: String
}

// события для квестов и логов
// data class - просто удобство, он хранит данные как пакет и автоматически применяет toString

data class ItemAdded(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int,
    val leftOver: Int
): GameEvent

data class ItemUsed(
    override val playerId: String,
    val itemId: String
): GameEvent

data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int,
): GameEvent

data class EffectApplied(
    override val playerId: String,
    val effectId: String,
    val tick: Int,
): GameEvent

data class QuestStepCompleted(
    override val playerId: String,
    val questId: String,
    val stepIndex: Int,
): GameEvent

class EventBus{
    typealias Listener = (GameEvent) -> Unit
    // функция принимающая GameEvent возвращает пустоту

    private val listeners = mutableListOf<Listener>()

    fun subscribe(listener: Listener){
        listeners.add(listener)
    }

    fun publish(event: GameEvent){
        for(listener in listeners){
            listener(event)
        }
    }
}

// квестовая система

class QuestSystem(
    private val bus: EventBus // шина событий - через нее будет подписываться и читать события
){
    val questId = "q_training"

    val processByPlayer = mutableStateOf<Map<String, Int>>(emptyMap())

    init {
        bus.subscribe { event ->
            hadleEvent(event)
        }
    }

    private fun getStep(playerId: String): Int{
        return processByPlayer.value[playerId] ?: 0
        // ?: - если ключа не найдется вернуть 0 (вместо null)
    }

    private fun setStep(playerId: String, step: Int){
        val newMap = processByPlayer.value.toMutableMap()
        // создаем словарь, чтобы состояние изменилось и UI его прочитал
        newMap[playerId] = step
        processByPlayer.value = newMap.toMap()
    }

    private fun completeStep(playerId: String, stepIndex: Int){
        setStep(playerId, stepIndex + 1)
        // публикуем событие "шаг квеста выполнен"
        bus.publish(
            QuestStepCompleted(
                playerId,
                questId,
                stepIndex
            )
        )
    }

    private fun hadleEvent(event: GameEvent){
        // решаем влияет ли событие на квест (реагировать ли на событие)
        val player = event.playerId
        val step = getStep(player)

        // если квест уже выполнен
        if (step >= 2) return

        when(event){
            is ItemAdded -> {
                // шаг квеста 0:
                if (step == 0 && event.itemId == WOOD_SWORD.id){
                    completeStep(player, 0)
                }
            }

            is DamageDealt -> {
                // шаг квеста 1 ударить манекен мечом
                if (step == 1 && event.targetId == "dummy" && event.amount >= 10){
                    completeStep(player, 1)
                }
            }
            else -> {}
        }
    }
}

// функция инвентаря

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

fun pushLog(game: GameState, text: String){
    val old = game.eventLog.value

    val updated = old + text

    game.eventLog.value = updated.takeLast(20)
}