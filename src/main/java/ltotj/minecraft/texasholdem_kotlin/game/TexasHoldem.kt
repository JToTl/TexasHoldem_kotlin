package ltotj.minecraft.texasholdem_kotlin.game

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.con
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.currentPlayers
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.playable
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.plugin
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.pluginTitle
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.texasHoldemTables
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.vault
import ltotj.minecraft.texasholdem_kotlin.MySQLManager
import ltotj.minecraft.texasholdem_kotlin.Utility.createGUIItem
import ltotj.minecraft.texasholdem_kotlin.Utility.getYenString
import ltotj.minecraft.texasholdem_kotlin.game.command.TexasHoldem_Command
import ltotj.minecraft.texasholdem_kotlin.game.utility.Card
import ltotj.minecraft.texasholdem_kotlin.game.utility.Deck
import ltotj.minecraft.texasholdem_kotlin.game.utility.PlayerCards
import ltotj.minecraft.texasholdem_kotlin.game.utility.PlayerGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPlayer
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.lang.Double.max
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt


open class TexasHoldem(val masterPlayer: Player, val maxSeat: Int, val minSeat: Int, val rate: Int):Thread(){

    protected open val playerList=ArrayList<PlayerData>()
    protected val seatMap=HashMap<UUID, Int>()
    protected val deck= Deck()
    val firstChips=con.getInt("firstNumberOfChips")
    var community=ArrayList<Card>()
    var pot=0
    var bet=0
    var turnCount=0
    var firstSeat=0
    var roundTimes=1
    var foldedList=ArrayList<Int>()
    var allInList=ArrayList<Int>()
    var isRunning=false
    private val mySQL = MySQLManager(Main.plugin, "TexasHoldem")
    private val startTime=Date()

    open inner class PlayerData(val player: Player, val seat: Int) {
        open val playerGUI = PlayerGUI(seat,"TexasHoldem")
        val playerCards = PlayerCards()
        var addedChips = 0
        var hand = 0.0
        var instBet = 0
        var playerChips=firstChips
        var action=false
        var preCall= AtomicBoolean()
        var totalBetAmount=0 //allIn時の配分計算用

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
            if(playerChips==bet+addedChips-instBet){
                allIn()
                return true
            }
            playerGUI.removeButton()
            bet+=addedChips
            playerChips+=instBet-bet
            totalBetAmount+=bet-instBet
            setChips(seat,instBet,bet)
            instBet=bet
            addedChips=0
            action=true
            if(bet==0){
                setItemAlPl(chipPosition(seat), createGUIItem(Material.LIME_DYE,1,"§f§lチェック済み"))
            }
            return true
        }

        fun setActionButtons(){
            playerGUI.setActionButton(46)
            playerGUI.setActionButton(51)
            if(playerChips+instBet>bet){
                if(bet!=0) {
                    playerGUI.setActionButton(47)
                    playerGUI.setActionButton(50)
                }
            }
            if(bet==0){
                playerGUI.setActionButton(48)
                playerGUI.setActionButton(49)
            }
        }

        fun drawCard(){
            if(foldedList.contains(seat)){
                playerCards.addCard(Card(-1,-1))
            }
            else {
                val card = deck.draw()
                playerCards.addCard(card)
            }
        }

        fun raiseBet(){
            if(bet+addedChips-instBet<playerChips) {
                addedChips++
                playerGUI.reloadRaiseButton(addedChips)
            }
        }

        fun downBet(){
            if(addedChips>0){
                addedChips--
                playerGUI.reloadRaiseButton(addedChips)
            }
        }

        fun fold() {
            foldedList.add(seat)
            setItemAlPl(chipPosition(seat),createGUIItem(Material.BARRIER,1,"§f§lフォールド"))
            action=true
        }

        open fun allIn():Boolean{
            instBet+=playerChips
            bet= kotlin.math.max(kotlin.math.max(instBet,playerChips),bet)
            totalBetAmount+=playerChips
            setItemAlPl(chipPosition(seat),createGUIItem(Material.NETHER_STAR,1,"§e§lオールイン:${instBet}枚"))
            playSoundAlPl(Sound.BLOCK_ENCHANTMENT_TABLE_USE,2F)
            playerChips=0
            allInList.add(seat)
            action=true
            return true
        }

