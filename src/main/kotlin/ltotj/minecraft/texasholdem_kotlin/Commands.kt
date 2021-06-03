package ltotj.minecraft.texasholdem_kotlin

import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.config
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.currentPlayers
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.playable
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.texasHoldemTables
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.vault
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPlayer
import org.bukkit.Bukkit.getPlayerUniqueId
import org.bukkit.entity.Player
import kotlin.math.abs

object Commands:CommandExecutor{

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean{
        if (args.isEmpty())return false
        if(sender.hasPermission("poker.switch")&& args[0] == "switch"){
            playable=!playable
            if(playable){
                sender.sendMessage("新規ゲームを開催可能にしました")
            }
            else{
                sender.sendMessage("新規ゲームを開催不可能にしました")
            }
            return true
        }
        if(sender !is Player){
            sender.sendMessage("プレイヤー以外は実行できません")
            return true
        }
        val uuid=sender.uniqueId
        when(args[0]){
            "start"->{
                if(playable)sender.sendMessage("[TexasHoldem]はただいま停止中です")
                else if(currentPlayers.containsKey(uuid))sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                else if(args.size<3||args[1].toIntOrNull()==null||args[2].toIntOrNull()==null||args[1].toInt()< config.getInt("minChipRate")||
                        args[1].toInt()> config.getInt("maxChipRate")|| abs(args[2].toInt() - 3) >1)sender.sendMessage("/poker start <チップ一枚あたりの金額:"+config.getDouble("minChipRate")+"以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人)")
                else if(vault.getBalance(uuid)<args[1].toDouble()* config.getDouble("firstNumberOfChips"))sender.sendMessage("所持金が足りません")
                else{
                    if(args.size>3&&args[3].toIntOrNull()!=null&&args[2].toInt()<args[3].toInt()&&args[3].toInt()<=4){
                        texasHoldemTables[uuid] = TexasHoldem(sender,args[3].toInt(),args[2].toInt(),args[1].toDouble())
                    }
                    else{
                        texasHoldemTables[uuid] = TexasHoldem(sender,4,args[2].toInt(),args[1].toDouble())
                    }
                    if(args.size>4&&args[4].toIntOrNull()!=null&&abs(args[4].toInt()-2)>1) texasHoldemTables[uuid]!!.roundTimes=args[4].toInt()
                    Bukkit.broadcast(Component.text("§l"+sender.name+"§aが§cチップ一枚"+args[1]+"円§r、§l§e募集人数"+args[2]+"〜"+ texasHoldemTables[uuid]!!.maxSeat+"人、§c周回数"+ texasHoldemTables[uuid]!!.roundTimes+"回§aで§7§lテキサスホールデム§aを募集中！§r/poker join "+sender.name+" §l§aで参加しましょう！ §4注意 参加必要金額"+(args[1].toDouble()*config.getDouble("firstNumberOfChips"))))
                    texasHoldemTables[uuid]?.start()
                }
            }
            "join"->{
                if(!playable)sender.sendMessage("[TexasHoldem]はただいま停止中です")
                else if(currentPlayers.containsKey(uuid))sender.sendMessage("あなたは既にゲームに参加しています！/poker open でゲーム画面を開きましょう！")
                else if(args.size<2)sender.sendMessage("/poker join <募集している人のID>")
                else if(getPlayer(args[1])==null|| texasHoldemTables.containsKey(getPlayerUniqueId(args[1])))sender.sendMessage(args[1]+"さんはゲームを開催していません")
                else if(vault.getBalance(sender.uniqueId)< texasHoldemTables[getPlayerUniqueId(args[1])]!!.rate*texasHoldemTables[getPlayerUniqueId(args[1])]!!.firstChips)sender.sendMessage("所持金が足りません")
                else if(!texasHoldemTables[getPlayerUniqueId(args[1])]!!.addPlayer(sender))sender.sendMessage("既にゲームが始まっています")
            }
            "help"->{
                sender.sendMessage("/poker start <チップ一枚あたりの金額:10000円以上> <最低募集人数:2〜4人> (最大募集人数:2〜4人) (周回数：１〜４):テキサスホールデムの参加者を募集します 参加人数分だけゲームが行われます §4注意 設定金額×"+config.getInt("firstNumberOfChips")+"が必要です")
                sender.sendMessage("/poker join <募集している人のID> :テキサスホールデムに参加します")
                sender.sendMessage("/poker open :参加中のゲーム画面を開きます")
                sender.sendMessage("/poker list :参加可能な部屋の一覧を表示します")
            }
            "open"->{
                if(currentPlayers.containsKey(uuid)) texasHoldemTables[currentPlayers[uuid]]!!.openInv(uuid)
                else sender.sendMessage("参加しているゲームがありません")
            }
            "list"->{
                sender.sendMessage("参加者募集中の部屋は以下の通りです")
                for(texasholdem in texasHoldemTables.values){
                    if(!texasholdem.isRunning)sender.sendMessage("主催者："+texasholdem.masterPlayer.name+" 必要金額§4"+texasholdem.rate*texasholdem.firstChips+" §r募集人数："+texasholdem.minSeat+"〜"+texasholdem.maxSeat+"人")
                }
            }
        }
        return false
    }


}