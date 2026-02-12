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

import lesson3.ItemStack
import lesson3.ItemType
import lesson3.Item


import lesson3.HEALING_POTION
import lesson3.SWORD

import lesson3.GameState

import lesson3.putIntoSlot
import lesson3.useSelected
import java.io.File

sealed interface GameEvent{
    val playerId: String
}

data class QuestStateCompleted(
    override val playerId: String,
    val questId: String,
    val stepId: Int
): GameEvent

data class PlayerProgressSaved(
    override val playerId: String,
    val questId: String,
    val stepId: Int
): GameEvent
data class ItemAdded(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int,
    val leftOver: Int
): GameEvent
data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int
): GameEvent

data class ItemUsed(
    override val playerId: String,
    val itemId: String
): GameEvent

data class EffectApplied(
    override val playerId: String,
    val effectId: String,
    val ticks: Int,
): GameEvent

typealias Listener = (GameEvent) -> Unit
// функция принимающая GameEvent возвращает пустоту
class EventBus{
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

class QuestSystem(
    private val bus: EventBus
){
    val questId = "q_training"
    val progressByPlayer = mutableStateOf<Map<String, Int>>(emptyMap())

    fun getStep(playerId: String): Int{
        return progressByPlayer.value[playerId] ?: 0
    }

    fun setStep(playerId: String, step: Int){
        val copy = progressByPlayer.value.toMutableMap()
        copy[playerId] = step
        progressByPlayer.value = copy.toMap()
    }

    fun completeStep(playerId: String, stepId: Int){
        val next = stepId + 1

        setStep(playerId, next)

        bus.publish(
            QuestStateCompleted(
                playerId,
                questId,
                stepId
            )
        )
    }
}

class SaveSystem(
    private val bus: EventBus,
    private val game: GameState,
    private val quest: QuestSystem
){
    init {
        bus.subscribe { event ->
            // ожидание событий сохранения прошресса - пищем в файл
            if (event is PlayerProgressSaved){
                saveProgress(event.playerId, event.questId, event.stepId)
            }
        }
    }
    private fun saveFile(playerId: String, questId: String): File {
        val dir = File("saves")
        if(!dir.exists()){
            dir.mkdirs() // mkdirs - создает папку (и родителей папки), если ее нет
        }
        // имя файла: saves/player_1_q_training.save
        return File(dir, "${playerId}_${questId}.save")
    }

    fun saveProgress(playerId: String, questId: String, stepId: Int){
        val f = saveFile(playerId, questId)

        // простое хранение сохранения в формате ключ = значение
        val text =
            "playerId=${playerId}\n" +
            "questId=${questId}\n" +
            "stepId=${stepId}\n" +
            "hp=${game.hp.value}\n" +
            "questId=${game.gold.value}\n"

        f.writeText(text) // write
    }

    fun loadProgress(playerId: String, questId: String){
        val f = saveFile(playerId, questId)
        if (!f.exists()) return

        val lines = f.readLines() // чтение файла построчно
        val map = mutableMapOf<String, String>()

        for (line in lines){
            val parts = line.split("=") // split делит строку на части \
            if (parts.size == 2){
                val key = parts[0]
                val value = parts[1]
                map[key] = value
            }
        }

        val loadedStep = map["stepId"]?.toIntOrNull() ?: 0
        // ?. - если не null - то вызови toIntOrNull
        // toIntOrNull - пытается перрвать строку в Int, иначе null
        // ?: - если получили null -> вернуть 0
        val loadedHp = map["hp"]?.toIntOrNull() ?: 100
        val loadedGold = map["gold"]?.toIntOrNull() ?: 0

        game.hp.value = loadedHp
        game.gold.value = loadedGold

        quest.setStep(playerId, loadedStep)
    }
}

fun pushLog(game: GameState, text: String){
    game.eventLog.value = (game.eventLog.value + text).takeLast(20)
}

fun main() = KoolApplication {
    val game = GameState()
    val bus = EventBus()
    val quests = QuestSystem(bus)
    val saves = SaveSystem(bus, game, quests)

    bus.subscribe { event ->
        val line = when (event) {
            is ItemAdded -> "ItedAdded: ${event.itemId} + ${event.countAdded} (осталось: ${event.leftOver})"
            is ItemUsed -> "ItemUsed: ${event.itemId}"
            is PlayerProgressSaved -> "Game Saved: ${event.questId} Step: ${event.stepId}"
            is DamageDealt -> "DamageDealt: ${event.amount} - ${event.targetId}"
            is EffectApplied -> "EffectApplied: ${event.effectId} +${event.ticks}"
            is QuestStateCompleted -> "QuestStepCompleted: ${event.questId} шаг: ${event.stepId + 1}"
            else -> {}
        }
        pushLog(game, "[${event.playerId}] $line")
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

            var potionTimeSec = 0f
            var regenTimeSec = 0f
            onUpdate {
                if (game.potionTicksLeft.value > 0) {
                    potionTimeSec += Time.deltaT
                    if (potionTimeSec >= 1f) {
                        potionTimeSec = 0f
                        game.potionTicksLeft.value -= 1
                        game.hp.value = (game.hp.value - 2).coerceAtLeast(0)
                    }
                } else {
                    potionTimeSec = 0f
                }

                if (game.regenTicksLeft.value > 0) {
                    regenTimeSec += Time.deltaT
                    if (regenTimeSec >= 1f) {
                        regenTimeSec = 0f
                        game.regenTicksLeft.value -= 1
                        game.hp.value = (game.hp.value + 1).coerceAtLeast(0)
                    }
                } else {
                    regenTimeSec = 0f
                }
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
                    Text("Игрок: ${game.playerId.use()}"){}
                    Text("HP: ${game.hp.use()}"){
                        modifier.margin(bottom = sizes.gap)
                    }

                    val step = quests.progressByPlayer.use()[game.playerId.use()] ?: 0
                    Text("прогресс квест: $step"){
                        modifier.margin(bottom = sizes.gap)
                    }

                    Text("ыбранный слот: ${game.selectedSlot.use() + 1}"){
                        modifier.margin(bottom = sizes.gap)
                    }

                    Row{
                        Button ("Сменить игрока"){
                            modifier.margin(end = 8.dp).onClick {
                                game.playerId.value =
                                    if (game.playerId.value == "Player") "Oleg" else "PLayer"
                            }
                        }

                        Button ("Загрузка последнее сохранение"){
                            modifier.onClick{
                                saves.loadProgress(game.playerId.value, quests.questId)
                                pushLog(game, "[${game.playerId.value}] загрузил сохранение из квеста ${quests.questId}")
                            }
                        }
                    }
                    Row { modifier.margin(top = sizes.smallGap)
                        Button("Получить меч (шаг 0)"){
                            modifier.margin(end = 8.dp).onClick{
                                    val pid = game.playerId.value
                                    quests.completeStep(pid, stepId = 0)
                                }
                        }
                        Button("Удврить манекен (шаг 1)"){
                            modifier.onClick{
                                val pid = game.playerId.value
                                quests.completeStep(pid, stepId = 1)
                            }
                        }
                    }
                    Text("Лог событий"){
                        modifier.margin(top = sizes.gap)
                        for (line in game.eventLog.use()){
                            Text(line){
                                modifier.font(sizes.smallText)
                            }
                        }
                    }
                }
            }
        }
    }
