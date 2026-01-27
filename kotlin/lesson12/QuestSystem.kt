package lesson12

// Квест = шаги выполнения (mini state Graph)
class QuestSystem{
    data class QuestProgressState(
        //флаги контроля состояния квеста
        var questStarted: Boolean = false,
        var stepTalked: Boolean = false,
        var stepKillKirill: Boolean = false,
        var killKirill: Boolean = false,
        var goldPaid: Boolean = false,
        var reportedBack: Boolean = false,
        var questCompleted: Boolean = false
    )


    val questStates = mutableMapOf<String, QuestProgressState>()

    var questId = "simple_dimple_0.0.1"
    var questIdTwo = "kill_kirill_or_pay_gold"
    private var progressByPlayer: MutableMap<String, QuestProgressState> = mutableMapOf()
    // progressByPlayer[playerId] - проверка состояния происхождения квеста конкретным игроком

//    private fun getState(questId: String, playerId: String): QuestProgressState{
//        return questStates.getOrPut("${questId}, ${playerId}") { QuestProgressState() }
//    }

    private fun checkQuestCompletion(questId: String, playerId: String, state: QuestProgressState) {
        when (questId) {
            questId -> {
                if (state.questStarted && state.stepTalked && state.killKirill && state.reportedBack) {
                    println("Квест $questId завершен для игрока $playerId через полное выполнение")
                    EventBus.post(GameEvent.QuestCompleted(playerId, questId))
                    questStates.remove("${questId}_$playerId")
                }
            }
            questIdTwo -> {
                if (state.questStarted && (state.killKirill || state.goldPaid)) {
                    val method = if (state.killKirill) "убийством" else "деньгами"
                    println("Квест $questId завершен для игрока $playerId через $method")
                    EventBus.post(GameEvent.QuestCompleted(playerId, questId))
                    questStates.remove("${questId}_$playerId")
                }
            }
        }
    }

//    fun register() {
//        EventBus.subscribe { event ->
//            val playerId = when (event) {
//                is GameEvent.DialogueStarted -> event.playerName
//                is GameEvent.CharacterDied -> event.killerName ?: event.playerId
//                is GameEvent.DialogueChoiceSelected -> event.playerName
//                is GameEvent.QuestStepCompleted -> event.playerId
//                is GameEvent.GoldPaid -> event.payerName
//                else -> ""
//            }
//
//            if (playerId.isEmpty()) return@subscribe
//
//            val simpleState = getState(questId, playerId)
//
//            val choiceState = getState(questId1, playerId)
//
//            when (event) {
//                is GameEvent.DialogueStarted -> {
//                    if (event.npcName == "Старый" && !simpleState.questStarted) {
//                        simpleState.questStarted = true
//                        simpleState.stepTalked = true
//                        println("Простой квест $questId начат для игрока $playerId")
//
//                        EventBus.post(GameEvent.QuestStarted(questId, playerId))
//                        EventBus.post(GameEvent.QuestStepCompleted(questId, "talk_to_elder", playerId))
//                        EventBus.post(GameEvent.PlayerProgressSaved(playerId, questId, "talk_to_elder"))
//                    }
//
//                    if (event.npcName == "Кирилл" && !choiceState.questStarted) {
//                        choiceState.questStarted = true
//                        choiceState.stepTalked = true
//                        println("Квест с выбором $questId1 начат для игрока $playerId через диалог с Кириллом")
//
//                        EventBus.post(GameEvent.QuestStarted(questId1, playerId))
//                        EventBus.post(GameEvent.QuestStepCompleted(questId1, "talk_to_kirill", playerId))
//                        println("[КВЕСТ] Игрок $playerId, выбери путь: убить Кирилла или заплатить 100 золотых")
//                    }
//                }
//
//                is GameEvent.CharacterDied -> {
//                    if (event.characterName == "Kirill" && event.killerName == playerId) {
//                        if (simpleState.questStarted) {
//                            simpleState.kirillKilled = true
//                            println("Шаг простого квеста: Кирилл убит игроком $playerId")
//
//                            EventBus.post(GameEvent.QuestStepCompleted(playerId, questId, "kill_kirill"))
//                            EventBus.post(GameEvent.PlayerProgressSaved(playerId, questId, "kill_kirill"))
//                        }
//
//                        if (choiceState.questStarted && !choiceState.goldPaid) {
//                            choiceState.kirillKilled = true
//                            println("[КВЕСТ ВЫБОРА] Игрок $playerId выбрал путь: убийство Кирилла")
//                            EventBus.post(GameEvent.QuestStepCompleted(questId1, "kill_kirill", playerId))
//                            checkQuestCompletion(questId1, playerId, choiceState)
//                        }
//                    }
//                }
//
//                is GameEvent.GoldPaid -> {
//                    if (event.recipientName == "Kirill" && event.payerName == playerId && event.amount >= 100) {
//                        if (choiceState.questStarted && !choiceState.kirillKilled) {
//                            choiceState.goldPaid = true
//                            println("[КВЕСТ ВЫБОРА] Игрок $playerId выбрал путь: заплатил ${event.amount} золотых")
//                            EventBus.post(GameEvent.QuestStepCompleted(questId1, "pay_gold", playerId))
//                            checkQuestCompletion(questId1, playerId, choiceState)
//                        }
//                    }
//                }
//
//                is GameEvent.DialogueChoiceSelected -> {
//                    if (event.npcName == "Старый" && event.choiceId == "report_done") {
//                        if (simpleState.questStarted) {
//                            println("Игрок $playerId отчитался о выполнении простого квеста")
//                            simpleState.stepReportedBack = true
//                            EventBus.post(GameEvent.QuestStepCompleted(questId, "report", playerId))
//                            EventBus.post(GameEvent.PlayerProgressSaved(playerId, questId, "report"))
//                            checkQuestCompletion(questId, playerId, simpleState)
//                        }
//                    }
//
//                    if (event.npcName == "Kirill" && event.choiceId == "pay_gold") {
//                        EventBus.post(GameEvent.GoldPaid(event.playerName, "Kirill", "oleg", 100))
//                    }
//                }
//                else -> {}
//            }
//
//            checkQuestCompletion(questId, playerId, simpleState)
//            checkQuestCompletion(questId1, playerId, choiceState)
//        }
//    }


