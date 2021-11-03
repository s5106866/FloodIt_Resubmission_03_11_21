@file:UseSerializers(IntMatrixSerializer::class)
@file:Suppress("unused")

package uk.ac.bournemouth.ap.floodit.test

import kotlinx.serialization.*
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.ac.bournemouth.ap.lib.matrix.int.IntMatrix
import uk.ac.bournemouth.ap.lib.matrix.forEachIndex
import uk.ac.bournemouth.ap.lib.matrix.int.mapInt
import uk.ac.bournemouth.ap.floodit.logic.FlooditGame
import uk.ac.bournemouth.ap.floodit.logic.FlooditGame.State
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import kotlin.math.floor
import kotlin.random.Random

abstract class FlooditTest {

    /** Convenience function to create a game given initialization information. */
    private fun createGameFromGivenData(gameInfo: GameInfo) =
        createGameFromGivenData(gameInfo.initialGame, gameInfo.colourCount, gameInfo.maxTurns)

    /**
     * This function is called to create a game with a specific configuration. The size is
     * determined by the [colours] matrix. This also determines the actual colours placed.
     *
     * @param colours The actual grid of colours used
     * @param colourCount The amount of different colours (and 1+<the highest colour value>)
     * @param maxTurns The amount of rounds that can be played before the game is lost.
     */
    abstract fun createGameFromGivenData(
        colours: IntMatrix,
        colourCount: Int,
        maxTurns: Int
    ): FlooditGame

    /**
     * This function is called to create a random game. Implementations should return a subclass
     * with the given properties.
     * @param width The width of the game
     * @param height The height of the game.
     * @param colourCount The (maximum) amount of colours. Note that colour numbers have to be below this
     * @param maxTurns The maximum amount of turns before the game is lost
     * @param random The random object used
     */
    abstract fun createRandomGame(
        width: Int,
        height: Int,
        colourCount: Int,
        maxTurns: Int,
        random: Random
    ): FlooditGame


    /**
     * Games, when created, should have the correct size (width and height) as passed along to the
     * factory function.
     */
    @DisplayName("Test games created with correct size")
    @ParameterizedTest(name = "Test games created with correct size: ({0}×{1} [{2}])")
    @MethodSource("testConfigurations")
    fun testCreateGameSizeCorrect(
        width: Int,
        height: Int,
        colourCount: Int,
        maxRounds: Int,
        seed: Long
    ) {
        val game = createRandomGame(width, height, colourCount, maxRounds, Random(seed))
        assertEquals(width, game.width, "The game should have the requested width")
        assertEquals(height, game.height, "The game should have the requested height")
    }

    /**
     * Some game size parameters are incorrect:
     * - dimensions of 0 in either direction
     * - negative dimensions in either direction
     */
    @Test
    @DisplayName("Test games cannot be created with incorrect sizes")
    fun testCreateGameSizeInCorrect() {
        val rnd = Random(5465413)
        assertThrows<Exception>("No game with zero width") {
            createRandomGame(0, 5, 5, 10, rnd)
        }
        assertThrows<Exception>("No game with zero height") {
            createRandomGame(7, 0, 6, 15, rnd)
        }
        assertThrows<Exception>("No game with negative width") {
            createRandomGame(-1, 5, 4, 12, rnd)
        }
        assertThrows<Exception>("No game with negative height") {
            createRandomGame(7, -1, 7, 19, rnd)
        }
    }

    /**
     * Games cannot have a maximum amount of turns of 0 or less.
     */
    @Test
    @DisplayName("Test games cannot be created with incorrect round counts")
    fun testCreateGameLowMaxRound() {
        val rnd = Random(5465413)
        assertThrows<Exception>("No game with zero rounds") {
            createRandomGame(10, 5, 5, 0, rnd)
        }
        assertThrows<Exception>("No game with negative rounds") {
            createRandomGame(8, 5, 4, -1, rnd)
        }
    }

