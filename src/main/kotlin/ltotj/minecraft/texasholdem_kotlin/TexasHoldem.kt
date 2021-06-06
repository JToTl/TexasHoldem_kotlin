package ltotj.minecraft.texasholdem_kotlin

import ltotj.minecraft.texasholdem_kotlin.Main.Companion.config
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.mySQL
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.playable
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.plugin
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.texasHoldemTables
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.vault
import ltotj.minecraft.texasholdem_kotlin.game.Card
import ltotj.minecraft.texasholdem_kotlin.game.Deck
import ltotj.minecraft.texasholdem_kotlin.game.PlayerCards
import ltotj.minecraft.texasholdem_kotlin.game.PlayerGUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPlayer
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.lang.Double.max
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.round


class TexasHoldem(val masterPlayer: Player, val maxSeat: Int, val minSeat: Int, val rate: Double):Thread(){

    private val playerList=ArrayList<PlayerData>()
    private val seatMap=HashMap<UUID, Int>()
    private val deck=Deck()
    val firstChips=config.getInt("firstNumberOfChips")
    var community=ArrayList<Card>()
    var pot=0
    var bet=0
    var turnCount=0
    var firstSeat=0
    var roundTimes=1
    var foldedList=ArrayList<Int>()
    var allInList=ArrayList<Int>()
    var winners=ArrayList<Int>()
    var isRunning=false
    val startTime:Date = TODO()

    inner class PlayerData(val player: Player, private val seat: Int) {
        val playerGUI = PlayerGUI(seat)
        val playerCards = PlayerCards()
        var addedChips = 0
        var hand = 0.0
        var instBet = 0
        var playerChips=firstChips
        var action=false

        fun getHead():ItemStack{
            val item = ItemStack(Material.PLAYER_HEAD)
            val skull = (item.itemMeta as SkullMeta)
            skull.owningPlayer = player
            item.itemMeta=skull
            return item
        }

        fun call():Boolean{
            if(bet+addedChips-instBet>playerChips){
                addedChips=0
                return false
            }
            bet+=addedChips
            playerChips+=instBet-bet
            setChips(seat, instBet, bet)
            instBet=bet
            addedChips=0
            action=true
            return true
        }

        fun drawCard(){
            val card=deck.draw()
            playerCards.addCard(card)
        }

        fun raiseBet(){
            if(addedChips==10)player.sendMessage("一度に上乗せできる枚数は十枚までです")
            else if(bet+addedChips-instBet<playerChips&&addedChips<10) addedChips++
        }

        fun downBet(){
            if(addedChips>0)addedChips--
        }

        fun fold() {
            foldedList.add(seat)
            action=true
        }

        fun allIn():Boolean{
            if(bet<playerChips)return false
            instBet+=playerChips
            playerChips=0
            allInList.add(seat)
            action=true
            return true
        }

        fun setUpCard(setSeat: Int, dif: Int){
            if(setSeat==seat)playerGUI.setCard(cardPosition(setSeat) + dif, playerCards.cards[dif])
            else playerGUI.setFaceDownCard(cardPosition(setSeat) + dif)
        }

    }

    private fun actionTime(dif: Int) {
        turnCount += dif
        while ((foldedList.size < playerList.size && playerList[turnSeat()].instBet != bet) || turnCount < playerList.size + dif) {
            setGUI(turnSeat())
            for (i in 600 downTo 0) {
                if (foldedList.contains(turnSeat())||allInList.contains(turnSeat())) break
                sleep(50)
                if (i % 20 == 0) {
                    playSoundAlPl(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 2F)
                    setClock(i / 20)
                }
                if (i == 0) {
                    playerList[turnSeat()].addedChips = 0
                    if (playerList[turnSeat()].call()) {
                        playerList[turnSeat()].fold()
                    }
                    break
                }
                if(playerList[turnSeat()].action){
                    playerList[turnSeat()].action=false
                    break
                }
            }
            playerList[turnSeat()].playerGUI.removeButton()
            removeItem(chipPosition(turnSeat()))
            removeItem(26)
            setCoin(turnSeat())
        }
        for(i in 0..playerList.size){
            if(!foldedList.contains(i)&&!allInList.contains(i)){
                removeItem(chipPosition(i))
                playSoundAlPl(Sound.BLOCK_GRAVEL_STEP, 2F)
                sleep(500);
            }
        }
        setPot()
        resetBet()
    }

    fun addPlayer(player: Player):Boolean{
        if(playerList.size>maxSeat||isRunning)return false
        seatMap[player.uniqueId]=playerList.size
        playerList.add(PlayerData(player, playerList.size))
        return true
    }

    private fun turnSeat():Int{
        return (turnCount+firstSeat)%playerList.size
    }

