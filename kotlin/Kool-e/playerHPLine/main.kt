package GodDamn

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
import de.fabmax.kool.physics.joints.DistanceJoint
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
import kotlinx.serialization.modules.SerializersModule
import javax.accessibility.AccessibleValue
import javax.management.ValueExp
import kotlin.collections.firstOrNull
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.math.sqrt

enum class QuestState{
    START,
    WAIT_HERB,
    GOOD_END,
    EVIL_END
}

data class NpcMemory(
    val hasMet: Boolean,
    val timesTalked: Int,
    val receivedHerb: Boolean,
    val sawPlayerNearSource: Boolean = false
)

data class PlayerState(
    val playerId: String,
    val playerHP: Int,
    val gridX: Int,
    val gridZ: Int,
    val questState: QuestState,
    val inventory: Map<String, Int>,
    val currentAreaId: String?,
    val hintText: String,
    val gold: Int,
    val isDead: Boolean
)

fun initialPlayerState(playerId: String): PlayerState {
    return if(playerId == "Stas"){
        PlayerState(
            "Stas",
            100,
            0,
            0,
            QuestState.START,
            emptyMap(),
            null,
            "Подойди к одной из локаций",
            3,
            false
        )
    }else{
        PlayerState(
            "Oleg",
            100,
            0,
            0,
            QuestState.START,
            emptyMap(),
            null,
            "Подойди к одной из локаций",
            3,
            false
        )
    }
}

sealed interface GameCommand{
    val playerId: String
}

data class CmdTakeDamage(
    override val playerId: String,

): GameCommand

sealed interface GameEvent{
    val playerId: String
}

data class TakeDamage(
    override val playerId: String,
    val playerHP: Int,
): GameEvent

data class ServerMessage(
    override val playerId: String,
    val text: String
): GameEvent

class GameServer {
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _commands = MutableSharedFlow<GameCommand>(extraBufferCapacity = 64)
    val commands: SharedFlow<GameCommand> = _commands.asSharedFlow()

    fun trySend(cmd: GameCommand): Boolean = _commands.tryEmit(cmd)

    private val _players = MutableStateFlow(
        mapOf(
            "Oleg" to initialPlayerState("Oleg"),
            "Stas" to initialPlayerState("Stas")
        )
    )

    val players: StateFlow<Map<String, PlayerState>> = _players.asStateFlow()

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            commands.collect { cmd ->
                processCommand(cmd)
            }
        }
    }

    fun setPlayerData(playerId: String, data: PlayerState) {
        val map = _players.value.toMutableMap()
        map[playerId] = data
        _players.value = map.toMap()
    }


    fun getPlayerData(playerId: String): PlayerState {
        return _players.value[playerId] ?: initialPlayerState(playerId)
    }

    private suspend fun processCommand(cmd: GameCommand){
        when(cmd){
            is CmdTakeDamage -> {
                val player = getPlayerData(cmd.playerId)
                val damage = 15
                val newHp = (player.playerHP - damage).coerceAtLeast(0)
                val updated = player.copy(playerHP = newHp)

                setPlayerData(cmd.playerId, updated)
                _events.emit(TakeDamage(cmd.playerId, newHp))
            }
        }
    }
}

class HudState{
    val activePlayerIdFlow = MutableStateFlow("Oleg")

    val activePlayerIdUi = mutableStateOf("Oleg")

    val playerSnapShot = mutableStateOf(initialPlayerState("Oleg"))

    val log = mutableStateOf<List<String>>(emptyList())
}

fun hudLog(hud: HudState, line: String){
    hud.log.value = (hud.log.value + line).takeLast(20)
}

fun eventToText(e: GameEvent): String{
    return when(e){
        is TakeDamage -> "Игрок ${e.playerId} получил 15 урона, осталось ${e.playerHP}"
        is ServerMessage -> "Server: ${e.text}"
        else -> ""
    }
}

fun main() = KoolApplication {
    val hud = HudState()
    val server = GameServer()
    val maxHp = 100

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
            setColor(Color.WHITE, 10f)
        }

        server.start(coroutineScope)
    }

    addScene {
        setupUiScene(ClearColorLoad)

        hud.activePlayerIdFlow
            .flatMapLatest { pid ->
                server.players.map { players -> players[pid] ?: initialPlayerState(pid) }
            }
            .onEach { player ->
                hud.playerSnapShot.value = player
                hud.activePlayerIdUi.value = player.playerId
            }
            .launchIn(coroutineScope)

        hud.activePlayerIdFlow
            .flatMapLatest { pid ->
                server.events.filter { it.playerId == pid }
            }
            .map { e -> "[${e.playerId}] ${e::class.simpleName}" }
            .onEach { line -> hudLog(hud, line) }
            .launchIn(coroutineScope)

        addPanelSurface {
            val player = hud.playerSnapShot.use()

            modifier
                .align(AlignmentX.End, AlignmentY.Bottom)
                .margin(16.dp)
                .size(270.dp, 80.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                val hpFrac = (player.playerHP.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
                val barW = 240.dp
                val barH = 18.dp
                val fillW = (barW.value * hpFrac).dp

                Text("HP ${player.playerHP} / $maxHp") {}
                modifier.margin(bottom = 6.dp)

                Box {
                    modifier
                        .size(barW, barH)
                        .background(RoundRectBackground(Color(0.15f, 0.15f, 0.15f, 0.95f), 9.dp))
                        .padding(2.dp)

                    Box {
                        val fillColor = when {
                            hpFrac > 0.6f -> Color(0.15f, 0.85f, 0.25f, 0.95f)
                            hpFrac > 0.3f -> Color(0.95f, 0.75f, 0.20f, 0.95f)
                            else -> Color(0.95f, 0.20f, 0.20f, 0.95f)
                        }
                        modifier
                            .size(fillW, barH - 4.dp)
                            .background(RoundRectBackground(fillColor, 7.dp))
                    }
                }
            }
            if (player.isDead) {
                Box {
                    modifier
                        .align(AlignmentX.Center, AlignmentY.Center)
                        .size(500.dp, 200.dp)

                    Text("YOU DIED"){}
                }
            }
        }

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(16.dp)
                .size(200.dp, 100.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                val player = hud.playerSnapShot.use()

                Text("Player: ${hud.activePlayerIdUi.use()}") {}
                modifier.margin(bottom = sizes.gap)

                Text("Player HP: ${player.playerHP}") {}
                modifier.margin(bottom = sizes.gap)


                Button("Take Damage") {
                    modifier.margin(end = 8.dp).onClick {
                        server.trySend(CmdTakeDamage(player.playerId))
                    }
                }
            }
        }
    }
}