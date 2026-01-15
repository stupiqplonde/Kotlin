package lesson9
import lesson8.DifficultyType
import lesson8.Item
import lesson8.ItemType
import kotlin.random.Random
import kotlin.random.Random.Default.nextDouble
import kotlin.random.Random.Default.nextInt

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
            println("Фу! Казуальщина (+10 ко всему), у врага -2 атака")
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

fun main() {
    println("старт")

    val difficulty = selectDifficulty()

    val player = Player(
        x = 0.0,
        y = 0.0,
        speed = 2.0,
        name = "Oleg",
        maxHealth = 100,
        baseAttack = Random.nextInt(10, 41),
        criticalChance = 20
    )

    val enemyCount = nextInt(2, 4)
    val enemies = mutableListOf<Enemy>()

    for (i in 1..enemyCount) {
        val xEnemy = nextDouble(25.0, 100.0)
        val attackE = nextInt(5, 20)
        val healthE = nextInt(40, 90)
        val enemy = Enemy(
            xEnemy,
            0.0,
            0.0,
            1,
            "Враг-$i",
            healthE,
            attackE,
        )
        enemies.add(enemy)
        applyDifficultyBonus(difficulty, enemy, player)
    }

    println("\nИгрок создан: ${player.name}")
    println("Урон игрока: ${player.baseAttack}")
    println("Количество врагов: $enemyCount")


    val possibleItems = listOf(
        Item(1, "Sword", "Простой как палец", 50, ItemType.WEAPON, 15, 0, 0),
        Item(2, "Шлем", "Защита головы", 30, ItemType.ARMOR, 0, 10, 0),
        Item(3, "Пиво", "Восстанавливает 20 HP", 20, ItemType.CONSUMABLE, 0, 0, 20),
    )

    val collectedItems = mutableListOf<Item>()
    val gameTime = GameTime()
    var currentEnemyIndex = 0
    var gameOver = false
    var victory = false

    println("Цель: достичь финиша на координате x = 110.0")

    while (!gameOver && player.x < 110.0) {
        gameTime.update()
        val dt = gameTime.deltaTimeSeconds

        player.update(dt)

        // Проверка на столкновение
        if (currentEnemyIndex < enemies.size) {
            val currentEnemy = enemies[currentEnemyIndex]

            val distance = (currentEnemy.x - player.x)

            if (distance <= 1.0) {
                println("\nвстреча с врагом")
                println("Игрок встретил ${currentEnemy.name} на позиции x = ${"%.1f".format(currentEnemy.x)}")

                val fightResult = fight(player, currentEnemy)

                if (!fightResult) {
                    gameOver = true
                    println("\nGame Over")
                    println("Игрок погиб в бою с ${currentEnemy.name}")
                    break
                } else {
                    // Игрок победил
                    println("\n${currentEnemy.name} повержен!")
                    currentEnemyIndex++

                }
            }
        }


        if (nextInt(100) < 30 && collectedItems.size < 4) {
            val randomItem = possibleItems.random()
            if (!collectedItems.contains(randomItem)) {
                collectedItems.add(randomItem)
                player.inventory.addItem(randomItem)
                println("\nы нашли предмет: ${randomItem.name}")
                println("инвентарь: ${player.inventory.getAllItems().size} предмет(ов)")
            }
        }


        if (currentEnemyIndex < enemies.size) {
            val nextEnemy = enemies[currentEnemyIndex]
            val distanceToEnemy = nextEnemy.x - player.x
            println("враг в ${"%.1f".format(distanceToEnemy)} метрах от вас")
        } else {
            println("все враги побеждены!")
        }

        println("до финиша: ${"%.1f".format(110.0 - player.x)} метров")

        Thread.sleep(500)

        if (player.x >= 110.0) {
            victory = true
            gameOver = true
        }
    }


    if (victory) {
        println("WIN")
        println("Игрок ${player.name} достиг финиша!")
    } else if (!player.isAlive()) {
        println("LOSE")
        println("Игрок ${player.name} погиб в пути")
    }

    println("\n=== статистика ===")
    println("Пройденое расстояние: ${"%.1f".format(player.x)}")
    println("Осталось здоровья: ${player.currentHealth}")
    println("Побеждено врагов: $currentEnemyIndex из $enemyCount")
    println("Собрано предметов: ${collectedItems.size}")
    println("Сложность: $difficulty")
}

fun fight(player: Player, enemy: Enemy): Boolean {
    println("\nбой")

    var round = 1

    while (player.isAlive() && enemy.isAlive()) {
        println("\nраунд $round")
        println("${player.name}: ${player.currentHealth}/${player.maxHealth} HP")
        println("${enemy.name}: ${enemy.currentHealth}/${enemy.maxHealth} HP")

        println("\nВаш ход:")
        println("1. Атаковать")
        println("2. Использовать предмет")
        println("3. Проверить статус")
        println("4. Посмотреть инвентарь")

        print("Выберите действие (1-4): ")
        val choice = readLine()?.toIntOrNull() ?: 1

        when (choice) {
            1 -> {
                player.attack(enemy)
            }
            2 -> {
                val potions = player.inventory.getAllItems().filter { it.type == ItemType.CONSUMABLE }

                if (potions.isEmpty()) {
                    println("У вас нет зелий!")
                } else {
                    println("\nДоступные зелья:")
                    potions.forEachIndexed { index, item ->
                        println("${index + 1}. ${item.name} (+${item.healAmount} HP)")
                    }

                    print("Выберите зелье: ")
                    val potionChoice = readLine()?.toIntOrNull()

                    if (potionChoice != null && potionChoice in 1..potions.size) {
                        val selectedPotion = potions[potionChoice - 1]
                        player.useConsumable(selectedPotion)
                    } else {
                        println("Неверный выбор, пропускаем ход!")
                    }
                }
            }
            3 -> {
                player.printStatus()
                enemy.printStatus()
                continue
            }
            4 -> {
                println("\nинвентарь:")
                player.inventory.getAllItems().forEachIndexed { index, item ->
                    println("${index + 1}. ${item.name} [${item.type}]")
                }
                continue
            }
            else -> {
                println("Неверный выбор! Автоматическая атака")
                player.attack(enemy)
            }
        }

        if (enemy.isAlive()) {
            println("\nХод врага:")
            enemy.attack(player)
        }

        round++
        Thread.sleep(500) // Пауза между раундами
    }

    return player.isAlive()
}