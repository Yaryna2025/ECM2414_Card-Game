import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertTrue;


/*
 * Tests for CardGame: input validation, pack loading, game setup and
 * a whole game including the exact content of the output files.
 */
public class CardGameTest {

    /*
     * JUnit 4 creates a fresh temporary folder before each test and deletes it automatically afterwards.
     * This field must be public so JUnit can access and configure the temporary folder rule.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ---------- helpers ----------

    // Converts the JUnit temporary folder into a standard Java Path object (which CardGame uses).
    private Path tempFolderPath() {
        return tempFolder.getRoot().toPath();
    }

    //  Takes raw number values and builds them into a list of Card objects.
    private static List<Card> cards(int... values) {
        // Make an empty list to collect the cards.
        List<Card> list = new ArrayList<>();
        // Create one card for each value given and add it to the list.
        for (int v : values) {
            list.add(new Card(v));
        }
        return list;
    }

    // Writes a list of text lines into a temporary file inside our safe temporary folder and returns its path.
    private Path writePack(String name, List<String> lines) throws IOException {
        // Build the full path of the file inside the temporary folder.
        Path file = tempFolderPath().resolve(name);
        // Write every line to the file, one per row.
        Files.write(file, lines);
        return file;
    }

    /*
     * Generates a list of 8 identical lines containing the same value (the exact size needed for 1 player's pack).
     * Valid pack must contain 8n rows (where n is the number of players). For a 1-player game (n=1), the pack file must have exactly 8 lines.
     * Instead of manually typing out a list of 8 numbers every time, I want to test pack loading or validation,
     * this helper builds instantly the 8-line list with a single command.
     */
    private static List<String> eightLines(String value) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            lines.add(value);
        }
        return lines;
    }

    /*
     * Counts how many times 'part' appears inside 'text'.
     * Used to check how many times the program printed a prompt or an error message.
     */
    private static int countOccurrences(String text, String part) {
        int count = 0;
        int from = 0;
        // indexOf gives -1 when there are no more matches, so keep looping while it finds one.
        while ((from = text.indexOf(part, from)) >= 0) {
            // Found one more match.
            count++;
            // Move past this match so the same one is not counted again.
            from += part.length();
        }
        return count;
    }

    // ---------- parsing the number of players ----------

    /*
     * Test 1: Parses valid positive whole numbers for the player count.
     * Verifies that standard numbers are accepted and accidental whitespaces are ignored.
     */
    @Test
    public void validatePlayerCountAcceptsPositiveWholeNumbers() {
        // Assert that valid whole numbers are parsed correctly into integers.
        assertEquals(1, CardGame.validatePlayerCount("1"));
        assertEquals(6, CardGame.validatePlayerCount("6"));
        // Accidental spaces around the number are ignored.
        assertEquals(10, CardGame.validatePlayerCount("  10  "));
    }

    /*
     * Test 2: Rejects all invalid player count inputs.
     * Verifies that zero, negative numbers, decimals, text, blank spaces, multiple numbers,
     * leading plus signs, massive numbers, and null values all throw an exception.
     */
    @Test
    public void validatePlayerCountRejectsEverythingElse() {
        // Set up an array of bad inputs that should not be accepted as a player count.
        // The last one is a whole number, but too big to fit into an int.
        String[] bad = {"0", "-1", "1.5", "abc", "", " ", "4 5", "+5", "999988899955"};
        for (String text : bad) {
            // Assert that every single bad text input throws an IllegalArgumentException.
            assertThrows("should reject '" + text + "'", IllegalArgumentException.class,
                    () -> CardGame.validatePlayerCount(text));
        }
        // Assert that passing a null (empty) value also throws an exception.
        assertThrows(IllegalArgumentException.class,
                () -> CardGame.validatePlayerCount(null));
    }

    // ---------- loading and validating a pack ----------

    /*
     * Test 3: Reads each line of pack files in exact file order.
     * Verifies that all card values (including boundary values and large numbers)
     * are correctly loaded into objects and unpacked into integers.
     */
    @Test
    public void loadPackReadsOneCardPerLineInFileOrder() throws Exception {
        // Create a test file containing valid card values, including a boundary value (0) and a large number.
        Path file = writePack("ok.txt",
                List.of("0", "7", "1000088", "4", "5", "3", "3", "9"));
        // Load the pack into a list of Card objects using the game method.
        List<Card> pack = CardGame.loadPack(file, 1);
        List<Integer> values = new ArrayList<>();
        // Unpack the Card objects into a simple, standard list of numbers, so they are easy to compare.
        for (Card c : pack) {
            values.add(c.getValue());
        }
        // Assert that the cards were read in the exact order they appeared in the file.
        assertEquals(List.of(0, 7, 1000088, 4, 5, 3, 3, 9), values);
    }

    /*
     * Test 4: Rejects pack files that contain the wrong number of lines.
     * Verifies that files which are too short, too long, empty, or have a mismatched line count,
     * for a given number of players (for example: an 8-line file given to a 2-player game), we throw an IllegalArgumentException.
     */
    @Test
    public void loadPackRejectsWrongNumberOfLines() throws Exception {
        // Create files with incorrect amounts of lines (too short, too long, or empty).
        // Too short: create a file with only 7 lines instead of the required 8 for a 1-player game.
        Path tooShort = writePack("short.txt", eightLines("1").subList(0, 7));
        // Too long: make 9 lines in total instead of 8.
        List<String> longer = eightLines("1");
        longer.add("1");
        Path tooLong = writePack("long.txt", longer);
        // Create an entirely empty file with 0 lines.
        Path empty = writePack("empty.txt", List.of());
        // A file with 8 lines, which is correct for 1 player but wrong for 2 players.
        Path forTwoPlayers = writePack("eight.txt", eightLines("1"));

        // Assert that files with the wrong line counts are rejected with an exception.
        assertThrows(IllegalArgumentException.class, () -> CardGame.loadPack(tooShort, 1));
        assertThrows(IllegalArgumentException.class, () -> CardGame.loadPack(tooLong, 1));
        assertThrows(IllegalArgumentException.class, () -> CardGame.loadPack(empty, 1));
        // Assert that an eight line pack is invalid if tested against a game, where two players are required (which needs 16 lines).
        assertThrows("8 lines is wrong for 2 players", IllegalArgumentException.class,
                () -> CardGame.loadPack(forTwoPlayers, 2));
    }

    /*
     * Test 5: Verifies that the card loader rejects invalid card line formats,
     * such as negative numbers, letters, decimals, blank lines, or oversized values.
     * Dynamically tests each edge case by inserting a single bad line into an
     * otherwise valid 8-line pack and ensuring an IllegalArgumentException is thrown.
     */

    @Test
    public void loadPackRejectsNegativeNonIntegerBlankAndOversizedLines() throws Exception {
        // Define various invalid card line formats.
        String[] bad = {"-1", "abc", "1.5", "", " ", "+1", "1 2", "99999999999"};
        for (String badLine : bad) {
            // Start with a valid 8-line pack, but slip a bad line into the middle position.
            List<String> lines = eightLines("1");
            lines.set(3, badLine);
            Path file = writePack("bad.txt", lines);
            // Assert that the loader catches the invalid line and throws an exception.
            assertThrows("should reject line '" + badLine + "'", IllegalArgumentException.class,
                    () -> CardGame.loadPack(file, 1));
        }
    }

    /*
     * Test 6: Verifies that trying to load a pack from a file path that does not exist
     * is handled by throwing an IOException.
     */
    @Test
    public void loadPackReportsAMissingFileAsAnIOException() {
        // Assert that trying to load a file name that does not exist throws an IOException.
        assertThrows(IOException.class,
                () -> CardGame.loadPack(tempFolderPath().resolve("nope.txt"), 1));
    }

    // ---------- prompting ----------

    /*
     * Test 7: Verifies that the player prompt loop handles repeated invalid user inputs.
     * Simulates a user typing multiple bad answers (letters, zero, negative numbers, and blank space)
     *  followed by a valid number, ensuring the program re-prompts, shows error messages, and recovers successfully.
     */
    @Test
    public void promptForPlayersAsksAgainUntilTheAnswerIsValid() {
        // Capture printed output messages in a temporary byte stream.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        // Simulate a user typing four invalid answers first (letters, zero, a negative number
        // and an empty line), followed by a valid answer ("4").
        Scanner in = new Scanner("abc\n0\n-2\n\n4\n");

        // Run the player prompt method using our fake user input and output streams.
        int n = CardGame.promptForPlayers(in, out);

        // The final accepted player count is 4.
        assertEquals(4, n);
        String printed = bytes.toString(StandardCharsets.UTF_8);
        // The question was shown 5 times: once at the start and once after each of the 4 bad answers.
        assertEquals(5, countOccurrences(printed, "Please enter the number of players:"));
        // Assert that the error message was shown 4 times, once for each bad answer.
        assertEquals(4, countOccurrences(printed, "Invalid number of players"));
    }

    /*
     * - Test 8: Verifies that the pack prompt loop handles missing or malformed pack files.
     * - Recreates the behavior of  a user typing a path to a non-existent file, then an incorrectly formatted pack file,
     *   and finally a valid pack file, ensuring the system re-prompts, catches errors, and loads successfully.
     */
    @Test
    public void promptForPackAsksAgainUntilAValidPackIsGiven() throws Exception {
        // Create a bad pack file and a valid pack file in advance.
        // The bad pack has only 3 lines, but a pack file must contain exactly 8 x n lines.
        Path bad = writePack("bad.txt", List.of("1", "2", "3"));
        // Display good pack, use eightLines("2"), which generates the exact number of lines required.
        Path good = writePack("good.txt", eightLines("2"));
        // Creates an in-memory buffer to store data printed by the game instead of printing it to the physical terminal screen.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // Allow the test to record every message printed by the program.
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        // Simulate a user typing a missing file path, then a bad pack path, then a valid pack path.
        Scanner in = new Scanner(
                tempFolderPath().resolve("missing.txt") + "\n" + bad + "\n" + good + "\n");

        // Run the pack prompting method.
        List<Card> pack = CardGame.promptForPack(in, out, 1);

        // Assert that the loaded pack successfully contains 8 cards in the end.
        assertEquals(8, pack.size());
        String printed = bytes.toString(StandardCharsets.UTF_8);
        // Assert that the prompt message was shown 3 times, once for each file the user typed.
        assertEquals(3, countOccurrences(printed, "Please enter location of pack to load:"));
        // The missing file was reported once, and the badly sized pack was reported once.
        assertEquals(1, countOccurrences(printed, "Could not read that pack file"));
        assertEquals(1, countOccurrences(printed, "Invalid pack:"));
    }

    // ---------- setting up a game ----------

    /**
     * Test 9: Verifies that the game constructor correctly distributes cards
     * in a strict round-robin fashion during initialization.
     * Confirms that player hands receive alternating cards first, followed by
     * the remaining cards filling up each player's deck in the correct order.
     */
    @Test
    public void constructorDealsHandsRoundRobinThenFillsDecksRoundRobin() {
        // Initialize a 2-player game with a pack of 16 cards numbered 1 to 16.
        CardGame game = new CardGame(2,
                cards(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                tempFolderPath());

        // Assert that the first 8 cards were dealt round-robin (one at a time) to the player hands.
        assertEquals("1 3 5 7", game.getPlayers().get(0).getHandString());
        assertEquals("2 4 6 8", game.getPlayers().get(1).getHandString());
        // Assert that the remaining 8 cards were filled round-robin into the decks.
        assertEquals("9 11 13 15", game.getDecks().get(0).getContents());
        assertEquals("10 12 14 16", game.getDecks().get(1).getContents());
    }

    /**
     * Test 10: Verifies that the game constructor rejects a card pack of the wrong size
     * for the specified player count (for example: passing 8 cards when a 2-player game requires 16),
     * ensuring an IllegalArgumentException is thrown.
     */
    @Test
    public void constructorRejectsAPackOfTheWrongSize() {
        // Assert that passing an incorrect total number of cards (for example: 8 cards for a 2-player game) throws an error.
        assertThrows(IllegalArgumentException.class,
                () -> new CardGame(2, cards(1, 2, 3, 4, 5, 6, 7, 8), tempFolderPath()));
    }

    // ---------- playing a whole game ----------

    /**
     * Test 11: Verifies that if a player is dealt a winning hand immediately during game initialization,
     * the game ends instantly, correctly writes all player and deck output log files,
     * and leaves the draw decks completely untouched since no turns were taken.
     */
    @Test
    public void immediateWinWritesEveryFileAndLeavesTheDecksUntouched() throws Exception {
        // Set up a game where Player 1 is immediately dealt a winning hand (four 1s).
        CardGame game = new CardGame(2,
                cards(1, 2, 1, 3, 1, 4, 1, 5, 5, 6, 7, 8, 5, 6, 7, 8), tempFolderPath());

        // Run the game loop and store the winning player number.
        int winner = game.play();

        // Check that player 1 won instantly.
        assertEquals(1, winner);
        // Check that player 1's output log file contains exactly the expected game history.
        assertEquals(List.of(
                "player 1 initial hand 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"),
                Files.readAllLines(tempFolderPath().resolve("player1_output.txt")));
        // Player 2's output file records being informed that player 1 won, and that player 2 left the game.
        assertEquals(List.of(
                "player 2 initial hand 2 3 4 5",
                "player 1 has informed player 2 that player 1 has won",
                "player 2 exits",
                "player 2 hand: 2 3 4 5"),
                Files.readAllLines(tempFolderPath().resolve("player2_output.txt")));
        // Because no turns were taken, check that the card decks stayed exactly the same.
        assertEquals(List.of("deck1 contents: 5 7 5 7"),
                Files.readAllLines(tempFolderPath().resolve("deck1_output.txt")));
        assertEquals(List.of("deck2 contents: 6 8 6 8"),
                Files.readAllLines(tempFolderPath().resolve("deck2_output.txt")));
    }

    /*
     * Test 12: Verifies that a one-player game, where the player has to take a turn before winning,
     * writes the exact expected lines (initial hand, draw, discard, current hand, win lines)
     * to the player's output file, and the correct final contents to the deck's output file.
     */
    @Test(timeout = 10000)
    public void onePlayerGameWritesTheExpectedTurnAndWinLines() throws Exception {
        // Create a 1-player game. The first 4 cards (1 1 1 2) are the player's hand.
        // The last 4 cards (1 3 3 3) fill deck 1, so the first card the player draws is a 1.
        CardGame game = new CardGame(1, cards(1, 1, 1, 2, 1, 3, 3, 3), tempFolderPath());

        // Play the game and check that player 1 is the winner.
        assertEquals(1, game.play());

        // Check that the player's output file has exactly the lines the specification asks for:
        // the player draws a 1, discards the 2 (the only non-preferred card), and then holds 1 1 1 1.
        assertEquals(List.of(
                "player 1 initial hand 1 1 1 2",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 2 to deck 1",
                "player 1 current hand is 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"),
                Files.readAllLines(tempFolderPath().resolve("player1_output.txt")));
        // Check the deck file. The deck started as 1 3 3 3, the 1 was drawn from the top,
        // and the 2 was discarded to the bottom, so it should now hold 3 3 3 2.
        assertEquals(List.of("deck1 contents: 3 3 3 2"),
                Files.readAllLines(tempFolderPath().resolve("deck1_output.txt")));
    }

    /*
     * Test 13: Verifies that a real multi-threaded game with two players ends cleanly.
     * Player 1 wins, every player still holds exactly 4 cards, no card is lost or duplicated
     * (conservation of cards), and all the output files are written.
     */
    @Test(timeout = 10000)
    public void multiPlayerGameEndsCleanlyAndConservesEveryCard() throws Exception {
        // The pack for 2 players has 16 cards. Player 1 can reach four 1s, and player 2 never can,
        // because 1 is the only value that appears four times in the pack.
        int[] values = {1, 3, 1, 3, 3, 4, 4, 4, 5, 6, 1, 7, 1, 8, 5, 9};
        // Create the game from those values.
        CardGame game = new CardGame(2, cards(values), tempFolderPath());

        // Play the game (this starts one thread for each player and waits for them all to finish)
        // and check that player 1 won.
        assertEquals(1, game.play());

        // Count every card still in the game and add up their values.
        int count = 0;
        int total = 0;
        // First go through the player's hands.
        for (Player p : game.getPlayers()) {
            // Every player must still hold exactly 4 cards after the game.
            assertEquals(4, p.getHand().size());
            for (Card c : p.getHand()) {
                count++;
                total += c.getValue();
            }
        }
        // Then go through the decks, taking cards out one by one until each deck is empty.
        for (DeckOfCards d : game.getDecks()) {
            Card c = d.drawCard();
            while (c != null) {
                count++;
                total += c.getValue();
                c = d.drawCard();
            }
        }
        // Work out what the total of the original pack was.
        int expectedTotal = 0;
        for (int v : values) {
            expectedTotal += v;
        }
        // Check that no card was lost or duplicated: same number of cards and same total value.
        assertEquals(values.length, count);
        assertEquals(expectedTotal, total);

        // Check that all the output files exist (one for each player and one for each deck).
        for (int i = 1; i <= 2; i++) {
            assertTrue(Files.exists(tempFolderPath().resolve("player" + i + "_output.txt")));
            assertTrue(Files.exists(tempFolderPath().resolve("deck" + i + "_output.txt")));
        }
    }

}
