

fun main(){
    val dialog = DialogProgress()
    val player_0 = "Oleg"
    val player_1 = "BR-BR"

    dialog.show(player_0)
    dialog.choose(player_0,"listen")

    dialog.show(player_1)
    dialog.choose(player_1,"asking_for_help")

    dialog.show(player_1)
    dialog.choose(player_1,"with_respect")

    dialog.show(player_0)
    dialog.choose(player_0,"go")

    dialog.show(player_0)
    dialog.choose(player_0,"W")

    dialog.show(player_0)
    dialog.choose(player_0,"bye")
}
