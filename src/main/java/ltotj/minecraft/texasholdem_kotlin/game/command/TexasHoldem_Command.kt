package ltotj.minecraft.texasholdem_kotlin.game.command

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.getTable
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.playable
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.plugin
import ltotj.minecraft.texasholdem_kotlin.MySQLManager
import ltotj.minecraft.texasholdem_kotlin.Utility.getYenString
import ltotj.minecraft.texasholdem_kotlin.game.TexasHoldem
import ltotj.minecraft.texasholdem_kotlin.game.utility.Card
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.lang.System.out
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import kotlin.math.abs


object TexasHoldem_Command: CommandExecutor, TabCompleter {


    val preJoinPlayersToManyRoundTimes=HashMap<Pair<String,UUID>,TexasHoldem>()

    fun createClickEventText_run(text:String,command:String):Component{
        return Component.text(text).clickEvent(ClickEvent.runCommand(command))
    }

    fun createHoverEventText(){
        Component.text().hoverEvent(HoverEvent.showText(
                Component.text("")
        ))
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean{
        if (args.isEmpty())return false
        if(sender.hasPermission("poker.admin")){
            when(args[0]){
                "test"->{
                    Main.texasHoldemTables[Main.currentPlayers[(sender as Player).uniqueId]]!!.getPlData((sender as Player).uniqueId)!!.playerCards.cards[args[1].toInt()]=Card(args[2].toInt(),args[3].toInt())
                    return true
                }
                "pot"->{
                    Main.texasHoldemTables[Main.currentPlayers[(sender as Player).uniqueId]]!!.pot=args[1].toInt()
                }
                "on" -> {
                    playable.set(true)
                    plugin.config.set("canPlay", true)
                    plugin.saveConfig()
                    sender.sendMessage("[${Main.pluginTitle}]をオンにしました")
                    return true
                }
                "off" -> {
                    playable.set(false)
                    plugin.config.set("canPlay", false)
                    plugin.saveConfig()
                    sender.sendMessage("[${Main.pluginTitle}]をオフにしました")
                    return true
                }
                "setChips"->{
                    if(args.size==1)return true
                    val chips=args[1].toIntOrNull()?:return true
                    plugin.config.set("firstNumberOfChips",chips)
                    plugin.saveConfig()
                    sender.sendMessage("初期チップ枚数を${chips}に変更しました")
                    return true
                }
                "ranking" -> {
                    Main.executor.execute {
                        val mysql=MySQLManager(Main.plugin,"TexasHoldemRankinig")
                        var rank=1
                        sender.sendMessage("§a§l獲得金額ランキング")
                        val result= mysql.query("select name,totalWin from playerData order by totalWin desc limit 10;")
                        if (result != null) {
                            while (true) {
                                try {
                                    if (!result.next()) break
                                    if (result.getInt("totalWin") <= 0) break
                                    sender.sendMessage( "§7§l${rank.toString() }.§b${result.getString("name").toString()}§e§l${getYenString(result.getInt("totalWin").toDouble())}")
                                    rank++
                                } catch (throwables: SQLException) {
                                    throwables.printStackTrace()
                                }
                            }
                            try {
                                result.close()
                            } catch (throwables: SQLException) {
                                throwables.printStackTrace()
                            }
                        }
                        try {
                            mysql.close()
                        } catch (throwables: NullPointerException) {
                            throwables.printStackTrace()
                        }
                    }
                    return true
                }
            }
        }
        if(sender !is Player){
            sender.sendMessage("プレイヤー以外は実行できません")
            return true
        }
        val uuid=sender.uniqueId
        when(args[0]){
            "start"->{
                if (!playable.get()) {
                    sender.sendMessage("[${Main.pluginTitle}]はただいま停止中です")
                    return true
                }
                if (Main.currentPlayers.containsKey(uuid)) {
                    sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                    return true
                }
                if(args.size<3){
                    sender.sendMessage("/poker start <チップ一枚あたりの金額:${getYenString(Main.con.getDouble("minChipRate"))}以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人)")
                    return true
                }
                val rate=args[1].toIntOrNull()?:0
                val minSeat=args[2].toIntOrNull()?:2
                val maxSeat=if(args.size<4) 4 else args[3].toIntOrNull()?:4
                val roundTimes=if(args.size<5) 1 else args[4].toIntOrNull()?: kotlin.run {
                    sender.sendMessage("周回回数は1~100000である必要があります")
                    return true
                }
                if(roundTimes>100000||roundTimes<1){
                    sender.sendMessage("周回回数は1~100000である必要があります")
                    return true
                }
                val firstChips=if(args.size<6) 30 else args[5].toIntOrNull()?:Main.con.getInt("firstNumberOfChips")
                if(firstChips<Main.con.getInt("firstNumberOfChips")||firstChips>200){
                    sender.sendMessage("初期チップ枚数は30以上200以下である必要があります")
                    return true
                }
                if(rate< Main.con.getInt("minChipRate") ||minSeat>maxSeat||rate> Main.con.getInt("maxChipRate") || abs(minSeat - 3) > 1) {
                    sender.sendMessage("/poker start <チップ一枚あたりの金額:${getYenString(Main.con.getDouble("minChipRate"))}以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人)")
                    return true
                }
                if (Main.vault.getBalance(uuid) < rate * firstChips) {
                    sender.sendMessage("所持金が足りません")
                    return true
                }
                val time=if(args.size>6)args[6].toIntOrNull()?:0 else 30
                if(time<30||time>180) {
                    sender.sendMessage("持ち時間は30~180秒である必要があります")
                    return true
                }
                Main.texasHoldemTables[uuid]= TexasHoldem(sender,maxSeat,minSeat,rate,roundTimes,firstChips,time)
                Main.texasHoldemTables[uuid]?.addPlayer(sender)
                if(firstChips==Main.con.getInt("firstNumberOfChips")){
                    Bukkit.broadcast(Component.text("§l" + sender.name + "§aが§cチップ一枚" + getYenString(args[1].toDouble()) + "§r、§l§e募集人数" + args[2] + "〜" + Main.texasHoldemTables[uuid]!!.maxSeat + "人、§c周回数" + Main.texasHoldemTables[uuid]!!.roundTimes + "回、§e持ち時間${time}秒§aで§7§lテキサスホールデム§aを募集中！§r/poker join " + sender.name + " §l§aで参加しましょう！  §4参加必要金額" + getYenString((args[1].toDouble() * Main.con.getDouble("firstNumberOfChips")))), Server.BROADCAST_CHANNEL_USERS)
                }
                else{
                    Bukkit.broadcast(Component.text("§l" + sender.name + "§aが§c初期チップ${firstChips}枚・チップ一枚" + getYenString(args[1].toDouble()) + "§r、§l§e募集人数" + args[2] + "〜" + Main.texasHoldemTables[uuid]!!.maxSeat + "人、§c周回数" + Main.texasHoldemTables[uuid]!!.roundTimes + "回、§e持ち時間${time}秒§aで§7§lテキサスホールデム§aを募集中！§r/poker join " + sender.name + " §l§aで参加しましょう！  §4参加必要金額" + getYenString((args[1].toDouble() * firstChips))), Server.BROADCAST_CHANNEL_USERS)
                }
                Bukkit.broadcast(createClickEventText_run("§e§l[ここをクリックでポーカーに参加]","/poker join ${sender.name}"))
                Main.texasHoldemTables[uuid]?.start()
                return true
            }
//            "start" -> {
//                if (!playable.get()) sender.sendMessage("[${Main.pluginTitle}]はただいま停止中です")
//                else if (Main.currentPlayers.containsKey(uuid)) sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
//                else if (args.size < 3 || args[1].toIntOrNull() == null || args[2].toIntOrNull() == null || args[1].toInt() < Main.con.getInt("minChipRate") ||
//                        args[1].toInt() > Main.con.getInt("maxChipRate") || abs(args[2].toInt() - 3) > 1) sender.sendMessage("/poker start <チップ一枚あたりの金額:${getYenString(Main.con.getDouble("minChipRate"))}以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人)")
//                else if (Main.vault.getBalance(uuid) < args[1].toInt() * Main.con.getDouble("firstNumberOfChips")) sender.sendMessage("所持金が足りません")
//                else {
//                    if (args.size > 3 && args[3].toIntOrNull() != null && args[2].toInt() <= args[3].toInt() && args[3].toInt() <= 4) {
//                        Main.texasHoldemTables[uuid] = TexasHoldem(sender, args[3].toInt(), args[2].toInt(), args[1].toInt())
//                    } else {
//                        Main.texasHoldemTables[uuid] = TexasHoldem(sender, 4, args[2].toInt(), args[1].toInt())
//                    }
//                    if (args.size > 4 && args[4].toIntOrNull() != null && abs(args[4].toInt() - 3) < 3) Main.texasHoldemTables[uuid]!!.roundTimes = args[4].toInt()
//                    Main.texasHoldemTables[uuid]?.addPlayer(sender)
//                    Bukkit.broadcast(Component.text("§l" + sender.name + "§aが§cチップ一枚" + getYenString(args[1].toDouble()) + "§r、§l§e募集人数" + args[2] + "〜" + Main.texasHoldemTables[uuid]!!.maxSeat + "人、§c周回数" + Main.texasHoldemTables[uuid]!!.roundTimes + "回§aで§7§lテキサスホールデム§aを募集中！§r/poker join " + sender.name + " §l§aで参加しましょう！  §4参加必要金額" + getYenString((args[1].toDouble() * Main.con.getDouble("firstNumberOfChips")))), Server.BROADCAST_CHANNEL_USERS)
//                    Bukkit.broadcast(createClickEventText_run("§e§l[ここをクリックでポーカーに参加]","/poker join ${sender.name}"))
//                    Main.texasHoldemTables[uuid]?.start()
//                }
//            }
            "join" -> {
                if (!playable.get()) sender.sendMessage("[${Main.pluginTitle}]はただいま停止中です")
                else if (Main.currentPlayers.containsKey(uuid)) sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                else if (args.size < 2) sender.sendMessage("/poker join <募集している人のID>")
                else if (Bukkit.getPlayer(args[1]) == null || !Main.texasHoldemTables.containsKey(Bukkit.getPlayerUniqueId(args[1]))) sender.sendMessage(args[1] + "さんはゲームを開催していません")
                else if (Main.vault.getBalance(sender.uniqueId) < Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.rate * Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.firstChips) sender.sendMessage("所持金が足りません")

                //歪すぎ
                else if (!Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.isRunning&&Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.roundTimes>20&& !preJoinPlayersToManyRoundTimes.contains(sender.uniqueId)){

                    preJoinPlayersToManyRoundTimes.filter { it.key.second==sender.uniqueId }.keys.forEach {
                        preJoinPlayersToManyRoundTimes.remove(it)
                    }

                    sender.sendMessage("§c§l[警告!!] 参加しようとしているゲームは周回数が§e§l${Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.roundTimes}§c§lのゲームです。非常に長い時間、もしくは参加者のうちの一人がチップを全て回収するまでゲームが続く可能性があります。以上を理解した上でなお参加したい場合は、以下のコマンドを入力してください。")

                    val confirmKey=UUID.randomUUID().toString().subSequence(0, 8).toString()
                    preJoinPlayersToManyRoundTimes[Pair(confirmKey,sender.uniqueId)]=Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!

                    sender.sendMessage("/poker confirm $confirmKey")
                    return true
                }
                else if (!Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.addPlayer(sender)) sender.sendMessage("既にゲームが始まっています")
            }
            "confirm"->{
                if (!playable.get()) {
                    sender.sendMessage("[${Main.pluginTitle}]はただいま停止中です")
                    return true
                }
                if (Main.currentPlayers.containsKey(uuid)){
                    sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                    return true
                }
                if(args.size<2){
                    sender.sendMessage("コマンドが違います")
                    return true
                }
                val key=Pair(args[1],sender.uniqueId)
                val pokerGame= preJoinPlayersToManyRoundTimes[key]?: kotlin.run {
                    sender.sendMessage("コマンドが違います")
                    return true
                }
                if(!Main.texasHoldemTables.values.contains(pokerGame)){
                    sender.sendMessage("既にゲームが終了しています")
                    preJoinPlayersToManyRoundTimes.remove(key)
                    return true
                }
                if (Main.vault.getBalance(sender.uniqueId) < pokerGame.rate * pokerGame.firstChips) {
                    sender.sendMessage("所持金が足りません")
                    return true
                }
                if(!pokerGame.addPlayer(sender)){
                    sender.sendMessage("既にゲームが始まっています")
                    preJoinPlayersToManyRoundTimes.remove(key)
                    return true
                }
                preJoinPlayersToManyRoundTimes.remove(key)

            }
            "help" -> {
                arrayOf("§e==============[${Main.pluginTitle}]==============",
                        "§e/poker start <チップ一枚あたりの金額:10000円以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人) (周回数：1〜5) §d-> テキサスホールデムの参加者を募集します 参加人数分だけゲームが行われます §d設定金額×${Main.con.getInt("firstNumberOfChips")}円が必要です",
                        "§e/poker join <募集している人のID> §d-> テキサスホールデムに参加します",
                        "§e/poker open §d-> 参加中のゲーム画面を開きます",
                        "§e/poker list §d-> 参加可能な部屋の一覧を表示します").forEach {
                            sender.sendMessage(it)
                }
            }
            "open" -> {
                if (Main.currentPlayers.containsKey(uuid)) Main.texasHoldemTables[Main.currentPlayers[uuid]]!!.openInv(uuid)
                else sender.sendMessage("参加しているゲームがありません")
            }
            "list" -> {
                sender.sendMessage("参加者募集中の部屋は以下の通りです")
                for (texasholdem in Main.texasHoldemTables.values) {
                    if (!texasholdem.isRunning) sender.sendMessage("主催者：" + texasholdem.masterPlayer.name + " 必要金額§4${getYenString(texasholdem.rate * texasholdem.firstChips.toDouble())} §r募集人数：" + texasholdem.minSeat + "〜" + texasholdem.maxSeat + "人")
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if(alias=="poker"){
            when(args.size){
                1 -> {
                    return mutableListOf("start", "join", "open", "help", "list")
                }
                2 -> {
                    when (args[0]) {
                        "start" -> {
                            return mutableListOf("１チップあたりの金額（電子マネー）")
                        }
                        "join" -> {
                            val list = mutableListOf<String>()
                            for (uuid in Main.texasHoldemTables.keys) {
                                list.add(Bukkit.getPlayer(uuid)?.name ?: continue)
                            }
                            return list
                        }
                    }
                }
                3, 4, 5,6,7 -> if (args[0] == "start") {
                    if (args.size == 3) return mutableListOf("最小募集人数（2〜4人）")
                    if (args.size == 4) return mutableListOf("最大募集人数（2〜4人）")
                    if (args.size == 5) return mutableListOf("周回数（1〜100000）")
                    if (args.size == 6) return mutableListOf("チップ数")
                }
            }
        }
        return mutableListOf()
    }


}