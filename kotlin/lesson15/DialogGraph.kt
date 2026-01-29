package lesson15

class DialogGraph{
    private val nodes = mutableMapOf<DialogState, DialogNode>()

    init {
        val greeting = DialogNode(
            DialogState.GREETING,
            "Привет доходяга"
        )
        val offer = DialogNode(
            DialogState.OFFER_QUEST,
            "Казахстан угрожает бомбардировкой"
        )
        val accepted = DialogNode(
            DialogState.ACCEPTED,
            "Отлично, примени мне его голову"
        )
        val refused = DialogNode(
            DialogState.REFUSED,
            "Жаль этого говнюка"
        )
        val completed = DialogNode(
            DialogState.QUEST_COMPLETED,
            "Good boy"
        )

        greeting.addChoice("work", DialogState.OFFER_QUEST)
        greeting.addChoice("bye", DialogState.END)

        offer.addChoice("accept", DialogState.ACCEPTED)
        offer.addChoice("refuse", DialogState.REFUSED)

        accepted.addChoice("bye", DialogState.END)
        refused.addChoice("bye", DialogState.END)
        completed.addChoice("bye", DialogState.END)

        listOf(greeting, offer, accepted, refused, completed).forEach {
            nodes[it.state] = it
        }

    }
    fun getNode(state: DialogState):  DialogNode{
        return nodes[state]!!
    }
}