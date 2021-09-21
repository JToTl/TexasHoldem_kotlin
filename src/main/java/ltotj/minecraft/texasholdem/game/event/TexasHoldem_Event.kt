package ltotj.minecraft.texasholdem_kotlin.game.event

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.getPlData
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.getTable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

object TexasHoldem_Event :Listener{

    private fun clickSound(player:Player){
        player.playSound(player.location,Sound.BLOCK_COMPOSTER_FILL,2F,2F)
    }

    private fun matchItem(item:ItemStack,slot:Int,key:String):Int{//不格好だけどしょうがないやん
        if(slot in 45..52&& Main.plugin.config.getString("$key.$slot.material")==item.type.toString()){
            return slot
        }
        return 0
    }

    @EventHandler
    fun InvClickEve(e:InventoryClickEvent){
        if(e.view.title() != text("TexasHoldem"))return
        e.isCancelled=true
        val player=e.whoClicked as Player
        val item=e.currentItem
        if(item!=null&&item.type!=Material.AIR&&item.type!=Material.WHITE_STAINED_GLASS_PANE) {
            if (!getPlData(player)?.action!!) {
                clickSound(player)
                when (matchItem(item,e.slot, "texasholdeminv")) {
                    46 -> getPlData(player)?.fold()
                    48 -> {
                        if (getTable(player)?.bet != 0) player.sendMessage("賭けチップが0でないのでチェックできません")
                        else getPlData(player)?.preCall!!.set(true)
                    }
                    47, 49 -> if (getTable(player)?.bet!! - getPlData(player)?.instBet!! <= getPlData(player)?.playerChips!!) {
                        getPlData(player)?.playerGUI?.setRaiseButton()
                    }
                    50 -> getPlData(player)?.preCall!!.set(true)
                    51 -> getPlData(player)?.allIn()
                }
                when (matchItem(item,e.slot, "raise")) {
                    45 -> {
                        getPlData(player)?.addedChips=0
                        getPlData(player)?.playerGUI?.setActionButtons()
                    }
                    46 -> getPlData(player)?.downBet()
                    49 -> getPlData(player)?.preCall!!.set(true)
                    52 -> getPlData(player)?.raiseBet()
                }
            }

        }
    }
}