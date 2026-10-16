/**
 * Holds the state shared by all player threads: who has won, and how many
 * players have checked their starting hand.
 * The methods are synchronized, so only one thread can use them at a time.
 */
public class GameState {

    private final int numberOfPlayers;
    /*
     * The winner's player number (0 means nobody has won yet).
     */
    private int winner = 0;
    private int checkedPlayers = 0;

    public GameState(int numberOfPlayers) {
        if (numberOfPlayers < 1) {
            throw new IllegalArgumentException("Number of players must be at least 1: " + numberOfPlayers);
        }
        this.numberOfPlayers = numberOfPlayers;
    }

    /*
     * Tries to make this player the winner.
     * Returns true if they won, or false if someone else already won first.
     */
    public synchronized boolean tryDeclareWinner(int playerId) {
        if (playerId < 1) {
            throw new IllegalArgumentException("Player id must be at least 1: " + playerId);
        }
        //Asks whether the winner box is still empty.
        if (winner == 0) {
            winner = playerId;
            return true;
        }
        // If winner was already non-zero, the if is skipped and return false runs.
        // That player does not print a win message.
        return false;
    }

    public synchronized int getWinner() {
        return winner;
    }

    public synchronized boolean isOver() {
        return winner != 0;
    }

    /*
     * Called once by each player after it has checked its starting hand.
     */
    public synchronized void markInitialCheckDone() {
        checkedPlayers++;
    }

    /*
     * Waits until every player has checked its starting hand.
     * it just checks again every 1ms until the count reaches the number of players
     */
    public void awaitAllInitialChecks() throws InterruptedException {
        while (!allChecked()) {
            Thread.sleep(1);
        }
    }

    private synchronized boolean allChecked() {
        return checkedPlayers >= numberOfPlayers;
    }
}
