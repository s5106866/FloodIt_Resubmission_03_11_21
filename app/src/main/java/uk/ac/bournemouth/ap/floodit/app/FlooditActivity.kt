package uk.ac.bournemouth.ap.floodit.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.example.student.floodit.StudentFlooditGame


class FlooditActivity : AppCompatActivity() {
    private var roundCounter: Int = 0
    private lateinit var counterText : TextView
    private lateinit var button0: Button
    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var button4: Button
    private lateinit var button5: Button
    private lateinit var buttonReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floodit)
        StudentFlooditGame(12,12,6,25)
        var game = StudentFlooditGame

        counterText = findViewById(R.id.roundCounter)
        button0 = findViewById(R.id.button_colour0)
        button1 = findViewById(R.id.button_colour1)
        button2 = findViewById(R.id.button_colour2)
        button3 = findViewById(R.id.button_colour3)
        button4 = findViewById(R.id.button_colour4)
        button5 = findViewById(R.id.button_colour5)
        buttonReset = findViewById(R.id.resetButton)


        //Each time one of the colour buttons is pressed runs a function for round count and colour
        button0.setOnClickListener{
            buttonPressed(0)        }
        button1.setOnClickListener{
            buttonPressed(1)            }
        button2.setOnClickListener{
            buttonPressed(2)        }
        button3.setOnClickListener{
            buttonPressed(3)        }
        button4.setOnClickListener{
            buttonPressed(4)        }
        button5.setOnClickListener{
            buttonPressed(5)        }

        //reset button will now end activity and launch again
        buttonReset.setOnClickListener{
            finish()
            startActivity(intent)        }
    }
    //adds 1 to round counter and plays colour selected
    private fun buttonPressed(clr: Int) {
        roundCounter += 1
        counterText.text = roundCounter.toString()
        if (roundCounter == 25){
            Toast.makeText(this, "Game Over - Maximum rounds Reached",
                Toast.LENGTH_LONG).show()        }
    }
}