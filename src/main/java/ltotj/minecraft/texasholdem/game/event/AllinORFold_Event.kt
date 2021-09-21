package ltotj.minecraft.texasholdem_kotlin.game.event

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.getPlData
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.getTable
import ltotj.minecraft.texasholdem_kotlin.game.AllinORFold
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

object AllinORFold_Event:Listener {

    private fun clickSound(player: Player){
        player.playSound(player.location, Sound.BLOCK_COMPOSTER_FILL,2F,2F)
    }

    private fun matchItem(item: ItemStack, slot:Int, key:String):Int{//不格好だけどしょうがないやん
        if(slot in 45..52&& Main.plugin.config.getString("$key.$slot.material")==item.type.toString()){
            return slot
        }
        return 0
    }

    @EventHandler
    fun InvClickEve(e: InventoryClickEvent){
        if(e.view.title()!= Component.text("AllinORFold"))return
        e.isCancelled=true
        val player=e.whoClicked as Player
        val item=e.currentItem
        if(item!=null&&item.type!= Material.AIR&&item.type!= Material.WHITE_STAINED_GLASS_PANE) {
            if (!getPlData(player)?.action!!) {
                clickSound(player)
                when (matchItem(item,e.slot, "texasholdeminv")) {
                    46 -> getPlData(player)?.fold()
                    51 -> getPlData(player)?.allIn()
                }
            }
        }
    }
}