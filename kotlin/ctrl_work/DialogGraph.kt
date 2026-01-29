

class DialogGraph{
    private val nodes = mutableMapOf<DialogState, DialogNode>()

    init {
        val greeting = DialogNode(
            DialogState.GREETING,
            "Привет BR-BR"
        )
        val asking_for_help = DialogNode(
            DialogState.ASKING_FOR_HELP,
            "Олег Дон, помоги орка убить!!!"
        )
        val to_answer = DialogNode(
            DialogState.TO_ANSWER,
            "..."
        )
//        val with_respect = DialogNode(
//            DialogState.WITH_RESPECT,
//            "Я тебе помогу, но забираю твою дочь..."
//        )
//        val without_respect = DialogNode(
//            DialogState.WITHOUT_RESPECT,
//            "Ты просишь помощи, но делаешь это без уважения пошел вон лох какашка"
//        )
        val offer = DialogNode(
            DialogState.OFFER_QUEST,
            "spasibo!!!"
        )
        val accepted = DialogNode(
            DialogState.ACCEPTED,
            "пойдем за свежим мясом)0"
        )
        val refused = DialogNode(
            DialogState.REFUSED,
            "Жаль этого говнюка"
        )
//        val br_alive = DialogNode(
//            DialogState.BR_BR_ALIVE,
//            "живи пока"
//        )
//        val br_died = DialogNode(
//            DialogState.BR_BR_DIED,
//            "покойся с миром, попрощайся с дочерью"
//        )
        val kill_br = DialogNode(
            DialogState.KILL_BR_BR,
            "Окр вмэр"
        )
        val kill_ork = DialogNode(
            DialogState.KILL_ORK,
            "Окр вмэр"
        )
        val completed = DialogNode(
            DialogState.QUEST_COMPLETED,
            "+rep & +дочь"
        )

        greeting.addChoice("listen", DialogState.ASKING_FOR_HELP)
        greeting.addChoice("bye", DialogState.END)

        asking_for_help.addChoice("asking_for_help", DialogState.TO_ANSWER)

        to_answer.addChoice("with_respect", DialogState.ACCEPTED)
        to_answer.addChoice("without_respect", DialogState.KILL_BR_BR)

        kill_br.addChoice("похоронить", DialogState.END)

        accepted.addChoice("bye", DialogState.END)
        accepted.addChoice("go", DialogState.KILL_ORK)
        kill_ork.addChoice("W", DialogState.QUEST_COMPLETED)
        kill_ork.addChoice("Esc", DialogState.END)
        completed.addChoice("bye", DialogState.END)

        listOf(greeting, offer, accepted, refused, completed).forEach {
            nodes[it.state] = it
        }

    }
    fun getNode(state: DialogState):  DialogNode{
        return nodes[state]!!
    }
}