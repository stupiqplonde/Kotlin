package lesson8

import lesson8.Player
import lesson8.Enemy
import lesson8.Inventory
import lesson8.DifficultyType
import lesson9.GameObject

fun selectDifficulty(): DifficultyType {
    println("\n=== ВЫБЕРИТЕ СЛОЖНОСТЬ ===")
    println("1. Лёгкая (Easy)")
    println("2. Обычная (Normal)")
    println("3. Сложная (Hard)")

    while (true) {
        print("Ваш выбор (1-3): ")
        val choice = readLine()?.toIntOrNull()

        return when (choice) {
            1 -> {
                println("Выбрана ЛЁГКАЯ сложность")
                DifficultyType.EASY

            }
            2 -> {
                println("Выбрана ОБЫЧНАЯ сложность")
                DifficultyType.NORMAL
            }
            3 -> {
                println("Выбрана СЛОЖНАЯ сложность")
                DifficultyType.HARD
            }
            else -> {
                println("Неверный выбор. Попробуйте снова.")
                continue
            }

        }
    }
}

fun applyDifficultyBonus(difficulty: DifficultyType, enemy: Enemy, player: Player) {
    when (difficulty) {
        DifficultyType.EASY -> {
            player.maxHealth += 10
            player.currentHealth += 10
            player.baseAttack += 10
            enemy.baseAttack -= 2
            println("фу! казуальщина (+10 ко всему), у врага -2 атака")
        }
        DifficultyType.NORMAL -> {
            println("Обычная сложность")
        }
        DifficultyType.HARD -> {
            player.maxHealth -= 5
            player.currentHealth -= 5
            player.baseAttack -= 5
            enemy.baseAttack += 2
            enemy.hitChance = 50
            println("На сложной сложности враг получает +2 к атаке и 50% шанс попадания!")
        }
    }
}

fun main(){
    var round = 1
    val player = Player(
        "Oleg",
        100,
        25

    )
    val enemy = Enemy(
        "Sid",
        110,
        20
    )

    val sword  = Item(
        1,
        "Sword",
        "Простой как палец",
        50,
        lesson8.ItemType.WEAPON,
        15,
        0,
        0

    )

    val helmet  = Item(
        2,
        "Шлем",
        "Защита головы",
        30,
        lesson8.ItemType.ARMOR,
        0,
        10,
        0


    )

    val beer = Item(
        3,
        "Пиво",
        "Восстанавливает 20 HP",
        20,
        lesson8.ItemType.WEAPON,
        0,
        0,
        20
    )

    println("=== сво ===")

    player.inventory.addItem(sword)
    player.inventory.addItem(beer)
    val difficulty = selectDifficulty()
    applyDifficultyBonus(difficulty, enemy, player)


    while (player.isAlive() && enemy.isAlive()) {
        println("\n" + "=".repeat(30))
        println("   Раунд $round  ")
        println("${player.name}: ${player.currentHealth} HP")
        println("${enemy.name}: ${enemy.currentHealth} HP")
        println("=".repeat(30))

        // Принимать выбор игрока
        println("\nВыберите действие:")
        println("1. Атака")
        println("2. Выпить зелье")
        println("3. Проверить статус")
        println("4. Инвентарь")

        print("Ваш выбор (1-4): ")
        val choice = readLine()?.toIntOrNull() ?: 1

        when (choice) {
            1 -> {
                player.attack(enemy)
            }
            2 -> {
                println("\nКакое зелье использовать")
                val potions = player.inventory.getAllItems().filter { it.type == ItemType.CONSUMABLE }

                if (potions.isEmpty()) {
                    println("У вас нет зелий")
                    continue
                }

                potions.forEachIndexed { index, potion ->
                    println("${index + 1}. ${beer.name} (+${beer.healAmount} HP)")
                }

                print("Выберите зелье: ")
                val potionChoice = readLine()?.toIntOrNull() ?: 1

                if (potionChoice in 1..potions.size) {
                    player.useConsumable(potions[potionChoice - 1])
                } else {
                    println("Неверно")
                }
            }
            3 -> {
                println(player.inventory.getAllItems())
                continue
            }
            4 -> {
                println("\n ИНВЕНТАРЬ ")
                player.inventory.getAllItems().forEachIndexed { index, item ->
                    println("${index + 1}. ${item.name} [${item.type}]")
                }
                continue
            }
            else -> {
                println("Неверный выбор! Автоматическая атака.")
                player.attack(enemy)
            }
        }

        // 7. Каждый цикл враг нас атакует (если жив)
        if (enemy.isAlive()) {
            enemy.attack(player)
        }

        round++

        Thread.sleep(800)
    }

    println("\n" + "=".repeat(40))
    println("конец сражения")
    when {
        player.isAlive() && !enemy.isAlive() -> {
            println("ПОБЕДА! ${player.name} победил ${enemy.name}")
            println("Сложность: $difficulty")
            println("Кол-во раундов: ${round - 1}")
        }
        !player.isAlive() && enemy.isAlive() -> {
            println("ПОРАЖЕНИЕ${enemy.name} победил ${player.name}")
            println("Сложность: $difficulty")
        }
        else -> println("ничья")
    }
    println("=".repeat(40))

    println("\nфинальный статус игрока:")
    println(player.printStatus())
}

