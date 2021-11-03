package uk.ac.bournemouth.ap.lib.matrix

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.ac.bournemouth.ap.lib.matrix.boolean.BooleanMatrix
import uk.ac.bournemouth.ap.lib.matrix.ext.Coordinate
import uk.ac.bournemouth.ap.lib.matrix.ext.isValid
import uk.ac.bournemouth.ap.lib.matrix.impl.MatrixCompanion
import uk.ac.bournemouth.ap.lib.matrix.impl.SparseMatrixCompanion
import kotlin.random.Random

interface TestBase<T, out V : SparseMatrix<T>> {
    val seedOffset: Int

    fun Random.nextValue(): T

    fun T.toInt(): Int

    fun Int.toT(): T

    fun SparseMatrix<*>.safeIsValid(x: Int, y: Int): Boolean

    fun create(
        random: Random,
        width: Int = random.nextInt(5, 15),
        height: Int = random.nextInt(5, 15),
        init: (Int, Int) -> T = { _, _ -> random.nextValue() }
    ): V

    fun Random.nextCoordinate(matrix: SparseMatrix<T>): Coordinate {
        return { Coordinate(nextInt(matrix.maxWidth), nextInt(matrix.maxHeight)) }
            .until {
                matrix.isValid(it)
            }
    }

    fun String.removeToStringPrefix():String {
        val prefixIdx = indexOf('(')
        val padding = "\n"+" ".repeat(prefixIdx-1)
        return substring(prefixIdx).replace(padding, "\n")
    }

    @Test
    fun testIterateIndices() {
        val rnd = Random(112 + seedOffset)
        val matrix = create(rnd)
        val width = matrix.maxWidth
        val height = matrix.maxHeight

        val seen = BooleanArray(width * height)
        for ((x, y) in matrix.indices) {
            val i = x + y * width
            assertFalse(seen[i], "Duplicate value in iterator test not expected")
            seen[i] = true
        }

        for (i in seen.indices) {
            val x = i % width
            val y = i / width
            assertEquals(matrix.isValid(x, y), seen[x + y * width]) {
                "Seen should correspond to validity"
            }
        }
    }

    @Test
    fun testInitCalledOnce() {
        val rnd = Random(seedOffset + 5463)
        val width = rnd.nextInt(5, 15)
        val height = rnd.nextInt(5, 15)
        var initInvokeCount = 0
        val m = create(rnd, width, height) { x, y ->
            initInvokeCount += 1
            rnd.nextValue()
        }
        val cellCount = m.indices.count()
        assertEquals(cellCount, initInvokeCount)
    }

    @Test
    fun testIterate() {
        val rnd = Random(145 + seedOffset)
        val width = rnd.nextInt(5, 15)
        val height = rnd.nextInt(5, 15)

        val expected = mutableMapOf<T, Int>()
        var expectedCount: Int = 0

        @Suppress("UNCHECKED_CAST")
        val matrix =
            create(random = rnd, width = width, height = height) { x, y ->
                (x + y * width).toT().also {
                    expectedCount += 1
                    expected.merge(it, 1) { old, new -> old + new }
                }
            }
        val seen = mutableMapOf<T, Int>()
        for (value in matrix) {
            seen.merge(value, 1) { old, new -> old + new }
        }

        assertEquals(expectedCount, expected.values.sum())
        assertEquals(expected.values.sum(), seen.values.sum())
        for ((k, v) in expected) {
            assertEquals(v, seen[k])
        }
    }

    @Test
    fun testContentEqualsCopyOf() {
        val rnd = Random(346 + seedOffset)
        val matrix = create(rnd)
        assertTrue(matrix.contentEquals(matrix))
        val copy = matrix.copyOf()
        assertTrue(matrix.contentEquals(copy))
        assertTrue(copy.contentEquals(matrix))
    }

    @Test
    fun testGetTooSmallXY() {
        val random = Random(seedOffset + 953)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.get(-1, -1)
            }
        }
    }

    @Test
    fun testGetTooSmallX() {
        val random = Random(seedOffset + 953)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.get(-1, random.nextInt(m.maxHeight))
            }
        }
    }

    @Test
    fun testGetTooLargeX() {
        val random = Random(seedOffset + 954)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.get(m.maxWidth, random.nextInt(m.maxHeight))
            }
        }
    }

    @Test
    fun testGetTooSmallY() {
        val random = Random(seedOffset + 955)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.get(random.nextInt(m.maxWidth), -1)
            }
        }
    }

    @Test
    fun testGetTooLargeY() {
        val random = Random(seedOffset + 956)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.get(random.nextInt(m.maxWidth), m.maxHeight)
            }
        }
    }

    @Test
    fun testGetTooLargeXY() {
        val random = Random(seedOffset + 956)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.get(m.maxWidth, m.maxHeight)
            }
        }
    }

}

