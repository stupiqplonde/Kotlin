package lesson12

// система логирования
class LogSystem{
    fun register(){
        EventBus.subscribe { event ->
            println("[INFO] полученно событие: $event")
            // fun chel (GameEvent) { dshfdkshf } || обычная функция
            // (GameEvent) -> { dshfdkshf } || лямбда
        }
    }
}

class AchievementSystem{
    private var killCount: Int = 0

    fun register(){
        EventBus.subscribe { event ->
            when(event){
                //when is - замена switch case - он проверяет в роли условия полученное событие
                is GameEvent.CharacterDied -> {
                    // проверяем убийца игрок - то прибавить к счетчику
                    if (event.killerName == "Oleg"){
                        killCount++
                        println("Счетчик убийств Олега: $killCount")

                        if (killCount == 1){
                            EventBus.post(GameEvent.AchievementUnlocked("Oleg","first_blood"))
                        }
                        if (killCount == 5){
                            EventBus.post(GameEvent.AchievementUnlocked("Oleg","m-m-monstr_kill"))
                        }
                        // важно нельзя все события грубо отправлять на выполнение здесь и сейчас через publish
                        // post нужен для строгой очереди выполнения игровых событий
                        // нужно это например, чтобы одно событие не выполнилось раньше другого, не перекрыло другое
                        // не наложилось на другое или логически не сломало порядок событий
                    }
                }
                else -> {}
            }
        }
    }
}