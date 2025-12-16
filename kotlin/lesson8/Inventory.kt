package lesson8

class Inventory {
    private val items: MutableList<Item> = mutableListOf()

    fun addItem(item: Item){
        items.add(item)
        println("В инвентарь добавлен ${item.name} ")
    }

    fun removeItem(item: Item): Boolean{
        val removed = items.remove(item)
        // remove - метод списков который удаляет 1 вхождение в спиок [1,2,3,4,5] - удалится только 1
        // remove в случае успеха вернет true
        if (removed) {
            // removed в условии без сравнения == "if removed is true"
            // то же самое что и: removed == true
            println("${item.name} УДАЛЕН из инвентаря")
        }else{
            println("! не удалось удалить ${item.name}")
        }
        return removed
    }

    fun printInventory(){
        if (items.isEmpty()){
            println("Инвентарь пуст")
            return
        }

        println("=== инвентарь ===")

        for((index, item) in items.withIndex()){
            //withIndex - получает не просто объект из класса, но и индекс, эти значения сохраняются во временную переменную
            println("${index + 1}. ${item.name} [${item.type}]")
        }
    }

    fun findItemByName(name: String): Item? {
        //?- это символ возвращающий нул если объект не найден (если нечего возвращать)
        for (item in items){
            if (item.name == name){
                // проверя\ем любое существование в списке предмета который соответствует поиску
                // Если предметов несколько то выведется первый из них
                return item
            }
            //Если предмет который не найден вернем null что будет для нас сигналом

        }
        return null
    }



    fun getAllItems(): List<Item>{
        //возвращение списка только для чтения
        // List<Item> - интерфейс означающий только для чтения

        return items.toList()
        // toList() создает новый List с теми же элементами
        // то есть из-за private внешний код не может повлиять на список items
    }
}