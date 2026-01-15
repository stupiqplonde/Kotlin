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

    println("=== Сцена 1: Игрок начинает диалог с NPC")
    EventBus.post(GameEvent.DialogueStarted("Старый", "Oleg", "Oleg"))
    EventBus.processQueue(10)

    println("\n=== Сцена 2: Встреча с Кириллом - начало квеста с выбором")
    EventBus.post(GameEvent.DialogueStarted("Кирилл", "Oleg", "Oleg"))
    EventBus.processQueue(10)

    println("\n=== Сцена 3: Игрок выбирает путь через оплату")
    EventBus.post(GameEvent.DialogueChoiceSelected("Кирилл", "Oleg", "pay_gold", "Oleg"))
    EventBus.processQueue(10)

    println("\n=== Сцена 4: Второй игрок пробует другой путь")
    EventBus.post(GameEvent.DialogueStarted("Кирилл", "Anna", "Anna"))
    EventBus.processQueue(5)

    EventBus.post(GameEvent.CharacterDied("Kirill", "Anna", "Kirill"))
    EventBus.processQueue(5)

    println("\n=== Сцена 5: Третий игрок пробует оба пути")
    EventBus.post(GameEvent.DialogueStarted("Кирилл", "Bob", "Bob"))
    EventBus.processQueue(5)

    EventBus.post(GameEvent.DialogueChoiceSelected("Кирилл", "Bob", "pay_gold", "Bob"))
    EventBus.processQueue(5)

    EventBus.post(GameEvent.CharacterDied("Kirill", "Bob", "Kirill"))
    EventBus.processQueue(10)
}


