package lesson12

class CombatSystemDemo{
    fun simulateFight(){
        EventBus.post(GameEvent.DamageDealt("Oleg","Oleg", "Kirill", 10))
        EventBus.post(GameEvent.DamageDealt("Oleg","Kirill", "Oleg", 5))

        EventBus.post(GameEvent.EffectApplied("Oleg","Oleg", "Яд"))

        EventBus.post(GameEvent.CharacterDied("Kirill", "Oleg"))
    }
}
