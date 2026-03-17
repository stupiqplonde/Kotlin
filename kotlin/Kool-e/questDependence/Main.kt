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
    THREAT
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

class QuestSystem {
    // здесь прописываем текст целей квестов по шагам для каждого квеста
    fun objectiveFor(q: QuestStateOnServer): String {

        if (q.status == QuestStatus.LOCKED){
            return "квест недоступен"
        }

        if (q.questId == "q_alchemist"){
            return when(q.step){
                0 -> "Поговори с Алхимиком"
                1 -> {
                    when(q.branch){
                        QuestBranch.NONE -> "Выбери путь: Help или Threat"
                        QuestBranch.HELP -> "Собери траву ${q.progressCurrent} / ${q.progressTarget}"
                        QuestBranch.THREAT -> "Собери золото ${q.progressCurrent} / ${q.progressTarget}"
                    }
                }
                2 -> "Вернись к Алхимику и заверши квест"
                else -> "Квест завершен"
            }
        }
        if (q.questId == "q_guard"){
            return when (q.step){
                0 -> "Поговори со Стражником"
                1 -> "Заплати стражнику золото: ${q.progressCurrent} / ${q.progressTarget}"
                2 -> "Сдай квест у стражника"
                else -> "Квест завершен"
            }
        }
        return "Неизвестный квест"
    }

    // подсказки куда идти - в будущем для карты и компаса
    private fun markerHintFor(questId: String, step: Int): String {
        return when (questId) {
            "q_alchemist" -> when (step) {
                0 -> "Идти к NPC: Алхимик"
                1 -> "Собрать Herb x2"
                2 -> "Вернись к Алхимику"
                else -> "Готово"
            }

            "q_guard" -> when (step) {
                0 -> "Идти к NPC: Страж"
                1 -> "Найди чем расплатиться со Стражем"
                else -> "Готово"
            }

            "q_cook" -> when (step) {
                0 -> "Иди к NPC: Алхимик"
                1 -> "Одолжи у Алхимика чан для зелья"
                else -> "Суп готов"
            }

            else -> ""
        }
    }
}