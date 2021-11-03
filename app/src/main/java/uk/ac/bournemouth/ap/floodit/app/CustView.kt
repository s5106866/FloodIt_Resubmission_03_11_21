package uk.ac.bournemouth.ap.floodit.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.example.student.floodit.StudentFlooditGame

class CustView: View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    /***var gameActivity = FlooditActivity()***/

    var game: StudentFlooditGame = StudentFlooditGame(10,10,6)
        set(value) {
            field = value
        }
    lateinit var setPaint : Paint

    private val colCount:Int = game.width
    private val rowCount:Int = game.height

    private var mGridPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }
    //Provides the Black outline between squares to split the grid
    private var mOutlinePaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 5F
    }

    //Sets the colour for value 0 to the same as the hex on the button for that value, #0000FF
    private var mPaintValue0: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLUE
    }

    //Same as above for Val 1 for Red #FF0000
    private var mPaintValue1: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.RED
    }

    //Same as above for Val 2 for Green #00FF00
    private var mPaintValue2: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.GREEN
    }

    //Same as above for Val 3 for Gray #888787
    private var mPaintValue3: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.GRAY
    }

    //Same as above for Val 4 for Magenta #FF00FF
    private var mPaintValue4: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.MAGENTA
    }

    //Same as above for Val 5 for Yellow #FFFF00
    private var mPaintValue5: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.YELLOW
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth: Float = width.toFloat()
        val viewHeight: Float = height.toFloat()

        val sizeX: Float = viewWidth / colCount.toFloat()
        val sizeY: Float = viewHeight / rowCount.toFloat()


        // Draws the full game board with a Blue background
        canvas.drawRect(0.toFloat(), 0.toFloat(), viewWidth, viewHeight, mGridPaint)

        /* Loops through every value in the 2d Array gets the value and sets it as the "colourCode"
         variable*/
        var colourCode : Int
        for (col in 0 until colCount) {
            for (row in 0 until rowCount) {
                colourCode = game[col, row]
                /* condensed IF statement and sets the colour of the current grid space as the
                appropriate colour */
                when (colourCode) {
                    0 -> {setPaint = mPaintValue0}
                    1 -> {setPaint = mPaintValue1}
                    2 -> {setPaint = mPaintValue2}
                    3 -> {setPaint = mPaintValue3}
                    4 -> {setPaint = mPaintValue4}
                    5 -> {setPaint = mPaintValue5}
                }
                //
                canvas.drawRect((sizeX*col),(sizeY*row),(sizeX*(col+1)),(sizeY*(row+1)),
                    setPaint)
                canvas.drawRect((sizeX*col),(sizeY*row),(sizeX*(col+1)),(sizeY*(row+1)),
                    mOutlinePaint)
            }
        }
    }
}