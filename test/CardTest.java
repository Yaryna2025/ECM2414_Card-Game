import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

// Test class for verifying the behavior, invariants, and edge cases of the Card class.
public class CardTest {

    // Test 1: Verifies that a card correctly stores and returns the integer value it was created with.
    @Test
    public void storesTheValueItWasGiven() {
        assertEquals(6, new Card(6).getValue());
    }

    // Test 2: Verifies a boundary condition, that 0 is accepted as a valid card face value.
    @Test
    public void zeroIsAValidDenomination() {
        assertEquals(0, new Card(0).getValue());
    }

    // Test 3: Verifies input validation, making sure that negative numbers are rejected.
    @Test
    public void negativeValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    // Test 4: Verifies the string representation of the card.
    @Test
    public void toStringIsTheFaceValue() {
        assertEquals("14", new Card(14).toString());
    }
}