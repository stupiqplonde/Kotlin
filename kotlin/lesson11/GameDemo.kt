package lesson11

fun main(){
    val quest = Quest(
        "Oleg",
        "Oleg_oleg"
    )

    val npc = Npc("Kirill")

    // нужно зарегистрировать нпс и квест в системе событий
    quest.register()
    npc.register()

    EventBus.publish(
        GameEvent.CharacterDied(
            "Kesha"
        )
    )

    EventBus.publish(
        GameEvent.CharacterDied(
            "Oleg"
        )
    )
}