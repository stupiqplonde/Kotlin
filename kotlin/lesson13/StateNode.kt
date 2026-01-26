//package lesson13
//
//import lesson12.GameEvent
//
//// узлы графа (StateNode)
//// будем явно описывать каждый переход, а не прятать за if else
//
//class StateNode(
//    val state: TrainingState
//    // какое состояние представляет этот узел
//){
//    // Все возможные переходы ИЗ этого сотояния
//    private val transitions = mutableMapOf<Class<out GameEvent>, TrainingState>()
//    // Класс GameEvent ИЛИ ЛЮБОЙ класс, который от него наследуется
//    // GameEvent - это иерархия событий (а события то него наследуются)
//    // out - модификатор ковариантности используется в обобщенных generic типах данных
//    // используется для указания, что параметризованный тип может быть использован в иерархии
//    // Если пишем класс который должен вернуть строго тип данных GameEvent, то используем out
//    // Словарь: ключ - тип события (например Dialogue)
//    // Значение - в какое состояние перейдем при данном событии
//
//    fun addTransition(
//        eventType: Class<out GameEvent>,
//        nextState: TrainingState
//    ){
//        transitions[eventType] = nextState
//    }
//
//    fun getNextState(event: GameEvent): TrainingState? {
//        // достаем класс события и ищем куда он может перейти в случае этого события
//        return transitions[event::class.java]
//        // СОБЫТИЕ -> СОСТОЯНИЕ
//        // event - Kotlin
//        // event::class - язык программирования
//        // event:class - скажи мне КАКОГО ТИПА данный объект
//        // :: - оператор ссылки
//        // он НЕ ИСПОЛЬЗУЕТ ОБЪЕКТ, а только ссылается на информацию о нем
//        // ссылка на класс объекта event
//        // .java - зачем?
//        // Kotlin и java существуют вместе одновременно
//        // Kotlin работаем поверх JVM (Java Virtual Machine)
//        // и создаваемый нами Map существующий как Java-класс
//        // event::class      - Kotlin класс
//        // event::class.java - Java класс
//        // ВАЖНО ЭТО НЕ 2 РАЗНЫХ КЛАССА - это 2 разных формы записи одного и того же типа
//        // Java тип здесь нам нужен тк Map<Class<>...> - использует именно Class из Java
//        // return transitions[event::class.java] - по-человечески
//        // Взять тип события которое пришло найти в таблице переходов (map), то в какое состояние
//        // мы должны перейти и верни его нам
//        // Пример если происходит событие DialogueStarted то код спрашивает:
//        // transitions[DialogueStarted] -> TALKING
//        // если события в таблице не нуждается -> верни null
//    }
//}

package lesson13

import lesson12.GameEvent

class StateNode(
    val state: TrainingState
) {
    private val transitions = mutableMapOf<Class<out GameEvent>, TrainingState>()
    private val conditionalTransitions = mutableMapOf<Class<out GameEvent>, (GameEvent) -> TrainingState?>()

    fun addTransition(
        eventType: Class<out GameEvent>,
        nextState: TrainingState
    ) {
        transitions[eventType] = nextState
    }

    fun addConditionalTransition(
        eventType: Class<out GameEvent>,
        conditionHandler: (GameEvent) -> TrainingState?
    ) {
        conditionalTransitions[eventType] = conditionHandler
    }

    fun getNextState(event: GameEvent): TrainingState? {
        val handler = conditionalTransitions[event::class.java]
        if (handler != null) {
            val result = handler(event)
            if (result != null) return result
        }

        return transitions[event::class.java]
    }
}