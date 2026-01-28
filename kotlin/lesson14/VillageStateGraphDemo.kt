package lesson14

import lesson12.EventBus
import lesson12.GameEvent

fun main(){
    val system = VillageQuestSystem()
    system.register()

    val player = "Oleg"
    val player_two = "ququi"
    val traitor = "Kirill"
    val secretTarget = "Орк"

    // герой
    EventBus.post(GameEvent.DialogueStarted(player, "Старый", player))

    EventBus.post(GameEvent.DialogueChoiceSelected(player,"Старый", player, "accept"))
    EventBus.post(GameEvent.CharacterDied(player,secretTarget, player))
    EventBus.post(GameEvent.DialogueChoiceSelected(player,"Старый", player, "report"))

    // предатель
    EventBus.post(GameEvent.DialogueStarted(traitor,"Старый", traitor))
    EventBus.post(GameEvent.DialogueChoiceSelected(traitor,"shneyne", traitor, "refuse"))
    EventBus.post(GameEvent.DialogueChoiceSelected(traitor,"shneyne", traitor, "help_kirill"))

    // инопланетянин
    EventBus.post(GameEvent.DialogueStarted(player_two,"Старый", player_two))
    EventBus.post(GameEvent.DialogueChoiceSelected(player_two, secretTarget, player_two, "secret"))

    EventBus.processQueue(100)

}