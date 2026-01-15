package lesson12

class NpcSystem{
    fun register(){
        EventBus.subscribe { event ->
            when(event) {
                is GameEvent.QuestStarted -> {
                    println("[NPCSystem] Oldy ждет результата")
                    EventBus.post(GameEvent.DialogueLineUnlocked("Oleg","Oldy", "remind_kirill"))
                }

                is GameEvent.QuestStepCompleted -> {
                    if(event.stepId == "kill_kirill")
                        println("[NPCSystem] Открыта новая реплика 'Кирилл готов?'")
                        EventBus.post(GameEvent.DialogueLineUnlocked("Oleg","Oldy", "ask_report"))
                }

                is GameEvent.QuestCompleted -> {
                    println("[NPCSystem] квест выполнен")
                    EventBus.post(GameEvent.DialogueLineUnlocked("Oleg","Oldy", "congrats"))
                }
                else -> {}
            }
        }
    }

}