package ltotj.minecraft.texasholdem_kotlin.game

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.Utility.getYenString
import ltotj.minecraft.texasholdem_kotlin.game.utility.PlayerGUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.lang.Double.max
import kotlin.math.round

class AllinORFold(masterPlayer: Player, maxSeat: Int, minSeat: Int, rate: Double) : TexasHoldem(masterPlayer, maxSeat, minSeat, rate) {

    private var noMoneyCount=0

    inner class PlayerData(player: Player, seat: Int): TexasHoldem.PlayerData(player, seat) {

        override val playerGUI= PlayerGUI(seat,"AllinORFold")

    }

    fun setAllinOrFoldButton(seat: Int){
        playerList[seat].playerGUI.setActionButton(46)
        playerList[seat].playerGUI.setActionButton(51)
    }

    override fun addPlayer(player: Player):Boolean{//メインスレッド専用
        if(playerList.size>maxSeat||isRunning)return false
        seatMap[player.uniqueId]=playerList.size
        Main.vault.withdraw(player.uniqueId,rate*firstChips)
        playerList.add(PlayerData(player, playerList.size))
        Main.currentPlayers[player.uniqueId]=masterPlayer.uniqueId
        player.openInventory(playerList[seatMap[player.uniqueId]!!].playerGUI.inv)
        if(playerList.size==1){
            playerList[0].playerGUI.setCoin(0,playerList[0].player.name,firstChips)
            playerList[0].playerGUI.inv.setItem(cardPosition(0)-1,playerList[0].getHead())
        }
        else{
            for(i in 0..playerList.size-2){
                playerList[i].playerGUI.setCoin(playerList.size-1,playerList[playerList.size-1].player.name,firstChips)
                playerList[i].playerGUI.inv.setItem(cardPosition(playerList.size-1)-1,playerList[playerList.size-1].getHead())
            }
            playerList[seatMap[player.uniqueId]!!].playerGUI.inv.contents = playerList[0].playerGUI.inv.contents
        }
        return true
    }

    override fun actionTime(dif: Int) {
        turnCount += dif
        while (foldedList.size+allInList.size<playerList.size&&((foldedList.size < playerList.size && playerList[turnSeat()].instBet != bet) || turnCount < playerList.size + dif)) {
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
                    playerList[turnSeat()].fold()
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

    override fun reset(){
        deck.reset()
        noMoneyCount=0
        for(playerData in playerList){
            playerData.reset()
            playerData.drawCard()
            playerData.drawCard()
            if(playerData.playerChips==0)noMoneyCount++
        }
        community.clear()
        foldedList.clear()
        turnCount=0
        pot=0
        setPot()
        for(i in 20..24)removeItem(i)
    }

    override fun setGUI(turnS: Int){
        for(i in 0..playerList.size){
            if(i==turnS)setAllinOrFoldButton(turnS)
            playerList[i].playerGUI.setTurnPBlo(turnS)
        }
    }

    override fun run() {
        for (i in 0..59) {
            if (i % 20 == 0&&i!=0) Bukkit.broadcast(Component.text("§l" + masterPlayer.name + "§aが§8§lオールイン§0§l・§f§lオア§0§l・§7§lフォールド§aを募集中・・・残り" + (60 - i) + "秒 §r/aof join " + masterPlayer.name + " §l§aで参加 §4注意 参加必要金額" + getYenString(firstChips * rate)),Server.BROADCAST_CHANNEL_USERS)
            if (playerList.size == maxSeat) break
            sleep(1000)
        }
        isRunning = true
        val seatSize = playerList.size
        if (seatSize < minSeat) {
            Bukkit.broadcast(Component.text("§l" + masterPlayer.name + "§aの§8§lオールイン§0§l・§f§lオア§0§l・§7§lフォールド§aは人数不足のため解散になりました"), Server.BROADCAST_CHANNEL_USERS)
            endGame()
            return
        }
        for (times in 0..seatSize * roundTimes) {
            reset()//ここで所持金0のプレイヤーをカウント
            if(noMoneyCount==playerList.size-1)break
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
                playerList[turnSeat()].addedChips = 1
                if (playerList[turnSeat()].call()) setCoin(turnSeat())
            }
            //プリフロップ
            actionTime(2)
            openCommunityCard(0)
            openCommunityCard(1)
            openCommunityCard(2)
            //フロップ
            openCommunityCard(3)
            //ターン
            openCommunityCard(4)

            playSoundAlPl(Sound.ITEM_BOOK_PAGE_TURN, 2F)
            for (i in 0..seatSize) if (!foldedList.contains(i)) openPlCard(i)
            sleep(2000)
//            var winHand = 0.0
//            for (i in 0..seatSize) {
//                if (!foldedList.contains(i)) {//displayHand実行によりhandが保存される
//                    winHand = max(winHand, round((playerList[i].hand / 10)))
//                    displayHand(i)
//                    sleep(5000)
//                    reloadGUI(i)
//                }
//            }
//            for (i in 0..seatSize) if (!foldedList.contains(i) && round((playerList[i].hand / 10)) == winHand) winners.add(i)
//            //pot分配
//            for (i in winners) playerList[i].playerChips += pot / winners.size
//            if (winners.size == 1) for (i in 0..7) {
//                setWinnerHead(i % 2 == 0, playerList[winners[0]].getHead())
//                sleep(500)
//            }
//            else {
//                setDrawItem()
//                sleep(4000)
//            }
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