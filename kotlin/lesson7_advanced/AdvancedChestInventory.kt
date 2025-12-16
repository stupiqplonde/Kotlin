package lesson7_advanced


class ChestInventory(
    private val maxSlots: Int// макс число слотов в сундуке
){
    //внутренний список слотов в инвентаре
    val chestSlots: MutableList<InventorySlot> = mutableListOf()

    fun addItemToChest(item: Item, amount: Int = 1){
        if (amount <= 0){
            println("Нельзя положить отрицательное число предметов $amount в сундук")
            return // Экстренная точка выхода из метода
        }

        var remaining = amount

        // Попытка положить в уже сущесчтвубщие слоты сундука с таким же предметом
        for (slot in chestSlots) {
            // перебор слотов в сундуке
            if (slot.item.id == item.id){

                val freeSpace = item.maxStackSize - slot.quantity
                // сколько предметов можно положить в слот с таким же предметом
                // maxStackSize - максимальное допустимое число стака
                //quantity - сколько уже лежит

                if (freeSpace > 0){
                    val toAdd = minOf(remaining, freeSpace)
                    // minOf(a, b) - берем мин из двух предложенных чисел
                    // remainind - сколько осталось добавить
                    // freeSpace - сколько можно положить

                    slot.quantity += toAdd
                    // уменьшаем остаток, числа предметов которых можно положить в ячейку

                    println("Добавленно $toAdd предмета ${item.name} в сущ слот. Теперь стак: ${slot.quantity}")

                    if (remaining == 0){
                        // Если все предметы добавленны в одну ячейку и нет остатков, то выходим из функции
                        return
                    }
                }
            }
        }

        // если еще остались предметы которые не поместились в ячейку то пробуем положить в новые слоты
        while (remaining > 0){
            if (chestSlots.size >= maxSlots){
                println("Сундук переполнен не удалось положить $remaining ${item.name}")
                return
            }

            val toAdd = minOf(remaining, item.maxStackSize)
            // в новый слот кладем либо все что осталось
            // Либо максимум что может влезть в один стак

            val newSlot = InventorySlot(item, toAdd)
            // создаем новый сллот сундука с нашим пердметом и оставшимся количеством toAdd

            chestSlots.add(newSlot)

            println("Создан новый слот сундука для ${item.name} с количеством $toAdd")

            remaining-= toAdd
        }




    }
    fun removeItemFromChest(item: Item, amount: Int = 1): Boolean{
        if (amount <= 0){
            println("Не удалось удалить $amount предметов")
            return false
        }

        var remaining = amount
        //сколько нужно удалить предметов
        //перебор всех слотов с конца
        for (i in chestSlots.size - 1 downTo 0){
            //downTo - считаем вниз по индексам до 0 включительно

            val slot = chestSlots[i]

            if (slot.item.id == item.id){
                if(slot.quantity <= remaining){
                    // в слоте меньше или равно тому числу предметов которое нужно удалить
                    remaining -= slot.quantity
                    println("Удален слот с предметом ${item.name}, количество: ${slot.quantity}")
                    chestSlots.removeAt(i)
                    // removeAt(i) - удаление из списка по индексу (те полное очищение слота инвентаря)

                    if (remaining == 0){
                        return true
                    }
                }else{
                    //если в слоте больше предметов чем надо удалить
                    slot.quantity -= remaining
                    println("уменьшено количество ${item.name} в слоте $remaining")
                    return true
                }
            }
        }
        println("Не удалось удалить $amount ${item.name} - не хватает в сундуке")
        return false
    }

    fun printChestInventory(){
        if (chestSlots.isEmpty()){
            println("Сундук пуст")
            return
        }
        println(" +++ СУНДУК (слотов: ${chestSlots.size} / $maxSlots) +++")

        for((index, slot) in chestSlots.withIndex()){
            // withIndex() - метод возвращающий пары (индекс, значение)
            // index - номер слота
            // slot - сам InventorySlot
            println("Слот ${index + 1}: ${slot.item.name}" + "| тип=${slot.item.type}" + "| кол-во=${slot.item.maxStackSize}")
            // index + 1 - для отображения перечисления не с 0, а с 1
        }


    }
    fun lootTo(player: Player) {
        if (chestSlots.isEmpty()) {
            println("Сундук пуст, нечего забирать")
            return
        }

        println("Забираем все предметы из сундука...")

        // Копируем слоты чтобы избежать ConcurrentModificationException
        val slotsToLoot = chestSlots.toList()

        // Перекладываем каждый слот в инвентарь игрока
        for (slot in slotsToLoot) {
            player.inventory.addItem(slot.item, slot.quantity)
        }

        // Очищаем сундук
        chestSlots.clear()

        println("Сундук очищен, все предметы перенесены в инвентарь игрока")
    }
}