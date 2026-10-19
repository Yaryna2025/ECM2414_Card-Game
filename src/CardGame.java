import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Runs the card game.
 * Asks for the number of players and the pack file, then deals the hands and decks
 * and starts the player threads. When finished, it writes the deck output files.
 */
public class CardGame {

    private int numberOfPlayers;
    // Directory where all output log files will be written.
    private Path outputDirectory;
    // Central hub that keeps all player threads synchronized and signals when game has ended.
    private GameState state;
    // Storing all the card decks in the system.
    private ArrayList<CardDeck> decks = new ArrayList<>();
    // Holding all the active player thread objects.
    private ArrayList<Player> players = new ArrayList<>();
    // Flag indicating wheather the game has already been played.
    private boolean played = false;

    public CardGame(int numberOfPlayers, List<Card> pack, Path outputDirectory) {
        if (numberOfPlayers < 1) {
            throw new IllegalArgumentException("Number of players must be at least 1: " + numberOfPlayers);
        }
        if (pack == null || outputDirectory == null) {
            throw new NullPointerException("pack and outputDirectory must not be null");
        }
        // Check the pack size (4 cards for each player's hand + 4 cards for each deck,
        // so 8 * n cards in total).
        if (pack.size() != 8 * numberOfPlayers) {
            throw new IllegalArgumentException("A pack for " + numberOfPlayers
                    + " players must contain " + (8 * numberOfPlayers)
                    + " cards but has " + pack.size());
        }
        this.numberOfPlayers = numberOfPlayers;
        this.outputDirectory = outputDirectory;
        this.state = new GameState(numberOfPlayers);

        for (int i = 1; i <= numberOfPlayers; i++) {
            decks.add(new CardDeck(i));
        }

        // Connect players to their left draw deck and right discard deck.
        // player i draws from deck i and discards to deck i+1, the last player discards to deck 1
        for (int i = 1; i <= numberOfPlayers; i++) {
            CardDeck leftDeck = decks.get(i - 1);
            // decks.get(i) is deck i+1 because the list starts at 0, so it's the deck on the right.
            // the % makes the last player wrap round to deck 1, e.g. with 4 players:
            // 1 % 4 = 1, 2 % 4 = 2, 3 % 4 = 3, 4 % 4 = 0 (deck 1)
            CardDeck rightDeck = decks.get(i % numberOfPlayers);
            players.add(new Player(i, leftDeck, rightDeck, state, outputDirectory));
        }

        // Deal each player 4 cards round-robin from the top of the pack.
        int handCards = 4 * numberOfPlayers;
        for (int i = 0; i < handCards; i++) {
            // i % numberOfPlayers goes 0, 1, 2, 3, 0, 1, ... so each player gets one card in turn
            players.get(i % numberOfPlayers).addToHand(pack.get(i));
        }
        // Deal the remaining pack cards into the decks round-robin
        for (int i = handCards; i < pack.size(); i++) {
            decks.get((i - handCards) % numberOfPlayers).addCard(pack.get(i));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<CardDeck> getDecks() {
        return decks;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    /**
     * Plays the game and returns the number of the winning player.
     * May be called only once.
     */
    public int play() throws IOException, InterruptedException {
        // Make sure the game can only be played once (synchronized so two threads can't both start it).
        synchronized (this) {
            if (played) {
                throw new IllegalStateException("This game has already been played");
            }
            played = true;
        }

        // Wrap each player in their own thread.
        Thread[] threads = new Thread[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++) {
            threads[i] = new Thread(players.get(i), "player-" + players.get(i).getId());
        }
        for (int i = 0; i < numberOfPlayers; i++) {
            threads[i].start();
        }
        // Wait for all player threads to finish.
        for (int i = 0; i < numberOfPlayers; i++) {
            threads[i].join();
        }

        writeDeckFiles();
        return state.getWinner();
    }

    /*
     * Saves the final contents of each deck into deckN_output.txt.
     */
    private void writeDeckFiles() throws IOException {
        for (int i = 0; i < decks.size(); i++) {
            CardDeck deck = decks.get(i);
            String contents = deck.getContents();
            String line = "deck" + deck.getId() + " contents:";
            if (!contents.isEmpty()) {
                line += " " + contents;
            }
            Path file = outputDirectory.resolve("deck" + deck.getId() + "_output.txt");
            FileWriter writer = new FileWriter(file.toFile());
            writer.write(line + System.lineSeparator());
            writer.close();
        }
    }

    /*
     * Checks the text typed by the user for the number of players.
     * It has to be a whole number of at least 1, otherwise it throws IllegalArgumentException.
     */
    static int parseNumberOfPlayers(String text) {
        if (text == null) {
            throw new IllegalArgumentException("No number of players given");
        }
        text = text.trim();
        if (!text.matches("[0-9]+")) {
            throw new IllegalArgumentException("'" + text + "' is not a positive whole number");
        }
        int n;
        try {
            n = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is too large");
        }

        // Performance warning for high thread counts (doesn't crash).
        if (n > 100) {
            System.out.println("Warning: Running with more than 100 players may impact performance due to high thread and file usage.");
        }

        // Must be a positive integer.
        if (n < 1) {
            throw new IllegalArgumentException("There must be at least 1 player");
        }

        return n;
    }

    /*
     * Reads the pack file and checks it has exactly 8n lines and every line
     * is a non-negative whole number. Returns the cards in order (top of the pack first).
     */
    static List<Card> loadPack(Path file, int n) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file.toFile()));
        String nextLine = reader.readLine();
        while (nextLine != null) {
            lines.add(nextLine);
            nextLine = reader.readLine();
        }
        reader.close();

        if (lines.size() != 8 * n) {
            throw new IllegalArgumentException("a pack for " + n + " players needs "
                    + (8 * n) + " lines but this file has " + lines.size());
        }
        ArrayList<Card> pack = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.matches("[0-9]+")) {
                throw new IllegalArgumentException("line " + (i + 1) + " ('" + line
                        + "') is not a non-negative integer");
            }
            try {
                pack.add(new Card(Integer.parseInt(line)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("line " + (i + 1) + " ('" + line + "') is too large");
            }
        }
        return pack;
    }

