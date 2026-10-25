import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*
 * Represents a player in the card game.
 * Each player runs on its own thread.
 * Every turn the player draws a card from the top of its leftDeck and
 * discards a card to the bottom of its rightDeck (the next deck in the ring).
 */
public class Player implements Runnable {

    // Every player always holds exactly this many cards once the game starts.
    public static final int HAND_SIZE = 4;

    // The player's number, which is also their preferred card value.
    private final int id;
    // The deck this player draws from.
    private final DeckOfCards leftDeck;
    // The deck this player discards to (the next deck in the ring).
    private final DeckOfCards rightDeck;
    // The game state shared by all players, used to find out who has won.
    private final GameState state;
    // The file this player writes its moves to, for example: player1_output.txt.
    private final Path outputFile;

    /*
     * The player's hand of cards. handSize is how many cards are actually in it
     * (less than 4 while the cards are still being dealt).
     */
    private Card[] hand = new Card[HAND_SIZE];
    private int handSize = 0;

    // What was drawn and discarded on the last turn, so we can write it to the file.
    private Card lastDrawn;
    private Card lastDiscarded;

    /*
     * Creates a player and works out the name of its output file.
     * The outputDirectory is the folder the file is written to.
     */
    public Player(int id, DeckOfCards leftDeck, DeckOfCards rightDeck, GameState state, Path outputDirectory) {
        // Player numbers start from 1, so anything less is rejected.
        if (id < 1) {
            throw new IllegalArgumentException("Player id must be at least 1: " + id);
        }
        // None of the other arguments can be missing, so reject null values.
        if (leftDeck == null || rightDeck == null || state == null || outputDirectory == null) {
            throw new NullPointerException("Player arguments must not be null");
        }
        // Save everything the player needs for the game.
        this.id = id;
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.state = state;
        // Build the output file name from the player number, for example: "player3_output.txt".
        this.outputFile = outputDirectory.resolve("player" + id + "_output.txt");
    }

    public int getId() {
        return id;
    }

    public synchronized Card getLastDrawn() {
        return lastDrawn;
    }

    public synchronized Card getLastDiscarded() {
        return lastDiscarded;
    }

    /*
     * Puts a dealt card into the player's hand during game setup
     * (making sure they don't get more than 4 cards).
     */
    public synchronized void addCardToHand(Card card) {
        // A null card cannot be added to the hand.
        if (card == null) {
            throw new NullPointerException("card must not be null");
        }
        // The hand is already full, so refuse a fifth card.
        if (handSize >= HAND_SIZE) {
            throw new IllegalStateException("Player " + id + " already holds " + HAND_SIZE + " cards");
        }
        // Put the card in the next free place and count it.
        hand[handSize] = card;
        handSize++;
    }

    // Returns a copy of the player's current cards, so other classes cannot change the hand.
    public synchronized List<Card> getHand() {
        List<Card> copy = new ArrayList<>();
        for (int i = 0; i < handSize; i++) {
            // Copy only the cards that are really in the hand.
            copy.add(hand[i]);
        }
        return copy;
    }

    // Returns the hand as values separated by single spaces, for example: "1 1 3 4".
    public synchronized String getHandString() {
        String text = "";
        for (int i = 0; i < handSize; i++) {
            // Put a space between the values, but not before the first one.
            if (i > 0) {
                text += " ";
            }
            // Add the value of this card to the text.
            text += hand[i].getValue();
        }
        return text;
    }

