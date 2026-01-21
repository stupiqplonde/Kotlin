package lesson13

enum class NpcState{
    // enum - перечисление всех возможных состояний Npc
    IDLE, // просто стоит
    WAITING, // ждет
    TALKING,
    REWARDED // уже выдал награду
}