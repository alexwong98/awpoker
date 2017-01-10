import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * <h1>PokerClient</h1>
 * <p>
 * This class represents the PokerClient-side of the poker game, where players
 * can recieve information from the server (i.e. their cards, the cards on the
 * table, other players' actions, etc.) as well as send information, such as
 * their decision for each betting round.
 * </p>
 * 
 * @author Alex Wong
 * @since 2016-01-19
 */
public class PokerClient {
	private final String POKER_RULES_URL = "http://www.pokerlistings.com/poker-rules-texas-holdem";
	private BufferedReader in;
	private PrintWriter out;
	private JFrame frame = new JFrame("Poker");
	private JPanel panel;
	private JTextArea textArea;
	private JTextField txtFieldRaise;
	private JScrollPane scrollPane;
	private JTextArea messageArea;
	private JPanel gamePanel;
	private JPanel card1;
	private JPanel card2;
	private JPanel card3;
	private JPanel card4;
	private JPanel card5;
	private JButton btnCheck;
	private JButton btnCall;
	private JButton btnFold;
	private JButton btnRaise;
	private JButton btnAllIn;
	private JPanel playerCard1;
	private JPanel playerCard2;
	private JLabel lblPot;
	private JLabel lblCurrentBet;
	private JLabel lblAmountToCall;
	private JLabel lblName;
	private JLabel lblChips;
	private JLabel lblAction;
	private JLabel lblWinner;
	private JLabel lblBlind;
	private JButton btnRules;
	int numMsgLines = 0;
	private String name;
	private int chips = 0;
	private int amountToCall = 0;
	private boolean acting = false;
	private final int MAX_MSG_LINES = 30;
	private final Sound DEAL = new Sound("deal");
	private final Sound CHECK = new Sound("check");
	private final Sound RAISE = new Sound("raise");
	private final Sound CALL = new Sound("call");
	private final Sound ALLIN = new Sound("allin");
	private final Sound FOLD = new Sound("fold");

	/**
	 * Constructs the PokerClient by laying out the GUI and registering a
	 * listener with the txtFieldRaise so that pressing Return in the listener
	 * sends the txtFieldRaise contents to the server. Note however that the
	 * txtFieldRaise is initially NOT editable, and only becomes editable AFTER
	 * the PokerClient receives the NAMEACCEPTED message from the server.
	 */
	public PokerClient() {

		initialize();
		messageArea.setEditable(false);
		textArea.setEditable(false);

		btnCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.println("check");
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				acting = false;
				out.println(" "); // this is a flush message, that tells the
									// server handler to continue listening for
									// input. without this message, the run()
									// method in the handler (PlayerClient)
									// class stops running.
			}
		});

		btnCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.println("call");
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				acting = false;
				out.println(" ");
			}
		});

		btnFold.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.println("fold");
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				acting = false;
				out.println(" ");
			}
		});

		btnRaise.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// only let the player raise an amount they can actually raise
				if (!txtFieldRaise.getText().equals("")) {
					if (!(Integer.parseInt(txtFieldRaise.getText()) > (chips - amountToCall))) {
						out.println("raise " + txtFieldRaise.getText());
						txtFieldRaise.setText("");
						try {
							Thread.sleep(50);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						acting = false;
						out.println(" ");
					}
				}

			}
		});

		btnAllIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int allIn = chips - amountToCall;
				out.println("raise " + allIn);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				acting = false;
				out.println(" ");

			}
		});

		btnRules.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				try {
					Desktop.getDesktop().browse(
							new URL(POKER_RULES_URL).toURI());
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}

			}
		});
	}

	/*
	 * creates all GUI components for the PokerClient
	 */
	public void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 900, 550);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		textArea = new JTextArea();
		panel.add(textArea);

		scrollPane = new JScrollPane();
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

		messageArea = new JTextArea();
		messageArea.setColumns(28);
		messageArea.setRows(10);
		messageArea.setText("");
		scrollPane.setRowHeaderView(messageArea);

		gamePanel = new JPanel();
		scrollPane.setViewportView(gamePanel);
		gamePanel.setLayout(null);

		card1 = new JPanel();
		card1.setBounds(41, 11, 80, 116);
		gamePanel.add(card1);

		card2 = new JPanel();
		card2.setBounds(131, 11, 80, 116);
		gamePanel.add(card2);

		card3 = new JPanel();
		card3.setBounds(221, 11, 80, 116);
		gamePanel.add(card3);

		card4 = new JPanel();
		card4.setBounds(311, 11, 80, 116);
		gamePanel.add(card4);

		card5 = new JPanel();
		card5.setBounds(401, 11, 80, 116);
		gamePanel.add(card5);

		btnCheck = new JButton("Check");
		btnCheck.setBounds(43, 400, 89, 23);
		gamePanel.add(btnCheck);

		btnCall = new JButton("Call");
		btnCall.setBounds(142, 400, 89, 23);
		gamePanel.add(btnCall);

		btnFold = new JButton("Fold");
		btnFold.setBounds(241, 400, 89, 23);
		gamePanel.add(btnFold);

		btnRaise = new JButton("Raise");
		btnRaise.setBounds(342, 400, 89, 23);
		gamePanel.add(btnRaise);

		btnAllIn = new JButton("All in");
		btnAllIn.setBounds(443, 400, 89, 23);
		gamePanel.add(btnAllIn);

		txtFieldRaise = new JTextField();
		txtFieldRaise.setBounds(342, 369, 89, 20);
		gamePanel.add(txtFieldRaise);
		txtFieldRaise.setColumns(10);

		playerCard1 = new JPanel();
		playerCard1.setBounds(164, 239, 80, 116);
		gamePanel.add(playerCard1);

		playerCard2 = new JPanel();
		playerCard2.setBounds(254, 239, 80, 116);
		gamePanel.add(playerCard2);

		lblPot = new JLabel("Pot: 0");
		lblPot.setFont(new Font("Tahoma", Font.BOLD, 15));
		lblPot.setHorizontalAlignment(SwingConstants.LEFT);
		lblPot.setBounds(205, 179, 125, 14);
		gamePanel.add(lblPot);

		lblCurrentBet = new JLabel("Current bet: ");
		lblCurrentBet.setHorizontalAlignment(SwingConstants.LEFT);
		lblCurrentBet.setBounds(10, 305, 120, 14);
		gamePanel.add(lblCurrentBet);

		lblAmountToCall = new JLabel("To call:");
		lblAmountToCall.setHorizontalAlignment(SwingConstants.LEFT);
		lblAmountToCall.setBounds(10, 319, 120, 14);
		gamePanel.add(lblAmountToCall);

		lblName = new JLabel("Player: ");
		lblName.setHorizontalAlignment(SwingConstants.LEFT);
		lblName.setBounds(43, 434, 125, 14);
		gamePanel.add(lblName);

		lblChips = new JLabel("Chips:");
		lblChips.setHorizontalAlignment(SwingConstants.LEFT);
		lblChips.setBounds(342, 434, 76, 14);
		gamePanel.add(lblChips);

		lblAction = new JLabel("TO ACT:");
		lblAction.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblAction.setBounds(205, 154, 118, 14);
		gamePanel.add(lblAction);

		lblWinner = new JLabel("");
		lblWinner.setHorizontalAlignment(SwingConstants.CENTER);
		lblWinner.setBounds(0, 204, 481, 14);
		gamePanel.add(lblWinner);

		lblBlind = new JLabel("");
		lblBlind.setHorizontalAlignment(SwingConstants.LEFT);
		lblBlind.setBounds(43, 375, 200, 14);
		gamePanel.add(lblBlind);

		btnRules = new JButton("Rules");
		btnRules.setBounds(490, 459, 70, 23);
		gamePanel.add(btnRules);

	}

	/*
	 * Changes the image for each card JPanel
	 */
	public void setCardImage(String cardURL, JPanel panel) {
		URL imageUrl = PokerClient.class.getResource(cardURL + ".png");
		// scales the image to the size of the JPanel
		ImageIcon imageIcon = new ImageIcon(new ImageIcon(imageUrl).getImage()
				.getScaledInstance(panel.getWidth(), panel.getHeight(),
						Image.SCALE_SMOOTH));
		JLabel cardIcon = new JLabel(imageIcon);

		// remove the previous card if there was one there before
		panel.removeAll();
		panel.updateUI();
		panel.add(cardIcon);
		panel.setVisible(true);
	}

	/**
	 * Used to prompt the user so they can connect to the server
	 * 
	 * @return the ip address of the server
	 */
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame,
				"Enter IP Address of the Server:", "Welcome to Poker",
				JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Used to determine a name for the PokerClient.
	 * 
	 * @return the name the player has decided upon.
	 */
	private String setName() {
		name = JOptionPane.showInputDialog(frame, "Choose a screen name:",
				"Screen name selection", JOptionPane.PLAIN_MESSAGE);
		lblName.setText("Player: " + name);
		return name;
	}

	/**
	 * connects to the server then enters the processing loop.
	 */
	private void run() throws IOException {

		// Make connection and initialize streams
		String serverAddress = getServerAddress();
		Socket socket = new Socket(serverAddress, 9001);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		boolean cardsDealt = false; // cards fully dealt

		// Process all messages from server, according to the protocol.
		while (true) {

			// enable or disable buttons based on whether or not the player,
			// based on the rules of the game, can use them
			if (acting && cardsDealt) {
				if (amountToCall == 0) {
					btnCall.setEnabled(false);
					btnRaise.setEnabled(true);
					btnFold.setEnabled(true);
					btnCheck.setEnabled(true);
				} else if (amountToCall > 0) {
					btnCheck.setEnabled(false);
					btnCall.setEnabled(true);
					btnRaise.setEnabled(true);
					btnFold.setEnabled(true);
				}
				if (chips - amountToCall <= 0) {
					btnAllIn.setEnabled(false);
					btnRaise.setEnabled(false);
				} else {
					btnAllIn.setEnabled(true);
				}

			} else {
				// turning on buttons only when a player is to act provides a
				// more intuitive UI, where they better know when it is their
				// turn
				btnCall.setEnabled(false);
				btnRaise.setEnabled(false);
				btnFold.setEnabled(false);
				btnCheck.setEnabled(false);
				btnAllIn.setEnabled(false);
			}

			// read input from server, and act appropriately
			String line = in.readLine();
			if (line.startsWith("SUBMITNAME")) {
				out.println(setName());
			} else if (line.startsWith("MESSAGE")) {
				checkMsgAreaCapacity();
				messageArea.append(line.substring(7) + "\n");
				numMsgLines++;
			} else if (line.startsWith("ACTION")) {
				acting = true;
			} else if (line.startsWith("CHIPS")) {
				lblChips.setText("Chips : " + line.substring(5));
			} else if (line.equals("BIGBLIND")) {
				lblBlind.setText("You are the big blind.");
			} else if (line.equals("SMALLBLIND")) {
				lblBlind.setText("You are the small blind.");
			} else if (line.startsWith("INFO")) {
				// updates who is to act, the pot, and the current bet
				String toAct = line.substring(4, line.indexOf(" "));
				if (name.equals(toAct)) {
					lblAction.setText("TO ACT: YOU");
				} else {
					lblAction.setText("TO ACT: " + toAct);
				}

				lblPot.setText("Pot: "
						+ line.substring(line.indexOf(" ") + 1,
								line.indexOf(" ", line.indexOf(" ") + 1)));
				lblCurrentBet.setText("Current bet: "
						+ line.substring(line.indexOf(" ",
								line.indexOf(" ") + 1) + 1));
			} else if (line.startsWith("HAND")) {
				// updates player's cards
				String card1 = line.substring(4).substring(0,
						line.substring(4).indexOf(" "));
				String card2 = line.substring(4)
						.substring(line.substring(4).indexOf(" ") + 1).trim();

				// this is to ensure the player does not act until the cards are
				// fully dealt
				cardsDealt = false;
				setCardImage(card1, playerCard1);
				DEAL.play();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setCardImage(card2, playerCard2);
				DEAL.play();
				cardsDealt = true;

			} else if (line.startsWith("TABLE")) { // display the table

				String input = line.substring(5);
				int numCards = Integer.parseInt(input.substring(0, 1));
				input = input.substring(2);

				if (numCards == 3) {
					String tableCard1 = input.substring(0, input.indexOf(" "));
					input = input.substring(input.indexOf(" ") + 1);
					String tableCard2 = input.substring(0, input.indexOf(" "));
					input = input.substring(input.indexOf(" ") + 1);
					String tableCard3 = input.substring(0, input.indexOf(" "));

					cardsDealt = false;

					setCardImage(tableCard1, card1);
					DEAL.play();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					setCardImage(tableCard2, card2);
					DEAL.play();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					setCardImage(tableCard3, card3);
					DEAL.play();
					cardsDealt = true;
				} else if (numCards == 4) {
					String tableCard4 = input.substring(0);
					setCardImage(tableCard4, card4);
					DEAL.play();
				} else if (numCards == 5) {
					String tableCard5 = input.substring(0);
					setCardImage(tableCard5, card5);
					DEAL.play();
				}

			} else if (line.startsWith("PERSONALINFO")) {
				// updates chips and amount to call labels
				lblChips.setText("Chips: "
						+ line.substring(12).substring(0,
								line.substring(12).indexOf(" ")));
				chips = Integer.parseInt(line.substring(12).substring(0,
						line.substring(12).indexOf(" ")));
				lblAmountToCall.setText("To call: "
						+ line.substring(12)
								.substring(line.substring(12).indexOf(" ") + 1)
								.trim());
				amountToCall = Integer.parseInt(line.substring(12)
						.substring(line.substring(12).indexOf(" ") + 1).trim());
			} else if (line.startsWith("FOLDED")) {
				// turns over cards when the player folds
				setCardImage("cardback", playerCard1);
				setCardImage("cardback", playerCard2);
			} else if (line.startsWith("WINNER")) {
				// updates the winner of the round, displays their winning hand
				checkMsgAreaCapacity();
				messageArea
						.append(line.substring(line.indexOf(".") + 2) + "\n");
				numMsgLines++;
				lblWinner.setText(line.substring(6, line.indexOf(".")));
			} else if (line.startsWith("ENDROUND")) {
				// clear everything to prepare for the next round
				messageArea.setText("");
				lblWinner.setText("");
				lblBlind.setText("");
				// note: the cards will be set visible when they are dealt again
				// at that point, their previous image icon will be removed and
				// a new one will be placed into the panel
				playerCard1.setVisible(false);
				playerCard2.setVisible(false);
				card1.setVisible(false);
				card2.setVisible(false);
				card3.setVisible(false);
				card4.setVisible(false);
				card5.setVisible(false);
			} else if (line.startsWith("OUT")) {
				// it's important it's done like this, as it simulates the "x"
				// being clicked
				// this tells the server to close the socket, remove from the
				// list of playerClients
				frame.dispatchEvent(new WindowEvent(frame,
						WindowEvent.WINDOW_CLOSING));
			} else if (line.startsWith("WONGAME")) {
				JOptionPane.showMessageDialog(frame, "You won!");
				frame.dispatchEvent(new WindowEvent(frame,
						WindowEvent.WINDOW_CLOSING));
			} else if (line.startsWith("CHECK")) {
				checkMsgAreaCapacity();
				messageArea.append(line.substring(5) + "\n");
				numMsgLines++;
				CHECK.play(); // sound effect
			} else if (line.startsWith("RAISE")) {
				checkMsgAreaCapacity();
				messageArea.append(line.substring(5) + "\n");
				numMsgLines++;
				RAISE.play();
			} else if (line.startsWith("FOLD")) {
				checkMsgAreaCapacity();
				messageArea.append(line.substring(4) + "\n");
				numMsgLines++;
				FOLD.play();
			} else if (line.startsWith("CALL")) {
				checkMsgAreaCapacity();
				messageArea.append(line.substring(4) + "\n");
				numMsgLines++;
				CALL.play();
			} else if (line.startsWith("ALLIN")) {
				checkMsgAreaCapacity();
				messageArea.append(line.substring(5) + "\n");
				numMsgLines++;
				ALLIN.play();
			}

		}
	}

	/**
	 * Checks whether or not the message area is full. If it is, it is cleared.
	 */
	public void checkMsgAreaCapacity() {
		if (numMsgLines == MAX_MSG_LINES) {
			messageArea.setText("");
			numMsgLines = 0;
		}
	}

	/**
	 * Creates the PokerClient and runs it
	 */
	public static void main(String[] args) throws Exception {
		PokerClient PokerClient = new PokerClient();
		PokerClient.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		PokerClient.frame.setVisible(true);
		PokerClient.run();

	}
}