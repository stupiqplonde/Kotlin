package lesson10

import kotlinx.coroutines.*

fun main() = runBlocking {
    // runBlocking - функция, которая запускает корутину и блокирует текущий поток
    // до тех пор, пока все внутри не завершится
    // чем удобно: игра не завершится пока мы не закончим ее логику

    val player = GameCharacter(
        "Oleg",
        70,
        30,
        8
    )

    val enemy = GameCharacter(
        "Pozornik",
        50,
        0,
        5
    )

    println("Начальный статус персов:")
    player.printStatus()
    enemy.printStatus()

    println("\n [~] Запуск регена маны игрока (3 за 2 сек)")
    player.startManaRegeneration(
        3,
        2000L
    )

    println("\n${enemy.name} наложил эффект яда на игрока ${player.name}")
    player.applyPosionEffect(
        2,
        5,
        1000L
    )

    println("на ${player.name} наложен бафф урона + 10")
    player.applyAttackBuff(
        10,
        20,
        1000L
    )
    player.printStatus()
    println("[~] запуск боя")

    val attackJob = launch {
        //launch - запуск корутины внутри runBlock

        repeat(6){ attackNumber ->
            // repeat(кол-во) - повторениу блока нужное кол-во раз

            delay(2000L)
            // ожидание 2 сек перед каждой атакой

            println("Попытка атаки ${attackNumber + 1}")
            player.attack(enemy)

            if (enemy.currentHealth <= 0){
                println("[~] ${enemy.name} побежден")
                return@launch
                // выход из корутины attackJob
                // если победа - прервать бой
            }
        }
    }
    // ждем либо пока закончится атака либо мониторинг
    // + даем доп время яду и регену отработать
    // ждем фикс время
    delay(10000L)
    // усыпляем главную корутину на 10 сек
    // тем самым даем проработать другим корутинам на фоне

    // останавливаем реген маны и эффект яда если они работают
    player.stopManaRegeneration()
    player.clearPoison()

    // на всякий случай если корутины атаки и мониторинга работают - останавливаем вручную
    // это делается для отчистки мусора из памяти
    attackJob.cancel()

    println("[!] завершение всех корутин")

    // runBlocking в функции main делает main корутиной
    // те пока код внутри main не закончит работу (корутины)
    // main не завершится
    // благоворя runBlocking мы можем безопасно использовать launch, delay и др функции
}
