package ltotj.minecraft.texasholdem_kotlin.game

import ltotj.minecraft.texasholdem_kotlin.Main.Companion.config
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack


class PlayerGUI(private val seat: Int){

    val inv= Bukkit.createInventory(null, 54, Component.text("TexasHoldem"))

    private fun createGUIItem(material: Material, amount: Int, name: String, lore: List<String>):ItemStack{
        val item=ItemStack(material, amount)
        val meta=item.itemMeta
        meta.displayName(Component.text(name))
        meta.lore(listToComponent(lore))
        item.setItemMeta(meta)
        return item
    }

    private fun createGUIItem(material: Material, amount: Int, name: String):ItemStack{
        val item=ItemStack(material, amount)
        val meta=item.itemMeta
        meta.displayName(Component.text(name))
        item.setItemMeta(meta)
        return item
    }

    private fun setGUIItem(slot: Int, material: Material, amount: Int, name: String, lore: List<String>){
        inv.setItem(slot, createGUIItem(material, amount, name, lore))
    }

    private fun setGUIItem(slot: Int, material: Material, name: String, lore: List<String>){
        inv.setItem(slot, createGUIItem(material, 1, name, lore))
    }

    private fun setGUIItem(slot: Int, material: Material, name: String){
        inv.setItem(slot, createGUIItem(material, 1, name))
    }

    fun setCard(slot: Int, card: Card){
        inv.setItem(slot, card.getCard())
    }

    fun setFaceDownCard(slot: Int){
        val material = Material.valueOf(config.getString("cardMaterial"))
        val item = ItemStack(material, 1)
        val meta = item.itemMeta
        meta.setCustomModelData(config.getInt("faceDownCard.customModelData"))
        meta.displayName(Component.text("???"))
        item.itemMeta = meta
        inv.setItem(slot,item)
    }

    private fun cardPosition(seat: Int):Int{
        when(seat){
            0 -> return 42
            1 -> return 37
            2 -> return 1
            3 -> return 6
        }
        return 0
    }

    private fun chipPosition(seat: Int):Int {
        when (seat) {
            0 -> return 35
            1 -> return 30
            2 -> return 12
            3 -> return 17
        }
        return 0
    }

    private fun listToComponent(list: List<String>):List<Component>{
        val cList=ArrayList<Component>()
        for(i in list.indices){
            cList.add(Component.text(list[i]))
        }
        return cList
    }

    fun enchantItem(slot:Int){
        val item=inv.getItem(slot)
        val meta=item?.itemMeta
        meta?.addEnchant(Enchantment.LURE, 1, true)
        item?.itemMeta = meta
        inv.setItem(slot,item)
    }

    fun setChips(seat: Int, amount: Int, rate: Double){
        setGUIItem(chipPosition(seat), Material.GOLD_NUGGET, amount, "§l§yチップ", listOf("§c" + rate + "円"))
    }

    fun setCoin(seat: Int, name: String, amount: Int){
        setGUIItem(cardPosition(seat) + 2, Material.GOLD_INGOT, "§c§l$name§r§wのチップ", listOf("§e$amount§w枚"))
    }

    fun setPot(pot: Int){
        setGUIItem(25, Material.GOLD_BLOCK, "§6現在の賭けチップ合計", listOf("§e§l$pot§w枚"))
    }

    fun setWinner(evenOrOdd:Boolean,head:ItemStack){
        if(evenOrOdd){
            inv.setItem(20,head)
            setGUIItem(21,Material.GOLD_BLOCK,"§l§aW§bI§cN§dN§eE§dR§f!")
            inv.setItem(22,head)
            setGUIItem(23,Material.GOLD_BLOCK,"§l§aW§bI§cN§dN§eE§dR§f!")
            inv.setItem(24,head)
        }
        else{
            setGUIItem(20,Material.GOLD_BLOCK,"§l§aW§bI§cN§dN§eE§dR§f!")
            inv.setItem(21,head)
            setGUIItem(22,Material.GOLD_BLOCK,"§l§aW§bI§cN§dN§eE§dR§f!")
            inv.setItem(23,head)
            setGUIItem(24,Material.GOLD_BLOCK,"§l§aW§bI§cN§dN§eE§dR§f!")
        }
    }

    fun setDrawGame(){
        for(i in 0..4)setGUIItem(20+i,Material.BARRIER,"DRAW",listOf("引き分け 同率一位のプレイヤーに賞金が分配されます"))
    }

    fun setRaiseButton(){
        for(i in 45..52){
            setGUIItem(i, Material.valueOf(config.getString("raise.$i.material")), config.getString("raise.$i.name"))
        }
    }

    fun setTurnPBlo(seat: Int){
        setGUIItem(chipPosition(seat) - 3, Material.DIAMOND_BLOCK, "§l§wターンプレイヤー")
    }

    fun reloadRaiseButton(add:Int){
        setGUIItem(49, Material.valueOf(config.getString("raise.49.material")), config.getString("raise.49.name"), listOf("§c" + add + "枚追加"))
    }

    fun setActionButton(){
        for (i in 45..52) {
            setGUIItem(i, Material.valueOf(config.getString("texasholdeminv.$i.material")), config.getString("texasholdeminv.$i.name"), listOf(config.getString("texasholdeminv.$i.lore")))
        }
    }

    fun removeButton(){
        for(i in 45..53){
            inv.setItem(i, ItemStack(Material.WHITE_STAINED_GLASS_PANE))
        }
    }

    fun reset(){

    }



}