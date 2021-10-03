package ltotj.minecraft.texasholdem_kotlin.game.command

import ltotj.minecraft.texasholdem_kotlin.Main
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.playable
import ltotj.minecraft.texasholdem_kotlin.Main.Companion.plugin
import ltotj.minecraft.texasholdem_kotlin.MySQLManager
import ltotj.minecraft.texasholdem_kotlin.Utility.getYenString
import ltotj.minecraft.texasholdem_kotlin.game.TexasHoldem
import net.kyori.adventure.text.Component
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
import kotlin.math.abs


object TexasHoldem_Command: CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean{
        if (args.isEmpty())return false
        if(sender.hasPermission("poker.admin")){
            when(args[0]){
                "on" -> {
                    playable.set(true)
                    plugin.config.set("canPlay", true)
                    plugin.saveConfig()
                    sender.sendMessage("[${Main.pluginTitle}]をオンにしました")
                }
                "off" -> {
                    playable.set(false)
                    plugin.config.set("canPlay", false)
                    plugin.saveConfig()
                    sender.sendMessage("[${Main.pluginTitle}]をオフにしました")
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
                }
            }
        }
        if(sender !is Player){
            sender.sendMessage("プレイヤー以外は実行できません")
            return true
        }
        val uuid=sender.uniqueId
        when(args[0]){
            "start" -> {
                if (!playable.get()) sender.sendMessage("[${Main.pluginTitle}]はただいま停止中です")
                else if (Main.currentPlayers.containsKey(uuid)) sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                else if (args.size < 3 || args[1].toIntOrNull() == null || args[2].toIntOrNull() == null || args[1].toInt() < Main.con.getInt("minChipRate") ||
                        args[1].toInt() > Main.con.getInt("maxChipRate") || abs(args[2].toInt() - 3) > 1) sender.sendMessage("/poker start <チップ一枚あたりの金額:${getYenString(Main.con.getDouble("minChipRate"))}以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人)")
                else if (Main.vault.getBalance(uuid) < args[1].toInt() * Main.con.getDouble("firstNumberOfChips")) sender.sendMessage("所持金が足りません")
                else {
                    if (args.size > 3 && args[3].toIntOrNull() != null && args[2].toInt() <= args[3].toInt() && args[3].toInt() <= 4) {
                        Main.texasHoldemTables[uuid] = TexasHoldem(sender, args[3].toInt(), args[2].toInt(), args[1].toInt())
                    } else {
                        Main.texasHoldemTables[uuid] = TexasHoldem(sender, 4, args[2].toInt(), args[1].toInt())
                    }
                    if (args.size > 4 && args[4].toIntOrNull() != null && abs(args[4].toInt() - 3) < 3) Main.texasHoldemTables[uuid]!!.roundTimes = args[4].toInt()
                    Main.texasHoldemTables[uuid]?.addPlayer(sender)
                    Bukkit.broadcast(Component.text("§l" + sender.name + "§aが§cチップ一枚" + getYenString(args[1].toDouble()) + "§r、§l§e募集人数" + args[2] + "〜" + Main.texasHoldemTables[uuid]!!.maxSeat + "人、§c周回数" + Main.texasHoldemTables[uuid]!!.roundTimes + "回§aで§7§lテキサスホールデム§aを募集中！§r/poker join " + sender.name + " §l§aで参加しましょう！ §4注意 参加必要金額" + getYenString((args[1].toDouble() * Main.con.getDouble("firstNumberOfChips")))), Server.BROADCAST_CHANNEL_USERS)
                    Main.texasHoldemTables[uuid]?.start()
                }
            }
            "join" -> {
                if (!playable.get()) sender.sendMessage("[${Main.pluginTitle}]はただいま停止中です")
                else if (Main.currentPlayers.containsKey(uuid)) sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                else if (args.size < 2) sender.sendMessage("/poker join <募集している人のID>")
                else if (Bukkit.getPlayer(args[1]) == null || !Main.texasHoldemTables.containsKey(Bukkit.getPlayerUniqueId(args[1]))) sender.sendMessage(args[1] + "さんはゲームを開催していません")
                else if (Main.vault.getBalance(sender.uniqueId) < Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.rate * Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.firstChips) sender.sendMessage("所持金が足りません")
                else if (!Main.texasHoldemTables[Bukkit.getPlayerUniqueId(args[1])]!!.addPlayer(sender)) sender.sendMessage("既にゲームが始まっています")
            }
            "help" -> {
                sender.sendMessage(arrayOf("§e==============[${Main.pluginTitle}]==============",
                        "§e/poker start <チップ一枚あたりの金額:10000円以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人) (周回数：1〜5) §d-> テキサスホールデムの参加者を募集します 参加人数分だけゲームが行われます §d注意 設定金額×${Main.con.getInt("firstNumberOfChips")}円が必要です",
                        "§e/poker join <募集している人のID> §d-> テキサスホールデムに参加します",
                        "§e/poker open §d-> 参加中のゲーム画面を開きます",
                        "§e/poker list §d-> 参加可能な部屋の一覧を表示します"))
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
                3, 4, 5 -> if (args[0] == "start") {
                    if (args.size == 3) return mutableListOf("最小募集人数（2〜4人）")
                    if (args.size == 4) return mutableListOf("最大募集人数（2〜4人）")
                    if (args.size == 5) return mutableListOf("周回数（1〜5）")
                }
            }
        }
        return mutableListOf()
    }


}