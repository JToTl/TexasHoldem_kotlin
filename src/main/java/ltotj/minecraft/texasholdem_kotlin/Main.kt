package ltotj.minecraft.texasholdem_kotlin

import ltotj.minecraft.texasholdem_kotlin.game.TexasHoldem
import ltotj.minecraft.texasholdem_kotlin.game.command.AllinORFold_Command
import ltotj.minecraft.texasholdem_kotlin.game.command.TexasHoldem_Command
import ltotj.minecraft.texasholdem_kotlin.game.event.AllinORFold_Event
import ltotj.minecraft.texasholdem_kotlin.game.event.TexasHoldem_Event
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

class Main : JavaPlugin() {

    companion object{

        lateinit var con: Config
        lateinit var texasHoldemTables:HashMap<UUID, TexasHoldem>
        lateinit var currentPlayers:HashMap<UUID, UUID>
        lateinit var plugin: JavaPlugin
        lateinit var playable:AtomicBoolean
        lateinit var vault: VaultManager
        val executor=Executors.newCachedThreadPool()
        var pluginTitle="TexasHoldem"


        fun getPlData(player: Player): TexasHoldem.PlayerData?{
            if(!currentPlayers.containsKey(player.uniqueId))return null
            return getTable(player)?.getPlData(player.uniqueId)
        }

        fun getTable(player: Player): TexasHoldem? {
            if(!currentPlayers.containsKey(player.uniqueId))return null
            return texasHoldemTables[currentPlayers[player.uniqueId]]
        }

    }

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        con=Config(this)
        texasHoldemTables=HashMap()
        currentPlayers=HashMap()
        plugin =this
        playable=AtomicBoolean()
        playable.set(config.getBoolean("canPlay"))
        vault = VaultManager(this)
        server.pluginManager.registerEvents(TexasHoldem_Event,this)
        getCommand("poker")!!.setExecutor(TexasHoldem_Command)

        executor.execute {
            val mysql=MySQLManager(this,"TexasHoldem_onEnable")
            mysql.execute("create table if not exists handsLog\n" +
                    "(\n" +
                    "    id int unsigned auto_increment,\n" +
                    "    gameId int unsigned,\n" +
                    "    P1card varchar(16) null,\n" +
                    "    P2card varchar(16) null,\n" +
                    "    P3card varchar(16) null,\n" +
                    "    P4card varchar(16) null,\n" +
                    "    community varchar(32) null,\n" +
                    "    foldP varchar(20) null,\n" +
                    "\n" +
                    "    primary key(id)\n" +
                    ");")
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}