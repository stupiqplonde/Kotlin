package lesson9
import lesson8.Inventory
import lesson8.Item
import lesson8.ItemType
import kotlin.random.Random

open class GameObject(
    // open - ключевое слово которое позволяет наследовать класс

    var x: Double, // x - позиционирование объекта по оси х
    var y: Double,


    var speed: Double // скорость перемещения объекта
){
    open fun update(deltaTimeMillis: Double){
        // open fun - метод который можно переопределять в наслединиках

        x += speed * deltaTimeMillis
//        y += speed * deltaTimeMillis
        // счет количества единиц ч за deltsTimeMillis
        // Пример:
        // speed = 2.0 (2 юнита в секунду), delta = 0.5 сек
        // d = 2.0 * 0.5 = 1.0 (1 юнит за пол секунды)
    }

}

//class Player(
//    x: Double,
//    y: Double, // x и speed наследуется у родителя
//    speed: Double,
//    val name: String
//) : GameObject(x, y, speed){    // <- наследование от родительского класса
//    fun printPosition(){
//        println("игрок $name находится по x = $x, находится по y = $y")
//    }
//}
class Player(
    x: Double,
    y: Double, // x и speed наследуется у родителя
    speed: Double,
    val name: String,
    var maxHealth: Int,
    var baseAttack: Int,
    var criticalChance: Int = 20,

): GameObject(x, y, speed){    // <- наследование от родительского класса
    fun printPosition(){
        println("игрок $name находится по x = $x, находится по y = $y")
    }

    fun getPosition(): Double{
        return x
    }

    var currentHealth: Int = maxHealth
    //var - динамически изменяемый показатель HP

    val inventory: Inventory = Inventory()
    // Создание инвентаря игроку

    var equippedWeapon: Item? = null
    // экипировка оружия
    var equippedArmor: Item? = null

    fun printStatus(){
        println("=== статус игрока: $name ===")
        println("HP: $currentHealth / $maxHealth")
        println("Damage: $baseAttack")

        if (equippedWeapon != null){
            println("Сейчас экипирован $equippedWeapon")
        }else{
            println("Вы без оружия")
        }

        if (equippedArmor != null){
            println("Сейчас экипирован $equippedArmor")
        }else{
            println("Пока вы с голой жопэ")
        }
    }

    fun equip(item: Item) {
        // Попытка экипировать предмет (оружие или броню)
        when (item.type) {
            // when - оператор выбора по значению (как switch)
            ItemType.WEAPON -> {
                // Если предмет - оружие
                equippedWeapon = item
                println("$name экипировал оружие: ${item.name}\n" +
                        "+ ${item.damageBonus} к урону")
                baseAttack += item.damageBonus
            }
            ItemType.ARMOR -> {
                // Если предмет - броня
                equippedArmor = item
                println("$name экипировал броню: ${item.name}\n" +
                        "+ ${item.defenseBonus} к броне")

            }
            ItemType.CONSUMABLE -> {
                // Если это расходник, экипировать нельзя
                println("Нельзя экипировать расходуемый предмет: ${item.name}\n" +
                        "восстановлено ${item.healAmount} здоровья")
                currentHealth += item.healAmount
            }
        }


    }

    fun takeDamage(amount: Int){
        currentHealth -= amount
    }

    fun isAlive(): Boolean{
        return currentHealth > 0
    }

    fun attack(enemy: lesson9.Enemy){
        enemy.currentHealth -= baseAttack
        val isCritical = Random.nextInt(100) < criticalChance
        if (isCritical) {
            baseAttack *= 2
            println("КРИТИЧЕСКИЙ УДАР!")
        }

        enemy.takeDamage(baseAttack)
        println("$name атаковал врага и нанёс $baseAttack урона" +
                if (isCritical) "(критический удар)!" else "!")
        return
    }

    fun useConsumable(item: Item){
        if (item.type != ItemType.CONSUMABLE){
            println("FAULT")
            return
        }
        if (!isAlive()) {
            println("$name мёртв и не может использовать предметы!")
            return
        }
        val hp = currentHealth
        currentHealth += item.healAmount
        if (currentHealth > maxHealth) currentHealth = maxHealth

        println("$name использовал ${item.name} и восстановил ${currentHealth - hp} HP")
        println("Текущее здоровье: $currentHealth/$maxHealth")

        inventory.removeItem(item)
    }
}

//class Enemy(
//    x: Double, // x и speed наследуется у родителя
//    y: Double,
//    speed: Double,
//    val id: Int
//) : GameObject(x, y, speed){    // <- наследование от родительского класса
//    fun printPosition(){
//        println("юнит $id находится по x = $x, находится по y = $y")
//    }
//}
class Enemy(
    x: Double, // x и speed наследуется у родителя
    y: Double,
    speed: Double,
    val id: Int,
    val name: String,
    val maxHealth: Int,
    var baseAttack: Int,
    var hitChance: Int = 80
): GameObject(x, y, speed){    // <- наследование от родительского класса
    fun printPosition(){
        println("юнит $id находится по x = $x, находится по y = $y")
    }
    var currentHealth: Int = maxHealth
    fun takeDamage(amount: Int) {
        currentHealth -= amount
        println("$name получил $amount урона. Осталось здоровья: $currentHealth")

        if (!isAlive()) {
            println("$name повержен!")
        }
    }

    fun isAlive(): Boolean = currentHealth > 0

    fun attack(player: lesson9.Player) {
        if (!isAlive()) return

        if (Random.nextInt(100) >= hitChance) {
            println("$name промахнулся! (шанс попадания: $hitChance%)")
            return
        }

        player.takeDamage(baseAttack)
    }

    fun printStatus(){
        println("=== статус энеми: $name ===")
        println("HP: $currentHealth / $maxHealth")
        println("Damage: $baseAttack")
    }
}