    /*
     * Checks if the player has won (all 4 cards in hand have the exact same value).
     * It compares every card to the first one.
     */
    public synchronized boolean hasWinningHand() {
        if (handSize != HAND_SIZE) {
            return false;
        }
        // Compare the other 3 cards with the first card.
        for (int i = 1; i < HAND_SIZE; i++) {
             // One card that is different means the player has not won.
            if (hand[i].getValue() != hand[0].getValue()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Plays one turn by drawing a card from the left deck and discarding a
     * non-preferred card to the right deck, as one atomic action.
     * Returns false (and changes nothing) if the left deck has no cards.
     */
    public boolean takeTurn() {
        // For deadlock prevention, always lock the deck with the lower ID first, unless the last player in the ring.
        // This makes sure players lock decks in the same order and do not get stuck waiting for each other.
        DeckOfCards firstLock = leftDeck;
        DeckOfCards secondLock = rightDeck;
        if (leftDeck.getId() > rightDeck.getId()) {
            firstLock = rightDeck;
            secondLock = leftDeck;
        }

        // Lock both decks and the player for the whole turn, so drawing and
        // discarding happen together without allowing other threads to interrupt that process.
        synchronized (firstLock) {
            synchronized (secondLock) {
                // Lock the player so other threads cannot read the hand while it is being changed.
                synchronized (this) {
                    // A turn only makes sense with a full hand.
                    if (handSize != HAND_SIZE) {
                        throw new IllegalStateException("Player " + id
                                + " must hold exactly " + HAND_SIZE + " cards to take a turn");
                    }
                    if (hasWinningHand()) {
                        throw new IllegalStateException("Player " + id + " already holds a winning hand");
                    }

                    // Take the card from the top of the left deck.
                    Card drawn = leftDeck.drawCard();
                    if (drawn == null) {
                        return false;
                    }

                    // Temporary array of 5 cards (hand + newly drawn card).
                    Card[] cards = new Card[HAND_SIZE + 1];
                    for (int i = 0; i < HAND_SIZE; i++) {
                        cards[i] = hand[i];
                    }
                    cards[HAND_SIZE] = drawn;

                    // Find the first card that doesn't match the player's preferred value to discard.
                    // The new card goes at the end, so the first unwanted card is always discarded.
                    int discardIndex = -1;
                    for (int i = 0; i < cards.length; i++) {
                        if (cards[i].getValue() != id) {
                            discardIndex = i;
                            break;
                        }
                    }
                    // Safety check: if all the cards have the preferred value, the player should have already won.
                    if (discardIndex == -1) {
                        throw new IllegalStateException("Player " + id + " has no non-preferred card to discard");
                    }

                    // Copy the other 4 cards back into the hand, keeping the same order.
                    int next = 0;
                    for (int i = 0; i < cards.length; i++) {
                        if (i != discardIndex) {
                            hand[next] = cards[i];
                            next++;
                        }
                    }
                    // Put the discarded card at the bottom of the right deck.
                    Card discarded = cards[discardIndex];
                    rightDeck.addCard(discarded);

                    lastDrawn = drawn;
                    lastDiscarded = discarded;
                    return true;
                }
            }
        }
    }

    /*
     * This is what each player thread runs:
     * 1. Writes the starting hand to the file.
     * 2. Waits for all players to be ready.
     * 3. Manages the drawing/discarding of cards until a player wins.
     * 4. Writes the final results and exits.
     */
    @Override
    public void run() {
        // Remember whether this player has told the game state it has checked its hand.
        boolean checkDone = false;
        // Open the player's output file.
        // It closes automatically at the end of the try block.
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            write(writer, "player " + id + " initial hand " + getHandString());

            // Check if player won immediately with the cards they were dealt.
            if (hasWinningHand()) {
                declareWin();
            }
            // Tell the game state this player has finished checking its starting hand.
            state.markInitialCheckDone();
            checkDone = true;

            // Wait until all players in the game have checked their starting hand before
            // anyone starts playing.
            state.awaitAllInitialChecks();

            // Keep taking turns until someone wins.
            while (!state.isOver()) {
                if (!takeTurn()) {
                    // If the left deck is empty, wait 1ms and try again.
                    Thread.sleep(1);
                    continue;
                }
                write(writer, "player " + id + " draws a " + getLastDrawn() + " from deck " + leftDeck.getId());
                write(writer, "player " + id + " discards a " + getLastDiscarded() + " to deck " + rightDeck.getId());
                write(writer, "player " + id + " current hand is " + getHandString());

                // Check whether the new hand wins the game.
                if (hasWinningHand()) {
                    declareWin();
                }
            }

            // Write the final game-over lines to the file.
            int winner = state.getWinner();
            if (winner == id) {
                // This player is the winner.
                write(writer, "player " + id + " wins");
                write(writer, "player " + id + " exits");
                write(writer, "player " + id + " final hand: " + getHandString());
            } else {
                // Another player won, so record that this player was told about it.
                write(writer, "player " + winner + " has informed player " + id
                        + " that player " + winner + " has won");
                write(writer, "player " + id + " exits");
                write(writer, "player " + id + " hand: " + getHandString());
            }
        } catch (IOException e) {
            // The file could not be written, so print a message instead of crashing the game.
            System.err.println("player " + id + " could not write " + outputFile + ": " + e.getMessage());
        } catch (InterruptedException e) {
            // Restore the interrupted status if the thread gets cut off mid-sleep.
            Thread.currentThread().interrupt();
        } finally {
            // Avoid deadlocks by ensuring that if it was not possible to complete the check for whatever reason
            // we mark an initial check as done to prevent blocking other players indefinitely.
            if (!checkDone) {
                state.markInitialCheckDone();
            }
        }
    }

    // Tells the game state this player has won, and prints the win message if it was first.
    private void declareWin() {
        if (state.tryDeclareWinner(id)) {
            System.out.println("player " + id + " wins");
        }
    }

    // Writes one line to the file and saves it straight away.
    private static void write(FileWriter writer, String line) throws IOException {
        // Add the line and a new line character to the file.
        writer.write(line + System.lineSeparator());
        // Flush so the line is saved immediately instead of waiting in a buffer.
        writer.flush();
    }
}