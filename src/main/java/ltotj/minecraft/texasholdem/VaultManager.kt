package ltotj.minecraft.texasholdem

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Created by takatronix on 2017/03/04.
 */
class VaultManager(private val plugin: JavaPlugin) {
    private fun setupEconomy(): Boolean {
        plugin.logger.info("setupEconomy")
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.logger.warning("Vault plugin is not installed")
            return false
        }
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            plugin.logger.warning("Can't get vault service")
            return false
        }
        economy = rsp.provider
        plugin.logger.info("Economy setup")
        return economy != null
    }

    /////////////////////////////////////
    //      残高確認
    /////////////////////////////////////
    fun getBalance(uuid: UUID?): Double {
        return economy!!.getBalance(Bukkit.getOfflinePlayer(uuid!!).player)
    }

    /////////////////////////////////////
    //      残高確認
    /////////////////////////////////////
    fun showBalance(uuid: UUID?) {
        val p: OfflinePlayer? = Bukkit.getOfflinePlayer(uuid!!).player
        val money = getBalance(uuid)
        p!!.player!!.sendMessage(ChatColor.YELLOW.toString() + "あなたの所持金は$" + money.toInt())
    }

    /////////////////////////////////////
    //      引き出し
    /////////////////////////////////////
    fun withdraw(player: Player, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(player.uniqueId)
        if (p == null) {
            Bukkit.getLogger().info(player.toString() + "は見つからない")
            return false
        }
        val resp = economy!!.withdrawPlayer(p, money)
        if (resp.transactionSuccess()) {
            if (p.isOnline) {
                p.player!!.sendMessage(ChatColor.YELLOW.toString() + "$" + money + "支払いました")
            }
            return true
        }
        return false
    }

    /////////////////////////////////////
    //      お金を入れる
    /////////////////////////////////////
    fun deposit(player: Player, money: Double): Boolean {
        val p = Bukkit.getOfflinePlayer(player.uniqueId)
        if (p == null) {
            Bukkit.getLogger().info(player.toString() + "は見つからない")
            return false
        }
        val resp = economy!!.depositPlayer(p, money)
        if (resp.transactionSuccess()) {
            if (p.isOnline) {
                p.player!!.sendMessage(ChatColor.YELLOW.toString() + "$" + money + "受取りました")
            }
            return true
        }
        return false
    }

    companion object {
        var economy: Economy? = null
    }

    init {
        setupEconomy()
    }
}