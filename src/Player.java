import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A player in the card game, each player runs on its own thread.
 * Every turn the player draws a card from the top of its leftDeck and
 * discards a card to the bottom of its rightDeck (the next deck in the ring).
 */
public class Player implements Runnable {

    public static final int HAND_SIZE = 4;

    /*
     * The player's number, which is also their preferred card value.
     */
    private int id;
    private CardDeck leftDeck;
    private CardDeck rightDeck;
    private GameState state;
    private Path outputFile;

    /*
     * The player's hand of cards. handSize is how many cards are actually in it
     * (less than 4 while the cards are still being dealt)
     */
    private Card[] hand = new Card[HAND_SIZE];
    private int handSize = 0;

    /*
     * What was drawn and discarded on the last turn, so we can write it to the file.
     */
    private Card lastDrawn;
    private Card lastDiscarded;

    public Player(int id, CardDeck leftDeck, CardDeck rightDeck, GameState state, Path outputDirectory) {
        if (id < 1) {
            throw new IllegalArgumentException("Player id must be at least 1: " + id);
        }
        if (leftDeck == null || rightDeck == null || state == null || outputDirectory == null) {
            throw new NullPointerException("Player arguments must not be null");
        }
        this.id = id;
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.state = state;
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
    public synchronized void addToHand(Card card) {
        if (card == null) {
            throw new NullPointerException("card must not be null");
        }
        if (handSize >= HAND_SIZE) {
            throw new IllegalStateException("Player " + id + " already holds " + HAND_SIZE + " cards");
        }
        hand[handSize] = card;
        handSize++;
    }

    /*
     * Returns a copy of the player's current cards, so other classes cannot change the hand.
     */
    public synchronized List<Card> getHand() {
        List<Card> copy = new ArrayList<>();
        for (int i = 0; i < handSize; i++) {
            copy.add(hand[i]);
        }
        return copy;
    }

    /*
     * Returns the hand as values separated by single spaces, e.g. "1 1 3 4"
     */
    public synchronized String getHandString() {
        String text = "";
        for (int i = 0; i < handSize; i++) {
            if (i > 0) {
                text += " ";
            }
            text += hand[i].getValue();
        }
        return text;
    }

    /*
     * Checks if the player has won (all 4 cards in hand have the exact same value).
     * it compares every card to the first one
     */
    public synchronized boolean hasWinningHand() {
        if (handSize != HAND_SIZE) {
            return false;
        }
        for (int i = 1; i < HAND_SIZE; i++) {
            if (hand[i].getValue() != hand[0].getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Plays one turn by drawing a card from the left deck and discarding a
     * non-preferred card to the right deck, as one atomic action.
     * Returns false (and changes nothing) if the left deck has no cards.
     */
    public boolean takeTurn() {
        // DEADLOCK PREVENTION:
        // Always lock the deck with the lower ID first. If player A and player B try
        // to lock deck 1 and deck 2 at the same time, locking the lower one first means
        // they never get stuck waiting on each other.
        CardDeck firstLock = leftDeck;
        CardDeck secondLock = rightDeck;
        if (leftDeck.getId() > rightDeck.getId()) {
            firstLock = rightDeck;
            secondLock = leftDeck;
        }

        synchronized (firstLock) {
            synchronized (secondLock) {
                synchronized (this) {
                    if (handSize != HAND_SIZE) {
                        throw new IllegalStateException("Player " + id
                                + " must hold exactly " + HAND_SIZE + " cards to take a turn");
                    }
                    if (hasWinningHand()) {
                        throw new IllegalStateException("Player " + id + " already holds a winning hand");
                    }

                    Card drawn = leftDeck.drawCard();
                    if (drawn == null) {
                        return false;
                    }

                    // Make an array of 5 with the 4 cards in the hand and the new card at the end.
                    Card[] cards = new Card[HAND_SIZE + 1];
                    for (int i = 0; i < HAND_SIZE; i++) {
                        cards[i] = hand[i];
                    }
                    cards[HAND_SIZE] = drawn;

                    // Loop through the cards one by one, the first card that does NOT match
                    // the player's preferred value is the one that gets discarded.
                    // The new card goes at the end, so the oldest unwanted card is always thrown
                    // away first and no non-preferred card gets held forever.
                    int discardIndex = -1;
                    for (int i = 0; i < cards.length; i++) {
                        if (cards[i].getValue() != id) {
                            discardIndex = i;
                            break;
                        }
                    }
                    // Safety check: if all the cards are the preferred value we should already have won.
                    if (discardIndex == -1) {
                        throw new IllegalStateException("Player " + id + " has no non-preferred card to discard");
                    }

                    // copy the other 4 cards back into the hand, keeping the same order
                    int next = 0;
                    for (int i = 0; i < cards.length; i++) {
                        if (i != discardIndex) {
                            hand[next] = cards[i];
                            next++;
                        }
                    }
                    Card discarded = cards[discardIndex];
                    rightDeck.addCard(discarded);

                    lastDrawn = drawn;
                    lastDiscarded = discarded;
                    return true;
                }
            }
        }
    }

    @Override
    public void run() {
        boolean checkDone = false;
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            write(writer, "player " + id + " initial hand " + getHandString());

            // Check if player won immediately with the cards they were dealt.
            if (hasWinningHand()) {
                declareWin();
            }
            state.markInitialCheckDone();
            checkDone = true;

            // Wait until all players in the game have checked their starting hand before
            // anyone starts taking turns.
            state.awaitAllInitialChecks();

            // keep taking turns until someone wins
            while (!state.isOver()) {
                if (!takeTurn()) {
                    // If the left deck is empty, wait 1ms and try again.
                    Thread.sleep(1);
                    continue;
                }
                write(writer, "player " + id + " draws a " + getLastDrawn() + " from deck " + leftDeck.getId());
                write(writer, "player " + id + " discards a " + getLastDiscarded() + " to deck " + rightDeck.getId());
                write(writer, "player " + id + " current hand is " + getHandString());

                if (hasWinningHand()) {
                    declareWin();
                }
            }

            // Write the final game-over lines to the file.
            int winner = state.getWinner();
            if (winner == id) {
                write(writer, "player " + id + " wins");
                write(writer, "player " + id + " exits");
                write(writer, "player " + id + " final hand: " + getHandString());
            } else {
                write(writer, "player " + winner + " has informed player " + id
                        + " that player " + winner + " has won");
                write(writer, "player " + id + " exits");
                write(writer, "player " + id + " hand: " + getHandString());
            }
        } catch (IOException e) {
            System.err.println("player " + id + " could not write " + outputFile + ": " + e.getMessage());
        } catch (InterruptedException e) {
            // Mark the thread as interrupted again if it got cut off.
            Thread.currentThread().interrupt();
        } finally {
            // Never leave the other players waiting at the start.
            if (!checkDone) {
                state.markInitialCheckDone();
            }
        }
    }

    private void declareWin() {
        // Try to register this player as the winner in the shared game state.
        // Only the first one to do it prints the win message.
        if (state.tryDeclareWinner(id)) {
            System.out.println("player " + id + " wins");
        }
    }

    /*
     * Writes one line to the file and saves it straight away.
     */
    private static void write(FileWriter writer, String line) throws IOException {
        writer.write(line + System.lineSeparator());
        writer.flush();
    }
}
