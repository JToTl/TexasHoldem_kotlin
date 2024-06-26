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

//    private fun matchItem(item:ItemStack,slot:Int,key:String):Int{
//        if(slot in 45..53&& Main.plugin.config.getString("$key.$slot.material")==item.type.toString()){
//            return slot
//        }
//        return 0
//    }

    private fun matchItem(item:ItemStack,slot:Int,key:String):Int{
        if(slot in 45..53) {
            when (key) {
                "texasholdeminv" -> {
                    when(slot){
                        46-> return if(item.type==Material.BLUE_STAINED_GLASS_PANE) slot else 0
                        47-> return if(item.type==Material.PINK_STAINED_GLASS_PANE) slot else 0
                        48-> return if(item.type==Material.RED_STAINED_GLASS_PANE) slot else 0
                        49-> return if(item.type==Material.YELLOW_STAINED_GLASS_PANE) slot else 0
                        50-> return if(item.type==Material.GREEN_STAINED_GLASS_PANE) slot else 0
                        53-> return if(item.type==Material.BROWN_STAINED_GLASS_PANE) slot else 0
                    }
                }
                "raise"->{
                    when(slot){
                        45-> return if(item.type==Material.BARRIER) slot else 0
                        46-> return if(item.type==Material.RED_WOOL) slot else 0
                        49-> return if(item.type==Material.GOLD_NUGGET) slot else 0
                        52-> return if(item.type==Material.BLUE_WOOL) slot else 0
                    }
                }
            }
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
            val playerData=getPlData(player)
            val table=getTable(player)
            if (playerData != null&&table!=null) {
                if (!playerData.action) {
                    clickSound(player)
                    when (matchItem(item, e.slot, "texasholdeminv")) {
                        46 -> playerData.fold()
                        48 -> {
                            if (table.bet != 0) player.sendMessage("賭けチップが0でないのでチェックできません")
                            else playerData.preCall.set(true)
                        }
                        47, 49 -> if (table.bet - playerData.instBet <= playerData.playerChips) {
                            playerData.setRaiseMenu()
                        }
                        50 -> playerData.preCall.set(true)
                        53 -> playerData.allIn()
                    }
                    when (matchItem(item, e.slot, "raise")) {
                        45 -> {
                            playerData.setActionButtons()
                            playerData.addedChips = 0
                        }
                        46 -> playerData.downBet()
                        49 -> playerData.preCall.set(true)
                        52 -> playerData.raiseBet()
                    }
                }

            }
        }
    }
}