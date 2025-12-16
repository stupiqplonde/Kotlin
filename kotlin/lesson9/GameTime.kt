package lesson9

class GameTime {
    private var lastTimeMillis: Long = System.currentTimeMillis()
    // var - так как время обновляется постоянно
    // : Long - это целое длинное число ( длиннее чем Int и позволяет сохранять в себе более точные значения
    // System.currentTimeMillis - возвращает текущее время ПК
    // В будущем время будет получаться (если не локалка) со стороны сервера
    // System - класс стандартной Java-библиотека обращается с системой (ваш ПК)
    // 1/1000 - секунды в миллисекундах

    var deltaTimeSeconds: Double = 0.0
    // delta - сколько секунд прошло между двумя кадрами
    // : Double - число с плавающей точкой (может быть дробным)

    var totalTimeSeconds: Double = 0.0
    // Общее время прошедшее от старта игры

    fun update(){
        //update - метод вызывающий каждый кадр для подсчета игрового времени

        val currentTimeMillis = System.currentTimeMillis()

        val deltaMillis = currentTimeMillis - lastTimeMillis
        // подсчет дельты (разница между кадрами во времени)
        // результат: сколько миллисекунд с последнего кадра

        deltaTimeSeconds = deltaMillis / 1000.0
        // перевод миллисекунд в секунды
        // 16 / 1000 = 0.016

        totalTimeSeconds += deltaTimeSeconds
        // накопление общего игрового времени

        lastTimeMillis = currentTimeMillis
        // обновление посчитанного времени предыдущего кадра
        // в следующем кадре будем считать разницу от этого временного момента
    }
}