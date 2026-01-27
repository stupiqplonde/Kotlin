package lesson12

sealed class GameEvent(open val playerId: String){
    // боевые события //
    data class CharacterDied(
        override val playerId: String,
        val characterName: String,
        val killerName: String? = null // перс может умереть сам либо убийца не будет известен
    ) : GameEvent(playerId)

    data class DamageDealt(
        override val playerId: String,
        val attackerName: String,
        val targetName: String,
        val amount: Int
    ) : GameEvent(playerId)

    data class EffectApplied(
        override val playerId: String,
        val characterName: String,
        val effectName: String
    ) : GameEvent(playerId)

    data class EffectEnded(
        override val playerId: String,
        val characterName: String,
        val effectName: String
    ) : GameEvent(playerId)

    // Квесты и прогресс
    data class  QuestStarted(
        override val playerId: String,
        val questId: String
    ) : GameEvent(playerId)

    data class QuestStepCompleted(
        override val playerId: String,
        val questId: String,
        val stepId: String
    ) : GameEvent(playerId)

    data class QuestCompleted(
        override val playerId: String,
        val questId: String,
        val stepId: String? = null
    ) : GameEvent(playerId)

    // НПС и диалоги

    data class DialogueStarted(
        override val playerId: String,
        val npcName: String,
        val playerName: String
    ) : GameEvent(playerId)

    data class DialogueChoiceSelected(
        override val playerId: String,
        val npcName: String,
        val playerName: String,
        val choiceId: String
    ) : GameEvent(playerId)

    data class DialogueLineUnlocked(
        override val playerId: String,
        val npcName: String,
        val lineId: String
    ) : GameEvent(playerId)

    // достижения

    data class AchievementUnlocked(
        override val playerId: String,
        val achievementId: String
    ) : GameEvent(playerId)

    data class GoldPaid(
        override val playerId: String = "1",
        val payerName: String,
        val recipientName: String,
        val amount: Int
    ) : GameEvent(playerId)

    data class PlayerProgressSaved(
        override val playerId: String,
        val questId: String,
        val stepId: String
    ) : GameEvent(playerId)

    data class StateChanged(
        override val playerId: String,
        val oldState: String,
        val newState: String
    ) : GameEvent(playerId)

    data class QuestStateChanged(
        override val playerId: String,
        val oldState: String,
        val newState: String
    ) : GameEvent(playerId)


}