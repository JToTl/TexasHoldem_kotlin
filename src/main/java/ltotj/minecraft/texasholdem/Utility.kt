package ltotj.minecraft.texasholdem_kotlin

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object Utility {

    fun getYenString(money:Double):String{//１２桁まで対応
        val yen=StringBuilder().append("円")
        val integerPart= floor(money)
        if(integerPart!=0.0) {
            val end = floor(log10(integerPart)).toInt() / 3
            for (i in 0 until end) {
                for (j in 0 until 3) {
                    yen.append(floor((integerPart - floor(integerPart / 10.0.pow(i * 3 + j + 1)) * 10.0.pow(i * 3 + j + 1)) / 10.0.pow(i * 3 + j)).toInt())
                }
                yen.append(",")
            }
            yen.append(floor(integerPart / 10.0.pow(end * 3)).toInt().toString().reversed())
            yen.reverse()
        }
        else yen.append("0").reverse()
        return yen.toString()
    }

}