package lesson10

import kotlinx.coroutines.*
// * - достает всё из библиотеки
// Подключение корутин типа: launch, delay, job и тд.

class GameCharacter(
    val name: String,
    val maxHealth: Int,
    val maxMana: Int,
    val baseAttack: Int
) {
    var currentHealth: Int = maxHealth

    var currentMana: Int = maxMana

    var canAttack: Boolean = true
    // флаг - точка опоры проверки состояния

    // хранение ссылки на корутину, для взаимодействия с ней в любое время
    private var manaRegenJob: Job? = null
    // Job? - "работа", на нее можно ссылаться, запускать, отменять
    // ? - возможность вернуть null

    private var potionJob: Job? = null

    fun printStatus() {
        println("статус персонажа $name ")
        println("HP: $currentHealth / $maxHealth")
        println("MP: $currentMana / $maxMana")
        println("Can Attack? $canAttack")
    }

    fun attack(target: GameCharacter) {
        if (!canAttack) {
            println("[!} $name не может атаковать!")
            return
        }

        if (currentHealth <= 0) {
            println("[!} $name sdox")
            return
        }

        val damage = baseAttack

        println("$name атакует ${target.name} и наносит $damage урина!")
        target.takeDamage(damage)

        startAttackCooldown(2000L)
        // 2000L - время в миллисекундах. L - тип данных Long
    }

    fun takeDamage(amount: Int) {
        if (amount <= 0) {
            println("$name не получает урина")
            return
        }

        currentHealth -= amount
        println("[-] $name получает $amount урина | HP: $currentHealth")

        if (currentHealth <= 0) {
            println("[!] $name повержен!")
        }
    }

    private fun startAttackCooldown(cooldownMillis: Long) {
        canAttack = false
        println("[#] $name получает кд на ${cooldownMillis} мс")

        // запуск новой корутины в "глоб области"
        GlobalScope.launch {
            // GlobalScope - глобальная область для корутин
            // корутины в этой области живут пока жив процесс
            // launch - запуск новой корутины

            delay(cooldownMillis)
            // delay - "задержать поток" или усыпить корутину на время
            // ВАЖНO: delay НЕ БЛОКИРУЕТ ВЕСЬ ПОТОК, только данную запущеннуб корутину
            // те пока кд перезаряжается - игра идет
            // Без корутин такая реализация невозможна (это многопоточность
            canAttack = true
            println("[*] кд атаки для $name прошел")
        }
    }

    fun startManaRegeneration(
        amuotPerTick: Int,
        intervalMillis: Long
    ) {
        if (manaRegenJob != null) {
            println("[!] Мана уже регенится")
            manaRegenJob?.cancel()
        }

        manaRegenJob = GlobalScope.launch {
            println("Реген маны для $name запущена")

            while (true) {
                delay(intervalMillis)

                if (currentHealth <= 0) {
                    println("[!] $name повержен, реген прерван")
                    break
                }

                if (currentMana >= maxMana) {
                    println("[!] мана $name уже полная")
                    continue
                }

                currentMana += amuotPerTick

                if (currentMana > maxMana) {
                    currentMana = maxMana
                }

                println("[+] $name регенит $amuotPerTick маны ($currentMana / $maxMana")
            }
            println("Корутина регена маны для $name завершена")
        }
    }

    fun stopManaRegeneration(){
        if (manaRegenJob == null){
            print("Регененация маны $name не запущена")
            return
        }

        manaRegenJob?.cancel()
        // cancel - заверщаем выполнение корутины

        println("регенерация маны для $name завершена")
        manaRegenJob = null
    }

    fun applyPosionEffect(
        damagePerTick: Int,
        ticks: Int,
        intervalMillis: Long
    ){
        if (potionJob != null){
            println("яд действует на $name эффект перезапущен")
            potionJob?.cancel()
        }

        potionJob = GlobalScope.launch {
            println("[-] $name отравлен, яд действует $ticks тиков")

            var remaningTicks = ticks

            while (remaningTicks > 0){
                delay(intervalMillis)
                // ждем интервал между тиками

                if (currentHealth <= 0){
                    println("$name погиб, яд не действует")
                    break
                }

                println("яд наносить $damagePerTick урина $name")
                takeDamage(damagePerTick)
                // получение урина от яда за 1 тик

                remaningTicks -= 1
                // после получения отнимаем один тик от оставшегося времени наложения

            }
            println("[!] Эффект яда на $name завершился")
        }
    }

    fun clearPoison(){
        // принудительное снятие эффекта
        if (potionJob == null){
            println("на $name нет эффектов")
            return
        }

        potionJob?.cancel()
        println("[!] с $name снять эффект")
        potionJob = null
    }
}