# ECM2414 Coursework - Card Game Simulation

## Multi-threaded card game simulation

A Java simulation of a multi-threaded card game with n players and n decks arranged in a ring. Players are numbered from 1 to n, while Java list and array indexes start from 0. The variable i is used in loops to identify players, decks or list positions, depending on the context. Each player i runs on its own thread, draws a card from deck i, and discards a card to deck i + 1. The last player discards to deck 1, completing the ring. Each player holds four cards and aims to be the first to hold four cards of the same value.

Each player has a preferred card value based on their player number. For example, player 1 prefers cards with value 1, player 2 prefers cards with value 2, and so on.

## Dependencies

- JDK 11 or later. The code uses `Path.of`, which was added in Java 11. It has only been run on OpenJDK Temurin 25.0.4.1 (macOS).
- JUnit 4.13.2 (`lib/junit-4.13.2.jar`). The tests use `assertThrows`, so 4.13 or later is needed.
- `lib/hamcrest-core-1.3.jar`, which JUnit 4 needs. There are no other dependencies.

## Folder structure

- `src/` holds the game source files: `Card.java`, `DeckOfCards.java`, `Player.java`, `GameState.java` and `CardGame.java` (default package).
- `test/` holds the test classes: `CardTest`, `DeckOfCardsTest`, `GameStateTest`, `PlayerTest` and `CardGameTest`.
- `lib/` holds `junit-4.13.2.jar` and `hamcrest-core-1.3.jar`.
- `build/` holds the compiled game classes, made by the compile step shown below.
- `out/` holds the compiled test classes, made by the test compile step shown below.
- `pack2.txt`, `pack3.txt`, `pack4.txt` and `packwin.txt` are sample pack files used to run the game with different inputs. See below for details.

The contents of `build/` and `out/` are included as compiled copies. You can delete them and rebuild with the commands below.

## Before you start

First, unzip `ECM2414_750084711.zip`. Inside, you will find `cardsTest.zip`, `cards.jar` and the written documentation.

Unzip `cardsTest.zip` into a new empty folder. Open a terminal and go to this folder, which contains the project files. Run every command below from this folder, because the program looks for pack files relative to where you run it.

```bash
pwd
ls
```

`ls` should list `src`, `test`, `lib` and the pack files.

## Compiling the game

```bash
javac -d build src/*.java
```

This prints nothing if there are no errors, and puts the `.class` files in `build/`.

## Running the game

```bash
java -cp build CardGame
```

The program asks for two things and asks again until each answer is valid. The game does not start until both are valid.

1. `Please enter the number of players:` Type a whole number of at least 1. There is no upper limit, but more than 100 players prints a warning about performance. The game still runs.
2. `Please enter location of pack to load:` Type the path of a pack file, for example `pack2.txt` if it is in the folder you ran the command from.

### Pack file format

A pack file is designed for a specific number of players. At the start of the game, each player must have 4 cards, and each deck must also have 4 cards. Therefore, the number of cards in the pack must be 8 times the number of players in the game, with one non-negative whole number per line.

### Sample pack files

- `packwin.txt` (2 players): Player 1 is dealt `1 1 1 1` and player 2 is dealt `2 3 4 5`. Player 1 always wins at once and no turns are played.
- `pack2.txt` (2 players): A normal game with turns. Player 1 wins after three turns.
- `pack3.txt` (3 players): A normal game with turns. The winner can change between runs.
- `pack4.txt` (4 players): Several players are dealt four equal cards, so the game ends at once. The winner can change between runs.

### Output

The game writes `2n` files into the folder you ran it from:

- Each `player[player_number]_output.txt` file contains:
  - The player's initial hand.
  - Each draw, discarded card, and updated hand.
  - The final result, including a win or notification that another player has won.
  - A message indicating that the player has exited the game.
  - The player's final hand.

- Each `deck[deck_number]_output.txt` has one line, for example `deck2 contents: 1 3 3 7`.

The winner is also printed in the terminal, for example `player 4 wins`.

If two players reach four of a kind at almost the same moment, the game ensures that only one winner is announced.

## Testing the game's functionality as a black box

Run the commands below from the project folder.

### 1. Compile

```bash
javac -d build src/*.java
```

Expected: No terminal output if compilation is successful. The command compiles the Java source files from `src/` and creates or updates the corresponding `.class` files in `build/`:

- `Card.class`
- `DeckOfCards.class`
- `Player.class`
- `GameState.class`
- `CardGame.class`

