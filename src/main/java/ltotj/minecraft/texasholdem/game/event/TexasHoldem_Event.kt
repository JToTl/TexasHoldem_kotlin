package ltotj.minecraft.texasholdem_kotlin.game.event

import ltotj.minecraft.texasholdem_kotlin.Main
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

object TexasHoldem_Event :Listener{

    private fun clickSound(player: Player){
        player.playSound(player.location, Sound.BLOCK_COMPOSTER_FILL,2F,2F)
    }

    @EventHandler
    fun InvClickEve(e: InventoryClickEvent){
        if(e.view.title()!=Component.text("TexasHoldem"))return
        e.isCancelled=true
        val player=e.whoClicked as Player
        val item=e.currentItem
        if(item!=null&&item.type!= Material.AIR&&item.type!= Material.WHITE_STAINED_GLASS_PANE) {
            if (!Main.getPlData(player)?.action!!) {
                clickSound(player)
                when (item.displayName()) {
                    Component.text("§w§lフォールド") -> Main.getPlData(player)?.fold()
                    Component.text("§w§lチェック") -> {
                        if (Main.getTable(player)?.bet != 0) player.sendMessage("賭けチップが0でないのでチェックできません")
                        else Main.getPlData(player)?.call()
                    }
                    Component.text("§w§lベット"), Component.text("§w§lレイズ") -> if (Main.getTable(player)?.bet!! - Main.getPlData(player)?.instBet!! <= Main.getPlData(player)?.playerChips!!) {
                        Main.getPlData(player)?.playerGUI?.setRaiseButton()
                    }
                    Component.text("§w§lレイズ")-> Main.getPlData(player)?.playerGUI?.setRaiseButton()
                    Component.text("§w§lオールイン") -> Main.getPlData(player)?.allIn()
                    Component.text("§4§l戻る")-> Main.getPlData(player)?.playerGUI?.setActionButtons()
                    Component.text("§c§l賭けチップを一枚減らす")-> Main.getPlData(player)?.raiseBet()
                    Component.text("§c§l賭けチップを一枚減らす")-> Main.getPlData(player)?.downBet()
                    Component.text("§a§l以下の枚数でチップを上乗せする")-> Main.getPlData(player)?.call()
                }
            }
        }
    }
}