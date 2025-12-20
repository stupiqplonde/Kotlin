package lesson8

enum class ItemType{
    WEAPON,
    ARMOR,
    CONSUMABLE
}

data class Item(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val type: ItemType,
    val damageBonus: Int = 0,
    val defenseBonus: Int = 0,
    val healAmount: Int = 0
)