interface SparseTestBase<T, V : SparseMatrix<T>> : TestBase<T, V> {
    val factory: SparseMatrixFactory<T, V>

    override fun create(random: Random, width: Int, height: Int, init: (Int, Int) -> T): V {
        return factory(width, height) { x, y ->
            if (random.nextInt(5) == 0) sparse else value(init(x, y))
        }
    }


    override fun SparseMatrix<*>.safeIsValid(x: Int, y: Int): Boolean = isValid(x, y)

    @Test
    fun testContentEqualsInit() {
        val rnd = Random(347 + seedOffset)
        val matrix = create(rnd) { _, _ -> rnd.nextValue() }
        assertTrue(matrix.contentEquals(matrix))
        val copy = factory(matrix.maxWidth, matrix.maxHeight, matrix.validator) { x, y -> matrix[x, y] }
        assertTrue(matrix.contentEquals(copy))
        assertTrue(copy.contentEquals(matrix))
    }

    @Test
    fun testContentNotEquals() {
        val rnd = Random(348 + seedOffset)
        val matrix = create(rnd) { _, _ -> rnd.nextValue() }
        assertTrue(matrix.contentEquals(matrix))
        val unequal = rnd.nextCoordinate(matrix)
        val copy = factory(matrix.maxWidth, matrix.maxHeight, matrix.validator) { x, y ->
            when {
                x == unequal.x && y == unequal.y -> (1 - matrix[x, y].toInt()).toT()
                else -> matrix[x, y]
            }
        }
        assertFalse(matrix.contentEquals(copy))
        assertFalse(copy.contentEquals(matrix))
    }

    @Test
    fun testToString() {
        val expected = """|( 1,  2,  3
            |  4,  5,  6
            |  7,  8,  9
            | 10, 11, 12)
        """.trimMargin()
        val data = factory(3, 4) { x, y -> value(((x + y * 3) + 1).toT()) }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

    @Test
    fun testCreateInit() {
        runCreateSparseTest<T>(
            rnd = Random(7642 + seedOffset),
            init = { r, _, _ -> if (r.nextInt(4) == 0) sparse else value(r.nextValue()) }
        ) { maxWidth, maxHeight, init -> factory(maxWidth, maxHeight, init) }
    }

    @Test
    fun testCreateValue() {
        val rnd = Random(6352 + seedOffset)
        val elemValue = rnd.nextValue()

        runCreateSparseTest<T>(
            rnd = rnd,
            init = { r, _, _ -> if (r.nextInt(5) == 0) sparse else value(elemValue) },
            create = { w, h, init ->
                val context = ArraySparseMatrix.valueCreator<T>()
                val validationData = BooleanMatrix(w, h) { x, y -> context.init(x, y).isValid }
                val validator: (Int, Int) -> Boolean = { x, y -> validationData[x, y] }
                factory(w, h, elemValue, validator)
            }
        )
    }

    @Test
    fun testCreateSparseFunValue() {
        val rnd = Random(6352 + seedOffset)
        val elemValue = rnd.nextValue()
        val validator: (Int, Int) -> Boolean = { x, y -> (x * 61 + y * 31) % 5 == 0 }

        runCreateSparseTest<T>(
            rnd = rnd,
            init = { r, x, y -> if (validator(x, y)) value(elemValue) else sparse },
            create = { w, h, init ->
                factory(w, h, elemValue, validator)
            }
        )
    }

    @Test
    fun testCreateSparseFunInit() {
        val rnd = Random(6352 + seedOffset)

        val validator: (Int, Int) -> Boolean = { x, y -> (x * 61 + y * 31) % 5 != 0 }

        var cnt = 0
        runCreateSparseTest<T>(
            rnd = rnd,
            init = { r, x, y ->
                if (validator(x, y)) value(r.nextValue()) else sparse
            },
            create = { w, h, init ->
                val context = ArraySparseMatrix.valueCreator<T>()
                factory(w, h, validator) { x, y -> context.init(x, y).value }
            }
        )
    }

    @Test
    fun testActuallySparse() {
        val rnd = Random(3124 + seedOffset)
        val matrix = create(rnd)

        assertNotEquals(matrix.maxWidth * matrix.maxHeight, matrix.count())
    }

    @Test
    fun testCopyOf() {
        val rnd = Random(345 + seedOffset)
        val matrix = factory(rnd.nextInt(5, 15), rnd.nextInt(5, 15)) { _, _ ->
            if (rnd.nextInt(4) == 0) sparse else value(rnd.nextValue())
        }
        val copy = matrix.copyOf()
        assertEquals(matrix.maxWidth, copy.maxWidth)
        assertEquals(matrix.maxHeight, copy.maxHeight)
        for ((x, y) in matrix.indices) {
            if (matrix.safeIsValid(x, y)) {
                assertTrue(copy.safeIsValid(x, y))
                assertEquals(
                    matrix[x, y],
                    copy[x, y]
                ) { "The values at coordinate ($x, $y) should be equal." }
            } else {
                assertFalse(copy.safeIsValid(x, y))
            }
        }
    }

    @Test
    fun testGetSparseXY() {
        val random = Random(seedOffset + 953)
        for (i in 0..20) {
            val m = create(random)
            val c = { Coordinate(random.nextInt(m.maxWidth), random.nextInt(m.maxHeight)) }
                .until { !m.isValid(it) }

            assertThrows<IndexOutOfBoundsException> {
                m.get(c)
            }
        }
    }

}

interface NonsparseTestBase<T, V : Matrix<T>> : TestBase<T, V> {
    val factory: NonSparseMatrixFactory<T, V>