### 2. Immediate win with `packwin.txt`

Run the game using the command stated below. The `-cp build` option tells Java to look for the compiled classes in the `build/` folder, and `CardGame` is the class that starts the game.

```bash
java -cp build CardGame
```

Type `2` as the number of players, because `packwin.txt` contains 16 cards and is designed for 2 players. Then enter `packwin.txt` as the pack file location.

Expected terminal output:

```
player 1 wins
```

Then look at the files using the `cat` command, which displays the contents of a file in the terminal:

```bash
cat player1_output.txt
cat player2_output.txt
cat deck1_output.txt
cat deck2_output.txt
```

`player1_output.txt` should be exactly:

```
player 1 initial hand 1 1 1 1
player 1 wins
player 1 exits
player 1 final hand: 1 1 1 1
```

`player2_output.txt` should be exactly:

```
player 2 initial hand 2 3 4 5
player 1 has informed player 2 that player 1 has won
player 2 exits
player 2 hand: 2 3 4 5
```
No turns were played, so each deck file still shows the four cards it was dealt, using the format `deck[number] contents: ...`. Each player holds four cards, giving 8 cards across the two players, and the two decks contain another 8 cards. Together, that is 16 cards (`8 x 2`), meaning no cards were lost. Everything in this step is the same on every run, because only player 1 can win and nobody takes a turn.

### 3. A game with turns using `pack2.txt`

Run the game again using `java -cp build CardGame`. Type `2` as the number of players, because `pack2.txt` contains 16 cards and is designed for 2 players. Then enter `pack2.txt` as the pack file location. Player 1 wins. `cat player1_output.txt` should show:

```
player 1 initial hand 1 1 3 4
player 1 draws a 5 from deck 1
player 1 discards a 3 to deck 2
player 1 current hand is 1 1 4 5
player 1 draws a 1 from deck 1
player 1 discards a 4 to deck 2
player 1 current hand is 1 1 5 1
player 1 draws a 1 from deck 1
player 1 discards a 5 to deck 2
player 1 current hand is 1 1 1 1
player 1 wins
player 1 exits
player 1 final hand: 1 1 1 1
```

Player 1's file is the same on every run because the cards in deck 1 are already arranged in the order it needs to win, and player 2 cannot win with this pack.

Things to check:

- Player 1 never discards a 1, which is its preferred value.
- Every draw is from deck 1 and every discard goes to deck 2.
- `cat player2_output.txt` ends with `player 1 has informed player 2 that player 1 has won`, then `player 2 exits`, then `player 2 hand: ...` with four values.
- Player 2's file and the two deck files can differ between runs because both players run in separate threads, so player 2 may complete a different number of turns before player 1 wins.
- Use `cat deck1_output.txt` and `cat deck2_output.txt` to check the cards remaining in each deck. Each player should still hold four cards at the end, and the two decks together should contain the remaining 8 cards, giving a total of 16 cards (`8 x 2`).

### 4. Three players using `pack3.txt`

Run the game again using `java -cp build CardGame`. Type `3` as the number of players, because `pack3.txt` contains 24 cards and is designed for 3 players. Then enter `pack3.txt` as the pack file location.

Unlike `pack2.txt`, the cards in `pack3.txt` are not arranged to guarantee a particular winner, because this test is intended to check how the game behaves with three players running in separate threads. The order in which players take turns can differ between runs, which may affect who wins.

Check the three player files and the three deck files:

```bash
cat player1_output.txt
cat player2_output.txt
cat player3_output.txt
cat deck1_output.txt
cat deck2_output.txt
cat deck3_output.txt
```

Things to check:

- The terminal displays `player [number] wins`, where `[number]` is the winning player's number.
- Each player's file starts with their initial hand of four cards, which will be the same on every run as dealing the cards follows the same order before the player threads start.
- For every turn a player completes, their file shows the card drawn, the card discarded and their current hand.
- Each player draws from their own deck and discards to the next deck. Player 3 discards to deck 1, which closes the circle of players.
- The winning player's file contains a winning message, an exit message and a final hand with four cards of the same value.
- The other two players' files contain a message informing them which player won, followed by an exit message and their last hand (`player [number] hand: ...`).
- Each player holds four cards at the end, giving 12 cards across the three players.
- Each deck file shows the cards remaining in that deck when the game ends. The three decks together should contain 12 cards, so the total across all players and decks is 24 (`8 x 3`), meaning no cards were lost.

