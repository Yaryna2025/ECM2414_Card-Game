import java.util.ArrayList;

/*
 * Card decks in this game operate as a FIFO queue.
 * Cards are taken from the top of the deck and added to the bottom.
 * The synchronized keyword makes deck operations atomic, so only one thread can use these methods at a time.
 */
public class DeckOfCards {

    private final int id;
    /*
     * Keeps track of the cards in order.
     * Index 0 is the top of the deck.
     */
    private final ArrayList<Card> cards = new ArrayList<>();

    public DeckOfCards(int id) {
        // Deck IDs start from 1, so throw an error if it's 0 or negative.
        if (id < 1) {
            throw new IllegalArgumentException("Deck id must be at least 1: " + id);
        }
        this.id = id;
    }

    // Returns the unique id number of this deck.
    public int getId() {
        return id;
    }

    /*
     * Card is added to the bottom of the deck.
     * Throws an exception if someone tries to add a null card.
     */
    public synchronized void addCard(Card card) {
        if (card == null) {
            throw new NullPointerException("card must not be null");
        }
        cards.add(card);
    }

    /*
     * Takes the top card off the deck and returns it.
     * If the deck is empty there's nothing to draw, so it returns null.
     */
    public synchronized Card drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }

    // Returns how many cards are in the deck.
    public synchronized int size() {
        return cards.size();
    }

    // Quick check to see if the deck has no cards.
    public synchronized boolean isEmpty() {
        return cards.isEmpty();
    }

    /*
     * Returns the card values from top to bottom, separated by spaces.
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