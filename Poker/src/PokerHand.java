/**
 * <h1>PokerHand</h1>
 * <p>
 * This class represents a pokerhand a player can have. Therefore, it contains
 * its value as a hand (i.e. the type of pokerhand, the defining values, the
 * kickers, etc.). They can be compared to one another using these values.
 * </p>
 * 
 * @author Alex
 * @since 2016-01-19
 */

public class PokerHand implements Comparable {
	private int[] valueOfHand = new int[6];
	private final int HIGH_CARD = 1;
	private final int PAIR = 2;
	private final int TWO_PAIR = 3;
	private final int TRIPLE = 4;
	private final int STRAIGHT = 5;
	private final int FLUSH = 6;
	private final int FULL_HOUSE = 7;
	private final int QUAD = 8;
	private final int STRAIGHT_FLUSH = 9;

	@Override
	public String toString() {
		String typeOfHand = "";
		String determiningRankOfHand = "";
		switch (valueOfHand[1]) {
		case 11:
			determiningRankOfHand = "jack";
			break;
		case 12:
			determiningRankOfHand = "queen";
			break;
		case 13:
			determiningRankOfHand = "king";
			break;
		case 14:
			determiningRankOfHand = "ace";
			break;
		default:
			determiningRankOfHand = valueOfHand[1] + "";
			break;
		}

		switch (valueOfHand[0]) {
		case 1:
			typeOfHand = "high card " + determiningRankOfHand;
			break;
		case 2:
			typeOfHand = "pair " + determiningRankOfHand + "s";
			break;
		case 3:
			typeOfHand = "two pair, high pair " + determiningRankOfHand + "s";
			break;
		case 4:
			typeOfHand = "triple " + determiningRankOfHand + "s";
			break;
		case 5:
			typeOfHand = "straight, high card " + determiningRankOfHand;
			break;
		case 6:
			typeOfHand = "flush, high card " + determiningRankOfHand;
			break;
		case 7:
			typeOfHand = "full house " + determiningRankOfHand + "s";
			break;
		case 8:
			typeOfHand = "quad " + determiningRankOfHand + "s";
			break;
		case 9:
			if (determiningRankOfHand.equals("ace")) {
				typeOfHand = "royal flush";
			} else {
				typeOfHand = "straight flush, high card "
						+ determiningRankOfHand;
			}

			break;
		}
		return typeOfHand;
	}

	/**
	 * This method is used so a winner can be determined (i.e. who has the best
	 * pokerhand), as well as which pokerhand of a player's numerous
	 * possibilities is the best
	 * 
	 * @param otherHand The other hand to be compared to. 
	 */
	@Override
	public int compareTo(Object otherHand) {
		// first compare poker hand rank
		if (valueOfHand[0] > ((PokerHand) otherHand).getValue()[0]) {
			return 1;
		} else if (valueOfHand[0] < ((PokerHand) otherHand).getValue()[0]) {
			return -1;
		} else { // this means they're the same tier of poker hand, need to
					// compare kickers (i.e. if both pairs, compare pairs. if
					// both have the same pair, go to next high card, etc.)
			for (int i = 1; i < valueOfHand.length; i++) {
				if (valueOfHand[i] > ((PokerHand) otherHand).getValue()[i]) {
					return 1;
				} else if (valueOfHand[i] < ((PokerHand) otherHand).getValue()[i]) {
					return -1;
				}
			}
			return 0; // if the code reaches here, that means that the two hands
						// are equal
		}

	}

	/**
	 * Used to get the value of the hand
	 * 
	 * @return the value of the hand
	 */
	public int[] getValue() {
		return valueOfHand;
	}

	/**
	 * Sets hand as a straight flush
	 * 
	 * @param highCard
	 *            The defining value of the hand
	 */
	public void setStraightFlush(int highCard) {
		valueOfHand[0] = STRAIGHT_FLUSH;
		valueOfHand[1] = highCard;
	}

