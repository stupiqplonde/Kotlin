package lesson12

object EventBus {
    // сохранение типа слушателя
    typealias Listener = (GameEvent) -> Unit
    // "переменная" для типов данных
    // создание псевдонима для типа данных
    // вместо (GameEvent) -> Unit будем писать псевдоним Listener

    // список слушателей хранить в Map
    // чтобы можно было опираться на id слушателя в базе
    private val listeners = mutableMapOf<Int, Listener>()
    private var nextId: Int = 1
    // id следующего подписчика

    // очередь событий (пошаговая обработка)
    private val eventQueue = ArrayDeque<GameEvent>()
    // ArrayDeque - двусторонняя очередь
    // Здесь будут храниться события, которые мы захотим обработать позже

    fun subscribe(listener: Listener): Int{
        val id = nextId
        nextId += 1

        listeners[id] = listener

        println("Подписчик добавлен id: $id Всего подписчиков: ${listeners.size}")
        return id
    }

    fun unsubscribe(id: Int){
        val removed = listeners.remove(id)

        if (removed != null){
            println("слушатель уудален. id: $id")
        }else{
            println("не удалось подписаться, не найден id: $id")
        }
    }

    fun subscribeOnce(listener: Listener): Int {
        //одноразовая подписка на событие
        //слушатель сам отпишется после первого полученного события

        var id: Int = -1
        // временная переменная для id
        id = subscribe { event ->
            listener(event)
            unsubscribe(id)
            //отреагировали и сразу отписались

        }

        return id
    }

    fun publish(event: GameEvent){
        //сразу пибликует и выполняет событие мнгновенно вызывая подписчиков
        println("Событие опубликованно: $event")
        for (listener in listeners.values){
            listener(event)
        }
    }

    fun post(event: GameEvent){
        //post - отложить событие в очередь выполнения (выполниться не сразу)
        eventQueue.addLast(event)
        //addLast - добавление в конец очереди
        println("событие добавленно в конец очереди (в очереди: ${eventQueue.size})")
    }

    fun processQueue(maxEvent: Int = 10){
        var processed = 0

        while (processed < maxEvent && eventQueue.isNotEmpty()){
            val event = eventQueue.removeFirst()
            //достает и удаляет первый элемент из очереди

            publish(event)

            processed++
        }
    }
}