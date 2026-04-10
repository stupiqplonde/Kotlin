package playerMovement

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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.processNextEventInCurrentThread
import kotlinx.serialization.modules.SerializersModule
import questJournal2.QuestPinned
import javax.accessibility.AccessibleValue
import javax.management.ValueExp
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.atan2 // угол по x/z
import kotlin.math.cos
import kotlin.math.max // большее из двух
import kotlin.math.sin

import java.awt.KeyEventDispatcher // перехватчик клавиатуры
import java.awt.KeyboardFocusManager // диспетчер фокуса окна
import java.awt.event.KeyEvent // само событие нажатия на клавишу

// реализация
// движение игрока с помощью клавиш
// свободное перемещение
// поворот игрока по направлению движения
// тестовый объект для взаимодействия
// follow-camera эффект, чтобы игрок оставался в центре сцены на экране

// Desktop keyboard bridge
// слушаем нажатия клавиш через AWT и каждый кадр читаем текущее состояние клавиатуры

object DesktopKeyboardState{
    private val pressedKeys = mutableSetOf<Int>()
    // наборы кодов клавиш, которые сейчас зажаты

    private val justPressedKeys = mutableSetOf<Int>()
    // набор клавиш которые были нажаты единожды

    private var isInstalled = false
    // флаг-подсказка

    // метод установки перехватчика клавы
    fun install(){
        if (isInstalled) return

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(
                object: KeyEventDispatcher{
                    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
                        when (e.id){
                            KeyEvent.KEY_PRESSED -> {
                                // если клавиша не была нажата ранее, то это новое событие
                                // можно добавить ее в justPressedKeys
                                if (!pressedKeys.contains(e.keyCode)){
                                    justPressedKeys.add(e.keyCode)
                                }
                                pressedKeys.add(e.keyCode)
                            }
                            KeyEvent.KEY_RELEASED -> {
                                // когда клавишу отпускают - удаляем ее из общих наборов клавиш
                                pressedKeys.remove(e.keyCode)
                                justPressedKeys.remove(e.keyCode)
                            }
                        }
                        return false
                        // false - не блокировать дальнейшую обработку
                    }
                }
            )
        isInstalled = true
    }

    fun isDown(keyCode: Int): Boolean{
        // проверка зажата ли клавиша сейчас
        return keyCode in pressedKeys
    }

    fun consemeJustPressed(keyCode: Int): Boolean{
        // один раз поймать новое нажатие
        // если клавиша есть в justPressedKeys
        // тогда вернем true и сразу удалим ее оттуда
        // так клавиши одиночного взаимодействия будут работать правитьно

        return if (keyCode in justPressedKeys){
            justPressedKeys.remove(keyCode)
            true
        }else{
            false
        }
    }
}

enum class QuestState{
    START,
    WAIT_HERB,
    GOOD_END,
    EVIL_END
}

enum class WorldObjectType{
    ALCHEMIST,
    HERB_SOURCE,
    CHEST,
    DOOR
}

data class WorldObjectDef(
    val id: String,
    val type: WorldObjectType,
    val worldX: Float,
    val worldZ: Float,
    val interactRadius: Float
)

data class ObstacleDef(
    val centerX: Float,
    val centerZ: Float,
    val halfSize: Float // половина размера квадрата препятствия
)

data class NpcMemory(
    val hasMet: Boolean,
    val timesTalked: Int,
    val receivedHerb: Boolean
)

data class PlayerState(
    val playerId: String,
    val worldX: Float,
    val worldY: Float,

    val yawDeg: Float, // куда смотрит игрок

    val moveSpeed: Float,

    val questState: QuestState,
    val inventory: Map<String, Int>,
    val gold: Int,

    val alchemistMemory : NpcMemory,
    val chestLooted: Boolean,
    val doorOpened: Boolean,

    val currentFocusId: String?,
    val hintText: String,

    val pinnedQuestEnabled: Boolean,
    val pinnedTargetId: String?
)





































