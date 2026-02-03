package lesson16


// sealed class UiState - UiEvent
// EventBus - потоки событий - Flow\SharedFlow - User Interface реагировал на любые действия и команды
// DialogueState + DialogueGraph + DialogueProgress
// - Nav Graph (экраны приложения и переходы между ними)
// - Экранные состояния (что на данный момент, показывает экран пользователю)
// - Сохранение прогресса (на каком этапе был пользователь, зашел он или нет фильтрация и настройки и тд)

//sealed class GameEventV2(open val playerId: String){
//    data class QuestStateChanged(
//        override val playerId: String,
//        val questId: String,
//        val oldState: String,
//        val newState: String
//    ): GameEventV2(playerId)
    // ЗАЧЕМ
    // UI узнает что необходимо обновить (о квесте)
    // NPC реагирует и узнает, что ему говорить
    // + удобство сохранения прогресса
//}