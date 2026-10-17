import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for verifying the behavior of the GameState class.
 */
public class GameStateTest {

    // ---- construction and initial state ----

    /** Test 1: Verifies that a newly created game state has no winner and the game is not over. */
    @Test
    public void nobodyHasWonAtTheStart() {
        GameState state = new GameState(3);
        assertEquals(0, state.getWinner());
        assertFalse(state.isOver());
    }

    /**
     * Test 2: Verifies input validation when creating a new GameState.
     * It ensures that passing an invalid player count (such as 0 or a negative number)
     * immediately throws an IllegalArgumentException to prevent the game from starting with incorrect rules.
     */
    @Test
    public void playerCountBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GameState(0));
    }

    // ---- winning conditions and validation ----

    /** Test 3: Verifies that the first player to declare a win succeeds, while later claims fail and the game ends. */
    @Test
    public void firstClaimWinsAndLaterClaimsFail() {
        GameState state = new GameState(3);
        assertTrue(state.tryDeclareWinner(2));
        assertFalse(state.tryDeclareWinner(1));
        assertFalse(state.tryDeclareWinner(2));
        assertEquals(2, state.getWinner());
        assertTrue(state.isOver());
    }

    /**
     * Test 4: Verifies input validation when a player attempts to declare a win.
     * It makes sure that passing an invalid player ID (such as 0 or a negative number)
     * throws an IllegalArgumentException and game state will be left unchanged and active.
     */
    @Test
    public void playerIdBelowOneIsRejected() {
        GameState state = new GameState(2);
        assertThrows(IllegalArgumentException.class, () -> state.tryDeclareWinner(0));
        assertFalse(state.isOver());
    }

}
