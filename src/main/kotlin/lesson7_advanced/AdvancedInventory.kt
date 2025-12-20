package lesson7_advanced

import java.time.temporal.TemporalAmount

class Inventory(
    private val maxSlots: Int// макс число слотов в инвентаре
){
    //внутренний список слотов в инвентаре
    val slots: MutableList<InventorySlot> = mutableListOf()
    // private - модификатор доступа, означает, что никто снаружи не сможет изменять список слотов

    fun addItem(item: Item, amount: Int = 1){
        if (amount <= 0){
            println("Нельзя положить отрицательное число предметов $amount")
        return // Экстренная точка выхода из метода
        }

        var remaining = amount

        // Попытка положить в уже сущесчтвубщие слоты инвентаря с таким же предметом
        for (slot in slots) {
            // перебор слотов в инвентаре
            if (slot.item.id == item.id){

                val freeSpace = item.maxStackSize - slot.quantity
                // сколько предметов можно положить в слот с таким же предметом
                // maxStackSize - максимальное допустимое число стака
                //quantity - сколько уже лежит

                if (freeSpace > 0){
                    val toAdd = minOf(remaining, b = freeSpace)
                    // minOf(a, b) - берем мин из двух предложенных чисел
                    // remainind - сколько осталось добавить
                    // freeSpace - сколько можно положить

                    slot.quantity += toAdd
                    // уменьшаем остаток, числа предметов которых можно положить в ячейку

                    println("Добавленно $toAdd предмета ${item.name} в сущ слот. Теперь стак: ${slot.quantity} ///")

                    if (remaining == 0){
                        // Если все предметы добавленны в одну ячейку и нет остатков, то выходим из функции
                        return
                    }
                }
            }
        }

        // если еще остались предметы которые не поместились в ячейку то пробуем положить в новые слоты
        while (remaining > 0){
            if (slots.size >= maxSlots){
                println("Инвентарь переполнен не удалось положить $remaining ${item.name}///")
                return
            }

            val toAdd = minOf(remaining, item.maxStackSize)
            // в новый слот кладем либо все что осталось
            // Либо максимум что может влезть в один стак

            val newSlot = InventorySlot(item, toAdd)
            // создаем новый сллот инвентаря с нашим пердметом и оставшимся количеством toAdd

            slots.add(newSlot)

            println("Создан новыйслот инвентаря для ${item.name} с количеством $toAdd")

            remaining-= toAdd
        }




    }
    fun removeItem(item: Item, amount: Int = 1): Boolean{
        if (amount <= 0){
            println("Не удалось удалить $amount предметов")
            return false
        }

        var remaining = amount
        //сколько нужно удалить предметов
        //перебор всех слотов с конца
        for (i in slots.size - 1 downTo 0){
            //downTo - считаем вниз по индексам до 0 включительно

            val slot = slots[i]

            if (slot.item.id == item.id){
                if(slot.quantity <= remaining){
                    // в слоте меньше или равно тому числу предметов которое нужно удалить
                    remaining -= slot.quantity
                    println("Удален слот с предметом ${item.name}, количество: ${slot.quantity}")
                    slots.removeAt(i)
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
        println("Не удалось уалить $amount ${item.name} - не хватает в инвентаре")
        return false
    }

    fun printInventory(){
        if (slots.isEmpty()){
            println("Инвентарь пуст")
            return
        }
        println(" +++ ИНВЕНТАРЬ (слотов: ${slots.size} / $maxSlots) +++")

        for((index, slot) in slots.withIndex()){
            //withIndex() - метод возвращающий пары (индекс, значение)
            // index - номер слота
            // slot - сам InventorySlot
            println("Слот ${index + 1}: ${slot.item.name}" + "| тип=${slot.item.type}" + "| кол-во=${slot.item.maxStackSize}")
            // index + 1 - для отображения перечисления не с 0, а с 1
        }


    }
}