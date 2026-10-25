import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/*
 * Executes the card game.
 * Asks for the number of players and the pack file, then deals the hands and decks
 * and starts the player threads. When finished, it writes the deck output files.
 */
public class CardGame {

    // How many players (and decks) are in the game.
    private final int playerCount;

    // Folder where the player and deck output files will be saved.
    private final Path outputDirectory;

    // Keeps all player threads synchronized and signals when game has ended.
    private final GameState state;

    // Stores all the decks in the game.
    private final ArrayList<DeckOfCards> decks = new ArrayList<>();

    // Holding all the players in the game. Each one runs on its own thread once play() starts.
    private final ArrayList<Player> players = new ArrayList<>();

    // Keeps track of whether the game has already been played.
    private boolean played = false;

    /*
     * Creates a new game using the number of players, pack of cards and output folder.
     * Sets up all players, decks, and distributes the initial hands and deck cards.
     */
    public CardGame(int playerCount, List<Card> pack, Path outputDirectory) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Number of players must be at least 1: " + playerCount);
        }
        if (pack == null) {
            throw new NullPointerException("pack must not be null");
        }
        if (outputDirectory == null) {
            throw new NullPointerException("outputDirectory must not be null");
        }
        // Check the pack size (4 cards for each player's hand + 4 cards for each deck, so 8 * n cards in total).
        if (pack.size() != 8 * playerCount) {
            throw new IllegalArgumentException("A pack for " + playerCount
                    + " players must contain " + (8 * playerCount)
                    + " cards but has " + pack.size());
        }

        this.playerCount = playerCount;
        this.outputDirectory = outputDirectory;
        this.state = new GameState(playerCount);

        // Create and initialize a card deck for each player (numbered 1 to playerCount).
        for (int i = 1; i <= playerCount; i++) {
            decks.add(new DeckOfCards(i));
        }

        // Connect players to their left draw deck and right discard deck.
        // Player i draws from deck i and discards to deck i+1, the last player discards to deck 1.
        for (int i = 1; i <= playerCount; i++) {
            DeckOfCards leftDeck = decks.get(i - 1);
            // decks.get(i) is deck i+1 because the list starts at 0, so it's the deck on the right.
            // The % makes the last player wrap round to deck 1, e.g. with 4 players:
            // 1 % 4 = 1, 2 % 4 = 2, 3 % 4 = 3, 4 % 4 = 0 (deck 1).
            DeckOfCards rightDeck = decks.get(i % playerCount);
            // Create the player with its two decks and add it to the list of players.
            players.add(new Player(i, leftDeck, rightDeck, state, outputDirectory));
        }

        // Deal each player 4 cards round-robin from the top of the pack.
        int handCards = 4 * playerCount;
        for (int i = 0; i < handCards; i++) {
            // i % playerCount goes 0, 1, 2, 3, 0, 1, ... so each player gets one card in turn.
            players.get(i % playerCount).addCardToHand(pack.get(i));
        }
        // Deal the remaining pack cards into the decks round-robin.
        // Using (i - handCards) % playerCount resets the index to 0 and wraps around
        // so cards are distributed evenly across all decks.
        for (int i = handCards; i < pack.size(); i++) {
            decks.get((i - handCards) % playerCount).addCard(pack.get(i));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<DeckOfCards> getDecks() {
        return decks;
    }

    public int getplayerCount() {
        return playerCount;
    }

    /*
     * Runs the game and returns the winning player's ID.
     * Can be called only once for each game.
     */
    public int play() throws IOException, InterruptedException {
        // Make sure only one game at a time is being played (synchronized so two threads can't both start it).
        synchronized (this) {
            if (played) {
                throw new IllegalStateException("This game has already been played");
            }
            played = true;
        }

        // Assign each player their own thread.
        Thread[] threads = new Thread[playerCount];
        for (int i = 0; i < playerCount; i++) {
            threads[i] = new Thread(players.get(i), "player-" + players.get(i).getId());
        }

        // Use .start() to start each player's thread.
        // This will make all players run in parallel.
        for (int i = 0; i < playerCount; i++) {
            threads[i].start();
        }
        // Wait for the threads of all players to end.
        for (int i = 0; i < playerCount; i++) {
            threads[i].join();
        }

        // Save all decks, they will not change now all players have finished.
        outputDeckFiles();
        return state.getWinner();
    }

    // Saves the contents of each deck into its own deckN_output.txt file.
    private void outputDeckFiles() throws IOException {
        // Go through each deck stored in the decks list.
        for (int i = 0; i < decks.size(); i++) {
            // Get the deck at the current index 'i'.
            DeckOfCards deck = decks.get(i);
            String contents = deck.getContents();
            String line = "deck" + deck.getId() + " contents:";
            // Only add a space if there are cards left in the deck, to prevent trailing space.
            if (!contents.isEmpty()) {
                line += " " + contents;
            }

            // Use resolve() to create the file path inside output folder.
            Path file = outputDirectory.resolve("deck" + deck.getId() + "_output.txt");

            // PrintWriter will add newlines using println().
            try (PrintWriter writer = new PrintWriter(file.toFile())) {
                writer.println(line);
            }
        }
    }

    /*
     * Check user input for number of players.
     * It must be an integer greater than 0, or the IllegalArgumentException will be thrown.
     */
    static int validatePlayerCount(String text) {
        if (text == null) {
            throw new IllegalArgumentException("The number of players must be provided");
        }
        text = text.trim();

        // Check if the input contains non-numeric characters.
        if (!text.matches("[0-9]+")) {
            throw new IllegalArgumentException("'" + text + "' is not a positive integer");
        }
        // Convert string to integer.
        int n;
        try {
            n = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is too large");
        }

        // Warn if more than 100 players input, because too many threads can make problems.
        if (n > 100) {
            System.out.println("Warning: More than 100 players may degrade performance.");
        }

        // Minimum player count is one.
        if (n < 1) {
            throw new IllegalArgumentException("Minimum player count is 1");
        }

        return n;
    }

    /*
     * Loads the pack file and checks that it contains exactly 8 * n lines.
     * Each line must have a non-negative whole number.
     * Returns a list of cards in order, top of the pack first.
     */
    static List<Card> loadPack(Path file, int n) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        // Open the file and read line by line.
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String nextLine = reader.readLine();
            while (nextLine != null) {
                lines.add(nextLine);
                nextLine = reader.readLine();
            }
        }

        // The pack must contain exactly 8*n lines (4 cards per player + 4 cards per deck).
        if (lines.size() != 8 * n) {
            throw new IllegalArgumentException("a pack for " + n + " players requires "
                    + (8 * n) + " lines but this file contains " + lines.size());
        }
        ArrayList<Card> pack = new ArrayList<>();
        // Go through every line read from the file.
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            // Every line must contain a single non-negative integer.
            if (!line.matches("[0-9]+")) {
                throw new IllegalArgumentException("line " + (i + 1) + " ('" + line
                        + "') is not a non-negative integer");
            }
            try {
                // Convert text read to an integer and add a new card to the pack.
                pack.add(new Card(Integer.parseInt(line)));
            } catch (NumberFormatException e) {
                // Throws an exception if number too big for java.
                throw new IllegalArgumentException("line " + (i + 1) + " ('" + line + "') value is too large");
            }
        }
        return pack;
    }

    /*
     * Asks repeatedly for the number of players until user provides a valid response.
     * 'in' is the Scanner used to capture user input and 'out' is the PrintStream providing messages to the user.
     */
    static int promptForPlayers(Scanner in, PrintStream out) {
        // Ask repeatedly until input is valid.
        while (true) {
            out.println("Please enter the number of players:");
            try {
                return validatePlayerCount(in.nextLine());
            } catch (IllegalArgumentException e) {
                // If the input is invalid, show the error and ask again.
                out.println("Invalid number of players: " + e.getMessage() + ".");
            }
        }
    }

    // Asks for the pack file location until a valid pack is provided.
    static List<Card> promptForPack(Scanner in, PrintStream out, int n) {
        // Repeatedly ask for pack file details until a pack is successfully loaded.
        while (true) {
            out.println("Please enter location of pack to load:");
            // Strip any surrounding spaces from file location input.
            String line = in.nextLine().trim();
            try {
                // Attempt to load pack from location provided. Return the cards if location was valid.
                return loadPack(Path.of(line), n);
            } catch (IOException e) {
                // Ask again for file location if the file was not found or could not be read.
                out.println("Could not read that pack file (" + e.getMessage() + "). Please try again.");
            } catch (IllegalArgumentException e) {
                // The contents of the pack file read were invalid. Provide an error and ask again for the file name.
                out.println("Invalid pack: " + e.getMessage() + ". Please enter a valid pack file.");
            }
        }
    }

    // Start the program and ask for the inputs, then create and play the game.
    public static void main(String[] args) {
        // Request the user to input the number of players and provide the card pack.
        // Set up a game and start it.
        // Create a Scanner to capture user input from the terminal.
        Scanner in = new Scanner(System.in);
        try {
            // Repeatedly ask until the user's input is a valid number of players.
            int n = promptForPlayers(in, System.out);
            // The pack provided must match the number of players given. Ask repeatedly until this is true.
            List<Card> pack = promptForPack(in, System.out, n);
            // At this point both inputs were valid, so create the game.
            // Path.of(".") means files will be saved in the current folder.
            CardGame game = new CardGame(n, pack, Path.of("."));
            game.play();
        } catch (NoSuchElementException e) {
            // Something prevented the user completing their input.
            System.err.println("Input ended before a valid game could be set up.");
        } catch (IOException e) {
            // It was not possible to write one of the output files.
            System.err.println("Could not write the output files: " + e.getMessage());
        } catch (InterruptedException e) {
            // If the thread was interrupted, mark it as interrupted.
            Thread.currentThread().interrupt();
        }
    }
}