    override fun create(random: Random, width: Int, height: Int, init: (Int, Int) -> T): V {
        return factory(width, height, init)
    }

    fun create(width: Int, height: Int, init: (Int, Int) -> T): V =
        factory(width, height, init)

    fun create(width: Int, height: Int, initValue: T): V =
        factory(width, height, initValue)


    override fun SparseMatrix<*>.safeIsValid(x: Int, y: Int): Boolean = true

    @Test
    fun testContentEqualsInit() {
        val rnd = Random(347 + seedOffset)
        val matrix = create(rnd) { _, _ -> rnd.nextValue() }
        assertTrue(matrix.contentEquals(matrix))
        val copy = create(matrix.width, matrix.height) { x, y -> matrix[x, y] }
        assertTrue(matrix.contentEquals(copy))
        assertTrue(copy.contentEquals(matrix))
    }

    @Test
    fun testContentNotEquals() {
        val rnd = Random(348 + seedOffset)
        val matrix = create(rnd) { _, _ -> rnd.nextValue() }
        assertTrue(matrix.contentEquals(matrix))
        val unequal = rnd.nextCoordinate(matrix)
        val copy = factory(matrix.maxWidth, matrix.maxHeight) { x, y ->
            when {
                x == unequal.x && y == unequal.y -> (1 - matrix[x, y].toInt()).toT()
                else -> matrix[x, y]
            }
        }
        assertFalse(matrix.contentEquals(copy))
        assertFalse(copy.contentEquals(matrix))
    }

    @Test
    fun testToString() {
        val expected = """|( 1,  2,  3
            |  4,  5,  6
            |  7,  8,  9
            | 10, 11, 12)
        """.trimMargin()
        val data = create(3, 4) { x, y -> ((x + y * 3) + 1).toT() }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

    @Test
    fun testCreateInit() {
        runCreateTest(rnd = Random(1351 + seedOffset), init = { it.nextValue() }, create = ::create)
    }

    @Test
    fun testCreateValue() {
        val rnd = Random(8792 + seedOffset)
        val elemValue = rnd.nextValue()

        runCreateTest(
            rnd = rnd,
            init = { elemValue },
            create = { w, h, _ -> create(w, h, elemValue) }
        )

    }

    @Test
    fun testCopyOf() {
        val rnd = Random(345 + seedOffset)
        val matrix = create(rnd.nextInt(5, 15), rnd.nextInt(5, 15)) { _, _ -> rnd.nextValue() }
        val copy = matrix.copyOf()
        assertEquals(matrix.maxWidth, copy.maxWidth)
        assertEquals(matrix.maxHeight, copy.maxHeight)
        for ((x, y) in matrix.indices) {
            if (matrix.safeIsValid(x, y)) {
                assertEquals(matrix[x, y], copy[x, y]) {
                    "The values at coordinate ($x, $y) should be equal."
                }
            }
        }
    }


}

interface MutableTestBase<T, out V : MutableSparseMatrix<T>> : TestBase<T, V> {

    @Test
    fun testUpdate() {
        val rnd = Random(seedOffset + 1234)
        val matrix = create(rnd)
        for (i in 0..9) {
            val c = rnd.nextCoordinate(matrix)
            val oldValue = matrix[c]
            val newValue = (-oldValue.toInt()).toT()
            matrix[c] = newValue
            assertNotEquals(oldValue, newValue)
            assertEquals(newValue, matrix[c])
        }
    }

