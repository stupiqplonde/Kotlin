package lesson7_advanced

class Player(
    val name: String,
    val inventory: Inventory
)

class Chest(
    val inventory: ChestInventory
)

fun main(){
    val inventory = Inventory(maxSlots = 5)
    val chestInventory = ChestInventory(maxSlots = 3)

    val chest = Chest(
        inventory = chestInventory
    )
    val player = Player(
        name = "Oleg",
        inventory = inventory
    )

    val sword  = Item(
        id = 1,
        name = "Sword",
        description = "Простой как палец",
        price = 50,
        type = ItemType.WEAPON,
        maxStackSize = 1
    )

    val helmet  = Item(
        id = 2,
        name = "Шлем",
        description = "Защита головы",
        price = 30,
        type = ItemType.ARMOR,
        maxStackSize = 1
    )

    val beer = Item(
        3,
        "Пиво",
        "Восстанавливает 20 HP",
        20,
        ItemType.CONSUMBLE,
        5
    )
    chest.inventory.addItemToChest(sword)
    player.inventory.addItem(sword)
    player.inventory.addItem(sword)
    player.inventory.addItem(sword)

    player.inventory.addItem(helmet)
    player.inventory.addItem(beer)

    player.inventory.printInventory()

    player.inventory.addItem(beer, 5)
    player.inventory.printInventory()
    player.inventory.removeItem(beer, 2)

    player.inventory.printInventory()
    chest.inventory.lootTo(player)
    player.inventory.printInventory()
    chest.inventory.addItemToChest(beer)
    chest.inventory.addItemToChest(sword)
    chest.inventory.addItemToChest(sword)
    chest.inventory.printChestInventory()
    chest.inventory.lootTo(player)
    chest.inventory.printChestInventory()
    player.inventory.printInventory()
    chest.inventory.printChestInventory()



}