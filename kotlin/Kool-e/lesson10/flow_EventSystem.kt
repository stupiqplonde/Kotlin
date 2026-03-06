package lesson10

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

// Flow корутины
import kotlinx.coroutines.launch                    // запуск корутин
import kotlinx.coroutines.flow.MutableSharedFlow    // радиостанция событий
import kotlinx.coroutines.flow.SharedFlow           // чтение для подписчиков
import kotlinx.coroutines.flow.MutableStateFlow     // табло состояний
import kotlinx.coroutines.flow.StateFlow            // только для чтения
import kotlinx.coroutines.flow.asSharedFlow         // отдать наружу только SharedFlow
import kotlinx.coroutines.flow.asStateFlow          // отдать только StateFlow

import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

// импорты Serialization
import kotlinx.serialization.Serializable           // аннотация, что можно сохранять
import kotlinx.serialization.builtins.ShortArraySerializer
import kotlinx.serialization.json.Json              // формат файла Json

import java.io.File                                 // для работы с файлами
import kotlin.io.readText

@Serializable
data class PlayerSave(
    val playerId: String,
    val hp: Int,
    val gold: Int,
    val poisonTicksLeft: Int,
    val attackSpeedBuffTicksLeft: Int,
    val attackCooldownMsLeft: Long,
    val questState: String
)

class NpcState(
    val npcId: String,
    val hp: Int
)

// События игровые - Flow будет рассылать их всем системам

sealed interface GameEvent{
    val playerId: String
}

data class AttackPressed(
    override val playerId: String,
    val targetId: String
): GameEvent

data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int
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

data class PoisonApplied(
    override val playerId: String,
    val ticks: Int,
    val damagePerTick: Int,
    val intervalMs: Long
): GameEvent

data class TalkedToNpc(
    override val playerId: String,
    val npcId: String
): GameEvent

data class SaveRequested(
    override val playerId: String
): GameEvent

data class CommandRejected(
    override val playerId: String,
    val reason: String
): GameEvent

data class AttackSpeedBuffApplied(
    override val playerId: String,
    val ticks: Int
): GameEvent

class GameServer{
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    // дополнительный буфер

    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _players = MutableStateFlow(
        mapOf(
            "Oleg" to PlayerSave("Oleg", 100, 0, 0, 0,0L, "START"),
            "Stas" to PlayerSave("Stas", 100, 0, 0, 0,0L, "START")
        )
    )

//    private val _npc = MutableStateFlow(
//        "Kirill" to NpcSave("Kirill", 50)
//    )

    val players: StateFlow<Map<String, PlayerSave>> = _players.asStateFlow()

    fun tryPublish(event: GameEvent): Boolean{
        return _events.tryEmit(event)
    }

    suspend fun publish(event: GameEvent){
        _events.emit(event)
    }

    fun updatePlayer(playerId: String, change: (PlayerSave) -> PlayerSave){
        // change - функция замены

        val oldMap = _players.value
        val oldPlayer = oldMap[playerId] ?: return

        val newPlayer = change(oldPlayer)

        val newMap = oldMap.toMutableMap()
        newMap[playerId] = newPlayer

        _players.value = newMap.toMutableMap()
    }

    fun getPlayer(playerId: String): PlayerSave{
        return _players.value[playerId] ?: PlayerSave(playerId, 100, 0, 0, 0, 0L, "START")
    }


}

class DamageSystem(
    private val server: GameServer
){
    fun onEvent(e: GameEvent){
        if (e is DamageDealt){
            server.updatePlayer(e.playerId) { player ->
                val newHp = (player.hp - e.amount).coerceAtLeast(0)

                player.copy(hp = newHp)
            }
        }
    }


}


class CooldownSystem(
    private val server: GameServer,
    private val scope: kotlinx.coroutines.CoroutineScope
){
    private val cooldownJobs = mutableMapOf<String, Job>()

    private val baseCooldownMs = 1200L
    private val buffedCooldownMs = 700L

    fun getCurrentCooldown(playerId: String): Long {
        val player = server.getPlayer(playerId)
        return if (player.attackSpeedBuffTicksLeft > 0) {
            buffedCooldownMs
        } else {
            baseCooldownMs
        }
    }

    fun startCooldown(playerId: String, totalMs: Long){
        cooldownJobs[playerId]?.cancel()

        server.updatePlayer(playerId) {player -> player.copy(attackCooldownMsLeft = totalMs)}

        val job = scope.launch {
            val step = 100L

            while (isActive && server.getPlayer(playerId).attackCooldownMsLeft > 0L){
                server.updatePlayer(playerId) { player ->
                    val left = (player.attackCooldownMsLeft - step).coerceAtLeast(0L)
                    player.copy(attackCooldownMsLeft = left)
                }
            }
        }

        cooldownJobs[playerId] = job
    }

    fun canAttack(playerId: String): Boolean{
        return server.getPlayer(playerId).attackCooldownMsLeft <= 0L
    }
}

