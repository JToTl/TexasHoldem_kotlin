package ltotj.minecraft.texasholdem_kotlin.game.utility


import ltotj.minecraft.texasholdem_kotlin.Main.Companion.con
import ltotj.minecraft.texasholdem_kotlin.Utility.createGUIItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack


class Card (val suit:Int,val num:Int){


    fun getCard():ItemStack{
        if(num==-1){
            return createGUIItem(Material.BARRIER,1,"§4§lこのゲームにはもう参加できません")
        }
        val material= Material.valueOf(con.getString("cardMaterial" ))
        val item=ItemStack(material,1)
        val meta=item.itemMeta
        meta.setCustomModelData(con.getInt("$suit.$num.customModelData"))
        meta.displayName(Component.text("${getSuit(suit)}§fの${((num-1)%13+1)}"))
        item.itemMeta = meta
        return item
    }

    fun getSuit(i:Int):String{
        when(i){
            0->return "§8§lスペード"
            1->return "§c§lダイヤ"
            2->return "§c§lハート"
            3->return "§8§lクローバー"
        }
        return ""
    }

}