    /**
     * When creating a random game, the actually generated colours need to be in the range
     * [0,colourCount>.
     */
    @DisplayName("Test games created are with correct amount of different colours")
    @ParameterizedTest(name = "Test games created are with correct amount of different colours: ({0}×{1} [{2}])")
    @MethodSource("testConfigurations")
    fun testCreateGameValidColourCounts(
        width: Int,
        height: Int,
        colourCount: Int,
        maxRounds: Int,
        seed: Long
    ) {
        val game = createRandomGame(width, height, colourCount, maxRounds, Random(seed))
        for (x in 0 until game.width) {
            for (y in 0 until game.height) {
                val colour = game[x, y]

                assertTrue(colour >= 0, "The lowest colour index is 0")

                assertTrue(colour < colourCount) {
                    "The maximum colour value should be ${colourCount - 1}"
                }
            }
        }
    }

    /**
     * Random games need to be actually random and cannot be the same game in different colour
     * configurations. The frequencies of colours should also diverge most of the time (not be
     * constant).
     */
    @DisplayName("Test that random configurations actually are random")
    @ParameterizedTest(name = "Test that random configurations actually are random: ({0}×{1} [{2}])")
    @MethodSource("testConfigurations")
    fun testRandomConfigurationsNotRepeated(
        width: Int,
        height: Int,
        colourCount: Int,
        maxRounds: Int,
        seed: Long
    ) {
        val random = Random(seed)
        var gameSortOfEquals = 0

        /*
         * The test will compare 10 pairs of random games. For each pair, the colour frequencies are
         * counted and then sorted (to make it independent of actual colours). At most
         * 1 out of 10 of these comparisons should be equal (which doesn't mean the games are
         * actually the same). If the frequencies are the same for both, these frequencies are used
         * to remap the colours for each and the resulting matrices compared. This makes for a
         * colour independent comparison, this should be unequal.
         */
        repeat(10) {
            val game1 = createRandomGame(width, height, colourCount, maxRounds, random)
            val game2 = createRandomGame(width, height, colourCount, maxRounds, random)

            val freq1 = Array(colourCount) { FreqCount() }
            val freq2 = Array(colourCount) { FreqCount() }
            var unEqual = 0
            for (x in 0 until game1.width) {
                for (y in 0 until game1.height) {
                    val c1 = game1[x, y]
                    val c2 = game2[x, y]

                    freq1[c1].update(x + y * width)
                    freq2[c2].update(x + y * width)
                    if (c1 != c2) {
                        ++unEqual
                    }
                }
            }
            assertTrue(unEqual > 0) { "Two separate games should have different configurations" }

            if (freq1.sorted() == freq2.sorted()) {
                val map1: IntArray = freq1.createReorderMap()
                val matrix1 = IntMatrix(width, height) { x, y -> map1[game1[x, y]] }

                val map2: IntArray = freq1.createReorderMap()
                val matrix2 = IntMatrix(width, height) { x, y -> map2[game2[x, y]] }

                assertNotEquals(matrix1, matrix2)


                println("Found identical frequencies: ${freq1.joinToString()} and ${freq2.joinToString()}\nGame 1: ${matrix1}\nGame2: $matrix2")
                if (matrix1 == matrix2) {
                    ++gameSortOfEquals
                }
            }
        }
        assertTrue(gameSortOfEquals <= 1) { "Colour frequencies should diverge at least 9 out of 10 times" }
    }

    /**
     * Check that when games are generated, colours should be used more or less evenly.
     */
    @DisplayName("Test that random colours are distributed evenly")
    @ParameterizedTest(name = "Test that random colours are distributed evenly: ({0}×{1} [{2}])")
    @MethodSource("testConfigurations")
    fun testCreateGameValidColourDistributions(
        width: Int,
        height: Int,
        colourCount: Int,
        maxRounds: Int,
        seed: Long
    ) {
        val random = Random(seed)
        /* Ensure enough sampling to reduce noise. Smaller grids need more iterations, but always
           at least 5 */
        val neededIterations = (4000 / (width * height)).coerceAtLeast(5)

        val counts = IntArray(colourCount)
        for (i in 1..neededIterations) {
            val game = createRandomGame(width, height, colourCount, maxRounds, random)
            for (x in 0 until game.width) {
                for (y in 0 until game.height) {
                    val colour = game[x, y]
                    counts[colour]++
                }
            }
        }

        // Determine the expected amount of pixels of each colour
        val expectedAmounts =
            (width * height * neededIterations).toDouble() / colourCount.toDouble()

        // The minimum required count is 1 or half the expected value (whichever is bigger)
        val minCount = floor(expectedAmounts / 2.0).toInt().coerceAtLeast(1)

        // Check the frequencies for each colour, maximums don't need too much checking as we check
        // all colours
        for (c in 0 until colourCount) {
            assertTrue(minCount <= counts[c]) {
                "Over $neededIterations iterations the occurence of colour $c should be at least 50% of a normal distribution ($minCount !< ${counts[c]}"
            }
        }
    }

