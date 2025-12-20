package lesson8
import kotlin.random.Random

class Enemy(
    val name: String,
    val maxHealth: Int,
    var baseAttack: Int,
    var hitChance: Int = 80
){
    var currentHealth: Int = maxHealth
    fun takeDamage(amount: Int) {
        currentHealth -= amount
        println("$name получил $amount урона. Осталось здоровья: $currentHealth")

        if (!isAlive()) {
            println("$name повержен!")
        }
    }

    fun isAlive(): Boolean = currentHealth > 0

    fun attack(player: Player) {
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