### 5. Several players dealt a winning hand using `pack4.txt`

Run the game again using `java -cp build CardGame`. Type `4` as the number of players, because `pack4.txt` contains 32 cards and is designed for 4 players. Then enter `pack4.txt` as the pack file location.

Unlike `pack2.txt` and `pack3.txt`, this pack is arranged so that all four players start with four cards of the same value. This means every player already has a winning hand without needing to draw or discard any cards.

However, only one player can be declared the winner. Since the players run in separate threads, the order in which they check their hands can differ between runs. Therefore, we cannot guarantee which player will be declared the winner.

The terminal should display `player [number] wins` once, where `[number]` is the winning player's number.

Check the four player files and the four deck files:

```bash
cat player1_output.txt
cat player2_output.txt
cat player3_output.txt
cat player4_output.txt
cat deck1_output.txt
cat deck2_output.txt
cat deck3_output.txt
cat deck4_output.txt
```

Things to check:

- The terminal displays only one winning message, even though all four players start with a winning hand.
- Each player's file starts with their initial hand of four cards.
- No draw, discard or current hand messages appear because the game ends before any turns are played.
- The winning player's file contains a winning message, an exit message and a final hand with four cards of the same value.
- The other three players' files contain a message informing them which player won, followed by an exit message and their last hand (`player [number] hand: ...`).
- Each player still holds four cards at the end, giving 16 cards across the four players.
- Each deck output file shows the four cards initially dealt to that deck, using the format `deck[number] contents: ...`. Since no turns are played, the cards in the decks do not change. The four decks contain 16 cards in total, giving 32 cards (`8 x 4`) across all players and decks, meaning no cards were lost.

### 6. Invalid input

Run the game again using `java -cp build CardGame`.

First, type `abc` as the number of players. The program should print an `Invalid number of players` message and ask for the number of players again. Try entering `0` and `-4` as well. Both should be rejected because the number of players must be a positive whole number.

Then type `2` as the number of players and enter a file name that does not exist, such as `main.txt`. The program should print a `Could not read that pack file (...)` message and ask for the pack file location again.

Finally, enter `pack2.txt` as the pack file location. Since this is a valid pack for 2 players, the game should start.

Things to check:

- The program rejects `abc`, `0` and `-4` as invalid numbers of players.
- After each invalid input, the program asks for the number of players again instead of ending.
- The program rejects `main.txt` because the file does not exist and asks for the pack file location again.
- After entering `pack2.txt`, the game starts normally without needing to restart the program.
- The terminal eventually displays `player 1 wins`, as expected for `pack2.txt`.

### 7. Cleaning up

Run the following commands from the project folder:

```bash
rm -f player*_output.txt deck*_output.txt
rm -rf build
```
The first command deletes the generated player and deck output files. The second command deletes the compiled game files in `build/`.

### Compiling and running the JUnit tests

The project includes JUnit tests for `Card`, `DeckOfCards`, `GameState`, `Player` and `CardGame`. These tests check the different classes within the program, including card handling, input validation, game setup and complete games.

Run the following commands from the project folder.

**1. Compile the game files**

Run the following commands to create the `build/` folder and compile all Java files from `src/` into it.

```bash
mkdir -p build
javac -d build src/*.java
```

**2. Compile the test files**

Make sure `junit-4.13.2.jar` and `hamcrest-core-1.3.jar` are in the `lib/` folder. JUnit is used to run the tests, while Hamcrest is a supporting library required by JUnit 4.13.2.

Run the following commands to create the `out/` folder and compile all Java test files from `test/` into it.

```bash
mkdir -p out
javac -cp build:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar -d out test/*.java
```

**3. Run all 40 JUnit tests**

```bash
java -cp out:build:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar org.junit.runner.JUnitCore CardTest DeckOfCardsTest GameStateTest PlayerTest CardGameTest | sed 's/^Tests run:/Tests completed:/'
```

A passing run ends with `OK (40 tests)`, meaning all 40 tests have passed. Three `player 1 wins` messages appear because three tests in `CardGameTest.java` run complete games with card values chosen so that Player 1 wins. The other tests also run, but they check different parts of the program, for example: card handling, input validation and game setup, without printing a winner.

If a test fails, the terminal displays `Tests completed: 40, Failures: 1` (if one of the 40 tests fails), along with information about the failed test. The `sed` command changes JUnit's original `Tests run` wording to `Tests completed` without changing the test results. This is my own decision to make the description clearer.

