import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// Test class for verifying the behavior, invariants, and edge cases of the DeckOfCards class.
public class DeckOfCardsTest {

    // ---- construction and error cases ----

    // Test 1: Verifies that a newly created deck is empty, has a size of 0, and correctly stores its ID.
    @Test
    public void newDeckIsEmptyAndKnowsItsId() {
        // Create deck number 3.
        DeckOfCards deck = new DeckOfCards(3);
        // The deck remembers the number it was given.
        assertEquals(3, deck.getId());
        // A new deck has no cards in it.
        assertEquals(0, deck.size());
        assertTrue(deck.isEmpty());
    }

    // Test 2: Verifies input validation, ensuring deck IDs below one (like 0) are rejected.
    @Test
    public void idBelowOneIsRejected() {
        // Deck numbers start from 1, so deck 0 must throw an exception.
        assertThrows(IllegalArgumentException.class, () -> new DeckOfCards(0));
    }

    // Test 3: Verifies that attempting to add a null card to the deck throws a NullPointerException.
    @Test
    public void addingNullIsRejected() {
        DeckOfCards deck = new DeckOfCards(1);
        // A missing card (null) cannot be added, so this must throw an exception.
        assertThrows(NullPointerException.class, () -> deck.addCard(null));
    }

    // Test 4: Verifies that drawing from an empty deck returns null and leaves the deck empty.
    @Test
    public void drawingFromEmptyDeckReturnsNullAndLeavesItEmpty() {
        DeckOfCards deck = new DeckOfCards(1);
        // There is no card to draw, so the result is null (and there is no crash).
        assertNull(deck.drawCard());
        // The deck is still empty afterwards.
        assertEquals(0, deck.size());
    }

    // ---- FIFO behavior ----

    // Test 5: Verifies First-In-First-Out (FIFO) ordering, cards come out in the exact order they went in.
    @Test
    public void cardsComeOutInTheOrderTheyWentIn() {
        DeckOfCards deck = new DeckOfCards(1);
        Card a = new Card(1);
        Card b = new Card(2);
        Card c = new Card(3);
        // Add the cards to the bottom of the deck in the order a, b, c.
        deck.addCard(a);
        deck.addCard(b);
        deck.addCard(c);
        // assertSame checks it is the exact same card object, so the order must be a, b, c.
        assertSame(a, deck.drawCard());
        assertSame(b, deck.drawCard());
        assertSame(c, deck.drawCard());
        // All three cards are gone now, so the next draw gives null.
        assertNull(deck.drawCard());
    }

    // ---- invariants ----

    // Test 6: Verifies that deck contents are formatted as top-to-bottom values separated by single spaces,
    // with no preceding or trailing space.
    @Test
    public void contentsAreListedTopToBottomSeparatedBySingleSpaces() {
        // Create deck number 2.
        DeckOfCards deck = new DeckOfCards(2);
        for (int v : new int[] {1, 3, 3, 7}) {
            deck.addCard(new Card(v));
        }
        assertEquals("1 3 3 7", deck.getContents());
    }

    // Test 7: Verifies that the contents string of an empty deck is an empty string.
    @Test
    public void contentsOfEmptyDeckIsEmptyString() {
        // A deck with no cards gives an empty text, with no spaces.
        assertEquals("", new DeckOfCards(1).getContents());
    }

    // Test 8: Verifies that cards added by many threads at once are never lost.
    @Test
    public void concurrentAddsDoNotLoseCards() throws InterruptedException {
        // Create one deck that all the threads will add cards to.
        DeckOfCards deck = new DeckOfCards(1);
        // Use 8 threads, and each one adds 1000 cards.
        int threadCount = 8;
        int perThread = 1000;
        // Make an array to hold the threads so we can wait for them later.
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            // Each thread runs this code: add 1000 cards to the same deck.
            threads[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    deck.addCard(new Card(i));
                }
            });
            threads[t].start();
        }
        // Wait for every thread to finish before checking the result.
        for (Thread thread : threads) {
            thread.join();
        }
        // 8 threads x 1000 cards = 8000. If any card was lost, the size would be smaller.
        assertEquals(threadCount * perThread, deck.size());
    }

}
