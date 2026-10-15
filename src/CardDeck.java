import java.util.ArrayList;

/**
 * A deck of cards that works like a queue (FIFO).
 * Cards are drawn from the top and discarded to the bottom.
 * Methods are synchronized so only one player thread can use the deck at a
 * time, preventing conflicts.
 */
public class CardDeck {

    private final int id;
    /*
     * Holds the cards in order, index 0 is the top of the deck.
     */
    private final ArrayList<Card> cards = new ArrayList<>();

    public CardDeck(int id) {
        // deck numbers start from 1, so anything less is rejected
        if (id < 1) {
            throw new IllegalArgumentException("Deck id must be at least 1: " + id);
        }
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /*
     * Adds a card to the bottom (back) of the deck.
     * Throws NullPointerException if the card is null.
     */
    public synchronized void addCard(Card card) {
        if (card == null) {
            throw new NullPointerException("card must not be null");
        }
        cards.add(card);
    }

    /*
     * Removes and returns the card at the top (front) of the deck.
     * Returns null if the deck is empty.
     */
    public synchronized Card drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }

    public synchronized int size() {
        return cards.size();
    }

    public synchronized boolean isEmpty() {
        return cards.isEmpty();
    }

    /*
     * Returns the card values from top to bottom, with a space between them.
     * For example: "2 4 5 5" or an empty string if the deck is empty.
     */
    public synchronized String getContents() {
        String text = "";
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                text += " ";
            }
            text += cards.get(i).getValue();
        }
        return text;
    }
}
