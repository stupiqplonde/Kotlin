//package lesson13
//
//import lesson12.GameEvent
//import lesson12.EventBus
//
//fun main(){
//    val system = TrainingStateSystem()
//    system.register()
//
//    val player = "Oleg"
//
//    EventBus.post(GameEvent.DialogueStarted(player, "Trainer", player))
//    EventBus.processQueue()
//    EventBus.post(GameEvent.DialogueStarted(player, "Trainer", player))
//    EventBus.processQueue()
//    EventBus.post(GameEvent.DialogueChoiceSelected(player, "Trainer", player, "accept"))
//    EventBus.processQueue()
//    EventBus.post(GameEvent.CharacterDied(player, "Dummy", player))
//    EventBus.processQueue()
//    EventBus.post(GameEvent.DialogueChoiceSelected(player, "Trainer", player, "completed"))
//    EventBus.processQueue()
//}

package lesson13

import lesson12.EventBus
import lesson12.GameEvent

fun main() {
    val system = TrainingStateSystem()
    system.register()
    val progress = TrainingProgress()

    println(" Запуск демо с несколькими игроками \n")

    val playerOleg = "Oleg"
    println(" Ход игрока $playerOleg ")

    EventBus.post(GameEvent.DialogueStarted(playerOleg, "Trainer", playerOleg))
    EventBus.processQueue()
    progress.debugStates()

    EventBus.post(GameEvent.DialogueChoiceSelected(playerOleg, "Trainer", playerOleg, "accept"))
    EventBus.processQueue()
    progress.debugStates()

    EventBus.post(GameEvent.CharacterDied(playerOleg, "Dummy", playerOleg))
    EventBus.processQueue()
    progress.debugStates()

    val playerInnokentiy = "Innokentiy"
    println("\n   Ход игрока $playerInnokentiy")

    EventBus.post(GameEvent.DialogueStarted(playerInnokentiy, "Trainer", playerInnokentiy))
    EventBus.processQueue()
    progress.debugStates()

    EventBus.post(GameEvent.DialogueChoiceSelected(playerInnokentiy, "Trainer", playerInnokentiy, "refuse"))
    EventBus.processQueue()
    progress.debugStates()

    println("\n Итоговые состояния ")
    progress.debugStates()

    println(" Продолжение пути Oleg ")
    EventBus.post(GameEvent.DialogueStarted(playerOleg, "Trainer", playerOleg))
    EventBus.processQueue()
    progress.debugStates()
}