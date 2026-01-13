package lesson11

class Quest(
    val id: String,
    val targetCharacter: String
){
    var isCompleted: Boolean = false

    fun register(){
        // регистрация квеста в системе событий

        EventBus.subscribe { event ->
            // Эта функция будет вызываться каждый раз когда в игре случается какое-то событие

            when(event){
                is GameEvent.CharacterDied -> {
                    if (event.characterName == targetCharacter && !isCompleted){
                        //проверяем соответствие моба с целью кыеста и состояние квеста
                        isCompleted = true
                        println("Квест $id выполнен! моб ${event.characterName} повержен")

                        //вызываем метод рассылки
                        EventBus.publish(
                            GameEvent.QuestCompleted(id)
                        )
                    }
                }
                else -> {
                    // else - все остальные события будут игнорированы
                }
            }
        }
    }
}