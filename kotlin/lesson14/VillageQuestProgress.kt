package lesson14

import lesson12.GameEvent

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
        } else{
            println("[QUEST GRAPH] - $playerId: событие ${event::class.simpleName} игнорировано. Игрок остался в состояния $current")
            // class.simpleName - это свойство, которое возвращает короткое имя класса(строку). Без указания пакета в котором он лежит
        }
    }
}
