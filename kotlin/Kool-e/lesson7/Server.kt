package lesson7

import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf

// в игре, которая зависит от общего игрового прогресса игроков клиент не должен уметь менять квесты, золото, инвентарь
// клиент можно будет взломать, только сервер будет решать что можно, а что нельзя, и сервер синхронизирует все
// между игроками одинаково

// Аннотации - разделение кусков кода на клиентские и серверные (мы сами говорим что где будет работать)
// правильная цепочка безопасного кода:
// 1. Клиент (через hud или кнопку) отправляет команду на сервер:
// "я поговорил с алхимиком"
// 2. Сервер принимает команду, проверяет правила, которые ему установили
// 3. Сервер рассылает события (GameEvent) с инфой (Reward / Refuse)
// 4. Клиент получает инфу о том можно ли пройти дальше

enum class QuestState{
    START,
    OFFERED,
    HELP_ACCEPTED,
    THREAT_ACCEPTED,
    EVIL_END,
    GOOD_END
}

data class DialogueOption(
    val id: String,
    val text: String
)

data class DialogueView(
    val npcName: String,
    val text: String,
    val options: List<DialogueOption>
)

class Npc(
    val id: String,
    val name: String
){
    fun dialogueFor(state: QuestState): DialogueView{
        return when(state){
            QuestState.START -> DialogueView(
                name,
                "привет нажми Talk чтобы начать диалог",
                listOf(
                    DialogueOption("talk", "Говорить")
                )
            )
            QuestState.OFFERED -> DialogueView(
                name,
                "Поможешь мне или будешь драться?",
                listOf(
                    DialogueOption("help", "Помочь"),
                    DialogueOption("threat", "Давай драться")
                )
            )
            QuestState.HELP_ACCEPTED -> DialogueView(
                name,
                "Спасибо! победа",
                listOf(
                    DialogueOption("win", "Победа")
                )
            )
            QuestState.THREAT_ACCEPTED -> DialogueView(
                name,
                "Не хочу драться уходи",
                listOf(
                    DialogueOption("lose", "Проигрышь")
                )
            )
            QuestState.GOOD_END -> DialogueView(
                name,
                "you're won",
                emptyList()
            )
            QuestState.EVIL_END -> DialogueView(
                name,
                "you're lose",
                emptyList()
            )
        }
    }
}

// GameState (показывает только HUD)

class ClientUiState{
    // состояния внутри него будут обновляться от серверных данных

    val playerId = mutableStateOf("Oleg")
    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)

    val questState = mutableStateOf(QuestState.START)
    val networkLagMs = mutableStateOf(350)

    val log = mutableStateOf<List<String>>(emptyList())
}

fun pushLog(ui: ClientUiState, text: String){
    ui.log.value = (ui.log.value + text).takeLast(20)
}

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

data class QuestStateChanged(
    override val playerId: String,
    val questId: String,
    val newState: String
) : GameEvent

data class PlayerProgressSaved(
    override val playerId: String,
    val reason: String
) : GameEvent

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

// команды - "запрос клиента на сервер"

sealed interface GameCommand{
    val playerId: String
}

data class CmdTalkToNpc(
    override val playerId: String,
    val npcId: String
) : GameCommand

data class CmdSelectChoice(
    override val playerId: String,
    val npcId: String,
    val choiceId: String
) : GameCommand

data class CmdLoadPlayer(
    override val playerId: String
) : GameCommand

// SERVER WORLD - серверные данные и обработка команд

// PlayerData
data class PlayerData(
    var hp: Int,
    var gold: Int,
    var questState: QuestState
)

// команда, которая ждет выполнения (симуляция пинга)
data class PendingCommand(
    val cmd: GameCommand,
    var delayLeftSec: Float
)

class ServerWorld(
    private val bus: EventBus
) {
    private val questId = "q_alchemist"

    // словарь всех игроков сервера
    private val serverPlayers = mutableMapOf<String, PlayerData>()

    // inbox - очередь выполнения команд с учетом пинга
    private val inbox = mutableListOf<PendingCommand>()

    // метод проверки существования игрока в бд, и если нет -> создаем
    private fun ensurePlayer(playerId: String): PlayerData {
        val existing = serverPlayers[playerId]
        if (existing != null) return existing

        // если пользователь существует в бд, то вернуть его если нет -> создаем
        val created = PlayerData(
            100,
            0,
            QuestState.START
        )
        serverPlayers[playerId] = created
        return created
    }

    // снимок серверных данных
    fun getSnapshot(playerId: String): PlayerData {
        val player = ensurePlayer(playerId)

        // копия важна тк мы в клиенте не может менять информацию об игроке
        // мы отправляем (return) новый объект PlayerData, чтобы клиент не мог прочесть и отобразить
        return PlayerData(
            player.hp,
            player.gold,
            player.questState
        )
    }

    fun sendCommand(cmd: GameCommand, networkLagMs: Int) {
        val lagSec = networkLagMs / 1000f
        // перевод миллисекунд в сек

        // добавляем в очередь выполнения команд
        inbox.add(
            PendingCommand(
                cmd,
                lagSec
            )
        )
    }

    // метод update вызывается каждый кадр, нужен для уменьшения задержки и выполнения команд который дошли
    fun update(deltaSec: Float){
        // delta - сколько прошло времени с прошлого кадра (Time.deltaT)
        // уменьшаем таймер у каждой команды за прошедшее delta время
        for (pending in inbox){
            pending.delayLeftSec -= deltaSec
        }

        // отфильтруем очередь в отдельный список с командами с готовыми к выполнению
        val ready = inbox.filter { it.delayLeftSec <= 0 }

        // удаляем команды, которые надо выполнить из списка очереди
        inbox.removeAll(ready)

        for (pending in ready){
            applyCommand(pending.cmd)
        }
    }

    private fun applyCommand(cmd: GameCommand){
        val player = ensurePlayer(cmd.playerId)

        when(cmd){
            is CmdTalkToNpc -> {
                // публикация события от сервера всей игре это подтверждение сервера, что игрок поговорил
                bus.publish(TalkedToNpc(cmd.playerId, cmd.npcId))

                // после рассылки сервер меняет соответственно правилам которые прописанны в dialogueFor
                val newState = nextQuestState(player.questState, TalkedToNpc(cmd.playerId, cmd.npcId), cmd.npcId)
                setQuestState(cmd.playerId, player, newState)
            }

            is CmdSelectChoice -> {
                // публикация события от сервера всей игре это подтверждение сервера, что игрок поговорил
                bus.publish(ChoiceSelected(cmd.playerId, cmd.npcId, cmd.choiceId))

                // после рассылки сервер меняет соответственно правилам которые прописанны в dialogueFor
                val newState = nextQuestState(player.questState, ChoiceSelected(cmd.playerId, cmd.npcId, cmd.choiceId), cmd.npcId)
                setQuestState(cmd.playerId, player, newState)
            }

            is CmdLoadPlayer -> {
                loadPlayerFromDisk(cmd.playerId, player)
                // после загрузки сохранения игрока - желательно тоже сохранить событием
                bus.publish(PlayerProgressSaved(cmd.playerId, "Игрок загрузил сохранение с диска"))
            }
        }
    }
}

































