class PoisonSystem(
    private val server: GameServer,
    private val scope: kotlinx.coroutines.CoroutineScope
){
    private val poisonJobs = mutableMapOf<String, Job>()

    fun onEvent(e: GameEvent, publishDamage: (DamageDealt) -> Unit){
        if (e is PoisonApplied){
            poisonJobs[e.playerId]?.cancel()

            server.updatePlayer(e.playerId) { player ->
                player.copy(poisonTicksLeft = player.poisonTicksLeft + e.ticks)
            }

            val job = scope.launch {
                while (isActive && server.getPlayer(e.playerId).poisonTicksLeft > 0){
                    delay(e.intervalMs)

                    server.updatePlayer(e.playerId) { player ->
                        player.copy(poisonTicksLeft = (player.poisonTicksLeft - 1).coerceAtLeast(0))
                    }

                    publishDamage(DamageDealt(e.playerId, "self", e.damagePerTick))
                }
            }
            poisonJobs[e.playerId] = job
        }
    }
}


class QuestSystem(
    private val server: GameServer,
    private val scene: kotlinx.coroutines.CoroutineScope
){
    private val questId = "q_alchemist"
    private val npcId = "alchemist"
    val quests = mutableMapOf<String, Map<String, String>>()

    fun onEvent(e: GameEvent, publish: (GameEvent) -> Unit){
        val player = server.getPlayer(e.playerId)

        when(e){
            is TalkedToNpc -> {
                if (e.npcId != npcId) return

                if (player.questState == "START"){
                    server.updatePlayer(e.playerId) { it.copy(questState = "OFFERED")}
                    publish(QuestStateChanged(e.playerId, questId, "OFFERED"))
                }
            }
            is ChoiceSelected -> {
                try {
                    if (e.npcId != npcId) return

                    if (player.questState == "OFFERED") {
                        val newState =
                            if (e.choiceId == "help") "GOOD_END"
                            else "EVIL_END"

                        server.updatePlayer(e.playerId) { it.copy(questState = newState) }
                        publish(QuestStateChanged(e.playerId, questId, newState))
                    }
                }catch (e: Exception){
                    publish(CommandRejected(player.playerId, "Ошибка перехода"))
                }
//                try {
//                    if (player.questState == "START") {
//                        if (e.choiceId == "help") {
//                            return
//                        }
//                    }
//                }catch (e: Exception){
//                    publish(CommandRejected(e.playerId, questId))
//                }
//                if (e.npcId != npcId) return
//
//                if (player.questState == "OFFERED"){
//                    val newState =
//                        if (e.choiceId == "help") "GOOD_END"
//                        else "EVIL_END"
//
//                    server.updatePlayer(e.playerId) {it.copy(questState = newState)}
//                    publish(QuestStateChanged(e.playerId, questId, newState))
//                }
            }
            else -> {}
        }
    }
}

class SaveSystem {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun file(playerId: String): File {
        val dir = File("saves")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$playerId.json")
    }

    fun save(player: PlayerSave) {
        val text = json.encodeToString(PlayerSave.serializer(), player)

        file(player.playerId).writeText(text)
    }

    fun load(playerId: String): PlayerSave? {
        val file = file(playerId)
        if (!file.exists()) return null

        val text = file.readText()

        return try{
            json.decodeFromString(PlayerSave.serializer(), text)
        }catch (e: Exception) {
            null
        }
    }
}

class AttackSpeedBuffSystem(
    private val server: GameServer,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    private val buffJobs = mutableMapOf<String, Job>()

    fun onEvent(e: GameEvent) {
        if (e is AttackSpeedBuffApplied) {
            server.updatePlayer(e.playerId) { player ->
                player.copy(attackSpeedBuffTicksLeft = player.attackSpeedBuffTicksLeft + e.ticks)
            }

            if (buffJobs.containsKey(e.playerId)) {
                return
            }

            val job = scope.launch {
                val tickRate = 1000L

                while (isActive) {
                    delay(tickRate)

                    val player = server.getPlayer(e.playerId)
                    if (player.attackSpeedBuffTicksLeft <= 0) {
                        break
                    }

                    server.updatePlayer(e.playerId) { player ->
                        player.copy(attackSpeedBuffTicksLeft = player.attackSpeedBuffTicksLeft - 1)
                    }
                }
                buffJobs.remove(e.playerId)
            }

            buffJobs[e.playerId] = job
        }
    }
}

