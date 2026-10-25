import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/*
 * Tests for the Player class: covers how players manage their cards and check for wins,
 * and drawing and discarding cards.
 */
public class PlayerTest {

    /*
     * JUnit 4 rule creating a fresh temporary directory for each test method
     * and deleting it afterwards.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ---------- helpers ----------

    // Helper method to convert JUnit's TemporaryFolder rule into a standard file Path.
    private Path tempFolderPath() {
        return tempFolder.getRoot().toPath();
    }

    // Helper method to deal specific card values directly into a player's hand for testing.
    private static void deal(Player player, int... values) {
        for (int v : values) {
            player.addCardToHand(new Card(v));
        }
    }

    // Add card values to the bottom of a DeckOfCards.
    private static void stack(DeckOfCards deck, int... values) {
        for (int v : values) {
            deck.addCard(new Card(v));
        }
    }

    // ---------- hand and winning-hand behavior ----------

    /*
     * Test 1: Verifies that a player can't hold more than 4 cards. If you try to add a 5th card,
     * it throws an error and leaves the original 4 cards unchanged.
     */
    @Test
    public void handCannotHoldMoreThanFourCards() {
        // Create player 1 with two decks, a game state and an output folder.
        Player p = new Player(1, new DeckOfCards(1), new DeckOfCards(2), new GameState(1), tempFolderPath());
        // Give the player a full hand of 4 cards.
        deal(p, 1, 2, 3, 4);
        // A fifth card must be refused with an exception.
        assertThrows(IllegalStateException.class, () -> p.addCardToHand(new Card(5)));
        // The hand is still the original 4 cards, in the same order.
        assertEquals("1 2 3 4", p.getHandString());
    }

    /*
     * Test 2: Verifies that trying to add a null card into a player's hand
     * correctly causes an error.
     */
    @Test
    public void addingNullToHandIsRejected() {
        Player p = new Player(1, new DeckOfCards(1), new DeckOfCards(2), new GameState(1), tempFolderPath());
        // Check that an attempt to add a null card to a hand throws an exception.
        assertThrows(NullPointerException.class, () -> p.addCardToHand(null));
    }

    /*
     * Test 3: Verifies that having 4 matching cards wins the game, even if they aren't
     * the player's favorite number.
     */
    @Test
    public void fourEqualCardsWinEvenWhenNotThePreferredValue() {
        // Player 1 prefers 1s, but we give them four 5s.
        Player p = new Player(1, new DeckOfCards(1), new DeckOfCards(2), new GameState(1), tempFolderPath());
        deal(p, 5, 5, 5, 5);
        // Four cards of the same value is a winning hand, whatever the value is.
        assertTrue(p.hasWinningHand());
    }

    // Test 4: Verifies that having 3 matching cards and 1 different card does not count as a win.
    @Test
    public void threeEqualCardsPlusADifferentOneIsNotWinning() {
        // Create player 1 and give them three 1s and one 2.
        Player p = new Player(1, new DeckOfCards(1), new DeckOfCards(2), new GameState(1), tempFolderPath());
        deal(p, 1, 1, 1, 2);
        // One card is different, so this is not a winning hand.
        assertFalse(p.hasWinningHand());
    }

    /*
     * Test 5: Verifies that having fewer than 4 cards (like only 3 matching cards)
     * does not count as a winning hand.
     */
    @Test
    public void incompleteHandIsNotWinning() {
        // Create player 1 and give them only three cards, all the same.
        Player p = new Player(1, new DeckOfCards(1), new DeckOfCards(2), new GameState(1), tempFolderPath());
        deal(p, 3, 3, 3);
        // A winning hand needs four cards, so three matching cards do not win.
        assertFalse(p.hasWinningHand());
    }

    // ---------- combined draw and discard operation (must execute as a single step) ----------

    /*
     * Test 6: Verifies that taking a turn correctly draws a card from the left deck,
     * discards an unwanted card to the right deck, and updates everything accurately.
     */
    @Test
    public void turnDrawsFromTopOfOwnDeckAndDiscardsToBottomOfNextDeck() {
        // Deck 1 (the left deck) holds 3 then 4. Deck 2 (the right deck) holds 9.
        DeckOfCards deck1 = new DeckOfCards(1);
        DeckOfCards deck2 = new DeckOfCards(2);
        stack(deck1, 3, 4);
        stack(deck2, 9);
        // Player 1 draws from deck 1 and discards to deck 2. The hand is 1 1 6 7.
        Player p = new Player(1, deck1, deck2, new GameState(2), tempFolderPath());
        deal(p, 1, 1, 6, 7);

        assertTrue(p.takeTurn());

        assertEquals(3, p.getLastDrawn().getValue());
        assertEquals(6, p.getLastDiscarded().getValue());
        assertEquals("1 1 7 3", p.getHandString());
        assertEquals("4", deck1.getContents());
        assertEquals("9 6", deck2.getContents());
    }

