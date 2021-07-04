package ltotj.minecraft.texasholdem.game

import kotlin.math.pow
import kotlin.math.roundToLong

class PlayerCards {
    var cards=ArrayList<Card>()

    fun addCard(card: Card){
        cards.add(card)
    }

    fun reset() {
        cards.clear()
    }

    private fun sortCardNum(sortedCards:ArrayList<Card>){
        for(i in 0 until sortedCards.size)for(j in i+1 until sortedCards.size){
            if(sortedCards[j].num<sortedCards[i].num){
                val insCard=sortedCards[j]
                sortedCards[j]=sortedCards[i]
                sortedCards[i]=insCard
            }
            else if(sortedCards[j].num==sortedCards[i].num&&sortedCards[j].suit<sortedCards[i].suit){
                val insCard=sortedCards[j]
                sortedCards[j]=sortedCards[i]
                sortedCards[i]=insCard
            }
        }
    }

    private fun sortCardSuit(sortedCards:ArrayList<Card>){
        for(i in 0 until sortedCards.size)for(j in i+1 until sortedCards.size){
            if(sortedCards[j].suit<sortedCards[i].suit){
                val insCard=sortedCards[j]
                sortedCards[j]=sortedCards[i]
                sortedCards[i]=insCard
            }
            else if(sortedCards[j].suit==sortedCards[i].suit&&sortedCards[j].num<sortedCards[i].num){
                val insCard=sortedCards[j]
                sortedCards[j]=sortedCards[i]
                sortedCards[i]=insCard
            }
        }
    }

