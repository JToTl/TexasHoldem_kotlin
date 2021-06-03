package ltotj.minecraft.texasholdem_kotlin

class Test {
    var startTime: Date? = null
    var endTime: Date? = null
    var deck: List<Card> = ArrayList()
    var seatmap: HashMap<Int, Seat> = HashMap()
    var playermap: HashMap<UUID, Int> = HashMap()
    var community: List<Card> = ArrayList()
    var masterplayer: Player? = null
    var tip = 0.0
    var firstChips = 0
    var pot = 0
    var bet = 0
    var foldcount = 0
    var maxseat = 0
    var minseat = 0
    var roundTimes = 1
    var isrunning = false
    var texasHoldem: TexasHoldem? = null

    class Card {
        var number = 0
        var suit = 0
        val card: ItemStack
            get() {
                val material: Material = Material.valueOf(GlobalClass.config.getString("cardMaterial"))
                val item = ItemStack(material, 1)
                val meta: ItemMeta = item.getItemMeta()
                meta.setCustomModelData(GlobalClass.config.getInt("$suit.$number.customModelData"))
                meta.setDisplayName(getSuit(suit) + "の" + ((number - 1) % 13 + 1))
                item.setItemMeta(meta)
                return item
            }

        fun getSuit(i: Int): String {
            when (i) {
                0 -> return "スペード"
                1 -> return "ダイヤ"
                2 -> return "ハート"
                3 -> return "クローバー"
            }
            return ""
        }
    }

    inner class Seat(p: Player) {
        var texasGui = TexasGui()
        var player: Player
        var myhands: List<Card> = ArrayList()
        var addChip = 0
        var instancebet = 0
        var playerChips = firstChips
        var hand = 0.0
        var action = ""
        var folded = false
        var head: ItemStack

        init {
            player = p
            GlobalClass.currentplayer.put(p.getUniqueId(), masterplayer.getUniqueId())
            head = ItemStack(Material.PLAYER_HEAD)
            val skull: SkullMeta = (head.getItemMeta() as SkullMeta)!!
            skull.setOwningPlayer(p)
            head.setItemMeta(skull)

            //シート作成時に徴収しまーす
            GlobalClass.vault.withdraw(p, tip * firstChips)
        }
    }

    internal inner class TexasGui {
        val inv: Inventory
        fun openInventory(player: Player) {
            player.openInventory(inv)
        }

        fun ownTurnInv(i: Int) {
            for (j in 45..52) {
                seatmap[i]!!.texasGui.putCustomItem(j, Material.valueOf(GlobalClass.config.getString("texasholdeminv.$j.material")), GlobalClass.config.getString("texasholdeminv.$j.name"), GlobalClass.config.getString("texasholdeminv.$j.lore"))
            }
        }

        protected fun createGUIItem(material: Material?, name: String?, vararg lore: String?): ItemStack {
            val item = ItemStack(material, 1)
            val meta: ItemMeta = item.getItemMeta()
            meta.setDisplayName(name)
            meta.setLore(Arrays.asList(lore))
            item.setItemMeta(meta)
            return item
        }

        init {
            inv = Bukkit.createInventory(null, 54, "TexasHoldem")
        }
    }
}