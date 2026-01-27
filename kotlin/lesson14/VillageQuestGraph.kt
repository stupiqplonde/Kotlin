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

        start.add(GameEvent.DialogueStarted::class.java, VillageQuestState.TALKED_TO_ELDER)

        talked.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.ACCEPTED_HELP )
        talked.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.REFUSED_HELP)

        accepted.add(GameEvent.CharacterDied::class.java, VillageQuestState.KILLED_KIRILL_SHAMAN)
        accepted.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.MADE_PEACE)

        refused.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.HELPED_KIRILL)

        refused.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.HERO_ENDING)
        madePeace.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.PEACE_ENDING)
        helpedKirill.add(GameEvent.DialogueChoiceSelected::class.java, VillageQuestState.BAD_ENDING)
        killedOrk.add(GameEvent.CharacterDied::class.java, VillageQuestState.SECRET_ENDING)

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