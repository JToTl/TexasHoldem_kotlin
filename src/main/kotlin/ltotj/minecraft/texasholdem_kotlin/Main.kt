package ltotj.minecraft.texasholdem_

import ltotj.minecraft.texasholdem_kotlin.Commands
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.mySQL
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.plugin
import ltotj.minecraft.texasholdem_kotlin.GlobalValues.Companion.vault
import ltotj.minecraft.texasholdem_kotlin.MySQLManager
import ltotj.minecraft.texasholdem_kotlin.VaultManager
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        plugin=this
        vault= VaultManager(this)
        mySQL= MySQLManager(this,"TexasHoldem")
        reloadConfig()
        getCommand("poker")!!.setExecutor(Commands)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}