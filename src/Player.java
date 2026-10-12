import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A player in the card game.
 * Every turn the player draws a card from the top of its leftDeck and
 * discards a card to the bottom of its rightDeck (the next deck in the ring).
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

    /*
     * What was drawn and discarded on the last turn, so we can write it to the file.
     */
    private Card lastDrawn;
    private Card lastDiscarded;

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

    public Card getLastDrawn() {
        return lastDrawn;
    }

    public Card getLastDiscarded() {
        return lastDiscarded;
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

    /**
     * Plays one turn by drawing a card from the left deck and discarding a
     * non-preferred card to the right deck.
     * Returns false (and changes nothing) if the left deck has no cards.
     */
    public boolean takeTurn() {
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
