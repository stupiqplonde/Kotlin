package questDependence

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.modules.audio.synth.SampleNode
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.
import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами
import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"
import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf
import jdk.jfr.DataAmount
import jdk.jfr.StackTrace

import kotlinx.coroutines.launch                    // запуск корутин
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

// Flow корутины
import kotlinx.coroutines.flow.MutableSharedFlow    // радиостанция событий
import kotlinx.coroutines.flow.SharedFlow           // чтение для подписчиков
import kotlinx.coroutines.flow.MutableStateFlow     // табло состояний
import kotlinx.coroutines.flow.StateFlow            // только для чтения
import kotlinx.coroutines.flow.asSharedFlow         // отдать наружу только SharedFlow
import kotlinx.coroutines.flow.asStateFlow          // отдать только StateFlow

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.processNextEventInCurrentThread
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

enum class QuestStatus{
    LOCKED,
    ACTIVE,
    COMPLETED,
    FAILED
}

enum class QuestMarker{
    NEW,
    PINNED,
    COMPLETED,
    LOCKED,
    NONE
}

enum class QuestBranch{
    NONE,
    HELP,
    THREAT,
    PAY,
    STEAL
}

data class QuestStateOnServer(
    val questId: String,
    val title: String,
    val status: QuestStatus,
    val step: Int,
    val branch: QuestBranch,
    val progressCurrent: Int,
    val progressTarget: Int,
    val isNew : Boolean,
    val isPinned: Boolean,
    val unlockRequiredQuestId: String?
)

data class QuestJournalEntry(
    val questId: String,
    val title: String,
    val status: QuestStatus,
    val objectiveText: String, // подсказка че делать дальше
    val progressText: String,
    val progressBar: String,
    val marker: QuestMarker,
    val markerHint: String,
    val branchText: String,
    val lockedReason: String
)


// события, которые будут влиять на UI и другие системы---

sealed interface GameEvent{
    val playerId: String
}

data class QuestBranchChosen(
    override val playerId: String,
    val questId: String,
    val branch: QuestBranch
): GameEvent

data class ItemCollected(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int
): GameEvent

data class GoldTurnedIn(
    override val playerId: String,
    val questId: String,
    val amount: Int
): GameEvent

