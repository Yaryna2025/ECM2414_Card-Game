/*
 * A playing card with a fixed number value
 * that can't be changed after it's made.
 */
public final class Card {

    /*
     * Private so other classes can't change the value directly, and final so it
     * can't be changed after the card is made (immutable).
     * This also makes Card thread safe because no thread can change its value.
     */
    private final int value;

    /*
     * Creates a card with the given value.
     * If the value is negative, it throws an IllegalArgumentException.
     * Otherwise, it saves the value into the card.
     */
    public Card(int value) {
        // Throw an error if the value is negative.
        if (value < 0) {
            throw new IllegalArgumentException("Card value must be non-negative: " + value);
        }
        this.value = value;
    }

    // The value is private, so other classes use this getter to get card's value.
    public int getValue() {
        return value;
    }

    // Converts the value into a String, for example: 4 into "4".
    @Override
    public String toString() {
        return "" + value;
    }
}