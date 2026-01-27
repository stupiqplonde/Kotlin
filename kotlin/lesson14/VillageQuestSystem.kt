package lesson14

import lesson12.EventBus
import lesson12.GameEvent

class VillageQuestSystem{
    private val progress = VillageQuestProgress()

    fun register(){
        EventBus.subscribe { event ->
            when(event){
                is GameEvent.DialogueStarted,
                is GameEvent.DialogueChoiceSelected,
                is GameEvent.CharacterDied -> {
                    progress.handle(event.playerId, event)
                }
                else -> {}
            }
        }
    }
}