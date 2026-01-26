//package lesson13
//
//import lesson12.GameEvent
//
//
//// Персональное состояние каждого игрока
//
//class TrainingProgress{
//    private val graph = TrainingStateGraph()
//
//    private val currentStateByPlayer = mutableMapOf<String, TrainingState>()
//
//    fun getState(playerId: String): TrainingState{
//        return currentStateByPlayer.getOrPut(playerId) { TrainingState.START }
//        // Если не находит активного состояния игрока - вернуть начальное состояние
//    }
//
//    fun handleEvent(playerId: String, event: GameEvent){
//        val currentState = getState(playerId)
//        val node = graph.getNode(currentState)
//
//        val nextState = node.getNextState(event)
//
//        if (nextState != null){
//            println("[STATE GRAPH] $playerId перешел из состояния $currentState -> в $nextState")
//            currentStateByPlayer[playerId] = nextState
//            // обновляем состояние (этап прогресса) для конкретного игрока
//        }else{
//            println("[STATE GRAPH] $playerId проигнорировал событие ${event::class.simpleName} состояние $currentState не изменено")
//        }
//    }
//}

package lesson13

import lesson12.GameEvent
import lesson12.EventBus

class TrainingProgress {
    private val graph = TrainingStateGraph()
    private val currentStateByPlayer = mutableMapOf<String, TrainingState>()

    fun getState(playerId: String): TrainingState {
        return currentStateByPlayer.getOrPut(playerId) { TrainingState.START }
    }

    fun handleEvent(playerId: String, event: GameEvent) {
        val currentState = getState(playerId)
        val node = graph.getNode(currentState)

        val nextState = node.getNextState(event)

        if (nextState != null && nextState != currentState) {
            val oldState = currentState
            currentStateByPlayer[playerId] = nextState

            println("[STATE GRAPH] $playerId перешел из состояния ($oldState) -> в ($nextState)")

            EventBus.post(GameEvent.StateChanged(
                oldState = oldState.name,
                newState = nextState.name,
                playerId = playerId
            ))
        } else {
            println("[STATE GRAPH] $playerId проигнорировал событие ${event::class.simpleName} состояние ($currentState) не изменено")
        }
    }

    fun debugStates() {
        println("\nТекущие состояния игроков")
        currentStateByPlayer.forEach { (player, state) ->
            println("$player: $state")
        }
        println("---\n")
    }
}