    /**
     * If the colourCount is lower than the actual maximum colour value this should throw an
     * exception.
     */
    @DisplayName("Check that a too low colour count throws an exception")
    @ParameterizedTest(name = "Check that a too low colour count throws an exception: {0}")
    @MethodSource("colourTestGames")
    fun testGamesCheckValidColourCounts(gameInfo: GameInfo) {
        assertThrows<Exception>("If the colour count is too low, throw an exception") {
            createGameFromGivenData(
                gameInfo.initialGame,
                gameInfo.colourCount - 1,
                gameInfo.maxTurns
            )
        }
    }

    /**
     * It should not be possible to create games with negative or zero colour count (that should
     * throw an exception).
     */
    @DisplayName("Test games have valid colour counts")
    @ParameterizedTest(name = "Test games have valid colour counts: ({0}×{1} [{2}])")
    @MethodSource("testConfigurations")
    fun testGamesCheckPositiveColourCounts(
        width: Int,
        height: Int,
        colourCount: Int,
        maxRounds: Int,
        seed: Long
    ) {
        val rnd = Random(seed)
        assertThrows<Exception>("Negative colours are impossible") {
            createRandomGame(width, height, rnd.nextInt(-55555, 0), maxRounds, rnd)
        }
        assertThrows<Exception>("No colours is not a valid game") {
            createRandomGame(width, height, 0, maxRounds, rnd)
        }
    }

    /**
     * It should not be possible to create a game with negative given colour values (this should
     * throw an exception).
     */
    @DisplayName("Test games cannot have negative colours")
    @ParameterizedTest(name = "Test games cannot have negative colours: {0}")
    @MethodSource("colourTestGames")
    fun testGamesCheckNoNegativeColours(gameInfo: GameInfo) {
        assertThrows<Exception>("Negative colours are impossible") {
            createGameFromGivenData(
                gameInfo.initialGame.mapInt { if (it == 0) -1 else it },
                gameInfo.colourCount,
                gameInfo.maxTurns
            )
        }
    }

    /**
     * When creating a game from an existing configuration this should create what was asked:
     * - Dimensions are matching
     * - Maximum turns are matching
     * - The colour counts are matching
     * - All cells match
     */
    @DisplayName("Creating a game from existing config")
    @ParameterizedTest(name = "Creating a game from existing config: {0}")
    @MethodSource("testGames")
    fun testCreateGameFromMatrix(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        assertEquals(gameInfo.initialGame.width, game.width) {
            "The width of the created game should be as requested"
        }
        assertEquals(gameInfo.initialGame.height, game.height) {
            "The height of the created game should be as requested"
        }
        assertEquals(0, game.round) { "Initially, the game should be in round 0" }
        assertEquals(gameInfo.maxTurns, game.maxTurns) {
            "The maximum rounds for a game should be as requested"
        }
        assertEquals(gameInfo.colourCount, game.colourCount) {
            "The (max) amount of colours in the game should be the same as requested"
        }

        gameInfo.initialGame.forEachIndex { x, y ->
            assertEquals(gameInfo.initialGame[x, y], game[x, y]) {
                "The game cells should be initialised as expected (for $x,$y)"
            }
        }
    }

    /**
     * A single flood move should be correct in how it updates the grid (and does a flood fill).
     * This is tested based upon given test data with pre and post expected grids.
     */
    @DisplayName("Test a single move")
    @ParameterizedTest(name = "Test a single move: {0}")
    @MethodSource("testGames")
    fun testSingleFlood(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)
        val move = gameInfo.moves.first()

        game.playColour(move.colour)

        // Check that for each coordinate in the grid, the colours are as expected. This tests
        // that the flood fill is implemented correctly.
        move.newState.forEachIndex { x, y ->
            assertEquals(move.newState[x, y], game[x, y]) {
                "The game cells after 1 move be as expected (for $x,$y)"
            }
        }

