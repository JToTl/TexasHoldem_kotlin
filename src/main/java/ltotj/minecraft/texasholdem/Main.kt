package ltotj.minecraft.texasholdem

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class Main : JavaPlugin() {

    companion object{

        lateinit var texasHoldemTables: ConcurrentHashMap<UUID, TexasHoldem>
        lateinit var currentPlayers:ConcurrentHashMap<UUID, UUID>
        lateinit var plugin: Plugin
        lateinit var playable:AtomicBoolean
        lateinit var vault: VaultManager
        lateinit var mySQL: MySQLManager


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
        texasHoldemTables=ConcurrentHashMap()
        currentPlayers=ConcurrentHashMap()
        plugin =this
        playable=AtomicBoolean()
        playable.set(config.getBoolean("canPlay"))
        vault = VaultManager(this)
        mySQL = MySQLManager(this, "texasholdem.TexasHoldem")
        server.pluginManager.registerEvents(EventList,this)
        getCommand("poker")!!.setExecutor(Commands)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}