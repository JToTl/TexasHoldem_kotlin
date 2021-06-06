package ltotj.minecraft.texasholdem_kotlin

import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.getPlData
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.getTable
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

object EventList:Listener {


    private fun clickSound(player:Player){
        player.playSound(player.location,Sound.BLOCK_COMPOSTER_FILL,2F,2F)
    }

    @EventHandler
    fun InvClickEve(e:InventoryClickEvent){
        if(e.view != Component.text("TexasHoldem"))return
        e.isCancelled=true
        val player=e.whoClicked as Player
        val item=e.currentItem
        if(item!=null&&item.type!=Material.AIR&&item.type!=Material.WHITE_STAINED_GLASS_PANE) {
            clickSound(player)
            if (!getPlData(player)?.action!!) {
                when (item.displayName()) {
                    Component.text("§w§lフォールド") -> getPlData(player)?.fold()
                    Component.text("§w§lチェック") -> {
                        if (getTable(player)?.bet != 0) player.sendMessage("賭けチップが0でないのでチェックできません")
                        else getPlData(player)?.call()
                    }
                    Component.text("§w§lベット"), Component.text("§w§lレイズ") -> if (getTable(player)?.bet!! - getPlData(player)?.instBet!! <= getPlData(player)?.playerChips!!) {
                        getPlData(player)?.playerGUI?.setRaiseButton()
                    }
                    Component.text("§w§lレイズ")-> getPlData(player)?.playerGUI?.setRaiseButton()
                    Component.text("§w§lオールイン") -> getPlData(player)?.allIn()
                    Component.text("§4§l戻る")->getPlData(player)?.playerGUI?.setActionButton()
                    Component.text("§c§l賭けチップを一枚減らす")->getPlData(player)?.raiseBet()
                    Component.text("§c§l賭けチップを一枚減らす")->getPlData(player)?.downBet()
                    Component.text("§a§l以下の枚数でチップを上乗せする")->getPlData(player)?.call()
                }
            }
        }
    }



}