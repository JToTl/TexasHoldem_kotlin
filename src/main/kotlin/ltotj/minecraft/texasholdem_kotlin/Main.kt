package ltotj.minecraft.texasholdem_kotlin

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap

class Main : JavaPlugin() {

    companion object{

        lateinit var config:Config
        lateinit var texasHoldemTables:HashMap<UUID,TexasHoldem>
        lateinit var currentPlayers:HashMap<UUID, UUID>
        lateinit var plugin: Plugin
        var playable:Boolean = false
        lateinit var vault:VaultManager
        lateinit var mySQL:MySQLManager


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
        plugin=this
        vault= VaultManager(this)
        mySQL= MySQLManager(this,"TexasHoldem")
        reloadConfig()
        server.pluginManager.registerEvents(EventList,this)
        getCommand("poker")!!.setExecutor(Commands)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}