        assertEquals(1, game.round) { "After a single move, the game should be in round 1" }
    }

    /**
     * Test an entire full game on that it correctly floods on each move. Don't look at this too
     * much before [testSingleFlood] has passed. It only checks the colour cells.
     */
    @DisplayName("Test full flooding games against expected results")
    @ParameterizedTest(name = "Test full flooding games against expected results: {0}")
    @MethodSource("testGames")
    fun testFloodFully(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        // For all the moves in the pre-prepared game
        for ((round, move) in gameInfo.moves.withIndex()) {
            // play the move
            game.playColour(move.colour)

            // Check the resulting grid
            move.newState.forEachIndex { x, y ->
                assertEquals(move.newState[x, y], game[x, y]) {
                    "The game cells after move $round be as expected (for $x,$y)\n" +
                            move.newState.toString("Expected: ") +
                            IntMatrix(
                                game.width,
                                game.height
                            ) { x, y -> game[x, y] }.toString("Found:     ")
                }
            }

        }
    }

    /**
     * Test that after each round, the current round is updated (by 1).
     */
    @DisplayName("After a move the round counts should be updated")
    @ParameterizedTest(name = "After a move the round counts should be updated: {0}")
    @MethodSource("testGames")
    fun testRoundCounts(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        var round = 0
        for (move in gameInfo.moves) {
            game.playColour(move.colour)
            ++round
            assertEquals(round, game.round) { "The rounds should be updated correctly." }
        }

        assertEquals(gameInfo.moves.size, game.round) {
            "After a moves the rounds should be as expected"
        }
    }

    /**
     * Check the after finishing a game, the game state is won/lost (and the amount of rounds played
     * is as expected).
     */
    @DisplayName("After finishing a game, check the state")
    @ParameterizedTest(name = "After finishing a game, check the state: {0}")
    @MethodSource("testGames")
    fun testFloodFinish(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        for (move in gameInfo.moves) {
            game.playColour(move.colour)
        }

        assertEquals(gameInfo.moves.size, game.round) {
            "After a moves the rounds should be as expected"
        }

        assertEquals(gameInfo.state, game.state) {
            "Game state should be as expected"
        }

        assertThrows<Exception>("After finishing, the game should not allow playing") {
            game.playColour(0)
        }
    }

    /**
     * Test that games with the amount of moves/turns equal to the maximum turn count can still
     * be won.
     */
    @DisplayName("Test that games using the maximum allowed rounds are still won")
    @ParameterizedTest(name = "Test that games using the maximum allowed rounds are still won: {0}")
    @MethodSource("testMaxGames")
    fun testFloodJustWon(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        for (move in gameInfo.moves) {
            game.playColour(move.colour)
        }

        assertEquals(gameInfo.moves.size, game.round) {
            "After a moves the rounds should be as expected"
        }

        assertEquals(gameInfo.maxTurns, game.round) {
            "These games are expected to be won in exactly the maximum amount of rounds"
        }
        assertEquals(State.WON, game.state) { "The game should have been won" }
    }

    /**
     * Test that the listeners are properly invoked, with correct parameters, for the various
     * game events.
     */
    @DisplayName("Test that listeners are invoked (correctly)")
    @ParameterizedTest(name = "Test that listeners are invoked (correctly): {0}")
    @MethodSource("testGames")
    fun testListeners(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        val listener = TestListener(game)
        game.addGameOverListener(listener)
        game.addGamePlayListener(listener)

        var round = 0
        for (move in gameInfo.moves.dropLast(1)) {
            ++round
            val lastInvocationCount = listener.invocationCount
            val actualChange = game[0, 0] != move.colour
            game.playColour(move.colour)

            assertEquals(round, listener.lastRound) { "The round parameter should be as expected" }

            if (actualChange) {
                assertEquals(lastInvocationCount + 1, listener.invocationCount) {
                    "The listener should be invoked exactly once when playing a colour"
                }
            } else {
                assertTrue(listener.invocationCount in lastInvocationCount..(lastInvocationCount + 1)) {
                    "In the case that a move doesn't change the colour, invocation of the listener" +
                            " is optional, but must not be done more than once"
                }
            }
            assertEquals(move.colour, listener.lastColour) {
                "The notified colour should be the same as the move"
            }
            assertFalse(listener.isFinished) {
                "Until the last round finished should not be true"
            }
            assertEquals(State.RUNNING, game.state) {
                "The game should not be won at this point yet"
            }
        }
        run {
            val move = gameInfo.moves.last()
            val lastInvocationCount = listener.invocationCount
            game.playColour(move.colour)
            assertEquals(lastInvocationCount + 2, listener.invocationCount) {
                "The last move must trigger both onGameChanged and onGameOver"
            }
            assertTrue(listener.isFinished) {
                "Won or lost, the game should be finished"
            }
            assertEquals(gameInfo.state == State.WON, listener.isWon) {
                "Game won information should match the expected win state"
            }
        }

    }

    /**
     * Test that listener registration is correct, either when listeners are added in between, or
     * removed in between.
     */
    @DisplayName("Test that changing the listeners works correctly")
    @ParameterizedTest(name = "Test that changing the listeners works correctly: {0}")
    @MethodSource("testTenPlusGames")
    fun testListenerModification(gameInfo: GameInfo) {
        val game = createGameFromGivenData(gameInfo)

        val listener1 = TestListener(game)
        val listener2 = TestListener(game)
        val listener3 = TestListener(game)
        val listener4 = TestListener(game)
        val listener5 = TestListener(game)
        game.addGameOverListener(listener1)
        game.addGamePlayListener(listener1)

        game.addGameOverListener(listener3)
        game.addGamePlayListener(listener3)
        game.addGamePlayListener(listener3) //register again to check we are not notified more than once

        val moveIterator = gameInfo.moves.iterator()
        run {
            game.playColour(moveIterator.next().colour)
            assertEquals(1, listener1.invocationCount) {
                "This listener is registered so should have been invoked"
            }
            assertEquals(0, listener2.invocationCount) {
                "This listener is registered so should not have been invoked"
            }
            assertEquals(1, listener3.invocationCount) {
                "Listeners should be invoked once only, even if registered more than once"
            }
        }

        game.removeGamePlayListener(listener1); listener1.unsetRound()
        game.addGamePlayListener(listener2)
        game.addGameOverListener(listener4)
        run {
            game.playColour(moveIterator.next().colour)
            assertEquals(1, listener1.invocationCount) {
                "This listener is no longer registered so should not been invoked"
            }
            assertEquals(1, listener2.invocationCount) {
                "This listener is now registered so should have been invoked"
            }
            assertEquals(2, listener3.invocationCount) {
                "This listener has not changed so should have been invoked twice"
            }
            assertEquals(0, listener4.invocationCount) {
                "This listener has only been registered for game over events"
            }
        }

        game.removeGamePlayListener(listener1) // This should not do anything
        game.addGamePlayListener(listener1)
        game.addGamePlayListener(listener4)
        run {
            game.playColour(moveIterator.next().colour)
            assertEquals(2, listener1.invocationCount) {
                "This listener is again registered so should have been invoked"
            }
            assertEquals(2, listener2.invocationCount) {
                "This listener is now registered so should have been invoked"
            }
            assertEquals(3, listener3.invocationCount) {
                "This listener has not changed so should have been invoked twice"
            }
            assertEquals(1, listener4.invocationCount) {
                "This listener has only been registered for game over events"
            }
        }

        game.removeGameOverListener(listener1)
        game.addGameOverListener(listener5)
        game.addGameOverListener(listener5)

        while (moveIterator.hasNext()) {
            game.playColour(moveIterator.next().colour)
        }

        assertFalse(listener1.isFinished) {
            "Listener 1 not should have been called on the game finishing"
        }
        assertFalse(listener2.isFinished) {
            "Listener 2 should have been called on the game finishing"
        }
        assertTrue(listener3.isFinished) {
            "Listener 3 should have been called on the game finishing"
        }
        assertTrue(listener4.isFinished) {
            "Listener 4 should have been called on the game finishing"
        }
        assertTrue(listener5.isFinished) {
            "Listener 5 should have been called on the game finishing"
        }

        assertEquals(1, listener5.invocationCount) {
            "Listener 5 should have been invoked once only"
        }

    }

    /**
     * The companion object contains some methods used to feed test data into the tests. The test
     * methods use these so that they are called multiple times with different parameters.
     */
    companion object {
        /**
         * Read game info from the resource with the given name.
         */
        private fun readGameInfo(resourceName: String): List<GameInfo> {
            return readTestData(resourceName)
        }

        /**
         * Inline function that automatically reads the expected list content (from the resource
         * with the given name).
         */
        private inline fun <reified T> readTestData(resourceName: String): List<T> {
            return readTestData(serializer(), resourceName)
        }

        /**
         * ReadTestData actually reads the data from the resource. It transparently decompresses
         * files ending on .gz, otherwise it expects uncompressed json.
         */
        private fun <T> readTestData(serializer: KSerializer<T>, resourceName: String): List<T> {
            val json = Json
            return FlooditTest::class.java.classLoader.getResourceAsStream(resourceName)!!
                .use { inStream ->
                    val reader = when {
                        resourceName.endsWith(".gz", true) ->
                            InputStreamReader(GZIPInputStream(inStream))

                        else -> InputStreamReader(inStream)
                    }

                    reader.use { input ->
                        json.decodeFromString(ListSerializer(serializer), input.readText())
                    }
                }

        }

        /**
         * Read a set of default test configurations (without actual game data).
         */
        @JvmStatic
        fun testConfigurations() = readTestData<GameConfiguration>("testConfigurations.json")
            .map { arrayOf(it.width, it.height, it.maxColours, it.maxRounds, it.seed) }

        /**
         * Read a list of random games, including moves and board statuses
         */
        @JvmStatic
        fun testGames() = readGameInfo("testRandomGames.json.gz")

        /**
         * Games that do not have missing colours.
         */
        @JvmStatic
        fun colourTestGames() = readGameInfo("testColourGames.json.gz")

        /**
         * This gets a list of games from the random games that have been won, it sets the max turn
         * count to the actual amount turns used
         */
        @JvmStatic
        fun testMaxGames() = readGameInfo("testRandomGames.json.gz").filter {
            it.state == State.WON
        }.map {
            it.copy(maxTurns = it.moves.size)
        }

        /**
         * Get the random games with at least 10 moves.
         */
        @JvmStatic
        fun testTenPlusGames() = readGameInfo("testRandomGames.json.gz").filter {
            it.moves.size > 10
        }
    }
}


