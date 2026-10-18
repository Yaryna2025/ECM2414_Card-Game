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

/**
 * Tests for CardGame: input validation, pack loading, game setup and
 * a whole game including the exact content of the output files.
 */
public class CardGameTest {

    /**
     * JUnit 4 creates a fresh temporary folder before each test and deletes it automatically afterwards.
     * This field must be public so JUnit can access and configure the temporary folder rule.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ---------- helpers ----------

    /**
     * Converts the JUnit temporary folder into a standard Java Path object (which CardGame uses).
     */
    private Path tempFolderPath() {
        return tempFolder.getRoot().toPath();
    }

    /**
     * Takes raw number values and builds them into a list of Card objects.
     */
    private static List<Card> cards(int... values) {
        List<Card> list = new ArrayList<>();
        for (int v : values) {
            list.add(new Card(v));
        }
        return list;
    }

    /**
     * Writes a list of text lines into a temporary file inside our safe temporary folder and returns its path.
     */
    private Path writePack(String name, List<String> lines) throws IOException {
        Path file = tempFolderPath().resolve(name);
        Files.write(file, lines);
        return file;
    }

    /**
     * Generates a list of 8 identical lines containing the same value (the exact size needed for 1 player's pack).
     * Valid pack must contain 8n rows (where n is the number of players). For a 1-player game (n=1), the pack file must have exactly 8 lines.
     * Instead of manually typing out a list of 8 numbers every time, I want to test pack loading or validation, this helper builds instantly the 8-line list with a single command.
     */
    private static List<String> eightLines(String value) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            lines.add(value);
        }
        return lines;
    }

    /**
     * Counts how many times a specific search phrase ('part') appears inside a block of text ('text').
     * It is required to keep asking the user for a valid player count or pack file if they type something wrong.
     */
    private static int countOccurrences(String text, String part) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(part, from)) >= 0) {
            count++;
            from += part.length();
        }
        return count;
    }

    // ---------- parsing the number of players ----------

    /**
     * Test 1: Parses valid positive whole numbers for the player count.
     * Verifies that standard numbers are accepted and accidental whitespaces are ignored.
     */
    @Test
    public void parseNumberOfPlayersAcceptsPositiveWholeNumbers() {
        // Assert that valid whole numbers are parsed correctly into integers.
        assertEquals(1, CardGame.parseNumberOfPlayers("1"));
        assertEquals(6, CardGame.parseNumberOfPlayers("6"));
        // Assert that accidental spaces surrounding the number are successfully ignored.
        assertEquals(10, CardGame.parseNumberOfPlayers("  10  "));
    }

    /**
     * Test 2: Rejects all invalid player count inputs.
     * Verifies that zero, negative numbers, decimals, text, blank spaces, multiple numbers,
     * leading plus signs, massive numbers, and null values all throw an exception.
     */
    @Test
    public void parseNumberOfPlayersRejectsEverythingElse() {
        // Display an array of bad inputs that should not be accepted as a player count.
        String[] bad = {"0", "-1", "1.5", "abc", "", " ", "4 5", "+5", "999988899955"};
        for (String text : bad) {
            // Assert that every single bad text input throws an IllegalArgumentException.
            assertThrows("should reject '" + text + "'", IllegalArgumentException.class,
                    () -> CardGame.parseNumberOfPlayers(text));
        }
        // Assert that passing a null (empty) value also throws an exception.
        assertThrows(IllegalArgumentException.class,
                () -> CardGame.parseNumberOfPlayers(null));
    }

    /**
     * Test 3: Verifies the limit of 100 players.
     * 100 players is still allowed, but 101 is too many and must throw an exception.
     */
    @Test
    public void parseNumberOfPlayersRejectsMoreThan100() {
        // Assert that exactly 100 is accepted (the boundary).
        assertEquals(100, CardGame.parseNumberOfPlayers("100"));
        // Assert that one more than the limit is rejected.
        assertThrows(IllegalArgumentException.class,
                () -> CardGame.parseNumberOfPlayers("101"));
    }

    // ---------- loading and validating a pack ----------

    /**
     * Test 4: Reads each line of pack files in exact file order.
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
        // Take the complex Card objects and unpacks them into a simple, standard list of numbers.
        for (Card c : pack) {
            values.add(c.getValue());
        }
        // Assert that the cards were read in the exact order they appeared in the file.
        assertEquals(List.of(0, 7, 1000088, 4, 5, 3, 3, 9), values);
    }

    /**
     * Test 5: Rejects pack files that contain the wrong number of lines.
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
        Path forTwoPlayers = writePack("eight.txt", eightLines("1"));

        // Assert that files with the wrong line counts are rejected with an exception.
        assertThrows(IllegalArgumentException.class, () -> CardGame.loadPack(tooShort, 1));
        assertThrows(IllegalArgumentException.class, () -> CardGame.loadPack(tooLong, 1));
        assertThrows(IllegalArgumentException.class, () -> CardGame.loadPack(empty, 1));
        // Assert that an eight line pack is invalid if tested against a game where two players are required (which needs 16 lines).
        assertThrows("8 lines is wrong for 2 players", IllegalArgumentException.class,
                () -> CardGame.loadPack(forTwoPlayers, 2));
    }

    /**
     * Test 6: Verifies that the card loader rejects invalid card line formats,
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

    /**
     * Test 7: Verifies that trying to load a pack from a file path that does not exist
     * is handled by throwing an IOException.
     */
    @Test
    public void loadPackReportsAMissingFileAsAnIOException() {
        // Assert that trying to load a file name that does not exist throws an IOException.
        assertThrows(IOException.class,
                () -> CardGame.loadPack(tempFolderPath().resolve("nope.txt"), 1));
    }

    // ---------- prompting ----------

    /**
     * Test 8: Verifies that the player prompt loop handles repeated invalid user inputs.
     * Simulates a user typing multiple bad answers (letters, zero, negative numbers, and blank space)
     *  followed by a valid number, ensuring the program re-prompts, shows error messages, and recovers successfully.
     */
    @Test
    public void promptForPlayersAsksAgainUntilTheAnswerIsValid() {
        // Capture printed output messages in a temporary byte stream.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        // Simulate a user typing four invalid answers first, followed by a valid answer ("4").
        Scanner in = new Scanner("abc\n0\n-2\n\n4\n");

        // Run the player prompt method using our fake user input and output streams.
        int n = CardGame.promptForPlayers(in, out);

        // Assert that the final successfully accepted player count is 4.
        assertEquals(4, n);
        String printed = bytes.toString(StandardCharsets.UTF_8);
        // Assert that the system re-prompted the user 5 times due to the invalid entries.
        assertEquals(5, countOccurrences(printed, "Please enter the number of players:"));
        // Assert that the error message was shown 4 times.
        assertEquals(4, countOccurrences(printed, "Invalid number of players"));
    }

    /**
     * - Test 9: Verifies that the pack prompt loop handles missing or malformed pack files.
     * - Simulates a user typing a path to a non-existent file, then an incorrectly formatted pack file,
     *   and finally a valid pack file, ensuring the system re-prompts, catches errors, and loads successfully.
     */
    @Test
    public void promptForPackAsksAgainUntilAValidPackIsGiven() throws Exception {
        // Create a bad pack file and a valid pack file in advance.
        // Display bad pack as the one which contains 3 lines, but game's validation rules state that a pack file must contain exactly 8×n lines.
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
        // Assert that the prompt message was shown 3 times.
        assertEquals(3, countOccurrences(printed, "Please enter location of pack to load:"));
        // Assert that file reading and pack validation errors were reported correctly.
        assertEquals(1, countOccurrences(printed, "Could not read that pack file"));
        assertEquals(1, countOccurrences(printed, "Invalid pack:"));
    }

    // ---------- setting up a game ----------

    /**
     * Test 10: Verifies that the game constructor correctly distributes cards
     * in a strict round-robin fashion during initialization.
     * Confirms that player hands receive alternating cards first, followed by
     * the remaining cards filling up each player's deck in the correct order.
     */
    @Test
    public void constructorDealsHandsRoundRobinThenFillsDecksRoundRobin() {
        // Initialize a 2-player game with a specific sequence of 16 card values.
        CardGame game = new CardGame(2,
                cards(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                tempFolderPath());

        // Assert that the first 8 cards were dealt round-robin to the player hands.
        assertEquals("1 3 5 7", game.getPlayers().get(0).getHandString());
        assertEquals("2 4 6 8", game.getPlayers().get(1).getHandString());
        // Assert that the remaining 8 cards were filled round-robin into the decks.
        assertEquals("9 11 13 15", game.getDecks().get(0).getContents());
        assertEquals("10 12 14 16", game.getDecks().get(1).getContents());
    }

    /**
     * Test 11: Verifies that the game constructor rejects a card pack of the wrong size
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
     * Test 12: Verifies that if a player is dealt a winning hand immediately during game initialization,
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
        // Check that player 1's output log file contains the exact expected game history.
        assertEquals(List.of(
                "player 1 initial hand 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"),
                Files.readAllLines(tempFolderPath().resolve("player1_output.txt")));
        // Check that player 2's output log file records being informed of player 1's win and that they left the game.
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

}
