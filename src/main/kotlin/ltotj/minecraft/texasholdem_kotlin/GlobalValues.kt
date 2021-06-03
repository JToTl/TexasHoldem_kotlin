package ltotj.minecraft.texasholdem_kotlin

import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.collections.HashMap

class GlobalValues {


    companion object{

        lateinit var config:Config
        lateinit var texasHoldemTables:HashMap<UUID,TexasHoldem>
        lateinit var currentPlayers:HashMap<UUID,UUID>
        lateinit var plugin: Plugin
        var playable:Boolean = false
        lateinit var vault:VaultManager
        lateinit var mySQL:MySQLManager


    }


}