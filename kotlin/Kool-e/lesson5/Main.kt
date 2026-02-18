package lesson5

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.math.FLT_EPSILON

import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.

import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами

import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"

import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf
import jdk.jshell.execution.JdiInitiator
import kotlinx.coroutines.flow.StateFlow
import lesson4.DamageDealt
import lesson4.EffectApplied
import lesson4.ItemAdded
import lesson4.ItemUsed
import lesson4.QuestStateCompleted
import lesson4.pushLog
import java.awt.Choice
import java.awt.dnd.DropTarget

import java.io.File

enum class ItemType{
    POTION,
    QUEST_ITEM,
    MONEY
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

val HERB = Item(
    "herb",
    "Herb",
    ItemType.QUEST_ITEM,
    16
)

val HEALING_POTION = Item(
    "potion_heal",
    "Heal Potion",
    ItemType.POTION,
    6
)

class GameState{
    val playerId = mutableStateOf("Oleg")

    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)

    val inventory = mutableStateOf(List<ItemStack?>(5) {null})
    // 5 слотов инвенторя - по умолчанию пустой

    val selectedSlot = mutableStateOf(0)
    val log = mutableStateOf<List<String>>(emptyList())
}

fun pushLog(game: GameState, text: String){
    game.log.value = (game.log.value + text).takeLast(20)
}

// система событий

sealed interface GameEvent{
    val playerId: String
}

data class TalkedToNpc(
    override val playerId: String,
    val npcId: String
) : GameEvent

data class ChoiceSelected(
    override val playerId: String,
    val npcId: String,
    val choiceId: String
) : GameEvent

data class ItemCollected(
    override val playerId: String,
    val itemId: String,
    val count: Int
) : GameEvent

data class ItemGivenToNpc(
    override val playerId: String,
    val npcId: String,
    val itemId: String,
    val count: Int
) : GameEvent

data class QuestStateChanged(
    override val playerId: String,
    val questId: String,
    val newState: String
) : GameEvent

data class PlayerProgressSaved(
    override val playerId: String,
    val questId: String,
    val stateName: String
) : GameEvent

// система рассылки и подписки

typealias Listener = (GameEvent) -> Unit

class EventBus{
    private val listeners = mutableStateListOf<Listener>()

    fun subscribe(listener: Listener){
        listeners.add(listener)
    }

    fun publish(event: GameEvent){
        for (l in listeners){
            l(event)
        }
    }
}

// графы

enum class QuestState{
    START,
    OFFERED,
    ACCEPTED_HELP,
    ACCEPTED_THREAT,
    HERB_COLLECTED,
    GOOD_END,
    EVIL_END
}

class StateGraph<S: Any, E: Any>(
    private val initial: S
    // S & E - обобщенные тип данных (generics)
    // S = State = тип состояния
    // пример - тут в виде типов будут START, OFFERED...
    // E = Event = тип события
    // пример - в виде типов данных TalkedToNpc ...
    // это нужно чтобы не создавать для каждой системы (квестов, ) отдельные StateGraph()
    // данный граф, можно использовать не только для квестов но и для Ai мобов Ui диалогов и тд

) {
    // карта переходов transitions - из состояния S -> (тип события -> функция, которая вычисляет
    private val transitions = mutableMapOf<S, MutableMap<Class<out E>, (E) -> S>>()
    // MutableMap<Class<out E>, (E) -> S>
    // ключ Class<out E> = класс события
    // (E) -> S "Функция берет событие -> и возвращает новое состояние"

    // on - добавление перехода между состояниями
    fun on(from: S, eventClass: Class<out E>, to: (E) -> S) {
        val byEvent = transitions.getOrPut(from) { mutableMapOf() }
        byEvent[eventClass] = to
    }

    fun next(current: S, event: E): S{
        // берем карту переход для данного состояния
        val byEvent = transitions[current] ?: return current

        // берем класс события
        val eventClass = event::class.java

        // собираем обработчик для данных типа события
        val handler = byEvent[eventClass] ?: return current

        return handler(event)
    }

    fun initialState(): S = initial
}