    private fun reset(){
        deck.reset()
        for(playerData in playerList){
            playerData.playerCards.reset()
            playerData.playerGUI.reset()
            playerData.hand=0.0
            playerData.addedChips=0
            playerData.instBet=0
            playerData.drawCard()
            playerData.drawCard()
        }
        community.clear()
        foldedList.clear()
        turnCount=0
        pot=0
        setPot()
    }

    private fun resetBet(){
        bet=0
        for(playerData in playerList)playerData.instBet=0
    }

    private fun setChips(seat: Int, start: Int, end: Int){
        for(i in start..end){
            playSoundAlPl(Sound.BLOCK_CHAIN_STEP, 2F)
            for(playerData in playerList){
                playerData.playerGUI.setChips(seat, i, rate)
            }
            sleep(200)
        }
    }

    private fun setCommunityCard(){
        for(i in 0..4){
            val card=deck.draw()
            community[i]=card
            playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
            for(playerData in playerList)playerData.playerGUI.setFaceDownCard(20 + i)
            sleep(350)
        }
    }

    private fun setCoin(seat: Int){
        for(playerData in playerList){
            playerData.playerGUI.setCoin(seat, playerList[seat].player.name, playerList[seat].playerChips)
        }
    }

    private fun setPlayerCard(seat: Int, dif: Int){
        for(playerData in playerList){
            playerData.setUpCard(seat, dif)
        }
    }

    private fun setWinnerHead(evenOrOdd: Boolean, head: ItemStack){
        for(playerData in playerList)playerData.playerGUI.setWinner(evenOrOdd, head)
    }

    private fun setDrawItem(){
        for(playerData in playerList)playerData.playerGUI.setDrawGame()
    }

    private fun setPot(){
        for(playerData in playerList){
            pot+=playerData.instBet
            playerData.instBet=0
        }
        for(playerData in playerList){
            playerData.playerGUI.setPot(pot)
        }
    }

    private fun displayHand(seat: Int) {
        playerList[seat].hand = playerList[seat].playerCards.getHand(community)
        val openCom = ArrayList<Int>()
        val openPlC = ArrayList<Int>()
        val ins = getDigit(playerList[seat].hand, 0, 2)
        val insSuit = getDigit(playerList[seat].hand, 13, 14)
        var flag = false
        for (i in 0..4) {
            for (j in 0..4) {
                if (!openCom.contains(j)&&((ins!=18&&ins!=15)||community[j].suit == insSuit)&& community[j].num == getDigit(playerList[seat].hand, 2 * i + 2, 2 * i + 4)) {
                    openCom.add(j)
                    flag = true
                    break;
                }
            }
            if (flag) {
                flag = false
                continue
            }
            if (!openPlC.contains(0)&&((ins!=18&&ins!=15)||playerList[seat].playerCards.cards[0].suit == insSuit)&&playerList[seat].playerCards.cards[0].num== getDigit(playerList[seat].hand, 2 * i + 2, 2 * i + 4)) openPlC.add(0)
            else openPlC.add(1)
        }
        for(i in 0..4){
            if(openPlC.contains(i))for(playerData in playerList)playerData.playerGUI.enchantItem(cardPosition(seat) + i)
            if(openCom.contains(i))for(playerData in playerList)playerData.playerGUI.enchantItem(20 + i)
            else removeItem(20 + i)
        }
    }

    private fun reloadGUI(seat: Int){
        for(playerData in playerList){
            for(i in 0..4)playerData.playerGUI.setCard(20 + i, community[i])
            playerData.playerGUI.setCard(cardPosition(seat), playerList[seat].playerCards.cards[0])
            playerData.playerGUI.setCard(cardPosition(seat) + 1, playerList[seat].playerCards.cards[1])
        }
    }

    private fun getDigit(f: Double, start: Int, end: Int):Int{
        val num=f.toString()
        if(num.length<end)return 0
        return num.substring(start, end).toInt()
    }

    fun getPlData(uuid: UUID): PlayerData? {
        if(!seatMap.containsKey(uuid))return null
        return playerList[seatMap[uuid]!!]
    }

