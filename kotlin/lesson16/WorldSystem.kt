package lesson16

import lesson12.EventBus
import lesson12.GameEvent
import lesson14.*
import lesson15.*


class WorldSystem{
    private val questSystem = VillageQuestSystem()
    private val dialogSystem = DialogProgress()

    fun register(){
        questSystem.register()

        EventBus.subscribe { event ->
            when(event){
                is GameEvent.QuestStateChanged -> {
                    dialogSystem.onQuestStateChanged(
                        event.playerId,
                        event.nextState
                    )
                }
                else -> {}
            }
        }
    }

    fun talkToNpc(playerId: String){
        dialogSystem.show(playerId)
    }

    fun chooseDialog(playerId: String, choice: String){
        dialogSystem.choose(playerId, choice)
    }
}