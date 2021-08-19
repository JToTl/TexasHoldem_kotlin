package ltotj.minecraft.texasholdem_kotlin.game.utility


import ltotj.minecraft.texasholdem_kotlin.Main.Companion.con
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack


class Card (val suit:Int,val num:Int){


    fun getCard():ItemStack{
        val material= Material.valueOf(con.getString("cardMaterial" ))
        val item=ItemStack(material,1)
        val meta=item.itemMeta
        meta.setCustomModelData(con.getInt("$suit.$num.customModelData"))
        meta.displayName(Component.text("§c"+getSuit(suit) + "の" + ((num-1)%13+1)))
        item.setItemMeta(meta)
        return item
    }

    fun getSuit(i:Int):String{
        when(i){
            0->return "スペード"
            1->return "ダイヤ"
            2->return "ハート"
            3->return "クローバー"
        }
        return ""
    }

}