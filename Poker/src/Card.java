/**
 * <h1>Card</h1>
 * <p>
 * This class represents a card in a deck of playing cards, therefore it has
 * values such as its suit and rank.
 * </p>
 * 
 * @author Alex
 * @since 2016-01-19
 * 
 */
public class Card implements Comparable {
	private int suit;
	private int rank;

	/**
	 * Creates a card with a certain rank and suit
	 * 
	 * @param suit
	 *            The suit (diamond, club, heart, spade) of the card
	 * @param rank
	 *            The rank (2-ace) of the card
	 */
	public Card(int suit, int rank) {
		this.rank = rank;
		this.suit = suit;

	}

	/**
	 * Returns the card as a string containing its rank and suit
	 */
	public String toString() {
		return (suit + "-" + rank);
	}

	/**
	 * This is so ace can be treated as both a "1" and the highest value card
	 * i.e. for determining straight
	 * @return whether or not the card is an ace
	 */
	public boolean isAce() {
		if (rank == 14) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the rank of the card.
	 * 
	 * @return the rank of the card.
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * Returns the suit of the card.
	 * 
	 * @return the suit of the card.
	 */
	public int getSuit() {
		return suit;
	}

	/**
	 * Cards are compared based on first their suit, then their rank
	 */
	@Override
	public int compareTo(Object otherCard) {
		//first compare rank
		if (rank > ((Card) otherCard).getRank()) {
			return 1;
		} else if (rank < ((Card) otherCard).getRank()) {
			return -1;
		} else { // same rank, so compare suits
			if (suit > ((Card) otherCard).getSuit()) {
				return 1;
			} else { // has to be lower suit, there are no two of the same card
						// in a deck
				return -1;
			}
		}
	}

}