    /*
     * Asks for the number of players until a valid answer is given.
     */
    static int promptForPlayers(Scanner in, PrintStream out) {
        while (true) {
            out.println("Please enter the number of players:");
            try {
                return parseNumberOfPlayers(in.nextLine());
            } catch (IllegalArgumentException e) {
                out.println("Invalid number of players: " + e.getMessage() + ".");
            }
        }
    }

    /*
     * Asks for a pack location until a valid pack file is given.
     */
    static List<Card> promptForPack(Scanner in, PrintStream out, int n) {
        while (true) {
            out.println("Please enter location of pack to load:");
            String line = in.nextLine().trim();
            try {
                return loadPack(Path.of(line), n);
            } catch (IOException e) {
                out.println("Could not read that pack file (" + e.getMessage() + "). Please try again.");
            } catch (IllegalArgumentException e) {
                out.println("Invalid pack: " + e.getMessage() + ". Please enter a valid pack file.");
            }
        }
    }

    public static void main(String[] args) {
        // Ask the user for the number of players and a valid card pack, then set up and play the game.
        Scanner in = new Scanner(System.in);
        try {
            int n = promptForPlayers(in, System.out);
            List<Card> pack = promptForPack(in, System.out, n);
            CardGame game = new CardGame(n, pack, Path.of("."));
            game.play();
        } catch (NoSuchElementException e) {
            System.err.println("Input ended before a valid game could be set up.");
        } catch (IOException e) {
            System.err.println("Could not write the output files: " + e.getMessage());
        } catch (InterruptedException e) {
            // Mark the thread as interrupted again if it got cut off.
            Thread.currentThread().interrupt();
        }
    }
}