/**
 * Listener implementation for testing that records its invocations for checking. It also
 * does some checks on individual events.
 */
class TestListener(private val expectedGame: FlooditGame) :
    FlooditGame.GamePlayListener, FlooditGame.GameOverListener {
    var lastColour: Int = -1
        private set
    var isFinished: Boolean = false
        private set
    var isWon: Boolean? = null
        private set
    var invocationCount = 0
        private set

    var lastRound: Int = -1
        private set

    fun unsetRound() {
        lastRound = -1
    }

    override fun onGameChanged(game: FlooditGame, round: Int) {
        lastColour = game[0, 0]

        assertEquals(expectedGame, game, "The listener should return the actual game")
        ++invocationCount
        if (lastRound >= 0) {
            assertEquals(lastRound + 1, round) {
                "The round should increase by 1 every time. Likely you don't ignore duplicate listener registrations"
            }
        }
        lastRound = round
    }

    override fun onGameOver(game: FlooditGame, rounds: Int, isWon: Boolean) {
        assertEquals(expectedGame, game, "The listener should return the actual game")
        ++invocationCount
        if (lastRound >= 0) {
            assertEquals(lastRound, rounds, "Game over should be invoked after onGameChanged")
        }
        this.isWon = isWon
        val expectedState = if (isWon) State.WON else State.LOST
        assertEquals(expectedState, game.state) {
            "The isWon parameter should equal the game property"
        }

        assertNotEquals(State.RUNNING, game.state) {
            "The game itself should be in won state on game over"
        }

        assertFalse(isFinished, "The game finished state should only be notified once")
        isFinished = true
    }
}

