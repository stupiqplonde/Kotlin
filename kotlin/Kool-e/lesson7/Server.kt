package lesson7

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
<<<<<<< HEAD
import lesson3.EffectApplied
import lesson3.HEALING_POTION
import lesson3.ItemAdded
import lesson3.ItemType
import lesson3.ItemUsed
import lesson3.SWORD
import lesson3.putIntoSlot
import lesson3.useSelected
import lesson5.pushLog
=======
>>>>>>> c68b5f3bb6aed7a0a4c568feaeafce7f357228c6
import java.awt.Choice
import java.awt.dnd.DropTarget
import java.io.File

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
    val newState: QuestState
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

data class CmdLoadDataToServer(
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
<<<<<<< HEAD
            
=======

>>>>>>> c68b5f3bb6aed7a0a4c568feaeafce7f357228c6
            is CmdLoadDataToServer -> {
                savePlayerToDisk(cmd.playerId)
                bus.publish(PlayerProgressSaved(cmd.playerId, "Игрок выгрузил сохранение на диск"))
            }
        }
    }

    // правила квеста (state machine)
    private fun nextQuestState(current: QuestState, event: GameEvent, npcId: String): QuestState {
        // npcId нужен чтобы не реагировать на других нпс не связанных с этапом квеста

        if (npcId != "alchemist") return current

        return when (current) {
            QuestState.START -> when (event) {
                is TalkedToNpc -> QuestState.OFFERED
                else -> QuestState.START
                // если состояние квеста START и происходит событие TalkedToNpc тогда поменять состояние квеста на OFFERED
            }

            QuestState.OFFERED -> when (event) {
                is ChoiceSelected -> {
                    if (event.choiceId == "help") QuestState.HELP_ACCEPTED else QuestState.THREAT_ACCEPTED
                }

                else -> QuestState.OFFERED
            }

            QuestState.THREAT_ACCEPTED -> when (event) {
                is ChoiceSelected -> {
                    if (event.choiceId == "threat_confirm") QuestState.EVIL_END else QuestState.THREAT_ACCEPTED
                }

                else -> QuestState.THREAT_ACCEPTED
            }

            QuestState.HELP_ACCEPTED -> QuestState.GOOD_END
            QuestState.GOOD_END -> QuestState.GOOD_END
            QuestState.EVIL_END -> QuestState.EVIL_END
        }
    }

    private fun setQuestState(playerId: String, player: PlayerData, newState: QuestState) {
        val old = player.questState
        if (newState == old) return

        player.questState = newState

        bus.publish(
            QuestStateChanged(
                playerId,
                questId,
                newState
            )
        )

        bus.publish(
            PlayerProgressSaved(
                playerId,
                "Игрок перешел на новый этап квеста ${newState.name}"
            )
        )
    }

    // сохранение и загрузка на сервере

    private fun saveFile(playerId: String): File{
        val dir = File("saves")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${playerId}_server.save")
    }

    fun savePlayerToDisk(playerId: String){
        val player = ensurePlayer(playerId)
        val file = saveFile(playerId)

        val sb = StringBuilder()
        // пустой сборщик строк

        sb.append("playerId=").append(playerId).append("\n")
        // append - добавление текста в конец списка
        sb.append("hp=").append(player.hp).append("\n")
        sb.append("gold=").append(player.gold).append("\n")
        sb.append("questState=").append(player.questState.name).append("\n")
        // name - превратит enum в строку например "START"

        val text = sb.toString()

        file.writeText(text)
    }

    private fun loadPlayerFromDisk(playerId: String, player: PlayerData){
        val file = saveFile(playerId)
        if (!file.exists()) return

        val map = mutableMapOf<String, String>()
        // словарь для хранения двух частей строки с учетом разделителя
        // hp=100 - в ключ hp в значение 100

        for (line in file.readLines()){
            val parts = line.split("=")
            if (parts.size == 2){
                map[parts[0]] = parts[1]
            }
        }
        player.hp = map["hp"]?.toIntOrNull() ?: 100
        player.gold = map["gold"]?.toIntOrNull() ?: 0

        val  stateName = map["questState"] ?: QuestState.START.name

        player.questState = try {
            QuestState.valueOf(stateName)
        } catch (e: Exception){
            QuestState.START
        }
    }
}

// SaveSystem - отдельная система, которая слушает события и вызывает save на сервере
class SaveSystem(
    private val bus: EventBus,
    private val server: ServerWorld
){
    init {
        bus.subscribe { event ->
            if(event is PlayerProgressSaved){
                server.savePlayerToDisk(event.playerId)
            }
        }
    }
}

class Client(
    private val ui: ClientUiState,
    private val server: ServerWorld
){
    fun send(cmd: GameCommand){
        // UI -> Server отправка команды с текущим пингом
        server.sendCommand(cmd, ui.networkLagMs.value)
    }

    fun syncFromServer(){
        // берем снимок данных с сервера
        val snap = server.getSnapshot(ui.playerId.value)

        // после получения копии данных обновляем клиентский ui state
        ui.hp.value = snap.hp
        ui.gold.value = snap.gold
        ui.questState.value = snap.questState
    }
}

