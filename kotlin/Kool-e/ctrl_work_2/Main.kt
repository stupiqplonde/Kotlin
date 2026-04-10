package ctrl_work_2

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

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

import kotlin.math.sqrt

enum class QuestState {
    START,
    WAIT_HERB,
    GOOD_END,
    EVIL_END
}

enum class Facing {
    LEFT,
    RIGHT,
    FORWARD,
    BACK
}

enum class WorldObjectType {
    ALCHEMIST,
    HERB_SOURCE,
    CHEST
}

data class GridPos(val x: Int, val z: Int)

data class WorldObjectDef(
    val id: String,
    val type: WorldObjectType,
    val cellX: Float,
    val cellZ: Float,
    val interactRadius: Float = 1.7f,
    var remainingUses: Int = 3
)

data class NpcMemory(
    val hasMet: Boolean,
    val timesTalked: Int,
    val receivedHerb: Boolean,
    val sawPlayerNearSource: Boolean = false
)

data class PlayerState(
    val playerId: String,
    val gridX: Int,
    val gridZ: Int,
    val questState: QuestState,
    val inventory: Map<String, Int>,
    val alchemistMemory: NpcMemory,
    val currentAreaId: String?,
    val hintText: String,
    val gold: Int,
    val facing: Facing
)


fun herbCount(player: PlayerState): Int = player.inventory["herb"] ?: 0

fun facingToYawDeg(facing: Facing): Float = when (facing) {
    Facing.FORWARD -> 0f
    Facing.RIGHT   -> 90f
    Facing.BACK    -> 180f
    Facing.LEFT    -> 270f
}

fun lerp(current: Float, target: Float, t: Float): Float = current + (target - current) * t

fun distance2D(ax: Float, az: Float, bx: Float, bz: Float): Float {
    val dx = ax - bx
    val dz = az - bz
    return sqrt(dx * dx + dz * dz)
}

fun initialPlayerState(playerId: String): PlayerState {
    return if (playerId == "Stas") {
        PlayerState(
            "Stas", 0, 0, QuestState.START, emptyMap(),
            NpcMemory(hasMet = true, timesTalked = 2, receivedHerb = false),
            null, "Подойди к одной из локаций", 3, Facing.FORWARD
        )
    } else {
        PlayerState(
            "Oleg", 0, 0, QuestState.START, emptyMap(),
            NpcMemory(hasMet = false, timesTalked = 0, receivedHerb = false),
            null, "Подойди к одной из локаций", 3, Facing.FORWARD
        )
    }
}


data class DialogueOption(
    val id: String,
    val text: String
)

data class DialogueView(
    val npcId: String,
    val text: String,
    val option: List<DialogueOption>
)

fun buildAlchemistDialogue(player: PlayerState): DialogueView {
    val herbs = herbCount(player)
    val memory = player.alchemistMemory

    return when (player.questState) {
        QuestState.START -> {
            val greeting = if (!memory.hasMet) "О привет" else "снова ты... я тебя знаю, ты ${player.playerId}"
            DialogueView(
                "Алхимик",
                "$greeting \n Хочешь помочь - принеси травку",
                listOf(
                    DialogueOption("accept_help", "Я принесу траву"),
                    DialogueOption("threat", "травы не будет, гони товар")
                )
            )
        }
        QuestState.WAIT_HERB -> {
            if (herbs < 3) {
                DialogueView("Алхимик", "Недостаточно, надо $herbs/3 травы", emptyList())
            } else {
                DialogueView(
                    "Алхимик",
                    "найс, прет как белый, давай сюда",
                    listOf(DialogueOption("give_herb", "Отдать 3 травы"))
                )
            }
        }
        QuestState.GOOD_END -> {
            val text = if (memory.receivedHerb)
                "Спасибо за помощь! Надеюсь, ты нашел сундук с наградой."
            else
                "Ты завершил квест, но npc все забыл..."
            DialogueView("Алхимик", text, emptyList())
        }
        QuestState.EVIL_END -> {
            DialogueView("Алхимик", "ты проиграл бетмен", emptyList())
        }
    }
}