	/**
	 * Set hand as a quad
	 * 
	 * @param quadRank
	 *            The defining value of the hand
	 */
	public void setQuad(int quadRank) {
		valueOfHand[0] = QUAD;
		valueOfHand[1] = quadRank;
	}

	/**
	 * Set hand as a full house
	 * 
	 * @param tripleRank
	 *            The first defining value of the hand
	 * @param doubleRank
	 *            The second defining value of the hand
	 */
	public void setFullHouse(int tripleRank, int doubleRank) {
		valueOfHand[0] = FULL_HOUSE;
		valueOfHand[1] = tripleRank;
		valueOfHand[2] = doubleRank;
	}

	/**
	 * Set hand as a flush
	 * 
	 * @param highCard
	 *            The first defining value of the hand
	 * @param kicker1
	 *            The second defining value of the hand
	 * @param kicker2
	 *            The third defining value of the hand
	 * @param kicker3
	 *            The fourth defining value of the hand
	 * @param kicker4
	 *            The fifth defining value of the hand
	 */
	public void setFlush(int highCard, int kicker1, int kicker2, int kicker3,
			int kicker4) {
		valueOfHand[0] = FLUSH;
		valueOfHand[1] = highCard;
		valueOfHand[2] = kicker1;
		valueOfHand[3] = kicker2;
		valueOfHand[4] = kicker3;
		valueOfHand[5] = kicker4;
	}

	/**
	 * Set the hand as a straight
	 * 
	 * @param highCard
	 *            The first defining value of the hand
	 */
	public void setStraight(int highCard) {
		valueOfHand[0] = STRAIGHT;
		valueOfHand[1] = highCard;
	}

	/**
	 * Set the hand as a triple
	 * 
	 * @param tripleRank
	 *            The first defining value of the hand
	 * @param kicker1
	 *            The second defining value of the hand
	 * @param kicker2
	 *            The third defining value of the hand
	 */
	public void setTriple(int tripleRank, int kicker1, int kicker2) {
		valueOfHand[0] = TRIPLE;
		valueOfHand[1] = tripleRank;
		valueOfHand[2] = kicker1;
		valueOfHand[3] = kicker2;

	}

	/**
	 * Set the hand as a two pair
	 * 
	 * @param double1Rank
	 *            The first defining value of the hand
	 * @param double2Rank
	 *            The second defining value of the hand
	 * @param kicker
	 *            The third defining value of the hand
	 */
	public void setTwoPair(int double1Rank, int double2Rank, int kicker) {
		valueOfHand[0] = TWO_PAIR;
		valueOfHand[1] = double1Rank;
		valueOfHand[2] = double2Rank;
		valueOfHand[3] = kicker;
	}

	/**
	 * Set hand as a pair
	 * 
	 * @param doubleRank
	 *            The first defining value of the hand
	 * @param kicker1
	 *            The second defining value of the hand
	 * @param kicker2
	 *            The third defining value of the hand
	 * @param kicker3
	 *            The fourth defining value of the hand
	 */
	public void setPair(int doubleRank, int kicker1, int kicker2, int kicker3) {
		valueOfHand[0] = PAIR;
		valueOfHand[1] = doubleRank;
		valueOfHand[2] = kicker1;
		valueOfHand[3] = kicker2;
		valueOfHand[4] = kicker3;
	}

	/**
	 * Set hand as a high card
	 * 
	 * @param highCard
	 *            The first defining value of the hand
	 * @param kicker1
	 *            The second defining value of the hand
	 * @param kicker2
	 *            The thirddefining value of the hand
	 * @param kicker3
	 *            The fourth defining value of the hand
	 * @param kicker4
	 *            The fifth defining value of the hand
	 */
	public void setHighCard(int highCard, int kicker1, int kicker2,
			int kicker3, int kicker4) {
		valueOfHand[0] = HIGH_CARD;
		valueOfHand[1] = highCard;
		valueOfHand[2] = kicker1;
		valueOfHand[3] = kicker2;
		valueOfHand[4] = kicker3;
		valueOfHand[5] = kicker4;
	}

}
