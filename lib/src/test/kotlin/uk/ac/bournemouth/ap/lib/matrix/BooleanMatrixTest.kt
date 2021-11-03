package uk.ac.bournemouth.ap.lib.matrix

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.ac.bournemouth.ap.lib.matrix.boolean.*
import uk.ac.bournemouth.ap.lib.matrix.impl.MatrixCompanion
import uk.ac.bournemouth.ap.lib.matrix.impl.SparseMatrixCompanion
import kotlin.random.Random

/**
 * Common base class for testing all boolean matrices, it provides shared bits (to create values etc).
 * The seedOffset is used to allow different seeds for different subclasses.
 */
internal abstract class BooleanMatrixTestCommon<V : SparseBooleanMatrix>(
    final override val seedOffset: Int
) : TestBase<Boolean, V> {

    override fun Random.nextValue(): Boolean = nextBoolean()

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    override fun Boolean.toInt(): Int = if (this) 1 else -1

    override fun Int.toT(): Boolean = this > 0

}

/**
 * Base class for a sparse boolean matrix
 */
internal abstract class SparseBooleanMatrixTestBase<V : SparseBooleanMatrix> constructor(
    seedOffset: Int,
    companion: SparseMatrixCompanion<Boolean>
) : BooleanMatrixTestCommon<V>(seedOffset), SparseTestBase<Boolean, V> {

    final override val factory: SparseMatrixFactory<Boolean, V> =
        SparseMatrixFactory<Boolean, V>(companion)

    @Test
    override fun testToString() {
        val expected = """|( true, false,  true
            | false,  true, false
            |  true, false,  true
            | false,  true, false)
        """.trimMargin()
        val data = factory(3, 4) { x, y -> value(((x + y * 3) + 1) % 2 == 1) }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

    @Test
    fun testSparseToString() {
        val expected = """|( true, false,      
            | false,  true, false
            |  true,      ,  true
            | false,  true, false)
        """.trimMargin()
        val data = factory(3, 4) { x, y ->
            when ((x + y * 3) % 5) {
                2 -> sparse
                else -> value(((x + y * 3) + 1) % 2 == 1)
            }
        }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }
}

/**
 * Test base class for non-sparse boolean matrices
 */
internal abstract class BooleanMatrixTestBase<V : BooleanMatrix>(
    seedOffset: Int,
    companion: MatrixCompanion<Boolean>
) : BooleanMatrixTestCommon<V>(seedOffset),
    NonsparseTestBase<Boolean, V> {

    final override val factory: NonSparseMatrixFactory<Boolean, V> =
        NonSparseMatrixFactory(companion)

    @Test
    override fun testToString() {
        val expected = """|( true, false,  true
            | false,  true, false
            |  true, false,  true
            | false,  true, false)
        """.trimMargin()
        val data = create(3, 4) { x, y -> ((x + y * 3) + 1) % 2 == 1 }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

}

/** Test class for a read-only/immutable non-sparse boolean matrix. */
internal class BooleanMatrixTest : BooleanMatrixTestBase<BooleanMatrix>(216, BooleanMatrix),
    MatrixTestBase<Boolean, BooleanMatrix>


/** Test class for a mutable non-sparse boolean matrix. It adds some specific tests */
internal class MutableBooleanMatrixTest :
    BooleanMatrixTestBase<MutableBooleanMatrix>(937, MutableBooleanMatrix) {
    @Test
    fun testNewMatrixIndependent() {
        testCopyIndependent { BooleanMatrix(this) }
    }

    @Test
    fun testCreateInitWithBooleanFill() {
        runCreateTest(
            rnd = Random(1351 + super.seedOffset),
            init = { it.nextValue() }
        ) { width, height, init ->
            ArrayMutableBooleanMatrix(width, height).apply { fill(init) }
        }
    }

}

/** Test class for a read-only/immutable sparse boolean matrix. */
internal class SparseBooleanMatrixTest :
    SparseBooleanMatrixTestBase<SparseBooleanMatrix>(6543, SparseBooleanMatrix),
    SparseMatrixTestBase<Boolean, SparseBooleanMatrix>

/** Test class for a mutable sparse boolean matrix. */
internal class SparseMutableBooleanMatrixTest :
    SparseBooleanMatrixTestBase<MutableSparseBooleanMatrix>(435, MutableSparseBooleanMatrix),
    MutableSparseMatrixTestBase<Boolean, MutableSparseBooleanMatrix>