class HudState{
    val activePlayerId = mutableStateOf("Oleg")

    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)
    val poisonTicksLeft = mutableStateOf(0)
    val attackSpeedBuffTicksLeft = mutableStateOf(0)
    val questState = mutableStateOf("START")
    val attackCooldownMsLeft = mutableStateOf(0L)

    val log = mutableStateOf<List<String>>(emptyList())
}

fun hudLog(hud: HudState, line: String){
    hud.log.value = (hud.log.value + line).takeLast(20)
}

object Shared{
    var server: GameServer? = null
    var saver: SaveSystem? = null
    var cooldowns: CooldownSystem? = null
    var attackSpeedBuff: AttackSpeedBuffSystem? = null
    var quests: QuestSystem? = null
    var poison: PoisonSystem? = null
    var damage: DamageSystem? = null
}


fun main() = KoolApplication {
    val hud = HudState()
    val npc = NpcState("Kirill", 50)

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

        val server = GameServer()

        val saver = SaveSystem()
        val damage = DamageSystem(server)
        val cooldowns = CooldownSystem(server, coroutineScope)
        val poison = PoisonSystem(server, coroutineScope)
        val quests = QuestSystem(server, coroutineScope)
        val attackSpeedBuff = AttackSpeedBuffSystem(server, coroutineScope)

        Shared.server = server
        Shared.saver = saver
        Shared.damage = damage
        Shared.cooldowns = cooldowns
        Shared.poison = poison
        Shared.quests = quests
        Shared.attackSpeedBuff = attackSpeedBuff

        coroutineScope.launch{
            server.events.collect{ event ->
                damage.onEvent(event)
            }
        }

        coroutineScope.launch {
            server.events.collect { event ->
                poison.onEvent(event) { dmg ->
                    if (!server.tryPublish(dmg)){
                        coroutineScope.launch { server.publish(dmg) }
                    }
                }
            }
        }

        coroutineScope.launch {
            server.events.collect{ event ->
                quests.onEvent(event) {newEvent ->
                    if (!server.tryPublish(newEvent)){
                        coroutineScope.launch { server.publish(newEvent) }
                    }
                }
            }
        }

        coroutineScope.launch {
            server.events.collect{ event ->
                if (event is SaveRequested) {
                    val snapShot = server.getPlayer(event.playerId)
                    saver.save(snapShot)
                }
            }
        }
    }

    addScene {
        setupUiScene(ClearColorLoad)

        val server = Shared.server

        if (server != null){
            coroutineScope.launch {
                server.events.collect { event ->
                    val line = when (event){
                        is AttackPressed -> "${event.playerId} атаковал ${event.targetId}"
                        is DamageDealt -> "${event.targetId} получил ${event.amount} урона"
                        is PoisonApplied -> "на ${event.playerId} наложен яд на ${event.ticks} тиков"
                        is TalkedToNpc -> "${event.playerId} начал разговор с ${event.npcId}"
                        is ChoiceSelected -> "${event.playerId} выбрал ${event.choiceId}"
                        is SaveRequested -> "Запрос на сохранение"
                        is AttackSpeedBuffApplied -> "Бафф скорости атаки"
                        is CommandRejected -> "Ошибка перехода"
                        is QuestStateChanged -> "${event.playerId} перешел на новый этап квеста ${event.newState}"
                        else -> "Неизвестная команда"
                    }

                    hudLog(hud, "$line")
                }
            }

            coroutineScope.launch {
                server.players.collect { playersMap ->
                    val pid = hud.activePlayerId.value
                    val player = playersMap[pid] ?: return@collect

                    hud.hp.value = player.hp
                    hud.gold.value = player.gold
                    hud.poisonTicksLeft.value = player.poisonTicksLeft
                    hud.questState.value = player.questState
                    hud.attackCooldownMsLeft.value = player.attackCooldownMsLeft
                    hud.attackSpeedBuffTicksLeft.value = player.attackSpeedBuffTicksLeft
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

                Text("Npc: ${npc.npcId}") {}
                Text("Npc HP: ${hud.hp.use()}") {
                    modifier.margin(bottom = sizes.gap)
                }

                Text("Player: ${hud.activePlayerId.use()}") {}
                Text("HP: ${hud.hp.use()} Gold: ${hud.gold.use()}") {
                    modifier.margin(bottom = sizes.gap)
                }

                Text("QuestState: ${hud.questState.use()}") {}
                Text("Poison ticks left: ${hud.poisonTicksLeft.use()}") {}
                Text("Attack cooldown: ${hud.attackCooldownMsLeft.use()}Ms") {
                    modifier.margin(bottom = sizes.gap)
                }

                Row {
                    Button("Switch Player") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                hud.activePlayerId.value = if (hud.activePlayerId.value == "Oleg") "Stas" else "Oleg"

                            }
                    }
                    Button("Save JSON") {
                        modifier.onClick {
                            val server = Shared.server
                            if (server == null) return@onClick

                            val playerId = hud.activePlayerId.value

                            val event = SaveRequested(playerId)
                            val published = server.tryPublish(event)

                            if (published) {
                                hudLog(hud, "сохранено без использования корутин")
                            } else {
                                coroutineScope.launch {
                                    server.publish(event)
                                    hudLog(hud, "сохранено с использованием корутин")
                                }
                            }
                        }
                    }
                }
                Row {
                    Button("Target Attack") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                Shared.damage?.onEvent(DamageDealt(hud.activePlayerId.value, npc.npcId, 10))
                                Shared.server?.tryPublish(AttackPressed(hud.activePlayerId.value, npc.npcId))
                                hudLog(hud, "АТАКА")
                            }
                    }

                    Button("Buf Attack") {
                        modifier
                            .margin(end = 8.dp)
                            .onClick {
                                Shared.server?.tryPublish(AttackSpeedBuffApplied(hud.activePlayerId.value, 5))
                                hudLog(hud, "Скорость атаки +5")
                            }
                    }

                    Button("Сохранить") {
                        modifier
                            .margin(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            .onClick {
                                Shared.server?.tryPublish(SaveRequested(hud.activePlayerId.value))
                                hudLog(hud, "Сохранение данных ${hud.activePlayerId.value}")
                            }
                    }
                }

                Text("КВЕСТ") { modifier.margin(bottom = 4.dp) }

                when (hud.questState.use()) {
                    "START" -> {
                        Button("Говорить с алхимиком") {
                            modifier
                                .margin(vertical = 4.dp)
                                .onClick {
                                    Shared.server?.tryPublish(TalkedToNpc(hud.activePlayerId.value, "alchemist"))
                                }
                        }
                    }
                    "OFFERED" -> {
                        Row {
                            Button("Помочь") {
                                modifier
                                    .margin(end = 4.dp)
                                    .onClick {
                                        Shared.server?.tryPublish(ChoiceSelected(hud.activePlayerId.value, "alchemist", "help"))
                                    }
                            }
                            Button("Драться") {
                                modifier
                                    .margin(start = 4.dp)
                                    .onClick {
                                        Shared.server?.tryPublish(ChoiceSelected(hud.activePlayerId.value, "alchemist", "evil"))
                                    }
                            }
                        }
                    }
                    "GOOD_END" -> {
                    }
                    "EVIL_END" -> {
                    }
                }

                Box {
                    modifier
                        .height(1.dp)
                        .margin(vertical = 8.dp)
                }

                Text("ЛОГИ (последние 20)") { modifier.margin(bottom = 4.dp) }

                Box {
                    modifier
                        .height(400.dp)
                        .padding(8.dp)

                    Column {
                        val lines = hud.log.use()
                        if (lines.isEmpty()) {
                            Text("Нет событий...") { modifier }
                        } else {
                            for (line in lines) {
                                Text(line) {
                                    modifier
                                        .margin(bottom = 2.dp)
                                }
                            }
                        }
                    }
                }

                if (hud.attackCooldownMsLeft.use() > 0) {
                    Text("Кулдаун активен: ${hud.attackCooldownMsLeft.use()}ms") {
                        modifier
                            .margin(top = 8.dp)
                    }
                }

                if (hud.attackSpeedBuffTicksLeft.use() > 0) {
                    Text("Бафф скорости активен: ${hud.attackSpeedBuffTicksLeft.use()} сек (кулдаун 700ms)") {
                        modifier
                            .margin(top = 4.dp)
                    }
                }
            }
        }
    }
}

// 1. b) копатель 360
// 2. takeLast(n) — это функция, которая возвращает последние n элементов коллекции.
// 3. suspend  - это функция, которая может приостанавливать свое выполнение без блокировки потока.
// 4. Лямбда - это анонимная функция, которую можно передавать как аргумент, сохранять в переменную и использовать как обычное значение.
































