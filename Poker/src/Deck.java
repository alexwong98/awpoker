import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * <h1>Deck</h1>
 * <p>
 * This class represents a deck of cards. Therefore, it has methods such as deal
 * and shuffle, which do as suggested by their name.
 * </p>
 * 
 * @author Alex
 * @since 2016-01-19
 *
 */
public class Deck {
	private ArrayList<Card> deck;

	/**
	 * The constructor for Deck generates 52 cards, 13 cards of each of the four
	 * suits.
	 */
	public Deck() {
		deck = new ArrayList<Card>();
		for (int s = 1; s <= 4; s++) { // for each suit
			for (int r = 2; r <= 14; r++) { // for each rank (ranks range from
											// 2-14, two to ace)
				deck.add(new Card(s, r));
			}
		}
	}

	/**
	 * Used to deal cards to each player and the table, without having duplicate
	 * cards.
	 * 
	 * @return the top card of the deck.
	 */
	public Card deal() {
		Card dealtCard = deck.get(0); // get top card
		deck.remove(0); // remove the card from the deck after dealing it, so
						// the next card to be dealt will be dealt next

		return dealtCard;
	}

	/**
	 * Randomizes the order of the cards in the deck. 
	 */
	public void shuffle() {
		long seed = System.nanoTime();
		Collections.shuffle(deck, new Random(seed));
	}

}
