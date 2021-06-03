package ltotj.minecraft.texasholdem_kotlin.game


import ltotj.minecraft.texasholdem_kotlin.Config
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.config
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack


class Card (val suit:Int,val num:Int){


    fun getCard():ItemStack{
        val material= Material.valueOf(config.getString("cardMaterial" ))
        val item=ItemStack(material,1)
        val meta=item.itemMeta
        meta.setCustomModelData(config.getInt("$suit.$num.customModelData"))
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