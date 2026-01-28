package lesson14

import lesson12.GameEvent

class StateNodeV2(
    val state: VillageQuestState
) {
    private val transitions = mutableMapOf<Class<out GameEvent>, VillageQuestState>()
    private val rollback = mutableMapOf<Class<out GameEvent>, VillageQuestState>()

    fun add(eventType: Class<out GameEvent>, next: VillageQuestState, past: VillageQuestState) {
        transitions[eventType] = next
        rollback[eventType] = past
    }

    fun next(event: GameEvent): VillageQuestState? {
        return transitions[event::class.java]
    }

    fun past(event: GameEvent): VillageQuestState? {
        return rollback[event::class.java]
    }
}