sealed interface GameCommand { val playerId: String }
data class CmdStepMove(override val playerId: String, val stepX: Int, val stepZ: Int) : GameCommand
data class CmdDashForward(override val playerId: String) : GameCommand
data class CmdInteract(override val playerId: String) : GameCommand
data class CmdChooseDialogueOption(override val playerId: String, val optionId: String) : GameCommand
data class CmdResetPlayer(override val playerId: String) : GameCommand

sealed interface GameEvent { val playerId: String }
data class PlayerMoved(override val playerId: String, val newGridX: Int, val newGridZ: Int) : GameEvent
data class MovedBlocked(override val playerId: String, val blockedX: Int, val blockedZ: Int) : GameEvent
data class EnteredArea(override val playerId: String, val areaId: String) : GameEvent
data class LeftArea(override val playerId: String, val areaId: String) : GameEvent
data class InteractedWithNpc(override val playerId: String, val npcId: String) : GameEvent
data class InteractedWithHerbSource(override val playerId: String, val sourceId: String) : GameEvent
data class InteractedWithChest(override val playerId: String, val sourceId: String) : GameEvent
data class GoldCountChanged(override val playerId: String, val countGold: Int) : GameEvent
data class InventoryChanged(override val playerId: String, val itemId: String, val newCount: Int) : GameEvent
data class QuestStateChanged(override val playerId: String, val newState: QuestState) : GameEvent
data class NpcMemoryChanged(override val playerId: String, val memory: NpcMemory) : GameEvent
data class ServerMessage(override val playerId: String, val text: String) : GameEvent


class GameServer {
    private val minX = -5
    private val maxX = 5
    private val minZ = -4
    private val maxZ = 4

    private val blockedCells = setOf(
        GridPos(-1, 1), GridPos(0, 1), GridPos(1, 1), GridPos(1, 0), GridPos(-2, 0)
    )

    val worldObjects = mutableListOf(
        WorldObjectDef("alchemist", WorldObjectType.ALCHEMIST, -3f, 0f, 1.7f),
        WorldObjectDef("herb_source", WorldObjectType.HERB_SOURCE, 3f, 0f, 1.7f, remainingUses = 3),
        WorldObjectDef("treasure_box", WorldObjectType.CHEST, 7f, 0f, 1.7f)
    )

