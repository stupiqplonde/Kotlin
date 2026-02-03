package lesson16

import lesson12.EventBus
import lesson12.GameEvent
import lesson14.*
import lesson15.*

class VillageQuestProgress{
    private val graph = VillageQuestGraph()

    private val stateByPlayer = mutableMapOf<String, VillageQuestState>()

    fun getState(playerId: String): VillageQuestState{
        return stateByPlayer.getOrPut(playerId){ VillageQuestState.NOT_STARTED }
    }

    fun handle(playerId: String, event: GameEvent){
        val current = getState(playerId)
        val node = graph.getNode(current)
        val next = node.next(event)

        if (next != null){
            println("[QUEST GRAPH] - $playerId: $current -> $next")
            stateByPlayer[playerId] = next
            EventBus.post(
                GameEvent.QuestStateChanged(
                    playerId,
                    "village_quest",
                    current.name,
                    next.name
                )
            )
            // Ключевое для чего это нужно
            // State Graph теперь не просто менять состояние, а еще ОБЯЗАН сообщить миру, что состояние
            // квеста изменилось
        } else{
            println("[QUEST GRAPH] - $playerId: событие ${event::class.simpleName} игнорировано. Игрок остался в состояния $current")
            // class.simpleName - это свойство, которое возвращает короткое имя класса(строку). Без указания пакета в котором он лежит
        }
    }

    fun printPosition(playerId: String, event: GameEvent){
        val current = getState(playerId)
        val node = graph.getNode(current)
        val next = node.next(event)
        val past = node.past(event)
        println("[INFO] now: $current, next: $next, past: $past")
    }


}