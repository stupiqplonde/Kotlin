package lesson16

import lesson14.*
import lesson15.*

class DialogProgress{
    private val graph = DialogGraph()
    private val stateByPlayer = mutableMapOf<String, DialogState>()

    fun getState(playerId: String): DialogState{
        return stateByPlayer.getOrPut(playerId){
            DialogState.GREETING
        }
    }

    fun show(playerId: String){
        val node = graph.getNode(getState(playerId))
        node.print()
    }

    fun choose(playerId: String, choiceId: String){
        val current = getState(playerId)
        val node = graph.getNode(current)
        val next = node.getNextState(choiceId)

        if (next != null){
            println("[DIALOGUE] $playerId: $current -> $next")
            stateByPlayer[playerId] = next
        } else{
            println("Недопустимый выбор: $choiceId")
        }
    }

    // связка с квестом - метод реагирования на события и его изменений
    fun onQuestStateChanged(playerId: String, nextState: String){
        when(nextState){
            "ACCEPTED_HELP" -> {
                stateByPlayer[playerId] = DialogState.QUEST_IN_PROGRESS
                // если квест перешел в состояние принятого - то поменять реплики у NPC (сменить состояние диалога)
            }
            "HERO_ENDING" -> {
                stateByPlayer[playerId] = DialogState.QUEST_COMPLETED
            }
        }
    }
}