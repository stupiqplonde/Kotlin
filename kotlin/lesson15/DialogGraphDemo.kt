package lesson15

fun main(){
    val dialog = DialogProgress()
    val player = "Oleg"

    dialog.show(player)
    dialog.choose(player,"work")

    dialog.show(player)
    dialog.choose(player,"accept")

    dialog.show(player)
    dialog.choose(player,"bye")
}
