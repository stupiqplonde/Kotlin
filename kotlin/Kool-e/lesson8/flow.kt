package lesson8

// старая система событий EventBus на списках listeners - нормально для стартовых примеров, но в будущем вызовет проблемы
// например чем больше наша программа тем сложнее работать с подписками отменами конкуренцией кто и когда слушает
// для аналога есть flow - готовая стандартная система событий из kotlin coroutines

// прежняя система сохранений "key=value" - быстро и удобно для демо, но в реальной игре - ад
// много полей -> много ошибок -> трудные миграции
// kotlinx.serialization позволяет сохранять объекты почти одной строкой в формат JSON (и обратно)
// Json.encodeToString() \ decodeFromString()

// Flow
// пример:
// есть радиостанция - она пускает события, а слушатели подписываются и получают информацию о событиях
// во Flow есть два главных варианта
// 1. ShareFlow - наше радио событий
// как поток радиостанции, трансляции и тд - он существует даже когда никто не слушает и раздает события всем подписчикам
// аналогия с GameEvent (ударил, сказал, квест обновился)
// 2. StateFlow - табло состояний
// Это поток, который хранит одно текущее состояние и раздает всем подписчикам последнее известное состояние
// идеально для ServerState, PlayerState, QuestJournal ...]

// сохранение через сериализацию
// будем сохранять не строки вручную, а объект целиком
// PlayerData(hp, gold, ...) - это надежнее и легко масштабируемо (добавил поле и оно сразу попало в JSON)
// @Serializable - аннотация (памяткой) "этот класс который мы пометили можно сохранить или загрузить"

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
import de.fabmax.kool.modules.ui2.UiModifier.*
import de.fabmax.kool.physics.geometry.PlaneGeometry
import kotlinx.coroutines.flow.Flow

// Flow корутины
import kotlinx.coroutines.launch                    // запуск корутин
import kotlinx.coroutines.flow.MutableSharedFlow    // радиостанция событий
import kotlinx.coroutines.flow.SharedFlow           // чтение для подписчиков
import kotlinx.coroutines.flow.MutableStateFlow     // табло состояний
import kotlinx.coroutines.flow.StateFlow            // только для чтения
import kotlinx.coroutines.flow.asSharedFlow         // отдать наружу только SharedFlow
import kotlinx.coroutines.flow.asStateFlow          // отдать только StateFlow
import kotlinx.coroutines.flow.collect              // слушать поток

// импорты Serialization
import kotlinx.serialization.Serializable           // аннотация, что можно сохранять
import kotlinx.serialization.encodeToString         // запись с файла
import kotlinx.serialization.decodeFromString       // чтение с файла
import kotlinx.serialization.json.Json              // формат файла Json
import lesson7.CmdLoadDataToServer
import lesson7.CmdLoadPlayer
import lesson7.QuestState

import java.io.File                                 // для работы с файлами
import kotlin.io.readText

// события игры создаем как раньше, но отправляем через flow

sealed interface GameEvent{
    val playerId: String
}

data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int
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

// серверные данные игрока - то, что мы хотим сохранить

@Serializable
data class PlayerSave(
    val playerId: String,
    val hp: Int,
    val gold: Int,
    val questStates: Map<String, String> // карта состояний questId -> newState
)

class ServerWorld(
    initialPlayerId: String
){
    // MutableSharedFlow - мы будем класть и приостанавливать выполнение события, пока подписчик не освободится
    // replay = 0 - это означает "не пересылать старые события новым подписчикам"
    private val _event = MutableSharedFlow<GameEvent>(0)

    val events: SharedFlow<GameEvent> = _event.asSharedFlow()
    // сохраняем только в режиме для чтения (изменить нельзя)

    private val _playerState = MutableStateFlow(
        PlayerSave(
            initialPlayerId,
            100,
            0,
            mapOf("q_training" to "START")
        )
    )

    val playerState: StateFlow<PlayerSave> = _playerState.asStateFlow()

    // команды сервера
    fun dealDamage(playerId: String, targetId: String, amount: Int){
        val old = _playerState.value

        val newHp = (old.hp - amount).coerceAtLeast(0)

        _playerState.value = old.copy(playerId, newHp)
    }

    fun setQuestState(playerId: String, questId: String, newState: String){
        val old = _playerState.value

        val newQuestState = old.questStates + (questId to newState)

        _playerState.value = old.copy(questStates = newQuestState)
    }

    suspend fun emitEvent(event: GameEvent){
        _event.emit(event)
        // emit - будет рассылать всем подписчикам
        // emit может разослать событие не сразу, если подписчики медленные (очередь потоков)
        // Готовим событие заранее и рассылаем его в корутине
    }

    fun applyLoaded(playerSave: PlayerSave){
        _playerState.value = playerSave
        PlayerProgressSaved(_playerState.value.playerId,"Loaded from JSON")
    }
}

// сериализация - сохранение данных в файл
class SaveSystem {
    // настройка формата сериализации
    // prettyprint - делает json красивым и читаемым структурно
    // encodedefaults - значение по умолчанию тоже будет записываться в файл
    private val json = Json{
        prettyPrint = true
        encodeDefaults = true
    }



    private fun saveFile(playerId: String): File{
        val dir = File("saves")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$playerId.json")
    }

    fun save(playerSave: PlayerSave){
        val text = json.encodeToString(playerSave)

        saveFile(playerSave.playerId).writeText(text)
    }

    fun load(playerId: String): PlayerSave?{
        val file = saveFile(playerId)
        if (!file.exists()) return null

        val text = file.readText()
        val server = ServerWorld(playerId)
        val playerSave = json.decodeFromString<PlayerSave>(text)

        try {
            server.applyLoaded(playerSave)
            return playerSave
        }catch (e: Exception){
            return null
        }
    }
}

class UiState{
    // состояния внутри него будут обновляться от серверных данных

    val activePlayerId = mutableStateOf("Oleg")
    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)

    val questState = mutableStateOf("START")

    val log = mutableStateOf<List<String>>(emptyList())
}

fun pushLog(ui: UiState, text: String){
    ui.log.value = (ui.log.value + text).takeLast(20)
}


fun main() = KoolApplication{
    val ui = UiState()

    val server = ServerWorld(initialPlayerId = ui.activePlayerId.value)
    val saver = SaveSystem()

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

        // подписки на flow надо запускатьт в корутинах
        // в kool у сцены есть coroutineScope тут и запускаем

        // Подписка 1: слушаем события server.events
        coroutineScope.launch {
            server.events.collect { event ->
                // collect - слушать поток (каждое событие будет попадать в данный слушатель)
                when (event) {
                    is DamageDealt -> pushLog(ui, "${event.playerId} нанес ${event.amount} урона ${event.targetId}")
                    is QuestStateChanged -> pushLog(ui, "${event.playerId} перешел на этап: ${event.newState} квеста ${event.questId}")
                    is PlayerProgressSaved -> pushLog(ui, "сохранен прогресс ${event.playerId} по причине ${event.reason}"   )
                }
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

            Row {
                modifier.margin(top = 6.dp)

                Button("загрузить сохранение") {
                    modifier.onClick{
                        saver.save(server.playerState.value)
                    }
                }

                Button("выгрузить сохранение") {
                    modifier.onClick{
                        saver.load(server.playerState.value.playerId)
                    }
                }
            }
        }
    }
}









































