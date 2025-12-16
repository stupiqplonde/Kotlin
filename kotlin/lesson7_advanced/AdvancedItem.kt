package lesson7_advanced

import javax.management.Descriptor

data class Item(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val type: ItemType,     //тип предмета
    val maxStackSize: Int   //макс число предметов в ячейке
)