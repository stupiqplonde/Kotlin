package lesson11

// EventBus - основа игроваой системы событий
// с его помощью обрабатываются и рассылаются все события в игре

// object - singlton
// это объект в единственном экземпляре на всю программу
object EventBus{
    //список слушателей
    private val listeners = mutableListOf<(GameEvent) -> Unit>()
    // (GameEvent) -> Unit это лямбда функция она принимает в роли параметра событие
    // и возвращает Unit (ничего)

    fun subscribe(listener: (GameEvent) -> Unit){
        //метод подписки на событие
        listeners.add(listener)
        // add - добавить нового подписчика в список подписанных на событие

        println("новый подписчик добавлен. Всего ${listeners.size}")
    }

    fun publish(event: GameEvent){
        // рассылка событий слушателям
        println("событие $event разослано")

        for (listener in listeners){
            listener(event)
            // вызов функции слушателя
        }
    }
}