    private var chestOpened = false  // альтернатива флагу видимости сундука

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
        scope.launch { commands.collect { processCommand(it) } }
    }

    fun getPlayerData(playerId: String): PlayerState =
        _players.value[playerId] ?: initialPlayerState(playerId)

    private fun updatePlayer(playerId: String, change: (PlayerState) -> PlayerState) {
        val oldMap = _players.value
        val oldPlayer = oldMap[playerId] ?: return
        val newPlayer = change(oldPlayer)
        _players.value = oldMap.toMutableMap().apply { this[playerId] = newPlayer }.toMap()
    }

    private fun isCellInsideMap(x: Int, z: Int): Boolean = x in minX..maxX && z in minZ..maxZ
    private fun isCellBlocked(x: Int, z: Int): Boolean = GridPos(x, z) in blockedCells

    private fun nearestObject(player: PlayerState): WorldObjectDef? {
        val px = player.gridX.toFloat()
        val pz = player.gridZ.toFloat()
        return worldObjects.filter { obj ->
            distance2D(px, pz, obj.cellX, obj.cellZ) <= obj.interactRadius
        }.minByOrNull { obj -> distance2D(px, pz, obj.cellX, obj.cellZ) }
    }

    private suspend fun refreshPlayerArea(playerId: String) {
        val player = getPlayerData(playerId)
        val nearest = nearestObject(player)
        val oldAreaId = player.currentAreaId
        val newAreaId = nearest?.id

        if (oldAreaId != null) _events.emit(LeftArea(playerId, oldAreaId))
        if (newAreaId != null) _events.emit(EnteredArea(playerId, newAreaId))

        val newHint = when (newAreaId) {
            "alchemist"    -> "Подойди и нажми на алхимика"
            "herb_source"  -> "собери траву"
            "treasure_box" -> "Открой сундук и получи награду!"
            else           -> "Подойди к одной из локаций"
        }
        updatePlayer(playerId) { p ->
            p.copy(hintText = newHint, currentAreaId = newAreaId)
        }
    }

    private suspend fun attemptMove(
        playerId: String,
        deltaX: Int,
        deltaZ: Int,
        shouldSetFacing: Boolean = true
    ): Boolean {
        val player = getPlayerData(playerId)
        val targetX = player.gridX + deltaX
        val targetZ = player.gridZ + deltaZ

        val newFacing = if (shouldSetFacing) {
            when {
                deltaX < 0 -> Facing.LEFT
                deltaX > 0 -> Facing.RIGHT
                deltaZ < 0 -> Facing.FORWARD
                else       -> Facing.BACK
            }
        } else {
            player.facing
        }

        if (!isCellInsideMap(targetX, targetZ)) {
            _events.emit(ServerMessage(playerId, "Нельзя уйти за границы карты"))
            _events.emit(MovedBlocked(playerId, targetX, targetZ))
            if (shouldSetFacing) updatePlayer(playerId) { it.copy(facing = newFacing) }
            return false
        }

        if (isCellBlocked(targetX, targetZ)) {
            _events.emit(ServerMessage(playerId, "Путь заблокирован стеной"))
            _events.emit(MovedBlocked(playerId, targetX, targetZ))
            if (shouldSetFacing) updatePlayer(playerId) { it.copy(facing = newFacing) }
            return false
        }

        updatePlayer(playerId) { p ->
            p.copy(
                gridX = targetX,
                gridZ = targetZ,
                facing = if (shouldSetFacing) newFacing else p.facing
            )
        }
        _events.emit(PlayerMoved(playerId, targetX, targetZ))
        return true
    }

    private suspend fun processCommand(cmd: GameCommand) {
        when (cmd) {
            is CmdStepMove -> {
                if (attemptMove(cmd.playerId, cmd.stepX, cmd.stepZ, true)) {
                    refreshPlayerArea(cmd.playerId)
                }
            }

            is CmdDashForward -> {
                val player = getPlayerData(cmd.playerId)
                val (deltaX, deltaZ) = when (player.facing) {
                    Facing.FORWARD -> 0 to -2
                    Facing.BACK    -> 0 to 2
                    Facing.LEFT    -> -2 to 0
                    Facing.RIGHT   -> 2 to 0
                }
                val step1X = player.gridX + deltaX / 2
                val step1Z = player.gridZ + deltaZ / 2
                val step2X = player.gridX + deltaX
                val step2Z = player.gridZ + deltaZ

                if (!isCellInsideMap(step1X, step1Z) || isCellBlocked(step1X, step1Z) ||
                    !isCellInsideMap(step2X, step2Z) || isCellBlocked(step2X, step2Z)
                ) {
                    _events.emit(ServerMessage(cmd.playerId, "Рывок невозможен - препятствие на пути"))
                    return
                }

                if (attemptMove(cmd.playerId, deltaX, deltaZ, false)) {
                    _events.emit(ServerMessage(cmd.playerId, "Рывок вперед! +2 клетки"))
                    refreshPlayerArea(cmd.playerId)
                }
            }

            is CmdInteract -> {
                val player = getPlayerData(cmd.playerId)
                val obj = nearestObject(player) ?: run {
                    _events.emit(ServerMessage(cmd.playerId, "Рядом нет объектов для взаимодействия"))
                    return
                }

                when (obj.type) {
                    WorldObjectType.ALCHEMIST -> {
                        if (player.alchemistMemory.sawPlayerNearSource) {
                            _events.emit(ServerMessage(cmd.playerId, "Так... ты тут был... ааа трава-то, где?"))
                            return
                        }
                        val newMemory = player.alchemistMemory.copy(
                            hasMet = true,
                            timesTalked = player.alchemistMemory.timesTalked + 1
                        )
                        updatePlayer(cmd.playerId) { it.copy(alchemistMemory = newMemory) }
                        _events.emit(InteractedWithNpc(cmd.playerId, obj.id))
                        _events.emit(NpcMemoryChanged(cmd.playerId, newMemory))
                    }

                    WorldObjectType.HERB_SOURCE -> {
                        val herbSource = worldObjects.find { it.id == obj.id }
                        // проверка: квест должен быть в состоянии WAIT_HERB, и источник не должен быть исчерпан
                        if (player.questState != QuestState.WAIT_HERB) {
                            _events.emit(ServerMessage(cmd.playerId, "Трава сейчас не нужна, сначала возьми квест у алхимика"))
                            return
                        }
                        if (herbSource == null || herbSource.remainingUses <= 0) {
                            _events.emit(ServerMessage(cmd.playerId, "Источник травы иссяк..."))
                            return
                        }

                        val newMemory = player.alchemistMemory.copy(sawPlayerNearSource = true)
                        updatePlayer(cmd.playerId) { it.copy(alchemistMemory = newMemory) }

                        val oldCount = herbCount(player)
                        val newCount = oldCount + 1
                        val newInventory = player.inventory + ("herb" to newCount)
                        updatePlayer(cmd.playerId) { it.copy(inventory = newInventory) }

                        herbSource.remainingUses--
                        _events.emit(ServerMessage(cmd.playerId, "Трава собрана! Осталось использований: ${herbSource.remainingUses}"))

                        _events.emit(InteractedWithHerbSource(cmd.playerId, obj.id))
                        _events.emit(InventoryChanged(cmd.playerId, "herb", newCount))
                    }

                    WorldObjectType.CHEST -> {
                        if (chestOpened) {
                            _events.emit(ServerMessage(cmd.playerId, "Сундук уже открыт и пуст."))
                            return
                        }
                        if (player.questState != QuestState.GOOD_END) {
                            _events.emit(ServerMessage(cmd.playerId, "Сундук заперт. Нужно сначала помочь алхимику."))
                            return
                        }
                        val newGold = player.gold + 10
                        updatePlayer(cmd.playerId) { it.copy(gold = newGold) }
                        chestOpened = true
                        _events.emit(InteractedWithChest(cmd.playerId, obj.id))
                        _events.emit(GoldCountChanged(cmd.playerId, newGold))
                        _events.emit(ServerMessage(cmd.playerId, "Ты открыл сундук и нашел 10 золотых монет! Сундук опустел."))
                    }
                }
            }

            is CmdChooseDialogueOption -> {
                val player = getPlayerData(cmd.playerId)
                if (player.currentAreaId != "alchemist") {
                    _events.emit(ServerMessage(cmd.playerId, "Сначала подойди к алхимику"))
                    return
                }

                when (cmd.optionId) {
                    "accept_help" -> {
                        val dist = distance2D(player.gridX.toFloat(), player.gridZ.toFloat(), -3f, 0f)
                        if (dist <= 1.7f) {
                            if (player.questState != QuestState.START) {
                                _events.emit(ServerMessage(cmd.playerId, "Путь помощи можно выбрать только в начале квеста"))
                                return
                            }
                            updatePlayer(cmd.playerId) { it.copy(questState = QuestState.WAIT_HERB) }
                            _events.emit(QuestStateChanged(cmd.playerId, QuestState.WAIT_HERB))
                            _events.emit(ServerMessage(cmd.playerId, "Алхимик просит собрать 3 травы. Ищи их у источника на востоке."))
                        } else {
                            _events.emit(ServerMessage(cmd.playerId, "Ты отошел слишком далеко от Алхимика"))
                        }
                    }
                    "give_herb" -> {
                        if (player.questState != QuestState.WAIT_HERB) {
                            _events.emit(ServerMessage(cmd.playerId, "Сейчас нельзя сдать траву"))
                            return
                        }
                        val herbs = herbCount(player)
                        if (herbs < 3) {
                            _events.emit(ServerMessage(cmd.playerId, "Недостаточно травы. Нужно 3."))
                            return
                        }
                        val newCount = herbs - 3
                        val newInventory = if (newCount <= 0) player.inventory - "herb"
                        else player.inventory + ("herb" to newCount)
                        val newMemory = player.alchemistMemory.copy(receivedHerb = true)
                        updatePlayer(cmd.playerId) { p ->
                            p.copy(
                                inventory = newInventory,
                                gold = p.gold + 5,
                                questState = QuestState.GOOD_END,
                                alchemistMemory = newMemory
                            )
                        }
                        _events.emit(InventoryChanged(cmd.playerId, "herb", newCount))
                        _events.emit(NpcMemoryChanged(cmd.playerId, newMemory))
                        _events.emit(QuestStateChanged(cmd.playerId, QuestState.GOOD_END))
                        _events.emit(ServerMessage(cmd.playerId, "Алхимик получил траву и выдал тебе золото! Теперь найди сундук с наградой (7,0)."))
                    }
                    else -> {
                        _events.emit(ServerMessage(cmd.playerId, "Неизвестный формат диалога"))
                    }
                }
            }

            is CmdResetPlayer -> {
                // сброс состояния мира
                worldObjects.find { it.id == "herb_source" }?.remainingUses = 3
                chestOpened = false
                updatePlayer(cmd.playerId) { initialPlayerState(cmd.playerId) }
                _events.emit(ServerMessage(cmd.playerId, "Игрок и мир сброшены к начальному состоянию"))
                refreshPlayerArea(cmd.playerId)
            }
        }
    }
}


