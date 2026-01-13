package lesson11

class Npc (val name: String){
    fun register(){
        EventBus.subscribe { event ->
            when(event){
                is GameEvent.QuestCompleted -> {
                    println("NPC $name готов выдать награду за квест ${event.questId}")
                }

                else -> {}
            }
        }
    }
}