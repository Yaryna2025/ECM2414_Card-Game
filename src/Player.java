import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A player in the card game.
 * The player will draw from leftDeck and discard to rightDeck.
 */
public class Player {

    public static final int HAND_SIZE = 4;

    /*
     * The player's number, which is also their preferred card value.
     */
    private int id;
    private CardDeck leftDeck;
    private CardDeck rightDeck;
    private Path outputFile;

    /*
     * The player's hand of cards. handSize is how many cards are actually in it
     * (less than 4 while the cards are still being dealt)
     */
    private Card[] hand = new Card[HAND_SIZE];
    private int handSize = 0;

    public Player(int id, CardDeck leftDeck, CardDeck rightDeck, Path outputDirectory) {
        if (id < 1) {
            throw new IllegalArgumentException("Player id must be at least 1: " + id);
        }
        if (leftDeck == null || rightDeck == null || outputDirectory == null) {
            throw new NullPointerException("Player arguments must not be null");
        }
        this.id = id;
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.outputFile = outputDirectory.resolve("player" + id + "_output.txt");
    }

    public int getId() {
        return id;
    }

    /*
     * Puts a dealt card into the player's hand during game setup
     * (making sure they don't get more than 4 cards).
     */
    public void addToHand(Card card) {
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
    public List<Card> getHand() {
        List<Card> copy = new ArrayList<>();
        for (int i = 0; i < handSize; i++) {
            copy.add(hand[i]);
        }
        return copy;
    }

    /*
     * Returns the hand as values separated by single spaces, e.g. "1 1 3 4"
     */
    public String getHandString() {
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
    public boolean hasWinningHand() {
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
}
