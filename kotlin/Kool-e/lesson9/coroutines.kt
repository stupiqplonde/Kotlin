package lesson9

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.math.sqrDistancePointToRay
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.ShortArraySerializer
import lesson7.ClientUiState


// в любой игре есть много процессов упирающихся на время
// например: яд тикает 1 раз в сек
// кулдаун улара 1.5 сек
// задержка сети 300мс
// квест с событием открывает дверь через 5 сек
// и тд

// если все эти процессы делать через onUpdate и таймер в ручную это быстро превращается в кашу

// Корутины решают эту проблему
// 1. позволяют писать время как обычный код: подождал -> сделала действие -> подождал -> сделала действие
// 2. в процессе выполнения не замораживаем игру и UI
// 3. удобно отменяются (яд перезапускается если наложить новый, а старый отменяем)

// Корутина - легковесная задача, которая может выполняться параллельно другим задачам и основному потоку

// основные команды корутин:
// launch{ ... } - запускает корутину (включить поток)
// delay(ms) - заставляет корутину ждать ограниченное число времени, но не замораживает саму игру

// Job + cancel()
// Job - контроллер управления корутиной
// cancel() - остановить выполнение корутины (например снять эффект яда)

// функция delay не будет работать за пределами корутины launch
// тк delay это suspend-функция
// suspend fun - функция, которая может приостанавливаться (ждать) - обычные функции так не умеют
// suspend функцию можно вызвать только внутри запущенной корутины или внутри такой же suspend функции

// scene.coroutineScope - это свой корутинный скуп Kool внутри сцены
// когда сцена будет закрываться - корутины внутри этой сцены автоматом прекратятся
// это просто безопаснее чем глобальные корутины, про которые мы можем забыть и не сохранить

class GameState{
    val playerId = mutableStateOf("Oleg")
    val  hp = mutableStateOf(100)
    val maxHp = 100

    val poisonTicksLeft = mutableStateOf(0)
    val regenTicksLeft = mutableStateOf(0)

    val attackCoolDownMsLeft = mutableStateOf(0L)

    val logLines = mutableStateOf<List<String>>(emptyList())
}

fun pushLog(game: GameState, text: String){
    game.logLines.value = (game.logLines.value + text).takeLast(20)
}

// EffectManager - система для эффектов повремени

class EffectManager(
    private val game: GameState,
    private val scope: kotlinx.coroutines.CoroutineScope
    // scope - место где будут запускаться и жить корутины
    // передаем сюда scene.coroutineScope чтобы все было привязано к сцене
) {
    private var poisonJob: Job? = null
    // Job - это задача-корутина

    private var regenJob: Job? = null

    fun applyPoison(ticks: Int, damagePerTick: Int, intervalMs: Long) {
        poisonJob?.cancel()
        // если яд уже был применен фннулируем его
        // ?. - безопасный вызов, если poisonJob == null, то cancel не вызовется

        // обновляем состояние игрового числа тиков яда
        game.poisonTicksLeft.value += ticks

        poisonJob = scope.launch {
            while (isActive && game.poisonTicksLeft.value > 0) {
                delay(intervalMs)
                game.poisonTicksLeft.value -= 1
                game.hp.value = (game.hp.value - damagePerTick).coerceAtLeast(0)
                pushLog(game, "Тик яда: -$damagePerTick, HP: ${game.hp.value} / ${game.maxHp}")
            }
        }
    }

    fun applyRegen(ticks: Int, healPerTick: Int, intervalMs: Long) {
        regenJob?.cancel()

        game.regenTicksLeft.value += ticks
        pushLog(game, "Эффект регена применен на ${game.playerId} длительность ${intervalMs}")

        regenJob = scope.launch {
            while (isActive && game.regenTicksLeft.value > 0) {
                delay(intervalMs)

                game.regenTicksLeft.value -= 1
                game.hp.value = (game.hp.value + healPerTick).coerceAtMost(game.maxHp)
                pushLog(game, "Тик регена: +$healPerTick, HP: ${game.hp.value} / ${game.maxHp}")
            }
            pushLog(game, "Эффект регена завершен")
        }
    }

    fun cancelPoison() {
        poisonJob?.cancel()
        poisonJob = null
        game.poisonTicksLeft.value = 0
        pushLog(game, "Яд снят (cancel)")
    }

    fun cancelRegen() {
        regenJob?.cancel()
        regenJob = null
        game.regenTicksLeft.value = 0
        pushLog(game, "реген снят (cancel)")
    }
}

class CooldownManager(
    private val game: GameState,
    private val scope: kotlinx.coroutines.CoroutineScope
){
    private var cooldownJob: Job? = null

    fun startAttackCooldown(totalMs: Long){
        cooldownJob?.cancel()

        game.attackCoolDownMsLeft.value = totalMs
        pushLog(game,"Кулдаун атаки ${totalMs}Ms")

        cooldownJob = scope.launch {
            val step = 100L

            while (isActive && game.attackCoolDownMsLeft.value > 0L){
                delay(step)
                game.attackCoolDownMsLeft.value = (game.attackCoolDownMsLeft.value - step).coerceAtLeast(0)
            }
        }
    }

    fun canAttack(): Boolean{
        return game.attackCoolDownMsLeft.value <= 0L
    }
}

fun main() = KoolApplication{
    val game = GameState()
    println("Запуск приложения")

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
        val effects = EffectManager(game, coroutineScope)
        val cooldowns = CooldownManager(game, coroutineScope)

        SharedActions.effects = effects
        SharedActions.cooldown = cooldowns
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
                Text("HP: ${game.hp.use()}"){}
                Text("Тики яда: ${game.poisonTicksLeft.use()}"){
                    modifier.margin(bottom = sizes.gap)
                }
                Text("Тики регена: ${game.regenTicksLeft.use()}"){
                    modifier.margin(bottom = sizes.gap)
                }
                Text("Тики кулдауна: ${game.attackCoolDownMsLeft.use()}"){
                    modifier.margin(bottom = sizes.gap)
                }

                Row {
                    Button("Яд +5"){
                        modifier.margin(end = 8.dp).onClick{
                            SharedActions.effects?.applyPoison(5, 2, 1000L)
                        }
                    }

                    Button("Отмена яда"){
                        modifier.margin(end = 8.dp).onClick{
                            SharedActions.effects?.cancelPoison()
                        }
                    }
                }

                Row {
                    modifier.margin(top = sizes.gap)
                    Button("Реген +5"){
                        modifier.margin(end = 8.dp).onClick{
                            SharedActions.effects?.applyRegen(5, 2, 1000L)
                        }
                    }

                    Button("Отмена регена"){
                        modifier.margin(end = 8.dp).onClick{
                            SharedActions.effects?.cancelRegen()
                        }
                    }
                }

                Row {
                    modifier.margin(top = sizes.smallGap)
                    Button("Атаковать (кд 1.2 сек"){
                        modifier.margin(end = 8.dp).onClick{
                            val cd = SharedActions.cooldown

                            if (cd == null){
                                pushLog(game, "CooldownManager еще не готов")
                                return@onClick
                            }

                            if (!cd.canAttack()){
                                pushLog(game,"Атаковать нельзя")
                                return@onClick
                            }

                            cd.startAttackCooldown(totalMs = 1200L)
                        }
                    }
                }

                Text("Логи:"){modifier.margin(top = sizes.gap)}

                val lines = game.logLines.use()
                for (line in lines){
                    Text(line){modifier.font(sizes.smallText)}
                }
            }
        }
    }
}

// shared action - мост между сценами

object SharedActions{
    var effects: EffectManager? = null
    var cooldown: CooldownManager? = null
}

