/**
 * This class represents the information needed for a game configuration
 */
@Serializable
data class GameConfiguration(
    val width: Int,
    val height: Int,
    val maxColours: Int,
    val maxRounds: Int,
    val seed: Long
)

/**
 * This class represents a single move in the game, with a colour and the grid that is the
 * consequent expected colour state after the move
 */
@Serializable
data class Move(val colour: Int, val newState: IntMatrix)

/**
 * This class represents an entire game play, including moves and final state (after those moves)
 */
@Serializable
data class GameInfo(
    val colourCount: Int,
    val initialGame: IntMatrix,
    val maxTurns: Int,
    val moves: List<Move>,
    val state: State
) {

    override fun toString(): String {
        val st = moves.last().newState
        val tl = st[0, 0]
        val w = when {
            moves.last().newState.toFlatArray().all { it == tl } ->
                "won"
            else -> "lost"
        }
        return "GameInfo(${initialGame.width}×${initialGame.height} - cnt=${colourCount} moves=${moves.size} - $w)"
    }
}

/**
 * A serializer that helps serializing IntMatrices with kotlinx.serialization.
 */
object IntMatrixSerializer : KSerializer<IntMatrix> {
    private val dataSerializer = IntArraySerializer()
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("IntMatrix") {
            element("width", Int.serializer().descriptor)
            element("height", Int.serializer().descriptor)
            element("data", dataSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): IntMatrix {
        return decoder.decodeStructure(descriptor) {
            var width: Int = -1
            var height: Int = -1
            var data: IntArray = intArrayOf()

            for (elementIdx in elementIndices(descriptor)) {
                when (elementIdx) {
                    0 -> width = decodeIntElement(descriptor, 0)
                    1 -> height = decodeIntElement(descriptor, 0)
                    2 -> data = decodeSerializableElement(descriptor, 2, dataSerializer)
                }
            }

            IntMatrix(width, height) { x, y -> data[x + y * width] }
        }
    }

    override fun serialize(encoder: Encoder, value: IntMatrix) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.width)
            encodeIntElement(descriptor, 1, value.height)
            encodeSerializableElement(descriptor, 2, dataSerializer, value.toFlatArray())
        }
    }

}

