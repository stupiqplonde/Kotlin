package lesson12

class SaveSystem{
    private val progress: MutableMap<String, MutableMap<String, MutableSet<String>>> = mutableMapOf()
    //progress[playerId][questId] = Набор выполненных шагов игрока
    // MutableMap<String, ... > - ключ playerId (словарь ключем которого будет игрок, а значения -его прогресс
    // MutableMap<String, MutableSet<String>> - ключ questId - все квесты игрока со всеми его шагами квеста
    // MutableMap<String> - набор шагов (stepId) которые уже выполнены игроком в квесте

    fun register(){
        EventBus.subscribe { event ->
            when (event) {
                is GameEvent.PlayerProgressSaved -> {
                    saveStep(event.playerId, event.questId, event.stepId)
                }
                else -> {}
            }
        }
    }

    fun saveStep(playerId: String, questId: String, stepId: String){
        val playerData = progress.getOrPut(playerId) {mutableMapOf()}
        // getOrPut(key) {...} - ищет ключ если находит то достает
        // если не находит то задает ему значение которое положено в {...}

        val questSteps = playerData.getOrPut(questId) {mutableSetOf()}

        val wasAdded = questSteps.add(stepId)
        //если шаг впервые добавлен то вернет true, иначе - false

        if (wasAdded){
            println("[SAVE] сохранено: игрок=$playerId, квест=$questId, шаг=$stepId")
        }else{
            println("[SAVE] шаг был сохранен ранее: игрок=$playerId, квест=$questId, шаг=$stepId")
        }
    }

}