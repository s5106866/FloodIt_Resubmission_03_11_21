package org.example.student.floodit

import uk.ac.bournemouth.ap.lib.matrix.int.IntMatrix
import uk.ac.bournemouth.ap.lib.matrix.int.MutableIntMatrix
import uk.ac.bournemouth.ap.floodit.logic.FlooditGame
import uk.ac.bournemouth.ap.floodit.logic.FlooditGame.State
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Concrete example implementation of the assignment.
 *
 * @param colourCount The amount of colours the game is played with.
 * @param grid The colour grid to use
 * @param round The round at this point (this should increase as turns are played).
 * @param maxTurns The amount of turns to win, before the game is lost.
 */
class StudentFlooditGame(/*get() = ("Remove this and create an actual maxTurns property.
    This is a placeholder to make the base code code compile")*/
    override val colourCount: Int, grid: IntMatrix, round: Int = 0,
    override val maxTurns: Int
) :
    FlooditGame {
    /*override val width: Int get() = ("Implement the width property.
    Note that the grid already has a width")*/
    override val width: Int get() = grid.width
    /*override val height: Int get() = ("Implement the height property.
    Note that the grid already has a height")*/
    override val height: Int get() = grid.height

    /**
     * Implement the round with a private set function, it shouldn't be possible to
     * change this without making an actual turn
     */
    override var round: Int = round
        private set
    /*
       As grid is read-only you will probably want to make a mutable copy of the grid and
        initialize it with the grid constructor parameter:
          private val grid = MutableIntMatrix(grid)
     */
    private val grid = MutableIntMatrix(grid)
    /**
     * The current state of the game. Initial state is running, but can become won/lost. Private
     * set as this should be changed externally.
     */
    override var state: State = State.RUNNING
        private set

    /**
     * A set of objects that have registered to be informed when the game state changes
     * (after a play has been made).
     */
    private val gamePlayListeners: MutableSet<FlooditGame.GamePlayListener> = HashSet()

    /**
     * A set of objects that have registered to be informed when the game has been won.
     */
    private val gameOverListeners: MutableSet<FlooditGame.GameOverListener> = HashSet()

    constructor(
        width: Int,
        height: Int,
        colourCount: Int,
        maxRounds: Int = defaultMaxRounds(width, height, colourCount),
        random: Random = Random(Random.nextLong())
    ) : this(
        colourCount,
        generateRandomGrid(width, height, colourCount, random),
        round = 0,
        maxTurns = maxRounds
    )


    override fun playColour(clr: Int) {
        //Update the round counter
        round += 1
        //("Notify listeners of the update")
        notifyMove(round)

        val playedColour : Int = clr
        val currentColour = get(0,0)

        /*calls the floodFill function starting with (0,0) to change it and same coloured neighbours
        to to played colour*/
        floodFill(0,0,currentColour,playedColour)

        //set variable that assumes the game is won until proven otherwise
        var isWon =true
        //gets the colour at point (0,0)
        val sourceColour : Int = get(0,0)
        //Loop through every node in 2d array checking if it is the same as point (0,0)
        for (x in 0 until width)
            for (y in 0 until height)
                //If any point does not have the same value then no longer assumes game is won
                if (get(x,y) != sourceColour) isWon=false

        //If "isWon" variable is not changed then game is won
        when {isWon -> { notifyWin(round)
            state=State.WON}
            //rounds are equal or less than max turns then game is lost
            round > maxTurns -> {
                //("notify listeners")
                this.notifyLoss(round)
                state=State.LOST
                return
            }
            else -> {
                gameOverListeners
            }
        }

        /*
          Please note that the most straightforward way of implementing flood fill is with a
          recursive algorithm. For this case you need to create an actual other function that does
          the flood fill given a specific start position (which doesn't continue if the cell has the
          colour of the flood).

         */

    }
    //declares function for floodFill process
    private fun floodFill(x:Int, y:Int, oldColour:Int, newColour:Int) {
        /*if old colour does not match new colour then exits loop otherwise will then set
        current node to new colour value*/
        if (get(x,y) != oldColour){
            grid[x,y] = newColour
            //recursively calls this function if any neighbor is same colour as one played
            if (x > 0 && x < (width-1)){
                floodFill(x + 1, y, oldColour, newColour) }// Floods right cell
            if (x > 0 && x < (width+1)){
                floodFill(x - 1, y, oldColour, newColour)} // Floods left cell
            if (y > 0 && y<(height-1)){
                floodFill(x, y + 1, oldColour, newColour) }// Floods lower cell
            if (y > 0 && y<(height+1)){
                floodFill(x, y - 1, oldColour, newColour) }// Floods upper cell
            }
    }

    override operator fun get(x: Int, y: Int): Int {
        //("Create a way to access the cells (by looking them up in the data property?)")
        return grid[x,y]
    }

    override fun notifyMove(round: Int) {
        gamePlayListeners.forEach { it.onGameChanged(this, round) }
    }

    override fun notifyWin(round: Int) {
        gameOverListeners.forEach { it.onGameOver(this, round, true) }
    }

    private fun notifyLoss(round: Int) {
        gameOverListeners.forEach { it.onGameOver(this, round, false) }
    }

    /**
     * Add the given listener to the set of listeners that want to listen to game updates.
     * @param listener The listener to add (if it is not there yet).
     */
    override fun addGamePlayListener(listener: FlooditGame.GamePlayListener) {
        if (listener !in gamePlayListeners) {
            gamePlayListeners.add(listener)
        }
    }

    /**
     * Remove the given listener from the game play listener set.
     * @param listener Listener to remove
     */
    override fun removeGamePlayListener(listener: FlooditGame.GamePlayListener) {
        gamePlayListeners.remove(listener)
    }

    /**
     * Add the given listener to the set of listeners that want to listen to game wins.
     * @param gameOverListener The listener to add (if it is not there yet).
     */
    override fun addGameOverListener(gameOverListener: FlooditGame.GameOverListener) {
        if (gameOverListener !in gameOverListeners) {
            gameOverListeners.add(gameOverListener)
        }
    }

    /**
     * Remove the given listener from the game win listener set.
     * @param gameOverListener Listener to remove
     */
    override fun removeGameOverListener(gameOverListener: FlooditGame.GameOverListener) {
        gameOverListeners.remove(gameOverListener)
    }

    companion object {

        fun defaultMaxRounds(width: Int, height: Int, colourCount: Int): Int {
            return sqrt(width * height * ((height + width) * 0.1) * colourCount).roundToInt()
        }

        @JvmStatic
        fun generateRandomGrid(width: Int, height: Int, colourCount: Int, random: Random):
                IntMatrix {
            /*  Creates a matrix with the given width and height and fill it with colours. */
            var row: Int = height
            var col: Int = width
            //Makes sure Width and Height ar positive integers otherwise will default to size 12
            if (width < 0) col = 12
            if (height < 0) row = 12
            val tempMatrix = MutableIntMatrix(width, height)
            /*iterates through the 2d array for height and width provided assigning a random number
            between 0 and "colourCount" provided as it goes*/

            for(x in 0 until col) {
                for (y in 0 until row) {
                    tempMatrix[x, y] = random.nextInt(colourCount)
                }
            }
            //Return the matrix
            return tempMatrix

        }

    }
}




