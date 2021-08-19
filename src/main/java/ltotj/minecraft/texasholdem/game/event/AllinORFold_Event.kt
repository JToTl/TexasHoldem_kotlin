package ltotj.minecraft.texasholdem_kotlin.game.event

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.game.AllinORFold
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

object AllinORFold_Event:Listener {

    private fun clickSound(player: Player){
        player.playSound(player.location, Sound.BLOCK_COMPOSTER_FILL,2F,2F)
    }

    @EventHandler
    fun InvClickEve(e: InventoryClickEvent){
        if(e.view != Component.text("AllinORFold"))return
        e.isCancelled=true
        val player=e.whoClicked as Player
        val item=e.currentItem
        if(item!=null&&item.type!= Material.AIR&&item.type!= Material.WHITE_STAINED_GLASS_PANE) {
            clickSound(player)
            if (!Main.getPlData(player)?.action!!) {
                when (item.displayName()) {
                    Component.text("§w§lフォールド") -> Main.getPlData(player)?.fold()
                    Component.text("§w§lオールイン") -> (Main.getPlData(player) as AllinORFold.PlayerData).allIn()
                }
            }
        }
    }
}