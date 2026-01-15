package lesson12

// Квест = шаги выполнения (mini state Graph)
class QuestSystem{
    //флаги контроля состояния квеста
    private var questStarted: Boolean = false
    private var stepTalked: Boolean = false
    private var stepKillKirill: Boolean = false
    private var stepReportedBack: Boolean = false

    private var questId = "simple_dimple_0.0.1"

    fun register(){
        EventBus.subscribe { event ->
            when(event){
                is GameEvent.DialogueStarted -> {
                    if (event.npcName == "Oldy" && !questStarted){
                        questStarted = true
                        stepTalked = true
                        println("Квест $questId начат через диалог с ${event.npcName}")

                        EventBus.post(GameEvent.QuestStarted("Oleg", questId))
                        EventBus.post(GameEvent.QuestStepCompleted("Oleg", questId, "talk_to_elder"))
                        EventBus.post(GameEvent.PlayerProgressSaved("Oleg","Oleg", questId, "talk_to_elder"))
                    }
                }

                is GameEvent.CharacterDied -> {
                    if (questStarted && event.characterName == "Kirill" && event.killerName == "Oleg"){
                        stepKillKirill = true
                        println("Шаг квеста: Кирилл убит Олегом")
                        EventBus.post(GameEvent.QuestStepCompleted("Oleg",questId, "kill_kirill"))
                        EventBus.post(GameEvent.PlayerProgressSaved("Oleg","Oleg", questId, "kill_kirill"))
                    }
                }

                is GameEvent.DialogueChoiceSelected -> {
                    if (questStarted && event.npcName == "Oldy" && event.choiceId == "report"){
                        stepReportedBack = true
                        println("Олег отчитался о выполнении квеста")
                        EventBus.post(GameEvent.QuestStepCompleted("Oleg",questId, "report_back"))
                        EventBus.post(GameEvent.PlayerProgressSaved("Oleg","Oleg", questId, "report_back"))
                    }
                }
                else -> {}
            }
            if (questStarted && stepTalked && stepKillKirill && stepReportedBack){
                println("Все шаги квеста выполнены")
                EventBus.post(GameEvent.QuestCompleted("Oleg",questId))

                questStarted = false
            }
        }
    }
}