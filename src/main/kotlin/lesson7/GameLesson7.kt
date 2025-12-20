package lesson7

class Player(
    val name: String
){
    val inventory = Inventory()
}

// Основная функция запуска программы точка входа и команд компилятору
// он запустит то что мы в нее запишем
fun main(){

    val sword = Item(
        id = 1,
        name = "Меч",
        description = "простой и не надежный",
        price = 50,
        weight = 3
    )

    val potion = Item(
        id = 2,
        name = "Пиво",
        description =  "Вкусное хмельное с пенкой",
        price = 25,
        weight = 1

    )

    val player = Player("Oleg")

    println("Игрок ${player.name} вошел в игру")

    player.inventory.addItem(sword)
    player.inventory.addItem(potion)

    println("содержимое инвентаря")
    player.inventory.printInventory()



}