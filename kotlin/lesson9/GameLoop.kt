package lesson9
import lesson8.DifficultyType
import lesson8.Item
import lesson8.ItemType
import lesson8.selectDifficulty
import kotlin.collections.get
import kotlin.inc
import kotlin.math.pow
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

fun applyDifficultyBonus(difficulty: DifficultyType, enemy: lesson9.Enemy, player: lesson9.Player) {
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
    val gameTime = GameTime()
    // создание игрового времени (считает время и дельту)
    val attackP = nextInt(10, 40)
    val attackE = nextInt(5, 20)
    val healthE = nextInt(40, 90)

    val player = Player(
        0.0,
        0.0,
        4.0,
        "Oleg",
        100,
        attackP,
    )
    val xEnemy = nextDouble(25.0, 100.0)
    val enemy = Enemy(
        xEnemy,
        0.0,
        0.0,
        1,
        "petya",
        healthE,
        attackE,
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

    val difficulty = selectDifficulty()
    applyDifficultyBonus(difficulty, enemy, player)


    println("запуск уровня")
    var x : Double = player.getPosition()
    while (x != 110.0) {
        gameTime.update()
        // обновляем время: deltaTimeSeconds and totalTimeSeconds

        val dt = gameTime.deltaTimeSeconds
        // локальная переменная считающая дельту

        player.update(dt)
        enemy.update(dt)
        // Обновляем позиции объектов по времени прошедших за кадр
        // тКаждая итерация while = 1 игровой кадр

//        println("Прошло времени: ${"%.3f".format(gameTime.totalTimeSeconds)} сек")
        // форматирование числа - число с тремя знаками после запятой


        val distance_x = enemy.x - player.x
        val distance_y = enemy.y - player.y
        val distance = (distance_x.pow(2) + distance_y.pow(2)).pow(0.5)
        println("расстояние между игроком и врагом: ${"%.3f".format(distance)}")
        player.printPosition()
        enemy.printPosition()
        fun fight(){
            while (player.isAlive() && enemy.isAlive()) {
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

                if (enemy.isAlive()) {
                    enemy.attack(player)
                }

                round++

                Thread.sleep(800)
            }

            println("конец сражения")
            when {
                player.isAlive() && !enemy.isAlive() -> {
                    println("ПОБЕДА! ${player.name} победил ${enemy.name}")

                }

                !player.isAlive() && enemy.isAlive() -> {
                    println("ПОРАЖЕНИЕ${enemy.name} победил ${player.name}")
                    println("Сложность: $difficulty")
                }
            }
        }
        if (distance_x <= 0.0 && distance_y <= 0.0) {
            fight()
            println(player.printStatus())


        }

        Thread.sleep(200)
        //Thead - класс для работы с потоками
        //sleep - метод полностью приостанавливает поток


    }
}