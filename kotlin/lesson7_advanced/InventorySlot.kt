package lesson7_advanced

data class InventorySlot(
    val item: Item,
    var quantity: Int
)

// var - т.к. кол-во предметов изменяемое