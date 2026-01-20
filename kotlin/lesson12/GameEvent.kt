package lesson12

sealed class GameEvent{
    // боевые события //
    data class CharacterDied(
        val playerId: String,
        val characterName: String,
        val killerName: String? = null // перс может умереть сам либо убийца не будет известен
    ) : GameEvent()

    data class DamageDealt(
        val playerId: String,
        val attackerName: String,
        val targetName: String,
        val amount: Int
    ) : GameEvent()

    data class EffectApplied(
        val playerId: String,
        val characterName: String,
        val effectName: String
    ) : GameEvent()

    data class EffectEnded(
        val playerId: String,
        val characterName: String,
        val effectName: String
    ) : GameEvent()

    // Квесты и прогресс
    data class  QuestStarted(
        val playerId: String,
        val questId: String
    ) : GameEvent()

    data class QuestStepCompleted(
        val playerId: String,
        val questId: String,
        val stepId: String
    ) : GameEvent()

    data class QuestCompleted(
        val playerId: String,
        val questId: String,
        val stepId: String? = null
    ) : GameEvent()

    // НПС и диалоги

    data class DialogueStarted(
        val playerId: String,
        val npcName: String,
        val playerName: String
    ) : GameEvent()

    data class DialogueChoiceSelected(
        val playerId: String,
        val npcName: String,
        val playerName: String,
        val choiceId: String
    ) : GameEvent()

    data class DialogueLineUnlocked(
        val playerId: String,
        val npcName: String,
        val lineId: String
    ) : GameEvent()

    // достижения

    data class AchievementUnlocked(
        val playerId: String,
        val achievementId: String
    ) : GameEvent()

    data class GoldPaid(
        val playerId: String = "1",
        val payerName: String,
        val recipientName: String,
        val amount: Int
    ) : GameEvent()

    data class PlayerProgressSaved(
        val playerId: String,
        val questId: String,
        val stepId: String
    ) : GameEvent()


}