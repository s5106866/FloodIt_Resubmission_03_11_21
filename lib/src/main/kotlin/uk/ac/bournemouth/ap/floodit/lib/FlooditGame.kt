package uk.ac.bournemouth.ap.floodit.logic

/**
 * This interface is implemented by classes that represent a *single* game of FloodIt. The game
 * is **stateful**, it exposes its state on a readonly basis and provides a [playColour] function
 * to actually make a move.
 */
interface FlooditGame {
    /** The amount of turns until the game is lost, if not fully one colour. */
    val maxTurns: Int
    /** The amount of columns in the game / the width */
    val width: Int

    /** The amount of rows in the game / the height */
    val height: Int

    /**
     * The amount of different colours in the game. Cannot be below 0
     */
    val colourCount: Int

    /**
     * Implement this function to return the current game round (starting with 1, every flood
     * operation updates the round.
     * @return The current round
     */
    val round: Int

    /**
     * This property determines the current game state, whether it is still active, won or lost.
     */
    val state: State

    /**
     * Get the colour at position (x,y)
     * @param x The column to change
     * @param y The row to change
     * @return The colour at the coordinates.
     */
    operator fun get(x: Int, y: Int): Int

    /** Default iterator to iterate over all cells. */
    operator fun iterator(): Iterator<Int> = iterator {
        for (y in 0 until height) {
            for (x in 0 until width) {
                yield(get(x, y))
            }
        }
    }

    /**
     * Function to use to actually implement a move (in the game the flow point is static (top left).
     * Implement this function to do a flood fill from the location with the given colour.
     * @param clr The colour to fill with.
     */
    fun playColour(clr: Int)

    /**
     * Add the given listener to the set of listeners that want to listen to game updates.
     * @param listener The listener to add (if it is not there yet).
     */
    fun addGamePlayListener(listener: GamePlayListener)

    /**
     * Remove the given listener from the game play listener set.
     * @param listener Listener to remove
     */
    fun removeGamePlayListener(listener: GamePlayListener)

    /**
     * Add the given listener to the set of listeners that want to listen to game wins.
     * @param gameOverListener The listener to add (if it is not there yet).
     */
    fun addGameOverListener(gameOverListener: GameOverListener)

    /**
     * Remove the given listener from the game win listener set.
     * @param gameOverListener Listener to remove
     */
    fun removeGameOverListener(gameOverListener: GameOverListener)

    /**
     * You should implement this function to call the gamePlayListeners with the given round.
     * @param round The round that the game is in.
     */
    fun notifyMove(round: Int)

    /**
     * You should implement this function to call the gameWinListeners with the given round.
     * @param round The round that the game is in / the amount of rounds used.
     */
    fun notifyWin(round: Int)

    /**
     * Enum with the states in which the game can be. Note that the initial state is running.
     */
    enum class State {
        /** The game has not finished yet. */
        RUNNING,
        /** The game has been won. */
        WON,
        /** The game was lost (ran out of turns). */
        LOST
    }

    /**
     * Listener interface for games handling color changes
     */
    fun interface GamePlayListener {
        /**
         * SAM function invoked when the game state has changed.
         * @param game The game that changed.
         * @param round The round the game is in.
         */
        fun onGameChanged(game: FlooditGame, round: Int)
    }

    /**
     * Listener interface for games handling win conditions
     */
    fun interface GameOverListener {
        /**
         * SAM function invoked when the game has been won.
         * @param game The game that changed.
         * @param rounds The rounds taken to win.
         */
        fun onGameOver(game: FlooditGame, rounds: Int, isWon: Boolean)
    }

}