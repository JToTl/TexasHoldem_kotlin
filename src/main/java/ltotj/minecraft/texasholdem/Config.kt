package ltotj.minecraft.texasholdem_kotlin

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.Plugin


class Config(private val plugin: Plugin) {
    private var config: FileConfiguration? = null
    fun load() {
        plugin.saveDefaultConfig()
        if (config != null) {
            plugin.reloadConfig()
        }
        config = plugin.config
    }

    fun getString(string: String): String {
        return try {
            config!!.getString(string)!!
        } catch (exception: Exception) {
            println("コンフィグから" + string + "の値を取るのに失敗しました")
            ""
        }
    }

    fun getInt(string: String): Int {
        return try {
            config!!.getInt(string)
        } catch (exception: Exception) {
            println("コンフィグから" + string + "の値を取るのに失敗しました")
            0
        }
    }

    fun getDouble(string: String): Double {
        return try {
            config!!.getDouble(string)
        } catch (exception: Exception) {
            println("コンフィグから" + string + "の値を取るのに失敗しました")
            0.0
        }
    }

    init {
        load()
    }
}