class HudState {
    val activePlayerIdFlow = MutableStateFlow("Oleg")
    val activePlayerIdUi = mutableStateOf("Oleg")
    val playerSnapShot = mutableStateOf(initialPlayerState("Oleg"))
    val log = mutableStateOf<List<String>>(emptyList())

    var rotateCubeLeft: (() -> Unit)? = null
    var rotateCubeRight: (() -> Unit)? = null
    var addCubeOnTop: (() -> Unit)? = null
}

fun hudLog(hud: HudState, line: String) {
    hud.log.value = (hud.log.value + line).takeLast(20)
}

fun formatInventory(player: PlayerState): String {
    return if (player.inventory.isEmpty()) "Inventory: пусто"
    else "Inventory: " + player.inventory.entries.joinToString { "${it.key} x${it.value}" }
}

fun currentObjective(player: PlayerState): String {
    val herbs = herbCount(player)
    return when (player.questState) {
        QuestState.START      -> "Подойди к алхимику и начни разговор"
        QuestState.WAIT_HERB  -> if (herbs < 3) "Собери 3 травы. Сейчас $herbs / 3"
        else "Вернись к алхимику и отдай 3 травы"
        QuestState.GOOD_END   -> "Квест завершен по хорошей ветке"
        QuestState.EVIL_END   -> "Квест завершен по плохой ветке"
    }
}

