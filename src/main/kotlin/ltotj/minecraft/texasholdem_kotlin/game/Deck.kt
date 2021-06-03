package ltotj.minecraft.texasholdem_kotlin.game

import kotlin.random.Random

class Deck {

    private var deck=ArrayList<Card>()
    private val random= Random

    fun reset(){
        deck=ArrayList<Card>()
        for(suit in 0..3)for(num in 2..14){
            deck.add(Card(suit,num))
        }
    }

    fun draw():Card{
        val card=deck[random.nextInt(deck.size)]
        deck.remove(card)
        return card
    }

}