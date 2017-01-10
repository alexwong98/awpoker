import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * <h1>PokerServer</h1>
 * <p>
 * A rendition of the classic game of Poker (Texas hold'em) for multiple
 * computers across a network. This class handles the logic of the poker game as
 * well as the establishment of connections with clients.
 * </p>
 * 
 * @author Alex Wong
 * @author http://cs.lmu.edu/ (for starting server/client concept)
 * @since 2016-01-19
 * 
 */

public class PokerServer {
	private final static int SMALL_BLIND = 2;
	private final static int BIG_BLIND = 4;
	private static int startingChips;
	private static int maxPlayers;
	private static int pot;
	private static int numFolded;
	private static int round;
	private static boolean onePlayerInRound = false;
	private static Deck deck;
	private static ArrayList<Card> table; // contains the five table cards

	/**
	 * the port that the server listens on
	 */
	private static final int PORT = 9001;

	/**
	 * set of names for every client. duplicates are prevented from being
	 * created.
	 */
	private static HashSet<String> names = new HashSet<String>();
	private static ArrayList<PrintWriter> playerInOut = new ArrayList<PrintWriter>();
	private static ArrayList<PlayerHandler> players = new ArrayList<PlayerHandler>();

	/**
	 * Waits for clients to connect, and when enough are connected, the poker
	 * game begins.
	 * 
	 * @throws Exception
	 *             if the Thread.sleep method fails.
	 * 
	 */
	public static void main(String[] args) throws Exception {
		// this frame is never visible, just created so JOptionPane dialogs can
		// be created
		JFrame frame = new JFrame();

		maxPlayers = Integer.parseInt(JOptionPane.showInputDialog(frame,
				"Enter number of players:", "Poker Server",
				JOptionPane.PLAIN_MESSAGE));

		// don't proceed until max players has been determined
		while (maxPlayers == 0) {
			Thread.sleep(100);
		}

		startingChips = Integer.parseInt(JOptionPane.showInputDialog(frame,
				"How many chips should each player start with?",
				"Poker Server", JOptionPane.PLAIN_MESSAGE));

		// don't proceed until max chips has been determined
		while (startingChips == 0) {
			Thread.sleep(100);
		}

		System.out.println("The poker server is running.");

		// open up a new socket at the specified port for clients to connect to
		ServerSocket listener = new ServerSocket(PORT);

		// loop until the desired number of clients have connected
		int i = 0;
		int numPlayers = 0;
		try {
			while (numPlayers < maxPlayers) {
				players.add(new PlayerHandler(listener.accept(), startingChips));
				System.out.println("starting " + i);
				players.get(i).start();
				numPlayers++;
				i++;
			}
		} finally {
			listener.close();
		}

		// don't proceed until all players have successfully joined
		// the server
		boolean allPlayersInitialized = false;
		while (!allPlayersInitialized) {
			// check if all players are initialized
			// "true until proven false" concept
			allPlayersInitialized = true;
			for (PlayerHandler PlayerHandler : players) {
				if (!PlayerHandler.isInitialized()) {
					allPlayersInitialized = false;
				}
			}

			pauseForMsgProcessing();
		}

		initializeGame();

		boolean onePlayerRemains = false;
		while (!onePlayerRemains) {

			runRound();

			// remove player from the list of clients if they have no chips
			for (int j = 0; j < players.size(); j++) {
				if (players.get(j).getChips() == 0) {
					players.get(j).setOutput("OUT");
					players.remove(j);
					j--;
				}
			}

			if (players.size() == 1) {
				// this cues the game to finish
				onePlayerRemains = true;
			}
		}

		pauseForMsgProcessing();

		players.get(0).setOutput("WONGAME");

		System.exit(1);

	}

	/**
	 * Runs through a round of Texas hold'em, reseting all variables, moving the
	 * blinds, and after the four stages have passed, determines the winner.
	 */
	public static void runRound() {
		// reset everything server side
		round++;
		pot = 0;
		numFolded = 0;
		deck = new Deck();
		table.clear();
		onePlayerInRound = false;
		// resets PlayerHandler private fields
		for (PlayerHandler player : players) {
			player.reset();
		}

		broadcastMsg("Round " + round, "MESSAGE");

		for (PlayerHandler player : players) {
			broadcastMsg(player.getPlayerName() + " has " + player.getChips()
					+ " chips.", "MESSAGE");
		}

		// move big and small blinds, and account for them in the pot/player's
		// amount betted
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).isBigBlind()) {
				players.get(i).setBigBlind(false);
				getNextPlayer(i).setBigBlind(true);
				getNextPlayer(i).setOutput("BIGBLIND");
				getNextPlayer(i).bet(BIG_BLIND);
				pot += BIG_BLIND;

				// that big blind is now small blind
				players.get(i).setSmallBlind(true);
				players.get(i).setOutput("SMALLBLIND");
				players.get(i).bet(SMALL_BLIND);
				pot += SMALL_BLIND;
				getPreviousPlayer(i).setSmallBlind(false);

				break;
			}
		}

		pauseForMsgProcessing();

		deck.shuffle();
		dealHands();

		// show players their hands (their two cards)
		for (PlayerHandler player : players) {
			player.lookAtHand();
		}

		pauseForMsgProcessing();

		// four stages, each representing a stage in the game
		// each stage (excluding the first, where no cards are dealt) has a
		// card(s) dealt then a round of betting, before the next stage is
		// reached.
		for (int stage = 0; stage <= 3; stage++) {
			if (stage == 1) {
				// deal the flop (3 cards)
				for (int i = 1; i <= 3; i++) {
					Card dealtCard = deck.deal();
					table.add(dealtCard);
					for (PlayerHandler player : players) {
						player.addCard(dealtCard);
					}

				}
			} else if (stage == 2 || stage == 3) {
				// for the turn and river, only one card is dealt
				Card dealtCard = deck.deal();
				table.add(dealtCard);
				for (PlayerHandler player : players) {
					player.addCard(dealtCard);
				}

			}

			// update table for clients
			for (PlayerHandler player : players) {
				player.lookAtTable(table);
			}

			// ensures that the cards are dealt (as there is a delay for
			// aesthetic purposes)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			runBetting(stage);

			// if one player is left in the round, stop betting process
			if (onePlayerInRound) {
				break;
			}
		}

		if (onePlayerInRound) {
			// this means that everyone else folded. this player wins by
			// default.
			for (PlayerHandler player : players) {
				if (!player.folded()) {
					player.addChips(pot);
					broadcastMsg(player.getPlayerName() + " wins " + pot
							+ " chips!", "MESSAGE");
					break;
				}
			}
		} else {
			// otherwise, the player with the best hand has to be determined
			for (PlayerHandler player : players) {
				player.calculateBestPokerHand();
				player.setOutput("MESSAGEYou have a " + player.getPokerHand());
			}

			pauseForMsgProcessing();

			ArrayList<PlayerHandler> winners = getWinners();

			// one winner, gets the whole pot
			if (winners.size() == 1) {
				winners.get(0).addChips(pot);
				broadcastMsg(winners.get(0).getPlayerName() + " wins with "
						+ winners.get(0).getPokerHand() + ". "
						+ winners.get(0).getPlayerName() + " wins " + pot
						+ " chips!", "WINNER");
			} else {
				// otherwise, split the pot between winners
				String tiedMessage = "";
				for (PlayerHandler winner : winners) {
					winner.addChips(pot / winners.size());
					tiedMessage += winner.getPlayerName() + " and ";
				}

				// to remove the last " and "
				tiedMessage = tiedMessage
						.substring(0, tiedMessage.length() - 6);
				broadcastMsg(tiedMessage + " tied with "
						+ winners.get(0).getPokerHand() + ". "
						+ "They each win " + pot / winners.size() + " chips!",
						"WINNER");
			}
		}

		broadcastMsg("Preparing for next round...", "MESSAGE");

		// allow players to reflect/rejoice before starting a new round
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// tell each client to clean up their interface for next round
		broadcastMsg("", "ENDROUND");

	}

	/**
	 * This is called after everytime an output message is sent. This ensures
	 * that ample time is given for the server to comprehend that output message
	 * and write it to the clients, before another output message is determined.
	 * Without this pause, output messages may overwrite one another.
	 */
	public static void pauseForMsgProcessing() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method writes a message to all clients
	 * 
	 * @param msg
	 *            The message to be sent.
	 * @param type
	 *            The type of message (i.e. whether it be a message for all
	 *            players, or information about the game, etc.)
	 */
	public static void broadcastMsg(String msg, String type) {
		for (PlayerHandler player : players) {
			player.setOutput(type + msg);
		}
		pauseForMsgProcessing();
	}

	/**
	 * This determines the winner of the round by comparing the player's best
	 * pokerhands.
	 * 
	 * @return the list of winners for the round (can be more than 1 winner).
	 */
	public static ArrayList<PlayerHandler> getWinners() {
		// clone the list of players so that it can be sorted (can't sort the
		// original, as order matters (i.e. blinds))
		ArrayList<PlayerHandler> listOfPlayers = (ArrayList<PlayerHandler>) players
				.clone();
		Collections.sort(listOfPlayers);
		// set the winner to be the first player, since the list has been
		// sorted (players are compared based on their pokerhands)
		PlayerHandler winner = listOfPlayers.get(listOfPlayers.size() - 1);
		ArrayList<PlayerHandler> listOfWinners = new ArrayList<PlayerHandler>();
		listOfWinners.add(winner);
		// in the case that any player has the same hand as the winner, add them
		// to the list of winners as well
		for (int i = listOfPlayers.size() - 2; i >= 0; i--) {
			if (listOfPlayers.get(i).compareTo(winner) == 0) {
				listOfWinners.add(listOfPlayers.get(i));
			}
		}

		return listOfWinners;
	}

	/**
	 * This method runs the betting of each round. Betting continues until 1. if
	 * it is the first stage, everyone calls the blind 2. everyone checks 3.
	 * everyone has called the latest raise 4. everyone folds except one person
	 * 
	 * @param stage
	 *            The current stage of the game that affects how betting is run
	 *            (i.e. before game starts, big blind acts last, but afterwards,
	 *            big blind always acts second)
	 */
	public static void runBetting(int stage) {

		// first position is the first person to act each round
		int firstPosition = 0;
		int currentBet = 0;
		if (stage == 0) { // before cards are laid down, first position is the
							// one after big blind. the minimum bet is the big
							// blind.
			currentBet = BIG_BLIND;
			for (int i = 0; i < players.size(); i++) {
				if (getPreviousPlayer(i).isBigBlind()) {
					firstPosition = i;
					break;
				}
			}
		} else { // for the rest of the game, first to act is the first player,
					// starting from the small blind, who has not folded
			currentBet = 0;
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).isSmallBlind()) {
					firstPosition = i;
					break;
				}
			}
		}

		boolean bettingOver = false;
		int currentPlayer = firstPosition;
		boolean goAllTheWay = false;
		int successfulActs = 0; // if playerSize() - numFolded ==
								// successfulActs,
		// all players in the game have acted.
		boolean lonePlayerActed = false;

		while (!bettingOver || onePlayerInRound) {

			if (!players.get(currentPlayer).folded()) {
				// check if one person is playing against all-ined players
				// if that's the case, just lay all the cards down
				// this is done by counting each player's move as a
				// "successful act"
				// despite them not doing anything (b/c they can't)

				// go all the way determines if all the cards can be laid down
				// or not. this occurs when no one else can act (or only one
				// person can act, which would be pointless)

				int playersToAct = players.size();
				int lastPlayerToAct = 0; // the player who has to act if
											// everyone else folded or all-ined

				for (int i = 0; i < players.size(); i++) {
					if (players.get(i).allIned() || players.get(i).folded()) {
						playersToAct--;
					} else {
						lastPlayerToAct = i;
					}
				}

				// this means that everyone all-ined/folded except for one
				if (playersToAct == 1) {
					// if the last player acted, then all the cards can be laid
					// down (no decisions left to make)
					if (players.get(lastPlayerToAct).acted()) {
						goAllTheWay = true;
					} else { // otherwise, wait until that player has acted
								// (then lay down all the cards)
						goAllTheWay = false;
					}
				}

				// if the person can act
				if (!players.get(currentPlayer).allIned && !goAllTheWay) {

					int amountToCall = players.get(currentPlayer).amountToCall(
							currentBet);

					// update clients with the current pot and person to act
					broadcastMsg(players.get(currentPlayer).getPlayerName()
							+ " " + pot + " " + currentBet, "INFO");

					// update each client with the amount they need to call
					// and their chips
					for (PlayerHandler player : players) {
						player.setOutput("PERSONALINFO" + player.getChips()
								+ " " + player.amountToCall(currentBet));
					}

					pauseForMsgProcessing();

					// prompt the current player to make a decision
					players.get(currentPlayer).setOutput("ACTION");

					String playerAction = players.get(currentPlayer)
							.getPlayerAction();

					// loop until player decision has been received and
					// established
					while (playerAction == null) {
						playerAction = players.get(currentPlayer)
								.getPlayerAction();
						pauseForMsgProcessing();
					}

					// player has the option to check, raise, or fold
					if (amountToCall == 0) {
						if (playerAction.equals("check")) {
							successfulActs++;
							broadcastMsg(players.get(currentPlayer)
									.getPlayerName() + " checks.", "CHECK");
							players.get(currentPlayer).act();
						} else if (playerAction.startsWith("raise")) {
							int raisedAmount = Integer.parseInt(playerAction
									.substring(6));
							// a raise is a call of the current bet, then a
							// raise on top
							players.get(currentPlayer).call(currentBet);
							pot += amountToCall;
							players.get(currentPlayer).raise(raisedAmount);
							pot += raisedAmount;
							successfulActs = 1; // resets the successful act
												// count b/c everyone needs to
												// decide if they want to call
												// the new raise, or fold
							currentBet += raisedAmount;

							if (players.get(currentPlayer).getChips() == 0) {
								// in the case that they raise and it's an
								// all-in
								players.get(currentPlayer).allIn();
								broadcastMsg(players.get(currentPlayer)
										.getPlayerName()
										+ " goes all in! ("
										+ raisedAmount + " chips).", "ALLIN");

							} else {
								broadcastMsg(players.get(currentPlayer)
										.getPlayerName()
										+ " raises "
										+ raisedAmount + ".", "RAISE");

							}

						} else if (playerAction.equals("fold")) {
							players.get(currentPlayer).fold();
							numFolded++;

							broadcastMsg(players.get(currentPlayer)
									.getPlayerName() + " folds.", "FOLD");

							// tells player to turn over his cards
							players.get(currentPlayer).setOutput("FOLDED");

							pauseForMsgProcessing();
						}
						// player has the option to call, raise, or fold
					} else if (amountToCall > 0) {

						if (playerAction.equals("call")) {
							successfulActs++;
							int chipsBeforeCalling = players.get(currentPlayer)
									.getChips();
							players.get(currentPlayer).call(currentBet);

							// if they go all-in by calling
							if (players.get(currentPlayer).getChips() == 0) {
								pot += chipsBeforeCalling;
								broadcastMsg(players.get(currentPlayer)
										.getPlayerName() + " calls (all in).",
										"ALLIN");
								players.get(currentPlayer).allIn();

							} else {
								pot += amountToCall;
								broadcastMsg(players.get(currentPlayer)
										.getPlayerName() + " calls.", "CALL");

							}

						} else if (playerAction.equals("fold")) {
							players.get(currentPlayer).fold();
							numFolded++;
							broadcastMsg(players.get(currentPlayer)
									.getPlayerName() + " folds.", "FOLD");

							// tells player to turn over his cards
							players.get(currentPlayer).setOutput("FOLDED");

							pauseForMsgProcessing();

						} else if (playerAction.startsWith("raise")) {
							int raisedAmount = Integer.parseInt(playerAction
									.substring(6));
							players.get(currentPlayer).call(currentBet);
							pot += amountToCall;
							players.get(currentPlayer).raise(raisedAmount);
							pot += raisedAmount;
							successfulActs = 1;
							currentBet += raisedAmount;

							if (players.get(currentPlayer).getChips() == 0) {
								players.get(currentPlayer).allIn();
								broadcastMsg(players.get(currentPlayer)
										.getPlayerName()
										+ " goes all in! ("
										+ raisedAmount + " chips).", "ALLIN");

							} else {
								broadcastMsg(players.get(currentPlayer)
										.getPlayerName()
										+ " raises "
										+ raisedAmount + ".", "RAISE");

							}

						}

					}

					// reupdate player clients with new information
					amountToCall = players.get(currentPlayer).amountToCall(
							currentBet);

					broadcastMsg(players.get(currentPlayer).getPlayerName()
							+ " " + pot + " " + currentBet, "INFO");

					for (PlayerHandler player : players) {
						player.setOutput("PERSONALINFO" + player.getChips()
								+ " " + player.amountToCall(currentBet));
					}

					pauseForMsgProcessing();

				} else { // this means that the player cannot act. count it as a
							// succesful act, and move to the next player
					successfulActs++;
				}

			}

			// this means that everyone who hasn't folded has acted
			if (players.size() - numFolded == successfulActs) {
				bettingOver = true;
			} else { // otherwise, move to next player (who hasn't folded)
				boolean isActivePlayer = false;

				while (!isActivePlayer) {
					if (currentPlayer == players.size() - 1) {
						currentPlayer = 0;
					} else {
						currentPlayer++;
					}

					if (!players.get(currentPlayer).folded()) {
						isActivePlayer = true;
					}

				}
			}

			if (players.size() - numFolded == 1) {
				onePlayerInRound = true;
				break;
			}

		}

		// at the end of betting, reset the amount betted in round
		// for each player, for the next betting round
		for (PlayerHandler player : players) {
			player.resetAmountBettedInRound();
		}

	}

	/**
	 * Deals two cards to each player
	 */
	public static void dealHands() {
		for (int c = 1; c <= 2; c++) {
			for (PlayerHandler player : players) {
				player.addCard(deck.deal());
			}
		}
	}

	/**
	 * Gets next player in turn (used for when calculating next player at last
	 * PlayerHandler in arraylist
	 * 
	 * @param i
	 *            The index of the current player.
	 * @return the next player.
	 */
	public static PlayerHandler getNextPlayer(int i) {
		if (i == players.size() - 1) {
			return players.get(0);
		} else {
			return players.get(i + 1);
		}
	}

	/**
	 * Gets previous player (used for when calculating previous player for first
	 * player in arraylist)
	 * 
	 * @param i
	 *            The index of the current player.
	 * @return the previous player.
	 */
	public static PlayerHandler getPreviousPlayer(int i) {
		if (i == 0) {
			return players.get(players.size() - 1);
		} else {
			return players.get(i - 1);
		}
	}

	/**
	 * Initializes the deck and table, and sets blinds.
	 */
	public static void initializeGame() {

		deck = new Deck();
		table = new ArrayList<Card>();

		players.get(1).setBigBlind(true);
		players.get(0).setSmallBlind(true);

		System.out.println("Game has been initialized.");
	}

	/**
	 * <h1>PlayerHandler</h1>
	 * <p>
	 * The PlayerHandler class represents both the player in the game (i.e.
	 * contains their chips, whether or not they folded), as well as the Handler
	 * for the actual client they are connected to (i.e. handles communication
	 * between the server and client programs).
	 * </p>
	 */
	private static class PlayerHandler extends Thread implements Comparable {
		private String name;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;
		private String outputMsg;
		private boolean initialized = false;
		private boolean acted = false;
		private boolean inGame = true;
		private boolean folded = false;
		private boolean allIned = false;
		private boolean smallBlind = false;
		private boolean bigBlind = false;
		private int chips;
		private int amountBettedInRound;
		private PokerHand bestPokerHand;
		private ArrayList<Card> availableCards = new ArrayList<Card>();
		private ArrayList<ArrayList<Card>> possibleHands = new ArrayList<ArrayList<Card>>();
		private String playerAction = null;

		/**
		 * PlayerHandler constructor, created everytime a client connects to the
		 * server.
		 * 
		 * @param socket
		 *            The socket on the server that the client connects to.
		 * @param chips
		 *            The number of chips the player starts with.
		 */
		public PlayerHandler(Socket socket, int chips) {
			this.socket = socket;
			this.chips = chips;
		}

		/**
		 * Resets variables, to refresh for the next round.
		 */
		public void reset() {
			allIned = false;
			folded = false;
			availableCards.clear();
			possibleHands.clear();
			bestPokerHand = null;
		}

		/**
		 * This is what is run throughout the program, after a connection has
		 * been established. The name of the player is determined, then the
		 * method enters a while(true) loop that processes output from the
		 * server and input from the client, and acts appropriately.
		 */
		public void run() {
			try {
				// Create character streams for the socket.
				in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				// request a name for the client (the synchronized (names)
				// ensures that there are no duplicate player names).
				while (true) {
					out.println("SUBMITNAME");
					name = in.readLine();
					if (name == null) {
						return;
					}
					synchronized (names) {
						if (!names.contains(name)) {
							names.add(name);
							break;
						}
					}
				}

				playerInOut.add(out);

				initialized = true;

				while (inGame) {
					try {

						if (outputMsg != null) {

							out.println(outputMsg);

							// the "ACTION" msg prompts the player for a
							// response
							// read the response and set it as the player's
							// decision
							if (outputMsg.startsWith("ACTION")) {
								String input = in.readLine();

								while (input == null) {
									input = in.readLine();
								}

								playerAction = input;

								System.out.println("Returned from " + name
										+ ": " + in.readLine());
								input = null;
							} else if (outputMsg.equals("OUT")
									|| outputMsg.equals("WONGAME")) {
								inGame = false;
							}

							outputMsg = null;

						}

						// prevents server from overloading/crashing
						Thread.sleep(10);
					} catch (InterruptedException ex) {
						System.out.println("Thread is being interrupted" + ex);
						Thread.currentThread().interrupt();

					}
				}

			} catch (IOException e) {
				System.out.println(e);
			} finally {
				// the client is closing. remove its name and its print writer
				// from the set/arraylist, and close its socket
				if (name != null) {
					names.remove(name);
				}
				if (out != null) {
					playerInOut.remove(out);
				}
				try {
					socket.close();
				} catch (IOException e) {
				}
			}

		}

		/**
		 * 
		 * @return whether or not the player all-ined.
		 */
		public boolean allIned() {
			return allIned;
		}

		/**
		 * Sets the player to be all in
		 */
		public void allIn() {
			allIned = true;
		}

		/**
		 * Used to update information, send server-wide messages, and prompt for
		 * responses from the clients.
		 * 
		 * @param outputMsg
		 *            The message to be sent to the corresponding client
		 */
		public void setOutput(String outputMsg) {
			this.outputMsg = outputMsg;
			System.out.println(outputMsg);
		}

		/**
		 * Used to calculate what the player can do, and how much they subtract
		 * from their chips if they call/raise
		 * 
		 * @param currentBet
		 *            The current bet of the betting round.
		 * @return the amount required to call the current bet.
		 */
		public int amountToCall(int currentBet) {
			return currentBet - amountBettedInRound;
		}

		/**
		 * Adds dealt cards to the PlayerHandler
		 * 
		 * @param card
		 *            The card dealt.
		 */
		public void addCard(Card card) {
			availableCards.add(card);
		}

		/**
		 * Used to increment a player's chips if they win a round.
		 * 
		 * @param chips
		 *            Amount of chips won.
		 */
		public void addChips(int chips) {
			this.chips += chips;
		}

		/**
		 * Used in the scenario that all players have folded/all-ined except for
		 * one. This is used to determine whether or not that player has acted
		 * yet. if they did, then all the cards are dealt. If not, that will
		 * occur after the player acts.
		 * 
		 * @return whether or not the player acted.
		 */
		public boolean acted() {
			return acted;
		}

		/**
		 * This method is used to receive the action that the client sends to
		 * the server.
		 * 
		 * @return the player's decision for the bet.
		 */
		public String getPlayerAction() {
			String action = playerAction;
			playerAction = null;
			return action;
		}

		/**
		 * Used for the big blind and small blind, who have to bet a certain
		 * amount at the beginning of each round
		 * 
		 * @param amount
		 *            The amount the player needs to put in.
		 */
		public void bet(int amount) {
			chips -= amount;
			amountBettedInRound += amount;
		}

		/**
		 * Used when a player raises the current bet.
		 * 
		 * @param newBet
		 *            The raised amount.
		 */
		public void raise(int newBet) {

			if (chips - newBet > 0) { // have enough to raise
				chips -= newBet;
				amountBettedInRound += newBet;
			} else { // all in
				amountBettedInRound += chips;
				chips = 0;
			}

			for (PlayerHandler player : players) {
				player.resetAct();
			}

			acted = true;

		}

		/**
		 * Used when the player calls (matches the current bet).
		 * 
		 * @param currentBet
		 *            The amount needed to call.
		 */
		public void call(int currentBet) {
			// if enough chips to call
			System.out.println("Chips " + chips);
			System.out.println("current bet: " + currentBet);
			System.out.println("amount betted :" + amountBettedInRound);
			if (chips > currentBet - amountBettedInRound) {
				chips -= (currentBet - amountBettedInRound);
				amountBettedInRound = currentBet;
			} else { // else, all in
				amountBettedInRound += chips;
				chips = 0;
			}

			acted = true;
		}

		/**
		 * Folds the player for the round.
		 */
		public void fold() {
			folded = true;
		}

		/**
		 * Used when determining if the player needs to act or not (i.e. folded
		 * player can't bet, so skip their betting turn)
		 * 
		 * @return whether or not the player folded.
		 */
		public boolean folded() {
			return folded;
		}

		/**
		 * Used after someone acts (calls, folds, raises, checks).
		 */
		public void act() {
			acted = true;
		}

		/**
		 * Used after someone raises. Everyone has to act again.
		 */
		public void resetAct() {
			acted = false;
		}

		/**
		 * Used after every betting round, setting the amount each player bet in
		 * the round to 0 for the next betting round (b/c the current bet starts
		 * at 0 again)
		 */
		public void resetAmountBettedInRound() {
			amountBettedInRound = 0;
		}

		/**
		 * This is used to determine how much the player has to bet (0, small
		 * blind, big blind), as well as the position of betting (i.e. small
		 * blind starts first for all rounds except for the first)
		 * 
		 * @return whether or not the player is the small blind.
		 */
		public boolean isSmallBlind() {
			return smallBlind;
		}

		/**
		 * This is used to determine how much the player has to bet (0, small
		 * blind, big blind), as well as the position of betting (i.e. big blind
		 * acts last in first round, but acts second in the rest).
		 * 
		 * @return whether or not the player is the big blind.
		 */
		public boolean isBigBlind() {
			return bigBlind;
		}

		/**
		 * Sets or unsets the player as small blind.
		 * 
		 * @param isSmallBlind
		 *            Whether or not the player is small blind.
		 */
		public void setSmallBlind(boolean isSmallBlind) {
			smallBlind = isSmallBlind;
		}

		/**
		 * Sets or unsets the player as big blind.
		 * 
		 * @param isBigBlind
		 *            Whether or not the player is big blind.
		 */
		public void setBigBlind(boolean isBigBlind) {
			bigBlind = isBigBlind;
		}

		/**
		 * Sends a message to the client, sending their hand and a prompt
		 * message (which prompts the client to display the graphics for each
		 * card).
		 */
		public void lookAtHand() {
			String cards = availableCards.get(0) + " " + availableCards.get(1);
			setOutput("HAND" + cards);
		}

		/**
		 * Sends a message to the client, sending the table cards and a prompt
		 * message (which prompts the client to display the graphics for each
		 * card on the table).
		 * 
		 * @param table
		 *            The list of cards on the table.
		 */
		public void lookAtTable(ArrayList<Card> table) {
			String cards = table.size() + "";
			if (table.size() == 3) {
				for (Card card : table) {
					cards += " " + card;
				}
				setOutput("TABLE" + cards + " ");
			} else if (table.size() == 4) {
				setOutput("TABLE" + "4 " + table.get(3));
			} else if (table.size() == 5) {
				setOutput("TABLE" + "5 " + table.get(4));
			}
		}

		/**
		 * Used when determining whether or not the game can be started.
		 * 
		 * @return whether or not the client has been initialized (after name
		 *         has been set).
		 */
		public boolean isInitialized() {
			return initialized;
		}

		/**
		 * Used to determine whether or not a player is out (i.e. 0 chips) and
		 * if the player went all in (i.e. if they raised/called and they have 0
		 * chips left)
		 * 
		 * @return the player's current chips.
		 */
		public int getChips() {
			return chips;
		}

		/**
		 * Calculates the best pokerhand out of all combinations of the player's
		 * available cards (i.e. those in his hand and those on the table).
		 */
		public void calculateBestPokerHand() {
			generatePokerHands(0, 1);

			// find the pokerhand value of each possible hand
			ArrayList<PokerHand> possiblePokerHands = new ArrayList<PokerHand>();
			for (ArrayList<Card> hand : possibleHands) {
				// sort, then determine the hand (sorting is required for the
				// process)
				Collections.sort(hand);
				possiblePokerHands.add(determinePokerHand(hand));
			}

			// once all the possible pokerhands have been generated/evaluated,
			// sort them to find the best one (last one in the list, as sorting
			// ranks them in ascending order)
			Collections.sort(possiblePokerHands);
			bestPokerHand = possiblePokerHands
					.get(possiblePokerHands.size() - 1);
		}

		/**
		 * Used to determine the value of each possible hand
		 * 
		 * @param hand
		 *            the set of five cards that can make a hand
		 * @return the PokerHand that the hand makes
		 */
		private PokerHand determinePokerHand(ArrayList<Card> hand) {

			PokerHand pokerHand = new PokerHand();

			// check if all same suit
			boolean isFlush = true;
			int startingSuit = hand.get(0).getSuit();
			for (int i = 1; i < 5; i++) { // check if the remaining 4 cards are
											// the
				// same suit
				if (hand.get(i).getSuit() != startingSuit) {
					isFlush = false;
				}
			}

			boolean isStraight = true;
			int previousRank = hand.get(0).getRank();
			for (int i = 1; i < 5; i++) { // check if the remaining 4 cards are
											// the
				if (hand.get(i).getRank() != previousRank + 1) { // break in
																	// chain,
					// can't be
					// straight
					isStraight = false;
					break;
				}
				previousRank++;
			}

			// make histogram of each card by rank
			// used to determine pairs, triples, full houses, and quads
			int[] histogramOfRanks = new int[14];

			for (int i = 0; i < 5; i++) {
				int currentRank = hand.get(i).getRank();
				histogramOfRanks[currentRank - 2]++; // b/c my cards value range
				// from 2-14
			}

			ArrayList<Integer> quads = new ArrayList<Integer>();
			ArrayList<Integer> triples = new ArrayList<Integer>();
			ArrayList<Integer> pairs = new ArrayList<Integer>();
			ArrayList<Integer> singles = new ArrayList<Integer>();

			for (int i = 0; i < histogramOfRanks.length; i++) {
				if (histogramOfRanks[i] == 1) {
					singles.add(i + 2); // i+2 b/c the cards' values start at 2,
										// not 0
				} else if (histogramOfRanks[i] == 2) {
					pairs.add(i + 2);
				} else if (histogramOfRanks[i] == 3) {
					triples.add(i + 2);
				} else if (histogramOfRanks[i] == 4) {
					quads.add(i + 2);
				}
			}

			// straight flush
			if (isFlush && isStraight) {
				pokerHand.setStraightFlush(singles.get(singles.size() - 1));
				return pokerHand;
			}

			// quadruples
			else if (!quads.isEmpty()) {
				pokerHand.setQuad(quads.get(0));
				return pokerHand;
			}

			// full house
			else if (!triples.isEmpty() && !pairs.isEmpty()) {
				pokerHand.setFullHouse(triples.get(triples.size() - 1),
						pairs.get(pairs.size() - 1));
				return pokerHand;

			}

			// flush
			else if (isFlush) {
				pokerHand.setFlush(singles.get(4), singles.get(3),
						singles.get(2), singles.get(1), singles.get(0));
				return pokerHand;
			}

			// straight
			else if (isStraight) {
				pokerHand.setStraight(singles.get(4));
				return pokerHand;
			}

			// triples
			else if (!triples.isEmpty() && pairs.isEmpty()) {
				pokerHand.setTriple(triples.get(triples.size() - 1),
						singles.get(1), singles.get(0));
				return pokerHand;

			}

			else if (!pairs.isEmpty() && triples.isEmpty()) {

				// two pair
				if (pairs.size() >= 2) {

					pokerHand.setTwoPair(pairs.get(pairs.size() - 1),
							pairs.get(pairs.size() - 2), singles.get(0));
					return pokerHand;
				}

				// pair
				else {
					pokerHand.setPair(pairs.get(pairs.size() - 1),
							singles.get(2), singles.get(1), singles.get(0));
					return pokerHand;
				}
			}

			else { // high card
				pokerHand.setHighCard(singles.get(4), singles.get(3),
						singles.get(2), singles.get(1), singles.get(0));
				return pokerHand;
			}
		}

		/**
		 * Recursive function that generates all possible combinations of hands
		 * (5 cards) for a given set of 7 cards
		 * 
		 * @param i
		 *            The location of the current card being used as the
		 *            reference point that is to be removed
		 * @param k
		 *            The location of the second card to be removed.
		 */
		private void generatePokerHands(int i, int k) {
			ArrayList<Card> currentHand = (ArrayList<Card>) availableCards
					.clone();
			// the concept behind this is to remove two different cards from the
			// set of seven each time in order to generate a new, unique set of
			// five cards each time. This is done by calculating all the
			// possibilities of the two cards that could be removed, and
			// recursively calling the function over and over again until all
			// the possibilities have been generated. The cards are removed in
			// the following pattern : (1,2), (1,3)...(1,7), (2,3),
			// (2,4)...(2,7)...(6,7). Once's 6,7 has been reached, all
			// possibilities have been generated.
			if (i < 5) {
				currentHand.remove(i);
				currentHand.remove(i + k - 1); // accommodate for new size
				addPossibleHands(currentHand);
				if (i + k < 6) { // still more possibilities with the same
									// starting
					// card 1
					generatePokerHands(i, k + 1);
				} else { // all possibilities for that starting card done
					// move to next starting card to remove
					generatePokerHands(i + 1, 1);
				}
			}
		}

		/**
		 * Used to get the value of the player's hand.
		 * 
		 * @return their best pokerhand out of all the possible pokerhands.
		 */
		public PokerHand getPokerHand() {
			return bestPokerHand;
		}

		/**
		 * After generating a combination, add it to the possible hands
		 * arraylist.
		 * 
		 * @param possibleHand
		 *            A possible combination of five cards.
		 */
		public void addPossibleHands(ArrayList<Card> possibleHand) {
			possibleHands.add(possibleHand);
		}

		/**
		 * Used to compare players. Players are compared by their pokerhands.
		 */
		@Override
		public int compareTo(Object otherPlayer) {
			return bestPokerHand.compareTo(((PlayerHandler) otherPlayer)
					.getPokerHand());
		}

		/**
		 * Used for sending broadcast messages (i.e. "____" calls).
		 * 
		 * @return the player's name.
		 */
		public String getPlayerName() {
			return name;
		}
	}
}
