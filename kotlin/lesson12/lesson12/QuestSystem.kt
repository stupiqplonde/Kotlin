package lesson12

// Квест = шаги выполнения (mini state Graph)
class QuestSystem{
    //флаги контроля состояния квеста
    private var questStarted: Boolean = false
    private var stepTalked: Boolean = false
    private var stepKillKirill: Boolean = false
    var goldPaid: Boolean = false
    private var stepReportedBack: Boolean = false

    private var questId = "simple_dimple_0.0.1"
    private var questId1 = "kill_kirill_or_pay_gold"

    private fun checkQuestCompletion(questId: String, playerId: String, state: QuestState) {
        when (questId) {
            simpleQuestId -> {
                if (state.questStarted && state.stepTalked && state.kirillKilled && state.stepReportedBack) {
                    println("Квест $questId завершен для игрока $playerId через полное выполнение")
                    EventBus.post(GameEvent.QuestCompleted(questId, playerId))
                    questStates.remove("${questId}_$playerId")
                }
            }
            choiceQuestId -> {
                if (state.questStarted && (state.kirillKilled || state.goldPaid)) {
                    val method = if (state.kirillKilled) "убийством" else "деньгами"
                    println("Квест $questId завершен для игрока $playerId через $method")
                    EventBus.post(GameEvent.QuestCompleted(questId, playerId))
                    questStates.remove("${questId}_$playerId")
                }
            }
        }
    }


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

    fun register() {
        EventBus.subscribe { event ->
            val playerId = when (event) {
                is GameEvent.DialogueStarted -> event.playerName
                is GameEvent.CharacterDied -> event.killerName ?: event.playerId
                is GameEvent.DialogueChoiceSelected -> event.playerName
                is GameEvent.QuestStepCompleted -> event.playerId
                is GameEvent.GoldPaid -> event.payerName
                else -> ""
            }

            if (playerId.isEmpty()) return@subscribe

            val simpleState = getState(simpleQuestId, playerId)

            val choiceState = getState(choiceQuestId, playerId)

            when (event) {
                is GameEvent.DialogueStarted -> {
                    if (event.npcName == "Старый" && !simpleState.questStarted) {
                        simpleState.questStarted = true
                        simpleState.stepTalked = true
                        println("Простой квест $simpleQuestId начат для игрока $playerId")

                        EventBus.post(GameEvent.QuestStarted(simpleQuestId, playerId))
                        EventBus.post(GameEvent.QuestStepCompleted(simpleQuestId, "talk_to_elder", playerId))
                        EventBus.post(GameEvent.PlayerProgressSaved(playerId, simpleQuestId, "talk_to_elder", playerId))
                    }

                    if (event.npcName == "Кирилл" && !choiceState.questStarted) {
                        choiceState.questStarted = true
                        choiceState.stepTalked = true
                        println("Квест с выбором $choiceQuestId начат для игрока $playerId через диалог с Кириллом")

                        EventBus.post(GameEvent.QuestStarted(choiceQuestId, playerId))
                        EventBus.post(GameEvent.QuestStepCompleted(choiceQuestId, "talk_to_kirill", playerId))
                        println("[КВЕСТ] Игрок $playerId, выбери путь: убить Кирилла или заплатить 100 золотых")
                    }
                }

                is GameEvent.CharacterDied -> {
                    if (event.characterName == "Kirill" && event.killerName == playerId) {
                        if (simpleState.questStarted) {
                            simpleState.kirillKilled = true
                            println("Шаг простого квеста: Кирилл убит игроком $playerId")

                            EventBus.post(GameEvent.QuestStepCompleted())
                            EventBus.post(GameEvent.PlayerProgressSaved(playerId, simpleQuestId, "kill_kirill", playerId))
                        }

                        if (choiceState.questStarted && !choiceState.goldPaid) {
                            choiceState.kirillKilled = true
                            println("[КВЕСТ ВЫБОРА] Игрок $playerId выбрал путь: убийство Кирилла")
                            EventBus.post(GameEvent.QuestStepCompleted(choiceQuestId, "kill_kirill", playerId))
                            checkQuestCompletion(choiceQuestId, playerId, choiceState)
                        }
                    }
                }

                is GameEvent.GoldPaid -> {
                    if (event.recipientName == "Kirill" && event.payerName == playerId && event.amount >= 100) {
                        if (choiceState.questStarted && !choiceState.kirillKilled) {
                            choiceState.goldPaid = true
                            println("[КВЕСТ ВЫБОРА] Игрок $playerId выбрал путь: заплатил ${event.amount} золотых")
                            EventBus.post(GameEvent.QuestStepCompleted(choiceQuestId, "pay_gold", playerId))
                            checkQuestCompletion(choiceQuestId, playerId, choiceState)
                        }
                    }
                }

                is GameEvent.DialogueChoiceSelected -> {
                    if (event.npcName == "Старый" && event.choiceId == "report_done") {
                        if (simpleState.questStarted) {
                            println("Игрок $playerId отчитался о выполнении простого квеста")
                            simpleState.stepReportedBack = true
                            EventBus.post(GameEvent.QuestStepCompleted(simpleQuestId, "report", playerId))
                            EventBus.post(GameEvent.PlayerProgressSaved(playerId, simpleQuestId, "report", playerId))
                            checkQuestCompletion(simpleQuestId, playerId, simpleState)
                        }
                    }

                    if (event.npcName == "Kirill" && event.choiceId == "pay_gold") {
                        EventBus.post(GameEvent.GoldPaid(event.playerName, "Kirill", 100, playerId))
                    }
                }
                else -> {}
            }

            checkQuestCompletion(simpleQuestId, playerId, simpleState)
            checkQuestCompletion(choiceQuestId, playerId, choiceState)
        }
    }
}