        fun reset(){
            hand=0.0
            addedChips=0
            instBet=0
            totalBetAmount=0
            playerCards.reset()
            preCall.set(false)
            for(playerData in playerList)playerGUI.setCoin(playerData.seat, playerData.player.name, playerData.playerChips)
        }

        fun setUpCard(setSeat: Int, dif: Int){
            if(setSeat==seat)playerGUI.setCard(cardPosition(setSeat) + dif, playerCards.cards[dif])
            else playerGUI.setFaceDownCard(cardPosition(setSeat) + dif)
        }

    }

    fun debugSetCard(player:Player,suit:Int,num:Int,dif:Int){
        getPlData(player.uniqueId)!!.playerCards.cards[dif]=Card(suit,num)
    }

    protected open fun actionTime(dif: Int) {
        turnCount += dif
        while ((((allInList.size+foldedList.size+1)<playerList.size||bet!=0)&&foldedList.size < playerList.size-1 && ((playerList[turnSeat()].instBet != bet) || turnCount < playerList.size + dif))) {
            val playerData = playerList[turnSeat()]
            setGUI(turnSeat())
            playerData.preCall.set(false)
            playerData.player.playSound(playerData.player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 2F, 2F)
            if (!foldedList.contains(turnSeat()) && !allInList.contains(turnSeat())) {
                for (i in 600 downTo 0) {
                    sleep(50)
                    if (i % 20 == 0) {
                        playSoundAlPl(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 2F)
                        setClock(i / 20)
                    }
                    if (i == 0) {
                        playerList[turnSeat()].addedChips = 0
                        playerList[turnSeat()].fold()
                        break
                    }
                    if (!playerList[turnSeat()].action && playerList[turnSeat()].preCall.get()) {
                        playerList[turnSeat()].preCall.set(false)
                        playerList[turnSeat()].call()
                    }
                    if (playerList[turnSeat()].action) {
                        playerList[turnSeat()].action = false
                        break
                    }
                }
            }
            playerList[turnSeat()].playerGUI.removeButton()
            removeItem(chipPosition(turnSeat()) - 3)
            removeItem(19)
            setCoin(turnSeat())
            turnCount += 1
        }
        for(i in 0 until playerList.size){
            if(!foldedList.contains(i)) {
                if (allInList.contains(i)) {
                    setItemAlPl(chipPosition(i), createGUIItem(Material.NETHER_STAR,1,"§e§lオールイン済み${playerList[i].totalBetAmount}枚"))
                    sleep(500)
                } else {
                    removeItem(chipPosition(i))
                    playSoundAlPl(Sound.BLOCK_GRAVEL_STEP, 2F)
                    sleep(500);
                }
            }
        }
        turnCount=0
        setPot()
        resetBet()
    }

    open fun addPlayer(player: Player):Boolean{//メインスレッド専用
        if(playerList.size>=maxSeat||isRunning)return false
        seatMap[player.uniqueId]=playerList.size
        vault.withdraw(player.uniqueId,rate*firstChips.toDouble())
        playerList.add(PlayerData(player, playerList.size))
        currentPlayers[player.uniqueId]=masterPlayer.uniqueId
        player.openInventory(playerList[seatMap[player.uniqueId]!!].playerGUI.inv)

        if(playerList.size==1){
            playerList[0].playerGUI.setCoin(0,playerList[0].player.name,firstChips)
            playerList[0].playerGUI.inv.setItem(cardPosition(0)-1,playerList[0].getHead())
        }
        else{
            for(i in 0..playerList.size-2){
                playerList[i].playerGUI.setCoin(0,playerList[i].player.name,firstChips)
                playerList[i].playerGUI.inv.setItem(cardPosition(playerList.size-1)-1,playerList[playerList.size-1].getHead())
            }
            playerList[seatMap[player.uniqueId]!!].playerGUI.inv.contents = playerList[0].playerGUI.inv.contents
        }
        return true
    }

    protected fun turnSeat():Int{
        return (turnCount+firstSeat)%playerList.size
    }

    protected open fun reset(){
        deck.reset()
        community.clear()
        foldedList.clear()
        allInList.clear()
        turnCount=0
        pot=0
        for(playerData in playerList){
            playerData.hand=0.0
            playerData.reset()
            removeItem(chipPosition(playerData.seat))
            if(playerData.playerChips==0){
                foldedList.add(playerData.seat)
            }
            playerData.drawCard()
            playerData.drawCard()
        }
        setPot()
        for(i in 20..24)removeItem(i)
    }

    fun createClickEventText_run(text:String,command:String):Component{
        return text(text).clickEvent(ClickEvent.runCommand(command))
    }

    protected fun resetBet(){
        bet=0
        for(playerData in playerList)playerData.instBet=0
    }

    protected fun setChips(seat: Int, start: Int, end: Int){
        for(i in start..end){
            playSoundAlPl(Sound.BLOCK_CHAIN_STEP, 2F)
            for(playerData in playerList){
                playerData.playerGUI.setChips(seat, i, rate)
            }
            sleep(200)
        }
    }

    protected fun setCommunityCard(){
        for(i in 0..4){
            val card=deck.draw()
            community.add(card)
            playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
            for(playerData in playerList)playerData.playerGUI.setFaceDownCard(20 + i)
            sleep(350)
        }
    }

    protected fun setCoin(seat: Int){
        for(playerData in playerList){
            playerData.playerGUI.setCoin(seat, playerList[seat].player.name, playerList[seat].playerChips)
        }
    }

    protected fun setPlayerCard(seat: Int, dif: Int){
        for(playerData in playerList){
            playerData.setUpCard(seat, dif)
        }
    }

    protected fun setWinnerHead(evenOrOdd: Boolean, head: ItemStack){
        for(playerData in playerList)playerData.playerGUI.setWinner(evenOrOdd, head)
    }

    protected fun setDrawItem(){
        for(playerData in playerList)playerData.playerGUI.setDrawGame()
    }

    protected fun setPot(){
        for(playerData in playerList){
            pot+=playerData.instBet
            playerData.instBet=0
        }
        for(playerData in playerList){
            playerData.playerGUI.setPot(pot)
        }
    }

    protected fun displayHand(seat: Int) {
        playerList[seat].hand = playerList[seat].playerCards.getHand(community)
        val openCom = ArrayList<Int>()
        val openPlC = ArrayList<Int>()
        val ins = getDigit(playerList[seat].hand, 0, 2)
        val insSuit = getDigit(playerList[seat].hand, 12, 13)
        var flag = false
        for (i in 0..4) {
            for (j in 0..4) {
                if (!openCom.contains(j)&&((ins!=18&&ins!=15)||community[j].suit == insSuit)&& community[j].num == getDigit(playerList[seat].hand, 2 * i + 2, 2 * i + 4)) {
                    openCom.add(j)
                    flag = true
                    break
                }
            }
            if (flag) {
                flag = false
                continue
            }
            if (!openPlC.contains(0)&&((ins!=18&&ins!=15)||playerList[seat].playerCards.cards[0].suit == insSuit)&&playerList[seat].playerCards.cards[0].num== getDigit(playerList[seat].hand, 2 * i + 2, 2 * i + 4)) openPlC.add(0)
            else openPlC.add(1)
        }
        playSoundAlPl(Sound.BLOCK_BEACON_ACTIVATE,2F)
        for(i in 0..4){
            if(openCom.contains(i))for(playerData in playerList)playerData.playerGUI.enchantItem(20 + i)
            else removeItem(20 + i)
        }
        for(i in 0..1){
            if(openPlC.contains(i))for(playerData in playerList)playerData.playerGUI.enchantItem(cardPosition(seat) + i)
            else removeItem(cardPosition(seat) + i)
        }
    }

    protected fun reloadGUI(seat: Int){
        for(playerData in playerList){
            for(i in 0..4)playerData.playerGUI.setCard(20 + i, community[i])
            playerData.playerGUI.setCard(cardPosition(seat), playerList[seat].playerCards.cards[0])
            playerData.playerGUI.setCard(cardPosition(seat) + 1, playerList[seat].playerCards.cards[1])
        }
    }

    private fun getDigit(f: Double, start: Int, end: Int):Int{
        val l = floor(f / 10.0.pow(13 - end))
        return (l - 10.0.pow(end-start) * floor(l / (10.0.pow(end-start)))).roundToInt()
    }

    fun getPlData(uuid: UUID): PlayerData? {
        if(!seatMap.containsKey(uuid))return null
        return playerList[seatMap[uuid]!!]
    }

    protected fun removeItem(slot: Int){
        for(playerData in playerList){
            playerData.playerGUI.inv.setItem(slot, ItemStack(Material.STONE, 0))
        }
    }

    protected fun cardPosition(seat: Int):Int{
        when(seat){
            0 -> return 42
            1 -> return 37
            2 -> return 1
            3 -> return 6
        }
        return 0
    }

    protected fun chipPosition(seat: Int):Int {
        when (seat) {
            0 -> return 35
            1 -> return 30
            2 -> return 12
            3 -> return 17
        }
        return 0
    }

    protected fun cancelGame(){
        for(i in 0 until playerList.size){
            vault.deposit(playerList[i].player.uniqueId, rate * playerList[i].playerChips.toDouble())
            playerList[i].playerChips=0//安全のため
            currentPlayers.remove(playerList[i].player.uniqueId)
            playerList[i].player.sendMessage("§e§lゲームがキャンセルされたため、返金されました")
        }
    }

    protected fun savePlayerData(){
        for(playerData in playerList){
            val result=mySQL.query("select totalWin,win from playerData where uuid='${playerData.player.uniqueId}';")
            if(result==null){
                mySQL.close()
                println("[$pluginTitle]データベース接続エラー")
                return
            }
            if(result.next()){
                mySQL.execute("update playerData set totalWin=${result.getInt("totalWin")+(playerData.playerChips - firstChips)*rate},win=${result.getInt("win")+kotlin.math.max(playerData.playerChips - firstChips,0)*rate} where uuid='${playerData.player.uniqueId}';")
            }
            else{
                mySQL.execute("insert into playerData(name,uuid,totalWin,win) values('${playerData.player.name}','${playerData.player.uniqueId}',${(playerData.playerChips - firstChips)*rate},${kotlin.math.max(playerData.playerChips - firstChips,0)*rate});")
            }
            result.close()
            mySQL.close()
        }
    }

    protected fun endGame() {
        sleep(1000)
        texasHoldemTables.remove(masterPlayer.uniqueId)
        val query = StringBuilder()
        query.append("INSERT INTO gameLog(startTime,endTime,gameName,P1,P2,P3,P4,chipRate,firstChips,P1Chips,P2Chips,P3Chips,P4Chips) VALUES('${getDateForMySQL(startTime)}','${getDateForMySQL(Date())}','TexasHoldem'")
        for (i in 0 until playerList.size) {
            query.append(",'" + playerList[i].player.name + "'")
            vault.deposit(playerList[i].player.uniqueId, rate * playerList[i].playerChips.toDouble())
            currentPlayers.remove(playerList[i].player.uniqueId)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                playerList[i].player.closeInventory()
            })
        }
        for (i in playerList.size until 4) query.append(",null")
        query.append(",$rate,$firstChips")
        for (i in 0 until playerList.size) query.append("," + playerList[i].playerChips + "")
        for (i in playerList.size until 4) query.append(",0")
        query.append(");")
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!mySQL.execute(query.toString())) {
                playable.set(false)
                println("テキサスホールデムのデータをDBに保存できませんでした 安全のため、新規ゲームを開催不可能にします")
            }
        })
        savePlayerData()
    }

    protected fun sendResult(){
        val message=ArrayList<String>()
        message.add("§4§l=============§aTexasHoldem.§dResult§4§l==============")
        for(playerData in playerList)message.add("§e" + playerData.player.name + "：§d" + playerData.playerChips + "枚")
        message.add("§4§l=========================================")
        for(playerData in playerList)for(str in message)playerData.player.sendMessage(str)
    }

    private fun getDateForMySQL(date: Date): String? {
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HHH:mm:ss")
        return df.format(date)
    }

    protected fun openPlCard(seat: Int){
        playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
        for(i in 0 until playerList.size){
            playerList[i].playerGUI.setCard(cardPosition(seat), playerList[seat].playerCards.cards[0])
            playerList[i].playerGUI.setCard(cardPosition(seat) + 1, playerList[seat].playerCards.cards[1])
        }
    }

    private fun setItemAlPl(slot: Int, item: ItemStack){
        for(playerData in playerList)playerData.playerGUI.inv.setItem(slot, item)
    }

    protected fun openCommunityCard(num: Int){
        playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
        for(playerData in playerList)playerData.playerGUI.setCard(20 + num, community[num])
        sleep(1000)
    }

    protected fun setClock(time: Int){
        setItemAlPl(19, ItemStack(Material.CLOCK, time))
    }

    protected open fun setGUI(turnS: Int){
        for(i in 0 until playerList.size){
            if(i==turnS)playerList[i].setActionButtons()
            playerList[i].playerGUI.setTurnPBlo(turnS)
        }
    }

    fun openInv(uuid: UUID){
        getPlayer(uuid)!!.openInventory(playerList[seatMap[uuid]!!].playerGUI.inv)
    }

    protected fun playSoundAlPl(sound: Sound, pitch: Float){
        for(playerData in playerList){
            playerData.player.playSound(playerData.player.location, sound, 2F, pitch)
        }
    }

    protected fun sendRewardGUI(seat:Int,rank:Int){//rankは0から
        val item=if(rank==0) ItemStack(Material.GOLD_INGOT) else ItemStack(Material.GOLD_NUGGET)
        val direction=if(seat<2) 1 else -1
        if(seat%3==0) {
            sleep(150)
            setItemAlPl(24, item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(24)
            setItemAlPl(24+9*direction, item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(24+9*direction)
            setItemAlPl(25 + 9*direction,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(25 + 9*direction)
            setItemAlPl(26 + 9*direction,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(26 + 9*direction)
            playSoundAlPl(Sound.ENTITY_PLAYER_LEVELUP, 2F)
        }
        else{
            sleep(150)
            setItemAlPl(24,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(24)
            setItemAlPl(23,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(23)
            setItemAlPl(22,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(22)
            setItemAlPl(21,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(21)
            setItemAlPl(21+9*direction,item)
            playSoundAlPl(Sound.BLOCK_NOTE_BLOCK_HARP,2F)
            sleep(150)
            removeItem(21+9*direction)
            playSoundAlPl(Sound.ENTITY_PLAYER_LEVELUP, 2F)
        }
    }

    protected fun showAndPayReward(){
        val handsList=ArrayList<ArrayList<Int>>()
        for (i in 0 until playerList.size) {
            if (!foldedList.contains(i)) {//displayHand実行によりhandが保存される
                displayHand(i)
                var k=0
                for(j in 0 until handsList.size+1){
                    if(j==handsList.size){//降順になるようにソート
                        var listA=arrayListOf(i) //新規に入れる手
                        var listB:ArrayList<Int>? = null //移す手
                        for(r in k until handsList.size){
                            listB=handsList[r]
                            handsList[r]=listA
                            listA=listB
                        }
                        handsList.add(listB?:listA)
                        break
                    }
                    if(round(playerList[handsList[j][0]].hand/10)==round(playerList[i].hand/10)){
                        handsList[j].add(i)
                        break
                    }
                    k=if(j==k&&round(playerList[handsList[j][0]].hand/10)>round(playerList[i].hand/10)) j+1 else k
                }
                sleep(5000)
                reloadGUI(i)
            }
        }
        //表示
        if (handsList[0].size==1) {
            for (i in 0..7) {
                setWinnerHead(i % 2 == 0, playerList[handsList[0][0]].getHead())
                playSoundAlPl(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,1F)
                sleep(500)
            }
        }
        else {
            setDrawItem()
            sleep(1500)
        }
        for(i in 0..4)removeItem(20 + i)

        for(i in 0 until playerList.size){
            if(!foldedList.contains(i))removeItem(chipPosition(i))
        }

        //pot分配
        var toBB=0
        var loop=0
        while(pot!=0&&loop<20){
            var instancePot=0
            var minBet=pot //最大値として値を一旦代入

            if(loop!=0){
                for(list in handsList){
                    for(i in (list.clone() as ArrayList<*>)){
                        if(playerList[i as Int].totalBetAmount==0){
                            list.remove(i)
                        }
                    }
                }
                while (handsList[0].size==0){
                    handsList.removeAt(0)
                    if(handsList.size==0)break;
                }
            }
            for(i in handsList[0]){
                println("${i},${handsList[0]}")
                minBet= minBet.coerceAtMost(playerList[i].totalBetAmount)
                println("minBetは$minBet")
            }
            for(playerData in playerList){
                val move=minBet.coerceAtMost(playerData.totalBetAmount)
                println("${playerData.player.name}のmoveは$move")
                instancePot+=move
                pot-=move
                playerData.totalBetAmount-=move
            }
            toBB+=instancePot%handsList[0].size
            instancePot/=handsList[0].size
            for(i in handsList[0]){
                println("${i},${handsList[0]}")
                playerList[i].playerChips+=instancePot
                sendRewardGUI(i,loop)
                println("${handsList[0]}")
            }
            loop++
        }
        if(loop>=20){
            playable.set(false)
        }
        if(toBB!=0){
            sleep(1000)
            playerList[firstSeat%playerList.size].playerChips+=toBB
            sendRewardGUI(firstSeat%playerList.size,2)
        }
    }

    override fun run() {
        for (i in 0..59) {
            if (i % 20 == 0&&i!=0) {
                Bukkit.broadcast(createClickEventText_run("§l" + masterPlayer.name + "§aが§7§lテキサスホールデム§aを募集中・・・残り" + (60 - i) + "秒 §r/poker join " + masterPlayer.name + " §l§aで参加  §4参加必要金額" + getYenString(firstChips * rate.toDouble()),"/poker join ${masterPlayer.name}"), Server.BROADCAST_CHANNEL_USERS)
                Bukkit.broadcast(TexasHoldem_Command.createClickEventText_run("§e§l[ここをクリックでポーカーに参加]", "/poker join ${masterPlayer.name}"))
            }
            if (playerList.size == maxSeat) break
            sleep(1000)
        }
        isRunning = true
        val seatSize = playerList.size
        if (seatSize < minSeat) {
            Bukkit.broadcast(Component.text("§l" + masterPlayer.name + "§aの§7テキサスホールデム§aは人数不足により解散しました"), Server.BROADCAST_CHANNEL_USERS)
            cancelGame()
            return
        }
        for (times in 0 until seatSize * roundTimes) {
            reset()
            if(foldedList.size==playerList.size-1){
                break
            }
            for (i in 0 until seatSize) {
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
                turnCount+=i
                playerList[turnSeat()].addedChips = 1
                if (playerList[turnSeat()].call()) {
                    setCoin(turnSeat())
                    playerList[turnSeat()].action=false
                }
            }
            turnCount=0
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
            for (i in 0 until seatSize) if (!foldedList.contains(i)) openPlCard(i)
            sleep(2000)

            showAndPayReward()

            sleep(4000)
            for(i in 0 until seatSize){
                removeItem(cardPosition(i))
                removeItem(cardPosition(i) + 1)
            }
            firstSeat+=1
        }
        endGame()
        sendResult()
    }
}