/**
 * Helper iterator class helping with the implementation of the [IntMatrixSerializer].
 */
private class CompositeDecoderIndices(
    private val decoder: CompositeDecoder,
    private val descriptor: SerialDescriptor
) : Iterable<Int>, Iterator<Int> {
    private var nextIdx = INVALID_INDEX
    override fun iterator(): Iterator<Int> = this

    override fun hasNext(): Boolean {
        if (nextIdx == CompositeDecoder.DECODE_DONE) return false
        nextIdx = decoder.decodeElementIndex(descriptor)
        return nextIdx != CompositeDecoder.DECODE_DONE
    }

    override fun next(): Int {
        return nextIdx.also {
            nextIdx = INVALID_INDEX
        }
    }

    companion object {
        const val INVALID_INDEX = -555
    }
}

/**
 * Function that defines colour reordering based upon frequencies (so colours are only dependent
 * on order).
 */
private fun Array<FreqCount>.createReorderMap(): IntArray = asSequence()
    .mapIndexed { origIdx, count -> origIdx to count }
    .sortedBy { (_, count) -> count } // Sort by count
    .mapIndexed { idx, origIdx -> origIdx to idx } // Figure out new index
    .sortedBy { (_, idx) -> idx } // Sort by original index
    .map { (_, idx) -> idx } // Get it back into
    .iterator().let { iterator -> IntArray(size) { iterator.next() } }

/**
 * Get an iterable over all element indices in the descriptor
 */
internal fun CompositeDecoder.elementIndices(descriptor: SerialDescriptor): Iterable<Int> =
    CompositeDecoderIndices(this, descriptor)

/**
 * Get an IntMatrix from a game that represents its status
 */
fun FlooditGame.toMatrix(): IntMatrix = IntMatrix(width, height, ::get)

/**
 * Helper class that stores frequency counts, but is also comparable (and can be updated with the
 * update function). The position can only be updated once.
 */
internal class FreqCount(private var count: Int = 0) : Comparable<FreqCount> {
    private var pos: Int = -1

    operator fun component1() = count
    operator fun component2() = pos

    fun update(pos: Int) {
        if (this.pos < 0) this.pos = pos
        ++count
    }

    override operator fun compareTo(other: FreqCount): Int = when (count) {
        other.count -> pos - other.pos
        else -> count - other.count
    }
}