fun formatMemory(memory: NpcMemory): String =
    "Встретился: ${memory.hasMet}, сколько раз поговорил: ${memory.timesTalked}, отдал траву: ${memory.receivedHerb}"

fun eventToText(e: GameEvent): String = when (e) {
    is PlayerMoved            -> "PlayerMoved (${e.newGridX}, ${e.newGridZ})"
    is MovedBlocked           -> "MovedBlocked (${e.blockedX}, ${e.blockedZ})"
    is EnteredArea            -> "EnteredArea ${e.areaId}"
    is LeftArea               -> "LeftArea ${e.areaId}"
    is InteractedWithNpc      -> "InteractedWithNpc ${e.npcId}"
    is InteractedWithHerbSource -> "InteractedWithHerbSource ${e.sourceId}"
    is InteractedWithChest    -> "InteractedWithChest ${e.sourceId}"
    is InventoryChanged       -> "InventoryChanged ${e.itemId} -> ${e.newCount}"
    is QuestStateChanged      -> "QuestStateChanged ${e.newState}"
    is NpcMemoryChanged       -> "NpcMemoryChanged Встретился: ${e.memory.hasMet}, разговоров: ${e.memory.timesTalked}, отдал траву: ${e.memory.receivedHerb}"
    is ServerMessage          -> "Server: ${e.text}"
    else                      -> ""
}