    @Test
    fun testFillValue() {
        val rnd = Random(seedOffset + 3243)
        val matrix = create(rnd)
        val count = matrix.indices.count()

        val newValue = rnd.nextValue()
        matrix.fill(newValue)
        assertEquals(count, matrix.count())

        assertTrue(matrix.all { it == newValue }) {
            "After fill, all values should equal the new value"
        }
    }

    @Test
    fun testFillFun() {
        val rnd = Random(seedOffset + 3243)
        val value = 0.toT()
        val matrix = create(rnd) { _, _ -> value }
        val rndData = Matrix(matrix.maxWidth, matrix.maxHeight) { _, _ -> rnd.nextValue() }
        val count = matrix.indices.count()
        assertTrue(matrix.all { it == value })

        matrix.fill { x, y -> rndData[x, y] }
        assertFalse(matrix.all { it == value })
        assertEquals(count, matrix.count())

        assertTrue(matrix.indices.all { c -> rndData[c] == matrix[c] }) {
            "After fill, all values should equal the new value"
        }
    }

    @Test
    fun testCopyOfIndependent() {
        testCopyIndependent { copyOf() }
    }

    @Test
    fun testSetTooSmallXY() {
        val random = Random(seedOffset + 953)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.set(-1, -1, random.nextValue())
            }
        }
    }

    @Test
    fun testSetTooSmallX() {
        val random = Random(seedOffset + 953)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.set(-1, random.nextInt(m.maxHeight), random.nextValue())
            }
        }
    }

    @Test
    fun testSetTooLargeX() {
        val random = Random(seedOffset + 954)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.set(m.maxWidth, random.nextInt(m.maxHeight), random.nextValue())
            }
        }
    }

    @Test
    fun testSetTooSmallY() {
        val random = Random(seedOffset + 955)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.set(random.nextInt(m.maxWidth), -1, random.nextValue())
            }
        }
    }

    @Test
    fun testSetTooLargeY() {
        val random = Random(seedOffset + 956)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.set(random.nextInt(m.maxWidth), m.maxHeight, random.nextValue())
            }
        }
    }

    @Test
    fun testSetTooLargeXY() {
        val random = Random(seedOffset + 956)
        for (i in 0..20) {
            val m = create(random)
            assertThrows<IndexOutOfBoundsException> {
                m.set(m.maxWidth, m.maxHeight, random.nextValue())
            }
        }
    }

}

interface ImmutableTestBase<T, V : SparseMatrix<T>> : TestBase<T, V> {

}

interface MatrixTestBase<T, V : Matrix<T>> : ImmutableTestBase<T, V>, NonsparseTestBase<T, V> {

}

interface SparseMatrixTestBase<T, V : SparseMatrix<T>> : ImmutableTestBase<T, V>,
    SparseTestBase<T, V> {
}

interface MutableMatrixTestBase<T, V : MutableMatrix<T>> :
    MutableTestBase<T, V>, NonsparseTestBase<T, V>

interface MutableSparseMatrixTestBase<T, V : MutableSparseMatrix<T>> : MutableTestBase<T, V>,
    SparseTestBase<T, V> {

    @Test
    fun testSetSparseXY() {
        val random = Random(seedOffset + 953)
        for (i in 0..20) {
            val m = create(random)
            val c = { Coordinate(random.nextInt(m.maxWidth), random.nextInt(m.maxHeight)) }
                .until { !m.isValid(it) }
            assertThrows<IndexOutOfBoundsException> {
                m.set(c, random.nextValue())
            }
        }
    }

}

internal inline fun <T> runCreateTest(
    rnd: Random,
    init: (Random) -> T,
    create: (Int, Int, (Int, Int) -> T) -> Matrix<T>
) {
    val width = rnd.nextInt(5, 15)
    val height = rnd.nextInt(5, 15)
    val data = Array(width) { Array<Any?>(height) { init(rnd) } }

    @Suppress("UNCHECKED_CAST")
    val matrix = create(width, height) { x, y -> data[x][y] as T }

    for (x in 0 until width) {
        for (y in 0 until height) {
            assertEquals(data[x][y], matrix[x, y]) {
                "($x,$y) should be ${data[x][y]}, not ${matrix[x, y]}"
            }
        }
    }
}


internal typealias SparseCreateFunction<T> = (Int, Int, SparseMatrix.SparseInit<T>.(Int, Int) -> SparseMatrix.SparseValue<T>) -> SparseMatrix<T>

