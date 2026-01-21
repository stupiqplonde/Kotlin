package lesson13

class TrainingNpc (
    val name: String,
    var state: NpcState = NpcState.IDLE
    // Здеcь хранится активное состояние npc ( по умолчания IDLE)
){
    fun onPlayerApproached(playerId: String){
        // Если игрок подошел

        if (state == NpcState.IDLE){
            println("$name: Привет, $playerId")
            state = NpcState.WAITING
            println("[INFO] NPC теперь в состоянии WAITING")
        }
    }

    fun onDialodueStarted(playerId: String){
        if (state == NpcState.WAITING){
            println("$name: я научу тебя драться")
            state = NpcState.TALKING
            println("[INFO] NPC теперь в состоянии TALKING")
        }
    }

    fun onDialogueChoiceSelected(playerId: String, choiceId: String){
        if (state == NpcState.TALKING && choiceId == "accept"){
            println("$name: Хорош! вот твоя награда")
            state = NpcState.REWARDED
            println("[INFO] NPC теперь в состоянии REWARDED")
        }
    }
}

