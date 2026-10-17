import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for verifying the behavior, invariants, and edge cases of the CardDeck class.
 */
public class CardDeckTest {

    // ---- construction and error cases ----

    /** Test 1: Verifies that a newly created deck is empty, has a size of 0, and correctly stores its ID. */
    @Test
    public void newDeckIsEmptyAndKnowsItsId() {
        CardDeck deck = new CardDeck(3);
        assertEquals(3, deck.getId());
        assertEquals(0, deck.size());
        assertTrue(deck.isEmpty());
    }

    /** Test 2: Verifies input validation, ensuring deck IDs below one (like 0) are rejected. */
    @Test
    public void idBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CardDeck(0));
    }

    /** Test 3: Verifies that attempting to add a null card to the deck throws a NullPointerException. */
    @Test
    public void addingNullIsRejected() {
        CardDeck deck = new CardDeck(1);
        assertThrows(NullPointerException.class, () -> deck.addCard(null));
    }

    /** Test 4: Verifies that drawing from an empty deck returns null and leaves the deck empty. */
    @Test
    public void drawingFromEmptyDeckReturnsNullAndLeavesItEmpty() {
        CardDeck deck = new CardDeck(1);
        assertNull(deck.drawCard());
        assertEquals(0, deck.size());
    }

    // ---- FIFO behaviour ----

    /** Test 5: Verifies First-In-First-Out (FIFO) ordering, cards come out in the exact order they went in. */
    @Test
    public void cardsComeOutInTheOrderTheyWentIn() {
        CardDeck deck = new CardDeck(1);
        Card a = new Card(1);
        Card b = new Card(2);
        Card c = new Card(3);
        deck.addCard(a);
        deck.addCard(b);
        deck.addCard(c);
        assertSame(a, deck.drawCard());
        assertSame(b, deck.drawCard());
        assertSame(c, deck.drawCard());
        assertNull(deck.drawCard());
    }

    // ---- invariants ----

    /** Test 6: Verifies that deck contents are formatted as top-to-bottom values separated by single spaces. */
    @Test
    public void contentsAreListedTopToBottomSeparatedBySingleSpaces() {
        CardDeck deck = new CardDeck(2);
        for (int v : new int[] {1, 3, 3, 7}) {
            deck.addCard(new Card(v));
        }
        assertEquals("1 3 3 7", deck.getContents());
    }

    /** Test 7: Verifies that the contents string of an empty deck is an empty string. */
    @Test
    public void contentsOfEmptyDeckIsEmptyString() {
        assertEquals("", new CardDeck(1).getContents());
    }

}
