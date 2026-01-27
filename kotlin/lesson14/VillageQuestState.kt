package lesson14

// 1 путь
// - поговорить со старым
// - согласиться на помощь
// - убить кирилла-шамана
// - вернуться и доложить о выполнении
// КОНЦОВКА - ГЕРОЙ ДЕРЕВНИ

// 2 Путь
// - говорит
// - СОГЛАШАЕТСЯ
// - не убивает с кириллом
// КОНЦОВКА - МИРНЫЙ ДОГОВОР

// 3 Путь
// - говорит
// - отказывается
// - помогает кириллу
// КОНЦОВКА - ДЕРЕВНЯ В ОГНЕ - ОТЧИВКА КАКОЙ ЦЕНОЙ

// 4 Путь
// - говорит
// - соглашается
// - убивает орка
// КОНЦОВКА - СЕКРЕТ

enum class VillageQuestState{
    NOT_STARTED,
    TALKED_TO_ELDER,

    ACCEPTED_HELP,
    REFUSED_HELP,

    KILLED_KIRILL_SHAMAN,
    MADE_PEACE,
    HELPED_KIRILL,
    KILLED_ORK,

    HERO_ENDING,
    PEACE_ENDING,
    BAD_ENDING,
    SECRET_ENDING
}