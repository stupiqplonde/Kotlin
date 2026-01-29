package lesson15

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
}