class QuestSystem(
    private val bus: EventBus
){
    val questId = "q_alchemist"

    val stateByPlayer = mutableStateOf<Map<String, QuestState>>(emptyMap())

    private val graph = StateGraph<QuestState, GameEvent>(QuestState.START)

    init {
        graph.on(QuestState.START, TalkedToNpc::class.java){ _ ->
            QuestState.OFFERED
        }

        graph.on(QuestState.OFFERED, ChoiceSelected::class.java){ e ->
            val ev = e as ChoiceSelected
            if (ev.choiceId == "help") QuestState.ACCEPTED_HELP else QuestState.ACCEPTED_HELP
        }

        graph.on(QuestState.ACCEPTED_HELP, ItemCollected::class.java){ e ->
            val ev = e as ItemCollected
            if (ev.itemId == "help") QuestState.HERB_COLLECTED else QuestState.ACCEPTED_HELP
        }

        graph.on(QuestState.HERB_COLLECTED, ItemCollected::class.java){ e ->
            val ev = e as ItemGivenToNpc
            if (ev.itemId == "help") QuestState.GOOD_END else QuestState.HERB_COLLECTED
        }

        graph.on(QuestState.ACCEPTED_HELP, ItemCollected::class.java){ e ->
            val ev = e as ChoiceSelected
            if (ev.choiceId == "help") QuestState.EVIL_END else QuestState.ACCEPTED_THREAT
        }

        bus.subscribe { event ->
            advance(event)
        }
    }

    fun getState(playerId: String): QuestState{
        return stateByPlayer.value[playerId] ?: graph.initialState()
    }

    fun setState(playerId: String, state: QuestState){
        val copy = stateByPlayer.value.toMutableMap()
        copy[playerId] = state
        stateByPlayer.value = copy.toMap()
    }

    private fun advance(event: GameEvent) {
        val pid = event.playerId
        val current = getState(pid)
        val next = graph.next(current, event)

        // Если состояние изменилось, то фиксируем и публикуем системные события
        if (next != current){
            setState(pid, next)

            bus.publish(
                QuestStateChanged(
                    pid,
                    questId,
                    next.name
                )
            )

            bus.publish(
                QuestStateChanged(
                    pid,
                    questId,
                    next.name
                )
            )
        }
    }
}

class SaveSystem(
    private val bus: EventBus,
    private val game: GameState,
    private val quests: QuestSystem
){
    init {
        bus.subscribe { event ->
            if (event is PlayerProgressSaved){
                save(event.playerId, event.playerId, event.stateName)
            }
        }
    }

    private fun saveFile(playerId: String, questId: String): File{
        val dir = File("saves")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${playerId}_${questId}.save")
    }

    private fun save(playerId: String, questId: String, stateName: String){
        val f = saveFile(playerId, questId)

        val text =
            "playerId=$playerId\n" +
                    "questId=${questId}\n" +
                    "state=${stateName}\n" +
                    "hp=${game.hp.value}\n" +
                    "gold=${game.gold.value}"

        f.writeText(text)
    }
}

fun addItem(slots: List<ItemStack?>, item: Item, addCount: Int): Pair<List<ItemStack?>, Int>{
    var left = addCount
    val newSlot = slots.toMutableList()

    for (i in newSlot.indices){
        val s = newSlot[i] ?: continue
        if (s.item.id == item.id && item.maxStack > 1 && left > 0){
            val free = item.maxStack - s.count
            val toAdd = minOf(left, free)
            newSlot[i] = ItemStack(item, s.count + toAdd)
            left -= toAdd
        }
    }

    for (i in newSlot.indices){
        if(left <= 0) break
        if (newSlot[i] == null){
            val toPlace = minOf(left, item.maxStack)
            newSlot[i] = ItemStack(item, toPlace)
            left -= toPlace
        }
    }

    return Pair(newSlot, left)
}

fun removeItem(slots: List<ItemStack?>, itemId: String, count: Int): Pair<List<ItemStack?>, Boolean> {
    var need = count
    val newSlots = slots.toMutableList()

    for(i in newSlots.indices) {
        val s = newSlots[i] ?: continue
        if (s.item.id == itemId && need > 0) {
            val take = minOf(need, s.count)
            val leftInStack = s.count - take
            need -= take

            newSlots[i] = if (leftInStack <= 0) null else ItemStack(s.item, leftInStack)
        }
    }

    val success = (need == 0)

    return Pair(newSlots, success)
}

data class DialogueOption(
    val id: String,
    val text: String
)

data class DialogueView(
    val text: String,
    val options: List<DialogueOption>
)