    private fun removeItem(slot: Int){
        for(playerData in playerList){
            playerData.playerGUI.inv.setItem(slot, ItemStack(Material.STONE, 0))
        }
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

    private fun endGame() {
        texasHoldemTables.remove(masterPlayer.uniqueId)
        sleep(1000)
        val query = StringBuilder().append("INSERT INTO texasholdem_gamedata(startTime,endTime,P1,P2,P3,P4,chipRate,firstChips,P1Chips,P2Chips,P3Chips,P4Chips) VALUES('" + getDateForMySQL(startTime) + "','" + getDateForMySQL(Date()) + "'")
        for (i in 0..playerList.size) {
            query.append(",'" + playerList[i].player.name + "'")
            vault.deposit(playerList[i].player.uniqueId, rate * playerList[i].playerChips)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                playerList[i].player.closeInventory()
            })
        }
        if (playerList.size != 4) for (i in playerList.size + 1..4) query.append(",null")
        query.append(",'$rate','$firstChips'")
        for (i in 0..playerList.size) query.append(",'" + playerList[i].playerChips + "'")
        if (playerList.size != 4) for (i in playerList.size + 1..4) query.append(",0")
        query.append(");")
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (mySQL.execute(query.toString())) {
                playable=false
                println("テキサスホールデムのデータをDBに保存できませんでした 安全のため、新規ゲームを開催不可能にします")
            }
        })
    }

    private fun sendResult(){
        val message=ArrayList<String>()
        message.add("§4§l====================§a結果§4§l====================")
        for(playerData in playerList)message.add("§e"+playerData.player.name+"：§d"+playerData.playerChips+"枚")
        message.add("§4§l==========================================")
        for(playerData in playerList)for(str in message)playerData.player.sendMessage(str)
    }

    private fun getDateForMySQL(date: Date): String? {
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HHH:mm:ss")
        return df.format(date)
    }

    private fun openPlCard(seat: Int){
        playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
        for(i in 0..playerList.size){
            playerList[i].playerGUI.setCard(cardPosition(seat), playerList[seat].playerCards.cards[0])
            playerList[i].playerGUI.setCard(cardPosition(seat) + 1, playerList[seat].playerCards.cards[1])
        }
    }

    private fun setItemAlPl(slot: Int, item: ItemStack){
        for(playerData in playerList)playerData.playerGUI.inv.setItem(slot, item)
    }

    private fun openCommunityCard(num: Int){
        for(playerData in playerList)playerData.playerGUI.setCard(20 + num, community[num])
        sleep(1000)
    }

    private fun setClock(time: Int){
        setItemAlPl(26, ItemStack(Material.CLOCK, time))
    }

    private fun setGUI(turnS: Int){
        for(i in 0..playerList.size){
            if(i==turnS)playerList[i].playerGUI.setActionButton()
            playerList[i].playerGUI.setTurnPBlo(turnS)
        }
    }

    fun openInv(uuid: UUID){
        getPlayer(uuid)!!.openInventory(playerList[seatMap[uuid]!!].playerGUI.inv)
    }

    private fun playSoundAlPl(sound: Sound, pitch: Float){
        for(playerData in playerList){
            playerData.player.playSound(playerData.player.location, sound, 2F, pitch)
        }
    }

    override fun run() {
        for (i in 0..59) {
            if (i % 10 == 0) Bukkit.broadcast(Component.text("§l" + masterPlayer.name + "§aが§7§lテキサスホールデム§aを募集中・・・残り" + (60 - i) + "秒 §r/poker join " + masterPlayer.name + " §l§aで参加 §4注意 参加必要金額" + firstChips * rate))
            if (playerList.size == maxSeat) break
            sleep(1000)
        }
        isRunning = true
        val seatSize = playerList.size
        if (seatSize < minSeat) {
            Bukkit.broadcast(Component.text("§l" + masterPlayer.name + "§aの§7テキサスホールデム§aは人が集まらなかったので中止しました"))
            endGame()
            return
        }
        for (times in 0..seatSize * roundTimes) {
            reset()
            for (i in 0..seatSize) {
                playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
                setPlayerCard(i, 0)
                sleep(500)
                playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
                setPlayerCard(i, 1)
                sleep(500)
            }
            setCommunityCard()
            //SBとBBの強制ベット
            for (i in 0..1) {
                playerList[turnSeat()].addedChips = 1
                if (playerList[turnSeat()].call()) setCoin(turnSeat())
            }
            //プリフロップ
            actionTime(2)
            openCommunityCard(0)
            openCommunityCard(1)
            openCommunityCard(2)
            //フロップ
            actionTime(0)
            openCommunityCard(3)
            //ターン
            actionTime(0)
            openCommunityCard(4)
            //リバー
            actionTime(0)
            playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
            for (i in 0..seatSize) if (!foldedList.contains(i)) openPlCard(i)
            sleep(2000)
            var winHand = 0.0
            for (i in 0..seatSize) {
                if (!foldedList.contains(i)) {//displayHand実行によりhandが保存される
                    winHand = max(winHand, round((playerList[i].hand / 10)))
                    displayHand(i)
                    sleep(5000)
                    reloadGUI(i)
                }
            }
            for (i in 0..seatSize) if (!foldedList.contains(i) && round((playerList[i].hand / 10)) == winHand) winners.add(i)
            //pot分配
            for (i in winners) playerList[i].playerChips += pot / winners.size
            if (winners.size == 1) for (i in 0..7) {
                setWinnerHead(i % 2 == 0, playerList[winners[0]].getHead())
                sleep(500)
            }
            else {
                setDrawItem()
                sleep(4000)
            }
            firstSeat+=1
            for(i in 0..seatSize){
                removeItem(cardPosition(i))
                removeItem(cardPosition(i) + 1)
            }
            for(i in 0..4)removeItem(20 + i)
            firstSeat+=1
        }
        endGame()
        sendResult()
    }
}