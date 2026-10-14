import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the card game.
 * Sets up the decks and players and deals out the cards.
 */
public class CardGame {

    private int numberOfPlayers;
    private Path outputDirectory;
    private ArrayList<CardDeck> decks = new ArrayList<>();
    private ArrayList<Player> players = new ArrayList<>();

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
            players.add(new Player(i, leftDeck, rightDeck, outputDirectory));
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
}
