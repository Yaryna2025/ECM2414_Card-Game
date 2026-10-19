/**
 * A playing card with a fixed number value
 * that can't be changed after it's made.
 */
public final class Card {

    /*
     * Private so outside classes can't change the value directly, and final so it
     * can never change after the card is made (immutable).
     * This also makes Card thread safe because no thread can change it.
     */
    private final int value;

    /*
     * Creates a card.
     * It checks if the value is less than 0.
     * If it is negative, it rejects negative values by throwing
     * an IllegalArgumentException, otherwise it saves the
     * value into the card.
     */
    public Card(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Card value must be non-negative: " + value);
        }
        this.value = value;
    }

    /*
     * Value is private, so other classes need this getter to look at the value of
     * the card.
     */
    public int getValue() {
        return value;
    }

    /*
     * Converts the value into a String, for example: 4 into "4".
     */
    @Override
    public String toString() {
        return "" + value;
    }
}