**4. Repeat the tests five times**

To check that the tests pass consistently, run:

```bash
for i in 1 2 3 4 5; do
    java -cp out:build:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar org.junit.runner.JUnitCore CardTest DeckOfCardsTest GameStateTest PlayerTest CardGameTest | sed 's/^Tests run:/Tests completed:/'
done
```

Each successful run should end with `OK (40 tests)`. We run all the tests five times to check that they pass each time, including the tests that use multiple threads.
We run the tests more than once because the game uses threads, and errors caused by threads can be non-deterministic, meaning they might appear in some runs but not others.

### What the tests cover

- `CardTest` (4 tests): checks the stored card value, that 0 is accepted, that negative values are rejected and the `toString` output.

- `DeckOfCardsTest` (8 tests): checks creating decks, rejecting invalid deck IDs and null cards, drawing from an empty deck, drawing cards in the order they were added (FIFO - first in, first out), and the text showing the deck contents. The latter is checked to make sure that the contents are listed from top to bottom with spaces between the values, and that an empty deck gives an empty string. It also checks that no cards are lost when 8 threads add cards at the same time.

- `GameStateTest` (4 tests): verifies that there is no winner at the start, invalid player counts and IDs are rejected, and only the first player to claim a win succeeds.

- `PlayerTest` (11 tests): checks that a player can hold no more than four cards, rejecting null cards, winning hands (including when the four matching cards are not the player's preferred value) and incomplete hands. It also checks drawing from the left deck and discarding to the right deck, never discarding preferred cards, discarding the first unwanted card in the hand, doing nothing when the left deck is empty, rejecting a turn when the player already has a winning hand, and taking a turn correctly when the left deck has a higher ID (the last player in the ring).

- `CardGameTest` (13 tests): tests valid and invalid player counts and pack files, as well as handling missing input and asking again after invalid input. It also checks dealing cards to players and decks in the correct order (round robin), an immediate win, the exact output files of a one player game, and a two player game using separate threads. The two player game test also checks that the game ends correctly, every player still holds four cards, no cards are lost or duplicated, and all player and deck output files are created. File tests use JUnit's `TemporaryFolder`, so they do not leave files behind. Two of the tests that run a complete game have a 10 second timeout to stop them if they take too long.

## Building and running `cards.jar`

From the project folder, after compiling the game:

```bash
jar cfe cards.jar CardGame -C build . -C src .
```

This builds a jar whose main class is `CardGame`, containing both the `.class` files and the `.java` files. To check what went in:

```bash
jar tf cards.jar
```

You should see the five `.class` files and the five `.java` files, plus `META-INF/MANIFEST.MF`. Run it from a folder that contains a pack file:

```bash
java -jar cards.jar
```

It asks the same two questions as before and writes the output files into the folder you ran it from.

## Windows

The project was developed and tested on macOS. On Windows, some terminal commands may be different, such as using `;` instead of `:` in Java classpaths. The project has not been tested on Windows.


## The purpose of each source file

- `Card.java`: is a class that represents a card and ensures that cards can only have non-negative values and cannot change after creation, making them thread-safe.

- `DeckOfCards.java`: is a class that represents a first in, first out (FIFO) deck of cards. Cards are drawn from the top and added to the bottom, and all methods that access or change the deck are synchronized, so two threads cannot access or change the deck at the same time.

- `Player.java`: is a class that manages the actions of a single player running on its own thread. During each turn, the player draws a card from the left deck and discards a card that is not their preferred value to the right deck. It locks both decks (the one with the lower ID first) so drawing and discarding happen as one atomic action. It protects the player's hand by locking the player object while the hand is being changed and writes the player's output file.

- `GameState.java`: stores the state shared by all players, including who has won, and makes sure nobody starts drawing cards before every player has checked their starting hand.

- `CardGame.java`: the main file of the program. It reads the number of players and the pack file, deals the cards to players and decks, starts the player threads, waits for them to finish, and writes the deck output files.

## Design notes

- A player's turn locks both decks (the one with the lower ID first, to avoid deadlock), so drawing and discarding happen as one atomic action.

- `GameState.tryDeclareWinner` is synchronized, so only one player can successfully declare a win.

- The game makes every player wait until all players have checked their starting hands before anyone starts drawing cards. This makes sure that any immediate win is checked before normal turns begin.

- If a player's left deck is empty, the player waits for a short period of time and tries again instead of stopping the game. This lets the player continue when another player adds a card to that deck.