    fun getHand(community:ArrayList<Card>):Double {//フラッシュ系統のみ1桁目はsuit その他は0
        var dupSuit = 0
        var dupNum = 0
        var straight = false
        val judgedCards = ArrayList<Card>()
        judgedCards.addAll(cards)
        judgedCards.addAll(community)
        var hand = 0.0

        for (i in 0..6) for (j in i + 1..6) {
            if (judgedCards[i].suit == judgedCards[j].suit) dupSuit += 1
            if (judgedCards[i].num == judgedCards[j].num) dupNum += 1
        }
        if (dupSuit > 9) {//フラッシュ系統
            val insCards = ArrayList<Card>()
            sortCardSuit(judgedCards)
            insCards.add(judgedCards[3])
            for (i in 0 until 7) if (judgedCards[i].suit == judgedCards[3].suit && i != 3) insCards.add(judgedCards[i])
            sortCardNum(insCards)
            if (insCards[0].num * insCards[3].num == 10 && insCards[insCards.size - 1].num == 14) {//ローストレートフラッシュ
                hand = 1805040302140.0 + insCards[0].suit
                straight = true
            }
            for (i in insCards.size - 5 downTo 0) {
                if (insCards[i + 4].num == 4) {
                    hand = if (insCards[i + 4].num == 14) 1914131211100.0 + insCards[0].suit
                    else 1800000000000.0 + 1000000000.0 * insCards[i + 4].num + 10000000.0 * insCards[i + 3].num + 100000.0 * insCards[i + 2].num + 1000.0 * insCards[i + 1].num + 10.0 * insCards[i].num + 1.0*insCards[0].suit
                    straight = true
                    break
                }
            }
            if (!straight) {
                hand = 1500000000000.0+1.0*insCards[0].suit
                for (i in insCards.size - 1 downTo insCards.size - 5) {
                    hand += insCards[i].num * 100.0.pow(5 + i - insCards.size) * 10
                }
            }
        }
        sortCardNum(judgedCards)
        if (dupNum < 4 && dupSuit < 10) {//ストレート
            val insCards = ArrayList<Card>()
            insCards.add(judgedCards[0])
            for (i in 1..6) if (judgedCards[i].num != judgedCards[i - 1].num) insCards.add(judgedCards[i])
            if (insCards[0].num * insCards[3].num == 10 && insCards[insCards.size - 1].num == 14) {//ローストレート
                hand = 1405040302140.0
                straight = true
            }
            for (i in insCards.size-5 downTo 0) {
                if (insCards[i + 4].num - insCards[i].num == 4) {
                    hand = 1400000000000.0
                    for (j in 4 downTo 0) hand += insCards[i + j].num * 100.0.pow(j) * 10
                    straight = true
                    break
                }
            }
        }
        when (dupNum) {
            9, 7 -> hand = 1700000000000.0 + 1010101000.0 * judgedCards[3].num + 10.0 * (judgedCards[2].num + judgedCards[6].num - judgedCards[3].num)
            6 -> hand = if (judgedCards[1].num == judgedCards[2].num && judgedCards[4].num == judgedCards[5].num) 1600000000000.0 + 1010100000.0 * judgedCards[4].num + judgedCards[2].num * 1010.0
            else 1700000000000.0 + 1010101000.0 * judgedCards[3].num + 10 * (judgedCards[2].num + judgedCards[6].num - judgedCards[3].num)
            5, 4 -> {//FH
                var k=0
                for (i in 6 downTo 2) {
                    if (judgedCards[i].num == judgedCards[i - 2].num) {
                        hand = 1600000000000.0 + judgedCards[i].num * 1010100000.0
                        k = i
                        break
                    }
                }
                for (i in 6 downTo 1) {
                    if (judgedCards[i].num == judgedCards[i - 1].num && judgedCards[i].num != judgedCards[k].num) {
                        hand += judgedCards[i].num * 1010;
                        break
                    }
                }
            }
        }

        if (!straight && dupSuit < 10) {
            when (dupNum) {
                3 -> if ((judgedCards[6].num == judgedCards[5].num && judgedCards[6].num != judgedCards[4].num) || (judgedCards[0].num == judgedCards[1].num && judgedCards[0].num != judgedCards[2].num)) {
                    hand = 1200000000000.0 + judgedCards[5].num * 1010000000.0 + judgedCards[3].num * 101000.0 + 10 * (judgedCards[6].num + judgedCards[4].num + judgedCards[2].num - judgedCards[5].num - judgedCards[3].num)
                } else {
                    var k=0
                    for (i in 6 downTo 2) {
                        if (judgedCards[i].num == judgedCards[i - 1].num) {
                            hand = 1300000000000.0 + judgedCards[i].num * 1010100000.0
                            k = i
                            break;
                        }
                    }
                    var count = 0
                    for (i in 6 downTo 0) {
                        if (judgedCards[i].num == judgedCards[k].num) continue
                        hand += 100.0.pow(1 - count) * judgedCards[i].num * 10.0
                        count += 1
                        if (count == 2) break
                    }
                }
                2 -> {
                    var k=0
                    var r=0
                    for (i in 6 downTo 3) {
                        if (judgedCards[i].num == judgedCards[i - 1].num) {
                            hand = 1200000000000.0 + 1010000000.0 * judgedCards[i].num
                            k = i
                            break
                        }
                    }
                    for (i in k - 2 downTo 1) {
                        if (judgedCards[i].num == judgedCards[i - 1].num) {
                            hand += 101000.0 * judgedCards[i].num
                            r = i
                            break
                        }
                    }
                    for (i in 6 downTo 4) {
                        if (judgedCards[i].num != judgedCards[k].num && judgedCards[i].num != judgedCards[r].num) {
                            hand += judgedCards[i].num * 10.0
                            break
                        }
                    }
                }
                1 -> {
                    var k=0
                    for (i in 6 downTo 1) {
                        if (judgedCards[i].num == judgedCards[i - 1].num) {
                            hand = 1100000000000.0 + judgedCards[i].num * 1010000000.0
                            k = i
                            break
                        }
                    }
                    var count = 0
                    for (i in 6 downTo 0) {
                        if (judgedCards[i].num == judgedCards[k].num) continue
                        hand += 100.0.pow(2 - count) * judgedCards[i].num * 10.0
                        count += 1
                        if (count == 3) break
                    }
                }
                0 -> {
                    hand = 1000000000000.0
                    for (i in 6 downTo 2) {
                        hand += 100.0.pow(i - 2) * judgedCards[i].num * 10
                    }
                }
            }
        }
        return hand.roundToLong().toDouble()
    }
}