data class QuestCompleted(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestUnlocked(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestJournalUpdated(
    override val playerId: String
): GameEvent

// игрок открыл квест - поменять маркер NEW
data class QuestOpened(
    override val playerId: String,
    val questId: String
): GameEvent


data class QuestPinned(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestProgressed(
    override val playerId: String,
    val questId: String
): GameEvent

data class CommandRejected(
    override val playerId: String,
    val reason: String
): GameEvent

data class ServerMessage(
    override val playerId: String,
    val text: String
): GameEvent

// команды UI -> сервер---

sealed interface GameCommand{
    val playerId: String
}

// игрок открыл квест - поменять маркер NEW
data class CmdOpenQuest(
    override val playerId: String,
    val questId: String
): GameCommand


data class CmdPinQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class CmdProgressQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class CmdSwitchPlayer(
    override val playerId: String,
    val newPlayerId: String
): GameCommand

data class CmdAddQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class CmdGiveGold(
    override val playerId: String,
    val amount: Int
): GameCommand

data class CmdChooseBranch(
    override val playerId: String,
    val questId: String,
    val branch: QuestBranch
): GameCommand

data class CmdCollectItem(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int
): GameCommand

data class CmdTurnInGold(
    override val playerId: String,
    val amount: Int,
    val questId: String
): GameCommand


data class CmdGiveGoldDebug(
    override val playerId: String,
    val questId: String,
    val amount: Int
): GameCommand

data class CmdFinishQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class PlayerData(
    val playerId: String,
    val gold: Int,
    val inventory: Map<String, Int>
)

data class NpcData(
    val npcId: String,
    val inventory: Map<String, Int>
)

class BuyAndSellSystem{

}

class QuestSystem {
    // здесь прописываем текст целей квестов по шагам для каждого квеста
    fun objectiveFor(q: QuestStateOnServer): String {

        if (q.status == QuestStatus.LOCKED) {
            return "квест недоступен"
        }

        if (q.questId == "q_alchemist") {
            return when (q.step) {
                0 -> "Поговори с Алхимиком"
                1 -> {
                    when (q.branch) {
                        QuestBranch.NONE -> "Выбери путь: Help или Threat"
                        QuestBranch.HELP -> "Собери траву ${q.progressCurrent} / ${q.progressTarget}"
                        QuestBranch.THREAT -> "Собери золото ${q.progressCurrent} / ${q.progressTarget}"
                        else -> ""
                    }
                }

                2 -> "Вернись к Алхимику и заверши квест"
                else -> "Квест завершен"
            }
        }
        if (q.questId == "q_guard"){
            return when (q.step) {
                0 -> "Поговори со Стражником"
                1 -> "Заплати стражнику золото: ${q.progressCurrent} / ${q.progressTarget}"
                2 -> "Сдай квест у стражника"
                else -> "Квест завершен"
            }
        }

        if (q.questId == "q_cook"){
            return when (q.step){
                0 -> "Поговори с Торговцем"
                1 -> {
                    when (q.branch) {
                        QuestBranch.NONE -> "Выбери путь: Pay или Steal"
                        QuestBranch.PAY -> "Заплати за чечевицу ${q.progressCurrent} / ${q.progressTarget}"
                        QuestBranch.STEAL -> "Укради чечевицу ${q.progressCurrent} / ${q.progressTarget}"
                        else -> ""
                    }
                }
                2 -> "Вернись к Алхимику и свари суп"
                else -> "Квест завершен"
            }
        }
        return "Неизвестный квест"
    }

    // подсказки куда идти - в будущем для карты и компаса
    fun markerHintFor(q: QuestStateOnServer): String {
        if (q.status == QuestStatus.LOCKED) {
            return "сначала разблокируй квест"
        }

        if (q.questId == "q_alchemist") {
            return when (q.step) {
                0 -> "NPC: Алхимик"
                1 -> {
                    when (q.branch) {
                        QuestBranch.NONE -> "Выбери вариант диалога"
                        QuestBranch.HELP -> "Собери траву"
                        QuestBranch.THREAT -> "Найди золото"
                        else -> ""
                    }
                }
                2 -> "NPC: Алхимик"
                else -> "готово"
            }
        }

        if (q.questId == "q_guard") {
            return when (q.step) {
                0 -> "NPC: Стражник"
                1 -> "Найди золото для оплаты"
                2 -> "NPC: Стражник"
                else -> "готово"
            }
        }

        if (q.questId == "q_cook") {
            return when (q.step) {
                0 -> "NPC: Торговец"
                1 -> {
                    when (q.branch) {
                        QuestBranch.NONE -> "Выбери вариант диалога"
                        QuestBranch.PAY -> "Заплати за чечевицу"
                        QuestBranch.STEAL -> "Укради чечевицу"
                        else -> ""
                    }
                }
                2 -> "NPC: Алхимик"
                else -> "готово"
            }
        }

        return ""
    }

    fun branchTextFor(branch: QuestBranch): String {
        return when (branch) {
            QuestBranch.NONE -> "Путь не выбран"
            QuestBranch.HELP -> "Путь помощи"
            QuestBranch.THREAT -> "Путь угрозы"
            QuestBranch.PAY -> "Путь лоха"
            QuestBranch.STEAL -> "Путь воровства"
        }
    }

    fun lockedReasonFor(q: QuestStateOnServer): String{
        if (q.status != QuestStatus.LOCKED) return ""

        return if(q.unlockRequiredQuestId == null){
            "Причина блокировки неизвестна"
        }else{
            "Нужно завершить квест ${q.unlockRequiredQuestId}"
        }
    }

    fun markerFor(q: QuestStateOnServer): QuestMarker{
        return when{
            q.status == QuestStatus.LOCKED -> QuestMarker.LOCKED
            q.status == QuestStatus.COMPLETED -> QuestMarker.COMPLETED
            q.isPinned -> QuestMarker.PINNED
            q.isNew -> QuestMarker.NEW
            else -> QuestMarker.NONE
        }
    }

    fun progressBarText(current: Int, target: Int, blocks: Int = 10): String{
        if (target <= 0) return  ""

        val ratio = current.toFloat() / target.toFloat()
        // ratio - отношение прогресса к цели

        val filled = (ratio * blocks).toInt().coerceIn(10, blocks)
        // coerceIn - ограничение от 0 до ... blocks (10) числа

        val empty = blocks - filled

        return "▰".repeat(filled) + "▱".repeat(empty)
    }

    fun progressBarPercentages(current: Int, target: Int, blocks: Int = 10): String{
        if (target <= 0) return  ""

        val ratio = current.toFloat() / target.toFloat()
        // ratio - отношение прогресса к цели

        return "Прогресс: ${ratio * 100}%"
    }

    fun toJournalEntry(q: QuestStateOnServer): QuestJournalEntry{
        val progressText = if(q.progressTarget > 0) "${q.progressCurrent} / ${q.progressTarget}" else ""

        val progressBar = if(q.progressTarget > 0) progressBarText(q.progressCurrent, q.progressTarget) else ""

        return QuestJournalEntry(
            q.questId,
            q.title,
            q.status,
            objectiveFor(q),
            progressText,
            progressBar,
            markerFor(q),
            markerHintFor(q),
            branchTextFor(q.branch),
            lockedReasonFor(q)
        )
    }

    fun applyEvent(
        quests: List<QuestStateOnServer>,
        event: GameEvent
    ): List<QuestStateOnServer>{
        val copy = quests.toMutableList()

        for (i in copy.indices){
            val q = copy[i]

            if (q.status == QuestStatus.LOCKED) continue
            if (q.status == QuestStatus.COMPLETED) continue

            if (q.questId == "q_alchemist"){
                copy[i] = updateAlchemist(q, event)
            }

            if (q.questId == "q_guard"){
                copy[i] = updateGuard(q, event)
            }

            if (q.questId == "q_cook"){
                copy[i] = updateCook(q, event)
            }
        }
        return copy.toList()
    }

    private fun updateAlchemist(q: QuestStateOnServer, event: GameEvent): QuestStateOnServer{
        if (q.step == 0 && event is QuestBranchChosen && event.questId == q.questId){
            return when (event.branch){
                QuestBranch.HELP -> q.copy(
                    step = 1,
                    branch = QuestBranch.HELP,
                    progressCurrent = 0,
                    progressTarget = 3,
                    isNew = false
                )

                QuestBranch.THREAT -> q.copy(
                    step = 1,
                    branch = QuestBranch.THREAT,
                    progressCurrent = 0,
                    progressTarget = 10,
                    isNew = false
                )

                QuestBranch.NONE -> q

                else -> q
            }
        }

        if (q.step == 1 && q.branch == QuestBranch.HELP && event is ItemCollected && event.itemId == "Herb"){
            val newCurrent = (q.progressCurrent + event.countAdded).coerceAtMost(q.progressTarget)
            val update = q.copy(progressCurrent = newCurrent, isNew = false)

            if (newCurrent >= q.progressTarget){
                return update.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }

            return update
        }
        if (q.step == 1 && q.branch == QuestBranch.THREAT && event is GoldTurnedIn && event.questId == q.questId){
            val newCurrent = (q.progressCurrent + event.amount).coerceAtMost(q.progressTarget)
            val update = q.copy(progressCurrent = newCurrent, isNew = false)

            if (newCurrent >= q.progressTarget){
                return update.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }

            return update
        }
        return q
    }

    fun updateGuard(q: QuestStateOnServer, event: GameEvent): QuestStateOnServer{
        val base = if (q.step == 0){
            q.copy(step = 1, progressCurrent = 0, progressTarget = 5, isNew = false)
        }else q

        if (base.step == 1 && event is GoldTurnedIn && event.questId == base.questId){
            val newCurrent = (base.progressCurrent + event.amount).coerceAtMost(base.progressTarget)
            val updated = base.copy(progressCurrent = newCurrent, isNew = false)

            if (newCurrent >= base.progressTarget){
                return updated.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }
            return updated
        }
        return base
    }

    fun updateCook(q: QuestStateOnServer, event: GameEvent): QuestStateOnServer{
        if (q.step == 0 && event is QuestBranchChosen && event.questId == q.questId){
            return when (event.branch){
                QuestBranch.PAY -> q.copy(
                    step = 1,
                    branch = QuestBranch.PAY,
                    progressCurrent = 0,
                    progressTarget = 5,
                    isNew = false
                )

                QuestBranch.STEAL -> q.copy(
                    step = 1,
                    branch = QuestBranch.STEAL,
                    progressCurrent = 0,
                    progressTarget = 20,
                    isNew = false
                )

                QuestBranch.NONE -> q

                else -> q
            }
        }

        if (q.step == 1 && q.branch == QuestBranch.PAY && event is GoldTurnedIn && event.questId == q.questId){
            val newCurrent = (q.progressCurrent + event.amount).coerceAtMost(q.progressTarget)
            val update = q.copy(progressCurrent = newCurrent, isNew = false)

            if (newCurrent >= q.progressTarget){
                return update.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }

            return update
        }
        if (q.step == 1 && q.branch == QuestBranch.STEAL && event is ItemCollected && event.itemId == "Lentils"){
            val newCurrent = (q.progressCurrent + event.countAdded).coerceAtMost(q.progressTarget)
            val update = q.copy(progressCurrent = newCurrent, isNew = false)

            if (newCurrent >= q.progressTarget){
                return update.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }

            return update
        }
        return q
    }
}

class GameServer{
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _commands = MutableSharedFlow<GameCommand>(extraBufferCapacity = 64)
    val commands: SharedFlow<GameCommand> = _commands.asSharedFlow()

    fun trySend(cmd: GameCommand): Boolean = _commands.tryEmit(cmd)

    private val _players = MutableStateFlow(
        mapOf(
            "Oleg" to PlayerData("Oleg", 0, emptyMap()),
            "Stas" to PlayerData("Stas", 0, emptyMap())
        )
    )

    private val _npcs = MutableStateFlow(
        mapOf(
            "Seller" to NpcData("Seller", emptyMap())
        )
    )

    val players: StateFlow<Map<String, PlayerData>> = _players.asStateFlow()

    val npcs: StateFlow<Map<String, NpcData>> = _npcs.asStateFlow()

    private val _questByPlayer = MutableStateFlow(
        mapOf(
            "Oleg" to initialQuestList(),
            "Stas" to initialQuestList()
        )
    )
    val questByPlayer: StateFlow<Map<String, List<QuestStateOnServer>>> = _questByPlayer.asStateFlow()

    fun start(scope: kotlinx.coroutines.CoroutineScope, questSystem: QuestSystem){
        scope.launch {
            commands.collect { cmd ->
                processCommand(cmd, questSystem)
            }
        }
    }

    private fun buy(npcData: NpcData, playerData: PlayerData, itemId: String){
        val items = npcData.inventory
        val player = playerData.playerId


    }

    private fun setNpcData(npcId: String, data: NpcData){
        val map = _npcs.value.toMutableMap()
        map[npcId] = data
        _npcs.value = map.toMap()
    }

    private fun getNpcData(npcId: String): NpcData{
        return _npcs.value[npcId] ?: NpcData(npcId, emptyMap())
    }

    private fun setPlayerData(playerId: String, data: PlayerData){
        val map = _players.value.toMutableMap()
        map[playerId] = data
        _players.value = map.toMap()
    }

    private fun getPlayerData(playerId: String): PlayerData{
        return _players.value[playerId] ?: PlayerData(playerId, 0, emptyMap())
    }

    private fun setQuests(playerId: String, quests: List<QuestStateOnServer>){
        val map = _questByPlayer.value.toMutableMap()
        map[playerId] = quests
        _questByPlayer.value = map.toMap()
    }

    private fun getQuests(playerId: String): List<QuestStateOnServer>{
        return _questByPlayer.value[playerId] ?: emptyList()
    }

    private suspend fun processCommand(cmd: GameCommand, questSystem: QuestSystem){
        when(cmd){
            is CmdOpenQuest -> {
                val list = getQuests(cmd.playerId).toMutableList()

                for (i in list.indices){
                    if (list[i].questId == cmd.questId){
                        list[i] = list[i].copy(isNew = false)
                    }
                }
                setQuests(cmd.playerId, list)
                _events.emit(QuestJournalUpdated(cmd.playerId))
            }

            is CmdPinQuest -> {
                val list = getQuests(cmd.playerId).toMutableList()

                for (i in list.indices){
                    if (list[i].questId == cmd.questId){
                        list[i] = list[i].copy(isPinned = false)
                    }
                }
                setQuests(cmd.playerId, list)
                _events.emit(QuestJournalUpdated(cmd.playerId))
            }

            is CmdChooseBranch -> {
                val quests = getQuests(cmd.playerId)
                val target = quests.find { it.questId == cmd.questId }

                if (target == null){
                    _events.emit(ServerMessage(cmd.playerId, "Квест ${cmd.questId} не найден"))
                    return
                }
                if (target.status != QuestStatus.ACTIVE){
                    _events.emit(ServerMessage(cmd.playerId, "Квест ${cmd.questId} сейчас не активен"))
                }

                val ev = QuestBranchChosen(cmd.playerId, cmd.questId, cmd.branch)
                _events.emit(ev)

                val updated = questSystem.applyEvent(quests, ev)
                setQuests(cmd.playerId, updated)

                _events.emit(QuestJournalUpdated(cmd.playerId))
            }

            is CmdGiveGoldDebug -> {
                val player = getPlayerData(cmd.playerId)
                setPlayerData(cmd.playerId, player.copy(gold = player.gold + cmd.amount))
                _events.emit(ServerMessage(cmd.playerId, "Выдано золото +${cmd.amount}"))
            }

            is CmdTurnInGold -> {
                val player = getPlayerData(cmd.playerId)

                if(player.gold < cmd.amount){
                    _events.emit(ServerMessage(cmd.playerId, "Недостаточно богат, нужно ${cmd.amount}"))
                    return
                }

                setPlayerData(cmd.playerId, player.copy(gold = player.gold - cmd.amount))

                val ev = GoldTurnedIn(cmd.playerId, cmd.questId, cmd.amount)
                _events.emit(ev)

                val updated = questSystem.applyEvent(getQuests(cmd.playerId), ev)
                setQuests(cmd.playerId, updated)

                _events.emit(QuestJournalUpdated(cmd.playerId))
            }

            is CmdFinishQuest -> {
                finishQuest(cmd.playerId, cmd.questId)
            }
            else -> {}
        }
    }

    private suspend fun finishQuest(playerId: String, questId: String){
        val list = getQuests(playerId).toMutableList()

        val index = list.indexOfFirst { it.questId == questId }

        if (index == -1){
            _events.emit(ServerMessage(playerId, "Квест $questId не найден "))
            return
        }

        val q = list[index]

        if (q.status != QuestStatus.ACTIVE){
            _events.emit(ServerMessage(playerId, "Нельзя завершить $questId статус: ${q.status}"))
            return
        }

        if (q.step != 2){
            _events.emit(ServerMessage(playerId, "Нельзя завершить $questId, сначала дойди до этапа 2"))
            return
        }

        list[index] = q.copy(
            status = QuestStatus.COMPLETED,
            step = 3,
            isNew = false
        )

        setQuests(playerId, list)
        _events.emit(QuestCompleted(playerId, questId))

        unlockDependentQuest(playerId, questId)

        _events.emit(QuestJournalUpdated(playerId))
    }

    private suspend fun unlockDependentQuest(playerId: String, completedQuestId: String){
        val list = getQuests(playerId).toMutableList()
        var changed = false

        for (i in list.indices){
            val q = list[i]

            if (q.status == QuestStatus.LOCKED && q.unlockRequiredQuestId == completedQuestId){
                list[i] = q.copy(
                    status = QuestStatus.ACTIVE,
                    isNew = true
                )
                changed = true

                _events.emit(QuestUnlocked(playerId, q.questId))
            }
        }
        if (changed){
            setQuests(playerId, list)
        }
    }
}

fun initialQuestList(): List<QuestStateOnServer>{
    return listOf(
        QuestStateOnServer(
            "q_alchemist",
            "Помочь жессе пинкману",
            QuestStatus.ACTIVE,
            0,
            QuestBranch.NONE,
            0,
            0,
            true,
            false,
            null
        ),
        QuestStateOnServer(
            "q_guard",
            "Подкупить стража",
            QuestStatus.LOCKED,
            0,
            QuestBranch.NONE,
            0,
            0,
            false,
            false,
            "q_alchemist"
        ),
        QuestStateOnServer(
            "q_cook",
            "Сварить чечевичный супец",
            QuestStatus.LOCKED,
            0,
            QuestBranch.NONE,
            0,
            0,
            false,
            false,
            "q_guard"
        )
    )
}

class HudState{
    val activePlayerIdFlow = MutableStateFlow("Oleg")
    val activePlayerIdUi = mutableStateOf("Oleg")

    val gold = mutableStateOf(0)
    val inventoryText = mutableStateOf("Inventory(empty)")

    val questEntries = mutableStateOf<List<QuestJournalEntry>>(emptyList())
    val selectedQuests = MutableStateFlow<String?>(null)
    val selectedQuestId = mutableStateOf<String?>(null)

    val log = mutableStateOf<List<String>>(emptyList())
}

fun hudLog(hud: HudState, text: String){
    hud.log.value = (hud.log.value + text).takeLast(25)
}

fun markerSymbol(marker: QuestMarker): String{
    return when(marker){
        QuestMarker.NEW -> "❗"
        QuestMarker.PINNED -> "📍"
        QuestMarker.COMPLETED -> "✔"
        QuestMarker.LOCKED -> "❌"
        QuestMarker.NONE -> "🥚"
    }
}

fun journalSortRank(entry: QuestJournalEntry): Int{
    return when{
        entry.marker == QuestMarker.PINNED -> 0
        entry.marker == QuestMarker.NEW -> 1
        entry.status == QuestStatus.ACTIVE -> 2
        entry.status == QuestStatus.LOCKED -> 3
        entry.status == QuestStatus.COMPLETED -> 4
        else -> 5
    }
}

fun eventToText(event: GameEvent): String{
    return when(event){
        is QuestBranchChosen -> "QuestChosen ${event.questId} -> ${event.branch}"
        is ItemCollected -> "ItemCollected ${event.itemId} x ${event.countAdded}"
        is GoldTurnedIn -> "GoldTurnedIn ${event.questId} -${event.amount}"
        is QuestCompleted -> "QuestCompleted ${event.questId}"
        is QuestUnlocked -> "QuestUnlocked ${event.questId}"
        is QuestJournalUpdated -> "QuestJournalUpdated ${event.playerId}"
        is ServerMessage -> "Server ${event.text}"
        else -> ""
    }
}


fun main() = KoolApplication {
    val hud = HudState()
    val server = GameServer()
    val quests = QuestSystem()

    addScene {
        defaultOrbitCamera()
        addColorMesh {
            generate { cube { colored() } }

            shader = KslPbrShader {
                color { vertexColor() }
                metallic(0.7f)
                roughness(0.4f)
            }

            onUpdate {
                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
            }
        }

        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }

        server.start(coroutineScope, quests)
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
                Text {  }

                Text("Активные квесты:") { modifier.margin(top = sizes.gap) }

                val entries = hud.questEntries.use()

                for (q in entries) {
                    if (q.status == QuestStatus.ACTIVE || q.status == QuestStatus.LOCKED || q.status == QuestStatus.FAILED){
                        val symbol = markerSymbol(q.marker)

                        val line = "$symbol ${q.title}"
                        // Кнопка открытия квеста
                        Button(line) {
                            modifier.margin(bottom = sizes.smallGap).onClick {
                                hud.selectedQuestId.value = q.questId
                                // если квест открыт отправить серверу команду, что он уже не новый
                                server.trySend(CmdOpenQuest(hud.activePlayerIdUi.value, q.questId))
                            }
                        }
                    }
                }
            }
        }
        addPanelSurface {
            modifier
                .align(AlignmentX.End, AlignmentY.Top)
                .margin(16.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                val entries = hud.questEntries.use()
                val finishedQuests = entries.filter { it.status == QuestStatus.COMPLETED }

                Text("Конченые квесты: $finishedQuests") { modifier.margin(top = sizes.gap) }
            }
        }


    }
}

// ТЕСТОВАЯ ЧАСТЬ
// 1. a
// 2. b
// 3. Запустите команду git ls-files для получения списка файлов, затем используйте xargs wc -l для подсчета количества строк в каждом файле













































