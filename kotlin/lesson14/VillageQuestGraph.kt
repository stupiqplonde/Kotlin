package lesson14

import lesson12.GameEvent
import lesson12.EventBus



class VillageQuestGraph{
    private val nodes = mutableMapOf<VillageQuestState, StateNodeV2>()

    init {
        val start = StateNodeV2(VillageQuestState.NOT_STARTED)
        val talked = StateNodeV2(VillageQuestState.TALKED_TO_ELDER)
        val accepted = StateNodeV2(VillageQuestState.ACCEPTED_HELP)
        val refused = StateNodeV2(VillageQuestState.REFUSED_HELP)

        val killedKirill = StateNodeV2(VillageQuestState.KILLED_KIRILL_SHAMAN)
        val madePeace = StateNodeV2(VillageQuestState.MADE_PEACE)
        val helpedKirill = StateNodeV2(VillageQuestState.HELPED_KIRILL)

        val heroEnd = StateNodeV2(VillageQuestState.HERO_ENDING)
        val peaceEnd = StateNodeV2(VillageQuestState.PEACE_ENDING)
        val badEnd = StateNodeV2(VillageQuestState.BAD_ENDING)
        val secretEnd = StateNodeV2(VillageQuestState.SECRET_ENDING)
        val killedOrk = StateNodeV2(VillageQuestState.KILLED_ORK)

        start.add(GameEvent.DialogueStarted::class.java, VillageQuestState.TALKED_TO_ELDER, VillageQuestState.NOT_STARTED)

        talked.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.ACCEPTED_HELP, VillageQuestState.NOT_STARTED )
        talked.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.REFUSED_HELP, VillageQuestState.NOT_STARTED)

        accepted.add(GameEvent.CharacterDied::class.java, VillageQuestState.KILLED_KIRILL_SHAMAN, VillageQuestState.TALKED_TO_ELDER)
        accepted.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.MADE_PEACE, VillageQuestState.ACCEPTED_HELP)

        refused.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.HELPED_KIRILL, VillageQuestState.TALKED_TO_ELDER)

        refused.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.HERO_ENDING, VillageQuestState.KILLED_KIRILL_SHAMAN)
        madePeace.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.PEACE_ENDING, VillageQuestState.ACCEPTED_HELP)
        helpedKirill.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.BAD_ENDING, VillageQuestState.REFUSED_HELP)
        killedOrk.add(GameEvent.CharacterDied::class.java, VillageQuestState.SECRET_ENDING, VillageQuestState.ACCEPTED_HELP)

        // регистрируем список
        listOf(
            start, talked, accepted, refused, killedKirill, killedOrk,
            madePeace, helpedKirill, heroEnd, peaceEnd, badEnd, secretEnd
        ).forEach { nodes[it.state] = it }



    }

    fun getNode(state: VillageQuestState): StateNodeV2{
        return nodes[state]!!
    }
}