fun main() = KoolApplication {
    val ui = ClientUiState()
    val bus = EventBus()
    val server = ServerWorld(bus)
    val saveSystem = SaveSystem(bus, server)
    val client = Client(ui, server)

    val npc = Npc("alchemist", "Алхимик")

    bus.subscribe { event ->
        val line = when (event) {
            is TalkedToNpc -> "EVENT: игрок ${event.playerId} поговорил с ${event.npcId}"
            is ChoiceSelected -> "EVENT: игрок ${event.playerId} выбрал вариант ${event.choiceId}"
            is QuestStateChanged -> "EVENT: квест ${event.questId} перешел на этап ${event.newState}"
            is PlayerProgressSaved -> "EVENT: сохраненио для ${event.playerId} причина - ${event.reason}"
        }
        pushLog(ui, "[${event.playerId}] $line")
    }

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

        onUpdate {
            server.update(Time.deltaT) // сервер обрабатывает очередь команд
            client.syncFromServer() // клиент обновляет HUD из серверных данных
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
                Text("PLAYER: ${ui.playerId.use()}") {}
                Text("HP: ${ui.hp.use()}") {
                    modifier.margin(bottom = sizes.gap)
                }
                Text("GOLD: ${ui.gold.use()}") {}
                Text("QUEST STATE: ${ui.questState.use()}") {}
                Text("PING: ${ui.networkLagMs.use()}") {}
<<<<<<< HEAD
            }
            Row {
                modifier.margin(top = 6.dp)

                Button("ping 50 ms") {
                    modifier
                        .margin(end = 8.dp)
                        .onClick {
                            ui.networkLagMs.value = 50
                        }
                }

                Button("ping 350 ms") {
                    modifier
                        .margin(end = 8.dp)
                        .onClick {
                            ui.networkLagMs.value = 350
                        }
                }

                Button("ping 1200 ms") {
                    modifier.onClick {
                        ui.networkLagMs.value = 1200
                    }
                }
                Row {
                    modifier.margin(top = 6.dp)

                    Button("сменить игрока") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                ui.playerId.value = if(ui.playerId.value == "Oleg") "Player" else "Oleg"
                            }
                    }

                    Button("загрузить сохранение") {
                        modifier.onClick{
                            client.send(CmdLoadPlayer(playerId = String()))
                        }
                    }

                    Button("выгрузить сохранение") {
                        modifier.onClick{
                            client.send(CmdLoadDataToServer(playerId = String()))
                        }
                    }
=======

                val qState = ui.questState.use()
                Row {
                    modifier.margin(top = 6.dp)

                    Button("ping 50 ms") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                ui.networkLagMs.value = 50
                            }
                    }

                    Button("ping 350 ms") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                ui.networkLagMs.value = 350
                            }
                    }

                    Button("ping 1200 ms") {
                        modifier.onClick {
                            ui.networkLagMs.value = 1200
                        }
                    }
                    Row {
                        modifier.margin(top = 6.dp)

                        Button("сменить игрока") {
                            modifier
                                .margin(end = 8.dp)
                                .onClick {
                                    ui.playerId.value = if (ui.playerId.value == "Oleg") "Player" else "Oleg"
                                }
                        }

                        Button("загрузить сохранение") {
                            modifier.onClick {
                                client.send(CmdLoadPlayer(playerId = String()))
                            }
                        }

                        Button("выгрузить сохранение") {
                            modifier.onClick {
                                client.send(CmdLoadDataToServer(playerId = String()))
                            }
                        }
                    }
                }

                val dialog = npc.dialogueFor(qState)

                Text("${dialog.npcName}:"){
                    modifier.margin(top = sizes.gap)
                }
                Text(dialog.text){
                    modifier.margin(bottom = sizes.smallGap)
                }

                Row {
                    // перебор всех вариантов ответов диалога
                    for(opt in dialog.options){
                        Button(opt.text){
                            modifier.margin(end=8.dp).onClick{
                                val pid = ui.playerId.value

                                when(opt.id){
                                    "talk" -> client.send(CmdTalkToNpc(pid, "alchemist"))
                                    "help" -> client.send(CmdSelectChoice(pid, "alchemist", "help"))
                                    "thread" -> client.send(CmdSelectChoice(pid, "alchemist", "thread"))
                                    "threat_confirm" -> client.send(CmdSelectChoice(pid, "alchemist", "thread_confirm"))
                                }
                            }
                        }
                    }
                }
                // логироване
                Text("LOG:"){modifier.margin(top = sizes.gap)}
                for (line in ui.log.use()){
                    Text(line){modifier.font(sizes.smallText)}
>>>>>>> c68b5f3bb6aed7a0a4c568feaeafce7f357228c6
                }
            }
        }
    }
}
































































<<<<<<< HEAD

=======
>>>>>>> c68b5f3bb6aed7a0a4c568feaeafce7f357228c6
