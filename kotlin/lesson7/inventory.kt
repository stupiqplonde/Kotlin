package lesson7

class Inventory{
    // ВТОРИЧНЫЙ КОНСТРУКТОР

    //Список пердметов инвентаря
    private val items: MutableList<Item> = mutableListOf()
    val maxWeight: Int = 50 // Максимальный допустимый вес
    // private - модификатор доступа это поле видно только внутри класса Inventory и его объектов
    // <Item> - тип данных который принимает список (т.е. положит в него можем только классы объекта Item)

    fun getCurrentWeight(): Int {
        return items.sumOf { it.weight }
    }

    // Добавить предмет с проверкой веса
    fun addItem(item: Item) {
        val totalWeight = getCurrentWeight() + item.weight
        if (totalWeight <= maxWeight) {
            items.add(item)
            println("${item.name} добавлен в инвентарь")
        } else {
            println("Нельзя добавить предмет '${item.name}': превышен лимит веса!")
            println("Текущий вес: ${getCurrentWeight()}/${maxWeight}, вес предмета: ${item.weight}")
        }
    }

    fun removeItem(item: Item): Boolean {
        //(): Boolean - вернет метод после своего выполнения
        val removed = items.remove(item)
        // .remove удаление ключевого искомого элемента списка - возвращает true/false

        if (removed){
            println("Предмет ${item.name} удален из инвентаря")
        }else{
            println("Не удалось удалить предмет ${item.name}")
        }

        return removed
    }

    fun printInventory(){
        if(items.isEmpty()){
            println("Инвентарь пуст")
            return // досрочный выход из метода (код ниже не будет выполнен)
        }
        println("=== ИНВЕНТАРЬ ===")
        println("Общий вес: ${getCurrentWeight()}/${maxWeight}")
        for (item in items){
            // для каждого предмета в коллекции items
            // item - переменная временног цикла в нее по очереди будет временно класться каждый элемент списка
            println(" ${item.name} | id: ${item.id} | price: ${item.price} | weight: ${item.weight}")
        }
    }
}