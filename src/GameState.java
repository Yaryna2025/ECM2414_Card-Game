/*
 * Stores information shared between all player threads, such as who has won,
 * and how many players have checked their starting hand.
 * Most methods are synchronized, so only one thread can use them at a time.
 */
public class GameState {

    private final int playerCount;
    // The winner's player number (0 means nobody has won yet).
    private int winner = 0;
    // How many players have finished checking their starting hand so far.
    private int checkedPlayers = 0;

    public GameState(int playerCount) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Number of players must be at least 1: " + playerCount);
        }
        this.playerCount = playerCount;
    }

    /*
     * Tries to make this player the winner.
     * Returns true if they won, or false if someone else already won first.
     * It is synchronized so two players finishing at the same moment cannot both win.
     */
    public synchronized boolean tryDeclareWinner(int playerId) {
        if (playerId < 1) {
            throw new IllegalArgumentException("Player id must be at least 1: " + playerId);
        }
        // Check if there is no winner yet (winner is 0).
        if (winner == 0) {
            // This player is the first, so save them as the winner.
            winner = playerId;
            // Tell the caller that this player really won.
            return true;
        }
        // If there is already a winner, return false.
        // This means the player cannot be declared the winner.
        return false;
    }

    // Returns the winner's player number, or 0 if nobody has won yet.
    public synchronized int getWinner() {
        return winner;
    }

    /*
     * The game is over as soon as there is a winner (the winner is not 0).
     * The players check this before every turn so they know when to stop.
     */
    public synchronized boolean isOver() {
        return winner != 0;
    }

    // Called once by each player after it has checked its starting hand.
    public synchronized void markInitialCheckDone() {
        // Add one to the number of players who have finished checking.
        checkedPlayers++;
    }

    /*
     * Waits until every player has checked their starting hand.
     * It checks again every 1ms until all players are done.
     * This method is not synchronized so other players can still
     * call markInitialCheckDone() while it is waiting.
     */
    public void awaitAllInitialChecks() throws InterruptedException {
        // Until every player has checked their hand, keep looping.
        while (!allChecked()) {
            // Pause for 1 millisecond to avoid using too much CPU.
            Thread.sleep(1);
        }
    }

    /*
     * Returns true once every player has checked their starting hand.
     * It is synchronized so it always reads the updated count of players who finished checking.
     */
    private synchronized boolean allChecked() {
        return checkedPlayers >= playerCount;
    }
}