class Npc(
    val id: String,
    val name: String
){
    fun dialogueFor(state: QuestState): DialogueView {
        return when (state) {
            QuestState.START -> DialogueView(
                "Алхимик: Привет, подходи, перетрем",
                listOf(
                    DialogueOption("talk", "Поговорить")
                )
            )

            QuestState.OFFERED -> DialogueView(
                "Алхимик: Мне нужно, чтобы ты достал мне немного травы",
                listOf(
                    DialogueOption("help", "Окей, помогу тебе за долю"),
                    DialogueOption("threat", "Не, это ты мне давай траву")
                )
            )

            QuestState.ACCEPTED_HELP -> DialogueView(
                "Алхимик: Хорош мужик, жду тебя тут",
                listOf(
                    DialogueOption("collect_herb", "Собрать траву (симуляция)")
                )
            )

            QuestState.HERB_COLLECTED -> DialogueView(
                "Алхимик: Ну че, принес? Давай сюда!",
                listOf(
                    DialogueOption("give_herb", "Отдать траву")
                )
            )

            QuestState.ACCEPTED_THREAT -> DialogueView(
                "Алхимик: Э, малой, ты уверен?",
                listOf(
                    DialogueOption("threaten_confirm", "Да, гони сюда")
                )
            )

            QuestState.GOOD_END -> DialogueView(
                "Алхимик: Хорош мужик, выручил",
                emptyList()
            )

            QuestState.EVIL_END -> DialogueView(
                "Алхимик: Ладно, держи траву, ходи оглядывайся, земляк",
                emptyList()
            )
        }
    }
}

fun main() = KoolApplication {
    val game = GameState()
    val bus = EventBus()
    val quests = QuestSystem(bus)
    val saves = SaveSystem(bus, game, quests)

    val npc = Npc("alchemist", "Alchemist")

    bus.subscribe { e ->
        val line = when (e) {
            is QuestStateChanged -> "QuestStateChanged: ${e.questId} -> ${e.newState}"
            is PlayerProgressSaved -> "Saved: ${e.questId} state=${e.stateName}"
            is TalkedToNpc -> "TalkedToNpc: ${e.npcId}"
            is ChoiceSelected -> "ChoiceSelected: ${e.choiceId}"
            is ItemCollected -> "ItemCollected: ${e.itemId} x${e.count}"
            is ItemGivenToNpc -> "ItemGivenToNpc: ${e.itemId} x${e.count}"
        }
        pushLog(game, "[${e.playerId}] ${line}")
    }

    addScene {
        defaultOrbitCamera()

        addColorMesh {
            generate { cube { colored() } }

            shader = KslPbrShader {
                color { vertexColor() }
                metallic(0.8f)
                roughness(0.3f)
            }

            onUpdate {
                transform.rotate(45f.deg * Time.deltaT, Vec3f.Z_AXIS)
            }
        }

        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }
    }

    addScene {
        setupUiScene(ClearColorLoad)

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(16.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                Text("Player: ${game.playerId.use()}"){}
                Text("Hp: ${game.hp.use()}"){}
            }

            modifier.margin(bottom = sizes.gap)

            val state = quests.stateByPlayer.use()[game.playerId.use()] ?: QuestState.START
            Text("Quest State: ${state.name}"){}

            modifier.margin(bottom = sizes.gap)

            val view = npc.dialogueFor(state)
            Text("${npc.name}"){}
            Text(view.text){}

            modifier.margin(bottom = sizes.gap)

            Row {
                for (opt in view.options){
                    Button (opt.text){
                        modifier.onClick{
                            val pid = game.playerId.value

                            when(opt.id){
                                "talk" -> {
                                    bus.publish(TalkedToNpc(pid, npc.id))
                                }

                                "collect_herb" -> {
                                    val (updated, left) = addItem(game.inventory.value, HERB, 1)
                                    game.inventory.value = updated

                                    bus.publish(ItemCollected(pid, HERB.id, 1))

                                    if (left > 0) game.gold.value += left
                                }

                                "give_herb" -> {
                                    val (updated, success) = removeItem(game.inventory.value, HERB.id, 1)
                                    game.inventory.value = updated

                                    if (success){
                                        bus.publish(TalkedToNpc(pid, npc.id))

                                        val (updated, left) = addItem(game.inventory.value, HERB, 1)

                                        game.gold.value += 1
                                        game.inventory.value = updated

                                        if (left > 0) game.gold.value += left
                                    } else{
                                        pushLog(game, "[ERROR]: herb don't given")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}