    fun register(){
        EventBus.subscribe { event ->
            when(event){
                is GameEvent.DialogueStarted -> {
                    if (event.npcName == "Oldy"){
                        val state = getState(event.playerId)
                        if (!state.questStarted){
                            state.questStarted = true
                            state.stepTalked = true
                            println("Квест $questId начат игроком ${event.playerId} через диалог с ${event.npcName}")

                            EventBus.post(GameEvent.QuestStarted(event.playerId, questId))
                            completeStep(event.playerId, "talk_to_npc")
                        }

//                        EventBus.post(GameEvent.PlayerProgressSaved("Oleg","Oleg", questId, "talk_to_elder"))
                    }
                }

                is GameEvent.CharacterDied -> {
                    val state = getState(event.playerId)
                    if (state.questStarted &&
                        !state.killKirill &&
                        event.characterName == "Kirill" &&
                        event.killerName == "Oleg"
                        ){
                        state.killKirill = true
                        println("Игрок ${event.playerId} выполнил шаг квеста $questId: убить кирилла")
                        completeStep(event.playerId, "kill_kirill")
                    }
                }

                is GameEvent.DialogueChoiceSelected -> {
                    if (event.npcName == "Oldy" && event.choiceId == "report_done"){
                        val state = getState(event.playerId)
                        if (state.questStarted && state.killKirill && !state.reportedBack){
                            state.reportedBack = true
                            println("Игрок ${event.playerId} сдал квест")
                            completeStep(event.playerId, "report_back")
                        }
                    }
                }
                else -> {}
            }
            checkQuestCompletionForAllPlayers()
        }
    }

    private fun getState(playerId: String): QuestProgressState{
        return progressByPlayer.getOrPut(playerId) { QuestProgressState() }
    }

    private fun completeStep(playerId: String, stepId: String){
        EventBus.post(GameEvent.QuestCompleted(playerId, questId, stepId))
        EventBus.post(GameEvent.PlayerProgressSaved(playerId, questId, stepId))
    }

    private fun checkQuestCompletionForAllPlayers(){
        for((player, state) in progressByPlayer){
            if (!state.questCompleted &&
                state.questStarted &&
                state.stepTalked &&
                state.killKirill &&
                state.reportedBack
                ){
                    state.questStarted = true
                    println("Квест ${questId} завершен для игрока $player")
                EventBus.post(GameEvent.QuestCompleted(player, questId))
                EventBus.post(GameEvent.PlayerProgressSaved(player, questId, "QUEST_COMPLETED"))
            }
        }
    }
}



