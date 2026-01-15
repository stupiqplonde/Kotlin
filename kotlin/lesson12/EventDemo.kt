package lesson12

fun main(){
    val logSystem = LogSystem()
    logSystem.register()

    val achievementSystem = AchievementSystem()
    achievementSystem.register()

    val questSystem = QuestSystem()
    questSystem.register()

    val npcSystem = NpcSystem()
    npcSystem.register()

    EventBus.subscribeOnce { event ->
        println("[FIRST_EVENT]")
    }

    println("+ сцена первая +")

    EventBus.post(GameEvent.DialogueStarted("Oleg","Oldy", "Oleg"))
    EventBus.processQueue(50)

    println("+ сцена вторая: бой +")
    val combat = CombatSystemDemo()
    combat.simulateFight()

    EventBus.processQueue(50)

    println("+ сцена третья: доклад +")

    EventBus.post(
        GameEvent.DialogueChoiceSelected(
            "Oleg",
            "Oldy",
            "Oleg",
            "report_done"
        )
    )
    EventBus.processQueue(50)
}