    /*
     * Test 7: Checks that a player never throws away a card matching their favorite number,
     * even if they are holding multiple copies of it.
     */
    @Test
    public void preferredDenominationIsNeverDiscarded() {
        // Player 2 prefers 2s. Deck 2 (the left deck) holds a single 2.
        DeckOfCards deck2 = new DeckOfCards(2);
        DeckOfCards deck3 = new DeckOfCards(3);
        stack(deck2, 2);
        // The hand is three 2s and a 4, so the 4 is the only card that can be discarded.
        Player p = new Player(2, deck2, deck3, new GameState(3), tempFolderPath());
        deal(p, 2, 2, 2, 4);

        assertTrue(p.takeTurn());

        assertEquals(4, p.getLastDiscarded().getValue());
        assertEquals("2 2 2 2", p.getHandString());
        assertTrue(p.hasWinningHand());
        assertEquals("4", deck3.getContents());
    }

    /*
     * Test 8: Checks that if a player has multiple unwanted cards,
     * they discard the first one they encounter.
     */
    @Test
    public void firstNonPreferredCardIsDiscardedFirst() {
        // Deck 1 holds an 8 and then a 6, so the player draws them one at a time.
        DeckOfCards deck1 = new DeckOfCards(1);
        DeckOfCards deck2 = new DeckOfCards(2);
        stack(deck1, 8, 6);
        // Player 1 prefers 1s. The hand has two unwanted cards: a 3 and then a 4.
        Player p = new Player(1, deck1, deck2, new GameState(2), tempFolderPath());
        deal(p, 3, 4, 1, 1);

        assertTrue(p.takeTurn());
        assertEquals(3, p.getLastDiscarded().getValue());
        assertTrue(p.takeTurn());
        assertEquals(4, p.getLastDiscarded().getValue());
        assertEquals("1 1 8 6", p.getHandString());
    }

    /*
     * Test 9: Checks that if the left deck is empty, the player safely does nothing
     * instead of crashing.
     */
    @Test
    public void turnDoesNothingWhenLeftDeckIsEmpty() {
        // Deck 1 (the left deck) is empty. Deck 2 (the right deck) holds a 9.
        DeckOfCards deck1 = new DeckOfCards(1);
        DeckOfCards deck2 = new DeckOfCards(2);
        stack(deck2, 9);
        Player p = new Player(1, deck1, deck2, new GameState(2), tempFolderPath());
        deal(p, 1, 1, 4, 7);

        // There is nothing to draw, so the turn does not happen (false).
        assertFalse(p.takeTurn());

        assertEquals("1 1 4 7", p.getHandString());
        assertEquals("", deck1.getContents());
        assertEquals("9", deck2.getContents());
    }

    /*
     * Test 10: Checks that a player cannot take a turn if they already have a winning hand,
     * throwing an error instead.
     */
    @Test
    public void turnIsRejectedForAWinningHandAndNoCardIsTaken() {
        // Deck 1 holds a 9 that the player could draw.
        DeckOfCards deck1 = new DeckOfCards(1);
        stack(deck1, 9);
        Player p = new Player(1, deck1, new DeckOfCards(2), new GameState(2), tempFolderPath());
        deal(p, 1, 1, 1, 1);

        // A player who has already won must not take another turn, so this throws an exception.
        assertThrows(IllegalStateException.class, p::takeTurn);

        assertEquals("9", deck1.getContents());
        assertEquals("1 1 1 1", p.getHandString());
    }

    /*
     * Test 11: Ensures the last player in the ring handles flipped deck IDs
     * (left ID > right ID) correctly without breaking.
     */
    @Test
    public void turnWorksWhenTheLeftDeckHasTheHigherId() {
        // Deck 1 is on the right (smaller ID: 1).
        DeckOfCards deck1 = new DeckOfCards(1);

        // Deck 2 is on the left (larger ID: 2).
        DeckOfCards deck2 = new DeckOfCards(2);

        stack(deck2, 3);

        // Setup Player with left=deck2 (ID 2) and right=deck1 (ID 1).
        // This forces the code to test the "lock smaller ID first" path.
        Player p = new Player(2, deck2, deck1, new GameState(2), tempFolderPath());

        deal(p, 2, 2, 5, 6);

        assertTrue(p.takeTurn());

        assertEquals("2 2 6 3", p.getHandString());
        assertEquals("5", deck1.getContents());
        assertEquals("", deck2.getContents());
    }

}
