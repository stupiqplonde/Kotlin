

class DialogNode(
    val state: DialogState,
    val text: String
){
    // варианты выбора (переходы)
    // choiceId -> следующее состояние после выбора
    private val choices = mutableMapOf<String, DialogState>()

    fun addChoice(choiceId: String, nextState: DialogState){
        choices[choiceId] = nextState
    }

    fun getNextState(choiceId: String): DialogState? {
        return choices[choiceId]
    }

    fun print(){
        println("NPC говорит: \"$text\" ")
        println("Варианты: ")
        for (choice in choices.keys){
            println("> $choice")
        }
    }
}

