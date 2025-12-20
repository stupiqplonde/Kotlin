package lesson8
import kotlin.random.Random

class Player(
    val name: String,
    var maxHealth: Int,
    var baseAttack: Int,
    var criticalChance: Int = 20
){
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
            println("Пока вы с голой жопэ")
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

    fun attack(enemy: Enemy){
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