fun main() = KoolApplication {
    val hud = HudState()
    val server = GameServer()

    addScene {
        defaultOrbitCamera()

        // Пол
        for (x in -5..5) for (z in -4..4) {
            addColorMesh {
                generate { cube { colored() } }
                shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
            }.transform.translate(x.toFloat(), -1.2f, z.toFloat())
        }

        // Стены
        val wallCells = listOf(GridPos(-1,1), GridPos(0,1), GridPos(1,1), GridPos(1,0), GridPos(-2,0))
        for (cell in wallCells) {
            addColorMesh {
                generate { cube { colored() } }
                shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
            }.transform.translate(cell.x.toFloat(), -1.2f, cell.z.toFloat())
        }

        // Игрок
        val playerNode = addColorMesh {
            generate { cube { colored() } }
            shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
        }

        // Алхимик
        val alchemistNode = addColorMesh {
            generate { cube { colored() } }
            shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
        }.apply { transform.translate(-3f, 0f, 0f) }

        // Сундук (всегда виден, но открыть можно только после квеста)
        val treasureChestNode = addColorMesh {
            generate { cube { colored() } }
            shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
        }.apply { transform.translate(7f, 0f, 0f) }

        // Источник травы (всегда виден, но собирать можно только после взятия квеста)
        val herbNode = addColorMesh {
            generate { cube { colored() } }
            shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
        }.apply { transform.translate(3f, 0f, 0f) }

        // Освещение
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 8f)
        }

        server.start(coroutineScope)

        // Анимация игрока
        var renderX = 0f
        var renderZ = 0f
        var lastAppliedX = 0f
        var lastAppliedZ = 0f
        var lastAppliedYaw = 0f

        playerNode.onUpdate {
            val player = server.getPlayerData(hud.activePlayerIdFlow.value)
            val targetX = player.gridX.toFloat()
            val targetZ = player.gridZ.toFloat()
            val speed = Time.deltaT * 8f
            val t = if (speed > 1f) 1f else speed
            renderX = lerp(renderX, targetX, t)
            renderZ = lerp(renderZ, targetZ, t)

            val dx = renderX - lastAppliedX
            val dz = renderZ - lastAppliedZ
            playerNode.transform.translate(dx, 0f, dz)
            lastAppliedX = renderX
            lastAppliedZ = renderZ

            val targetYaw = facingToYawDeg(player.facing)
            val yawDelta = targetYaw - lastAppliedYaw
            playerNode.transform.rotate(yawDelta.deg, Vec3f.Y_AXIS)
            lastAppliedYaw = targetYaw
        }

        // Демо-объекты (вращаемый куб и стек)
        val rotatableCube = addColorMesh {
            generate { cube { colored() } }
            shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
        }.apply { transform.translate(0f, 0f, 3f) }

        val cubeStack = mutableListOf(rotatableCube)

        hud.rotateCubeLeft = { rotatableCube.transform.rotate((-10f).deg, Vec3f.Y_AXIS); hudLog(hud, "Поворот куба на -10°") }
        hud.rotateCubeRight = { rotatableCube.transform.rotate(10f.deg, Vec3f.Y_AXIS); hudLog(hud, "Поворот куба на +10°") }
        hud.addCubeOnTop = {
            val newCube = addColorMesh {
                generate { cube { colored() } }
                shader = KslPbrShader { color { vertexColor() }; metallic(0f); roughness(0.25f) }
            }
            val topY = cubeStack.size
            newCube.transform.translate(2f, topY.toFloat(), -2f)
            cubeStack.add(newCube)
            hudLog(hud, "Добавлен новый куб сверху (всего: ${cubeStack.size})")
        }
    }

    addScene {
        setupUiScene(ClearColorLoad)

        hud.activePlayerIdFlow
            .flatMapLatest { pid -> server.players.map { it[pid] ?: initialPlayerState(pid) } }
            .onEach { hud.playerSnapShot.value = it }
            .launchIn(coroutineScope)

        hud.activePlayerIdFlow
            .flatMapLatest { pid -> server.events.filter { it.playerId == pid } }
            .map { eventToText(it) }
            .onEach { hudLog(hud, "[${hud.activePlayerIdUi.value}] $it") }
            .launchIn(coroutineScope)

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(16.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                val player = hud.playerSnapShot.use()
                val dialogue = buildAlchemistDialogue(player)

                Text("Игрок: ${hud.activePlayerIdUi.use()}") { modifier.margin(bottom = sizes.gap) }
                Text("Позиция: x=${"%d".format(player.gridX)} z=${"%d".format(player.gridZ)}") {}
                Text("Смотрит: ${player.facing}") { modifier.margin(bottom = sizes.smallGap) }
                Text("Quest State: ${player.questState}") { modifier.font(sizes.smallText) }
                Text(currentObjective(player)) { modifier.font(sizes.smallText) }
                Text(formatInventory(player)) { modifier.font(sizes.smallText) }
                Text("Gold: ${player.gold}") { modifier.font(sizes.smallText) }
                Text("Hint: ${player.hintText}") { modifier.font(sizes.smallText) }
                Text("Npc Memory: ${formatMemory(player.alchemistMemory)}") {
                    modifier.font(sizes.smallText).margin(bottom = sizes.smallGap)
                }

                Row {
                    Button("Сменить игрока") {
                        modifier.margin(end = 8.dp).onClick {
                            val newId = if (hud.activePlayerIdUi.value == "Oleg") "Stas" else "Oleg"
                            hud.activePlayerIdUi.value = newId
                            hud.activePlayerIdFlow.value = newId
                        }
                    }
                    Button("Сбросить игрока") {
                        modifier.onClick { server.trySend(CmdResetPlayer(player.playerId)) }
                    }
                }

                Text("Движение по миру") { modifier.margin(top = sizes.gap) }
                Row {
                    Button("Лево") { modifier.margin(end = 8.dp).onClick { server.trySend(CmdStepMove(player.playerId, -1, 0)) } }
                    Button("Право") { modifier.margin(end = 8.dp).onClick { server.trySend(CmdStepMove(player.playerId, 1, 0)) } }
                    Button("Вперед") { modifier.margin(end = 8.dp).onClick { server.trySend(CmdStepMove(player.playerId, 0, -1)) } }
                    Button("Назад") { modifier.margin(end = 8.dp).onClick { server.trySend(CmdStepMove(player.playerId, 0, 1)) } }
                }

                Text("Специальные движения") { modifier.margin(top = sizes.gap) }
                Row {
                    Button("Рывок вперед (2 клетки)") {
                        modifier.margin(end = 8.dp).onClick { server.trySend(CmdDashForward(player.playerId)) }
                    }
                }

                Text("Взаимодействия") { modifier.margin(top = sizes.gap) }
                Row {
                    Button("Потрогать ближайшего") {
                        modifier.margin(end = 8.dp).onClick { server.trySend(CmdInteract(player.playerId)) }
                    }
                }

                Text("Управление объектом") { modifier.margin(top = sizes.gap) }
                Row {
                    Button("Вращать влево (-10°)") { modifier.margin(end = 8.dp).onClick { hud.rotateCubeLeft?.invoke() } }
                    Button("Вращать вправо (+10°)") { modifier.margin(end = 8.dp).onClick { hud.rotateCubeRight?.invoke() } }
                }
                Text("Добавление кубов") { modifier.margin(top = sizes.gap) }
                Row {
                    Button("Добавить куб сверху") { modifier.onClick { hud.addCubeOnTop?.invoke() } }
                }

                Text(dialogue.npcId) { modifier.margin(top = sizes.gap) }
                Text(dialogue.text) { modifier.margin(bottom = sizes.smallGap) }

                if (dialogue.option.isEmpty()) {
                    Text("Нет доступных вариантов ответа") { modifier.font(sizes.smallText).margin(bottom = sizes.gap) }
                } else {
                    Row {
                        for (option in dialogue.option) {
                            Button(option.text) {
                                modifier.margin(end = 8.dp).onClick {
                                    server.trySend(CmdChooseDialogueOption(player.playerId, option.id))
                                }
                            }
                        }
                    }
                }

                Text("Лог: ") { modifier.margin(top = sizes.gap) }
                for (line in hud.log.use()) {
                    Text(line) { modifier.font(sizes.smallText) }
                }
            }
        }
    }
}

// 1.1) b
// 1.2) c
// 1.3) a
// 1.4) a