//package lesson13
//
//import lesson12.GameEvent
//
//class TrainingStateGraph{
//    private val nodes = mutableMapOf<TrainingState, StateNode>()
//
//    init{
//        //init - блок, который выполнится при первом создании объекта
//
//        // создаем узлы (node)
//        val start = StateNode(TrainingState.START)
//        val approached = StateNode(TrainingState.APPROACHED)
//        val talking = StateNode(TrainingState.TALKING)
//        val accepted = StateNode(TrainingState.ACCEPTED)
//        val dummy_killed = StateNode(TrainingState.DUMMY_KILLED)
//        val completed = StateNode(TrainingState.COMPLETED)
//        val failed = StateNode(TrainingState.FAILED)
//
//        // описываем переходы (ЭТО САМ ГРАФ)
//        start.addTransition(
//            GameEvent.DialogueStarted::class.java,
//            TrainingState.APPROACHED
//        )
//        approached.addTransition(
//            GameEvent.DialogueStarted::class.java,
//            TrainingState.TALKING
//        )
//        talking.addTransition(
//            GameEvent.DialogueChoiceSelected::class.java,
//            TrainingState.ACCEPTED
//        )
//        failed.addTransition(
//            GameEvent.DialogueStarted::class.java,
//            TrainingState.FAILED
//        )
//        accepted.addTransition(
//            GameEvent.CharacterDied::class.java,
//            TrainingState.DUMMY_KILLED
//        )
//        dummy_killed.addTransition(
//            GameEvent.DialogueChoiceSelected::class.java,
//            TrainingState.COMPLETED
//        )
//
//        // кладем ноды на карту
//        nodes[start.state] = start
//        nodes[approached.state] = approached
//        nodes[talking.state] = talking
//        nodes[accepted.state] = accepted
//        nodes[failed.state] = failed
//        nodes[dummy_killed.state] = dummy_killed
//        nodes[completed.state] = completed
//    }
//
//    fun getNode(state: TrainingState): StateNode{
//        return nodes[state]!!
//        //!! - мы уверенны что не вернется null
//    }
//}

package lesson13

import lesson12.GameEvent

class TrainingStateGraph {
    private val nodes = mutableMapOf<TrainingState, StateNode>()

    init {
        val start = StateNode(TrainingState.START)
        val approached = StateNode(TrainingState.APPROACHED)
        val talking = StateNode(TrainingState.TALKING)
        val accepted = StateNode(TrainingState.ACCEPTED)
        val dummyKilled = StateNode(TrainingState.DUMMY_KILLED)
        val completed = StateNode(TrainingState.COMPLETED)
        val failed = StateNode(TrainingState.FAILED)

        talking.addConditionalTransition(GameEvent.DialogueChoiceSelected::class.java) { event ->
            if (event is GameEvent.DialogueChoiceSelected) {
                when (event.choiceId) {
                    "accept" -> TrainingState.ACCEPTED
                    "refuse" -> TrainingState.FAILED
                    else -> null
                }
            } else null
        }

        accepted.addConditionalTransition(GameEvent.DialogueChoiceSelected::class.java) { event ->
            if (event is GameEvent.DialogueChoiceSelected && event.choiceId == "complete") {
                TrainingState.COMPLETED
            } else null
        }

        start.addTransition(
            GameEvent.DialogueStarted::class.java,
            TrainingState.APPROACHED
        )
        approached.addTransition(
            GameEvent.DialogueStarted::class.java,
            TrainingState.TALKING
        )

        accepted.addTransition(
            GameEvent.CharacterDied::class.java,
            TrainingState.DUMMY_KILLED
        )

        dummyKilled.addTransition(
            GameEvent.DialogueStarted::class.java,
            TrainingState.COMPLETED
        )

        nodes[start.state] = start
        nodes[approached.state] = approached
        nodes[talking.state] = talking
        nodes[accepted.state] = accepted
        nodes[dummyKilled.state] = dummyKilled
        nodes[completed.state] = completed
        nodes[failed.state] = failed
    }

    fun getNode(state: TrainingState): StateNode {
        return nodes[state] ?: throw IllegalStateException("Нету: $state")
    }
}
