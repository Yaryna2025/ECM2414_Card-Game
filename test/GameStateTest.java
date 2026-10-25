import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// Test class for verifying the behavior of the GameState class.
public class GameStateTest {

    // ---- construction and initial state ----

    // Test 1: Verifies that a newly created game state has no winner and the game is not over.
    @Test
    public void nobodyHasWonAtTheStart() {
        // Create a game state for 3 players.
        GameState state = new GameState(3);
        // The winner is 0, which means nobody has won yet.
        assertEquals(0, state.getWinner());
        // The game is not over because there is no winner.
        assertFalse(state.isOver());
    }

    /*
     * Test 2: Verifies input validation when creating a new GameState.
     * It ensures that passing an invalid player count (such as 0 or a negative number)
     * immediately throws an IllegalArgumentException to prevent the game from starting with incorrect rules.
     */
    @Test
    public void playerCountBelowOneIsRejected() {
        // A game with 0 players makes no sense, so creating it must throw an exception.
        assertThrows(IllegalArgumentException.class, () -> new GameState(0));
    }

    // ---- winning conditions and validation ----

    // Test 3: Verifies that the first player to declare a win succeeds, while later claims fail and the game ends.
    @Test
    public void firstClaimWinsAndLaterClaimsFail() {
        // Create a game state for 3 players.
        GameState state = new GameState(3);
        // Player 2 is the first to claim a win, so the claim is accepted (true).
        assertTrue(state.tryDeclareWinner(2));
        // Player 1 claims a win after player 2, so the claim is refused (false).
        assertFalse(state.tryDeclareWinner(1));
        // Player 2 claiming again is also refused, because a winner already exists.
        assertFalse(state.tryDeclareWinner(2));
        // The winner is still player 2, the first one to claim.
        assertEquals(2, state.getWinner());
        assertTrue(state.isOver());
    }

    /*
     * Test 4: Verifies input validation when a player attempts to declare a win.
     * It makes sure that passing an invalid player ID (such as 0 or a negative number)
     * throws an IllegalArgumentException and game state will be left unchanged and active.
     */
    @Test
    public void playerIdBelowOneIsRejected() {
        // Create a game state for 2 players.
        GameState state = new GameState(2);
        // Player 0 does not exist (players start from 1), so the claim must throw an exception.
        assertThrows(IllegalArgumentException.class, () -> state.tryDeclareWinner(0));
        assertFalse(state.isOver());
    }

}
