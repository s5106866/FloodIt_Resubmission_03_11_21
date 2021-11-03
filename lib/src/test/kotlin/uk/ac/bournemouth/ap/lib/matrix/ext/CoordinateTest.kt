package uk.ac.bournemouth.ap.lib.matrix.ext

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import uk.ac.bournemouth.ap.lib.matrix.ext.Coordinate

/**
 * Test for the Coordinate type, to check that it works correctly.
 */
internal class CoordinateTest {

    @Test
    fun getPackedMax() {
        assertEquals(0x7fff7fff, Coordinate(0x7fff, 0x7fff).packed)
    }

    @Test
    fun getXMax() {
        assertEquals(0x7fff, Coordinate(0x7fff7fff).x)
    }

    @Test
    fun getYMax() {
        assertEquals(0x7fff, Coordinate(0x7fff7fff).y)
    }

    @Test
    fun getPackedMin() {
        assertEquals(0x80008000.toInt(), Coordinate(Short.MIN_VALUE.toInt(), Short.MIN_VALUE.toInt()).packed)
    }

    @Test
    fun getXMin() {
        assertEquals(Short.MIN_VALUE.toInt(), Coordinate(0x80008000.toInt()).x)
    }

    @Test
    fun getYMin() {
        assertEquals(Short.MIN_VALUE.toInt(), Coordinate(0x80008000.toInt()).y)
    }

    @Test
    fun getPackedMinusOne() {
        assertEquals(0xffffffff.toInt(), Coordinate(-1, -1).packed)
    }

    @Test
    fun getXMinusOne() {
        assertEquals(-1, Coordinate(0xffffffff.toInt()).x)
    }

    @Test
    fun getYMinusOne() {
        assertEquals(-1, Coordinate(0xffffffff.toInt()).y)
    }

    @Test
    fun getPackedZero() {
        assertEquals(0x0, Coordinate(0, 0).packed)
    }

    @Test
    fun getXZero() {
        assertEquals(0, Coordinate(0).x)
    }

    @Test
    fun getYZero() {
        assertEquals(0, Coordinate(0).y)
    }

    @Test
    fun getPackedPosPos() {
        assertEquals(0x03120144, Coordinate(786, 324).packed)
    }

    @Test
    fun getXPosPos() {
        assertEquals(786, Coordinate(0x03120144).x)
    }

    @Test
    fun getYPosPos() {
        assertEquals(324, Coordinate(0x03120144).y)
    }

    @Test
    fun getPackedNegNeg() {
        assertEquals(0xddd6fC64.toInt(), Coordinate(-8746, -924).packed)
    }

    @Test
    fun getXNegNeg() {
        assertEquals(-8746, Coordinate(0xddd6fC64.toInt()).x)
    }

    @Test
    fun getYNegNeg() {
        assertEquals(-924, Coordinate(0xddd6fC64.toInt()).y)
    }

    @Test
    fun getPackedNegPos() {
        assertEquals(0xFE9E124E.toInt(), Coordinate(-354, 4686).packed)
    }

    @Test
    fun getXNegPos() {
        assertEquals(-354, Coordinate(0xFE9E124E.toInt()).x)
    }

    @Test
    fun getYNegPos() {
        assertEquals(4686, Coordinate(0xFE9E124E.toInt()).y)
    }

    @Test
    fun getPackedPosNeg() {
        assertEquals(0x0CAEE20F, Coordinate(3246, -7665).packed)
    }

    @Test
    fun getXPosNeg() {
        assertEquals(3246, Coordinate(0x0CAEE20F).x)
    }

    @Test
    fun getYPosNeg() {
        assertEquals(-7665, Coordinate(0x0CAEE20F).y)
    }


}