internal inline fun <T> runCreateSparseTest(
    rnd: Random,
    init: SparseMatrix.SparseInit<T>.(Random, Int, Int) -> SparseMatrix.SparseValue<T>,
    create: SparseCreateFunction<T>
) {
    val width = rnd.nextInt(5, 15)
    val height = rnd.nextInt(5, 15)
    val context = ArraySparseMatrix.valueCreator<T>()
    val data = Array(width) { x -> Array(height) { y -> context.init(rnd, x, y) } }


    @Suppress("UNCHECKED_CAST")
    val matrix = create(width, height) { x, y -> data[x][y] }

    for (x in 0 until width) {
        for (y in 0 until height) {
            if (data[x][y].isValid) {
                assertEquals(data[x][y].value, matrix[x, y]) {
                    "($x,$y) should be ${data[x][y].value}, not ${matrix[x, y]}"
                }
            } else {
                assertFalse(matrix.isValid(x, y))
            }
        }
    }

}

internal inline fun <T, V : MutableSparseMatrix<T>> TestBase<T, V>.testCopyIndependent(doCopy: V.() -> SparseMatrix<T>) {
    val rnd = Random(seedOffset + 1245)
    val matrix = create(rnd)
    val context = ArraySparseMatrix.valueCreator<T>()

    val expectedCopy = Matrix(matrix.maxWidth, matrix.maxHeight) { x, y ->
        when {
            matrix.safeIsValid(x, y) -> context.value(matrix[x, y])
            else -> context.sparse
        }
    }

    val copy = matrix.doCopy()

    for (c in copy.indices) {
        when {
            copy.isValid(c) -> assertEquals(expectedCopy[c].value, copy[c])
            else -> assertFalse(copy.isValid(c))
        }
    }

    for (i in 1..((matrix.maxWidth * matrix.maxHeight) / 5)) {
        val c = rnd.nextCoordinate(matrix)
        val oldValue = expectedCopy[c].value
        val newValue = { rnd.nextValue() }.until { it != oldValue }
        assertNotEquals(oldValue, newValue) { "Old and new values should not be equal" }

        matrix[c] = newValue
        assertNotEquals(
            copy[c],
            matrix[c]
        ) { "Matrix should be different for $c:\n$copy\n\n  AND\n\n$matrix" }
        assertEquals(oldValue, copy[c])
    }

    for (c in copy.indices) {
        when {
            copy.isValid(c) -> assertEquals(expectedCopy[c].value, copy[c])
            else -> assertFalse(copy.isValid(c))
        }
    }
}

internal inline fun <T> (() -> T).until(condition: (T) -> Boolean): T {
    var value: T
    do {
        value = this()
    } while (!condition(value))
    return value
}

interface MatrixFactory<T, V : SparseMatrix<T>> {
}

class SparseMatrixFactory<T, V : SparseMatrix<T>>(private val companion: SparseMatrixCompanion<T>) :
    MatrixFactory<T, V> {

    operator fun invoke(
        maxWidth: Int,
        maxHeight: Int,
        validator: (Int, Int) -> Boolean,
        init: (Int, Int) -> T
    ): V {
        @Suppress("UNCHECKED_CAST")
        return companion(maxWidth, maxHeight, validator, init) as V
    }

    operator fun invoke(
        maxWidth: Int,
        maxHeight: Int,
        init: SparseMatrix.SparseInit<T>.(Int, Int) -> SparseMatrix.SparseValue<T>
    ): V {
        @Suppress("UNCHECKED_CAST")
        return companion(maxWidth, maxHeight, init) as V
    }

    operator fun invoke(
        maxWidth: Int,
        maxHeight: Int,
        initValue: T,
        validator: (Int, Int) -> Boolean
    ): V {
        @Suppress("UNCHECKED_CAST")
        return companion(maxWidth, maxHeight, initValue, validator) as V
    }

}

interface NonSparseMatrixFactoryB<T, V : Matrix<T>> : MatrixFactory<T, V> {
    operator fun invoke(width: Int, height: Int, init: (Int, Int) -> T): V
    operator fun invoke(width: Int, height: Int, initValue: T): V
}

class NonSparseMatrixFactory<T, V : Matrix<T>>(private val companion: MatrixCompanion<T>) :
    MatrixFactory<T, V> {
    operator fun invoke(width: Int, height: Int, init: (Int, Int) -> T): V {
        @Suppress("UNCHECKED_CAST")
        return companion(width, height, init) as V
    }

    operator fun invoke(width: Int, height: Int, initValue: T): V {
        @Suppress("UNCHECKED_CAST")
        return companion(width, height, initValue) as V
    }
}
