package lesson4

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.scene.*
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import de.fabmax.kool.pipeline.ClearColorLoad
import de.fabmax.kool.modules.ui2.*
import lesson3.Item
import lesson3.ItemStack
import lesson3.ItemType
import lesson3.HEALING_POTION
import lesson3.SWORD
import lesson3.GameState
import lesson3.putIntoSlot
import lesson3.useSelected
import java.io.File
import kotlin.random.Random

sealed interface GameEvent {
    val playerId: String
}

data class QuestStateCompleted(
    override val playerId: String,
    val questId: String,
    val stepId: Int
) : GameEvent

data class PlayerProgressSaved(
    override val playerId: String,
    val questId: String,
    val stepId: Int
) : GameEvent

data class ItemAdded(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int,
    val leftOver: Int
) : GameEvent

data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int
) : GameEvent

data class ItemUsed(
    override val playerId: String,
    val itemId: String
) : GameEvent

data class EffectApplied(
    override val playerId: String,
    val effectId: String,
    val ticks: Int
) : GameEvent

// функция принимающая GameEvent возвращает пустоту
typealias Listener = (GameEvent) -> Unit

class EventBus {
    private val listeners = mutableListOf<Listener>()
    fun subscribe(listener: Listener) {
        listeners.add(listener)
    }
    fun publish(event: GameEvent) {
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

    fun setStep(playerId: String, step: Int) {
        val copy = progressByPlayer.value.toMutableMap()
        copy[playerId] = step
        progressByPlayer.value = copy.toMap()
    }

    fun completeStep(playerId: String, stepId: Int) {
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
) {
    init {
        bus.subscribe { event ->
            // ожидание событий сохранения прошресса - пищем в файл
            if (event is PlayerProgressSaved) {
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

    fun saveProgress(playerId: String, questId: String, stepId: Int) {
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


    fun loadProgress(playerId: String, questId: String) {
        val f = saveFile(playerId, questId)
        if (!f.exists()) return
        loadFromFile(f)
    }


    fun loadFromFile(file: File) {
        if (!file.exists()) return
        val lines = file.readLines()
        val map = mutableMapOf<String, String>()
        for (line in lines) {
            val parts = line.split("=")
            if (parts.size == 2) map[parts[0]] = parts[1]
        }
        val playerId = map["playerId"] ?: return
        val questId = map["questId"] ?: return
        val loadedStep = map["stepId"]?.toIntOrNull() ?: 0
        val loadedHp = map["hp"]?.toIntOrNull() ?: 100
        val loadedGold = map["gold"]?.toIntOrNull() ?: 0

        game.hp.value = loadedHp
        game.gold.value = loadedGold
        quest.setStep(playerId, loadedStep)


        game.playerId.value = playerId
    }
}

fun pushLog(game: GameState, text: String) {
    game.eventLog.value = (game.eventLog.value + text).takeLast(20)
}

fun damageRange(weapon: String): IntRange = when (weapon) {
    "hand" -> 1..5
    "sword" -> 7..18
    else -> 0..0
}

fun main() = KoolApplication {
    val game = GameState().apply {
        playerId.value = "Player 1"
    }
    val bus = EventBus()
    val quests = QuestSystem(bus)
    val saves = SaveSystem(bus, game, quests)

    bus.subscribe { event ->
        val line = when (event) {
            is ItemAdded          -> "ItemAdded: ${event.itemId} +${event.countAdded} (остаток: ${event.leftOver})"
            is ItemUsed           -> "ItemUsed: ${event.itemId}"
            is PlayerProgressSaved -> "Сохранено: квест ${event.questId}, шаг ${event.stepId}"
            is DamageDealt        -> "Урон: ${event.amount} цели ${event.targetId}"
            is EffectApplied      -> "Эффект: ${event.effectId} +${event.ticks} тиков"
            is QuestStateCompleted -> "Квест выполнен: ${event.questId} шаг ${event.stepId + 1}"
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
            onUpdate { transform.rotate(45f.deg * Time.deltaT, Vec3f.Z_AXIS) }
        }
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 100f)
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
            } else potionTimeSec = 0f

            if (game.regenTicksLeft.value > 0) {
                regenTimeSec += Time.deltaT
                if (regenTimeSec >= 1f) {
                    regenTimeSec = 0f
                    game.regenTicksLeft.value -= 1
                    game.hp.value = (game.hp.value + 1).coerceAtMost(100)
                }
            } else regenTimeSec = 0f
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
                Text("Игрок: ${game.playerId.use()}") { modifier.margin(bottom = sizes.gap) }
                Text("HP: ${game.hp.use()}") { modifier.margin(bottom = sizes.gap) }

                val step = quests.progressByPlayer.use()[game.playerId.use()] ?: 0
                Text("Прогресс квеста: $step") { modifier.margin(bottom = sizes.gap) }
                Text("Выбранный слот: ${game.selectedSlot.use() + 1}") { modifier.margin(bottom = sizes.gap) }


                Row {
                    Button("Сменить игрока") {
                        modifier.margin(end = 8.dp).onClick {
                            game.playerId.value = when (game.playerId.value) {
                                "Player 1" -> "Player 2"
                                else       -> "Player 1"
                            }
                        }
                    }
                    Button("Загрузить последнее сохранение") {
                        modifier.onClick {
                            saves.loadProgress(game.playerId.value, quests.questId)
                            pushLog(game, "[${game.playerId.value}] загружено последнее сохранение квеста ${quests.questId}")
                        }
                    }
                    Button("Сохранить прогресс") {
                        modifier.margin(start = 8.dp).onClick {
                            val pid = game.playerId.value
                            val currentStep = quests.getStep(pid)
                            bus.publish(PlayerProgressSaved(pid, quests.questId, currentStep))
                        }
                    }
                }


                Row { modifier.margin(top = sizes.smallGap)
                    Button("Получить меч (шаг 0)") {
                        modifier.margin(end = 8.dp).onClick {
                            val pid = game.playerId.value
                            quests.completeStep(pid, stepId = 0)
                        }
                    }
                    Button("Ударить манекен (шаг 1)") {
                        modifier.onClick {
                            val pid = game.playerId.value
                            quests.completeStep(pid, stepId = 1)
                        }
                    }
                }

                Row { modifier.margin(top = sizes.smallGap)
                    Button("Атака рукой (1-5)") {
                        modifier.margin(end = 8.dp).onClick {
                            val pid = game.playerId.value
                            val dmg = damageRange("hand").random()
                            bus.publish(DamageDealt(pid, "Тренировочный манекен", dmg))
                        }
                    }
                    Button("Атака мечом (7-18)") {
                        modifier.onClick {
                            val pid = game.playerId.value
                            val dmg = damageRange("sword").random()
                            bus.publish(DamageDealt(pid, "Тренировочный манекен", dmg))
                        }
                    }
                }

                Text("Доступные сохранения:") { modifier.margin(top = sizes.gap) }
                val saveDir = File("saves")
                val saveFiles = if (saveDir.exists()) {
                    saveDir.listFiles()
                        ?.filter { it.isFile && it.name.startsWith("${game.playerId.use()}_${quests.questId}") }
                        ?.sortedByDescending { it.lastModified() } ?: emptyList()
                } else emptyList()

                if (saveFiles.isEmpty()) {
                    Text("(нет сохранений)") { modifier.font(sizes.smallText) }
                } else {
                    Column {
                        saveFiles.forEach { file ->
                            Button(file.name) {
                                modifier
                                    .margin(bottom = 4.dp)
                                    .background(
                                        if (file == saveFiles.firstOrNull())
                                            RoundRectBackground(Color.BLUE, 8.dp)
                                        else
                                            RoundRectBackground(Color.DARK_GRAY, 8.dp)
                                    )
                                    .onClick {
                                        saves.loadFromFile(file)
                                        pushLog(game, "[${game.playerId.value}] загружен файл ${file.name}")
                                    }
                            }
                        }
                    }
                }


                Text("Лог событий") { modifier.margin(top = sizes.gap) }
                Column {
                    game.eventLog.use().forEach { line ->
                        Text(line) { modifier.font(sizes.smallText) }
                    }
                }
            }
        }
    }
}