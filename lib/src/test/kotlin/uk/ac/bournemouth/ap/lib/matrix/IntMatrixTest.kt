package uk.ac.bournemouth.ap.lib.matrix

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.ac.bournemouth.ap.lib.matrix.impl.MatrixCompanion
import uk.ac.bournemouth.ap.lib.matrix.impl.SparseMatrixCompanion
import uk.ac.bournemouth.ap.lib.matrix.int.IntMatrix
import uk.ac.bournemouth.ap.lib.matrix.int.MutableIntMatrix
import uk.ac.bournemouth.ap.lib.matrix.int.MutableSparseIntMatrix
import uk.ac.bournemouth.ap.lib.matrix.int.SparseIntMatrix
import kotlin.random.Random

/**
 * Common base class for testing all Int matrices, it provides shared bits (to create values etc).
 * The seedOffset is used to allow different seeds for different subclasses.
 */
internal abstract class IntMatrixTestCommon<V : SparseIntMatrix>(
    final override val seedOffset: Int
) : TestBase<Int, V> {

    override fun Random.nextValue(): Int = nextInt()

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    override fun Int.toInt(): Int = this

    override fun Int.toT(): Int = this

}

/**
 * Base class for a sparse int matrix
 */
internal abstract class SparseIntMatrixTestBase<V : SparseIntMatrix>(
    seedOffset: Int,
    companion: SparseMatrixCompanion<Int>
) : IntMatrixTestCommon<V>(seedOffset), SparseTestBase<Int, V> {
    final override val factory: SparseMatrixFactory<Int, V> =
        SparseMatrixFactory(companion)

    @Test
    override fun testToString() {
        val expected = """|(  2794, -72601,  63241
            | -60345, -18908,  93390
            |  25256,  64424,  93775
            | -35190,  88215,  83637)
        """.trimMargin()
        val rnd = Random(8431+seedOffset)
        val data = factory(3, 4) { x, y -> value(rnd.nextInt(-99999,100000)) }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

    @Test
    fun testSparseToString() {
        val expected = """|(      ,       , -31612
            | -67838,  32296,       
            |  23232, -72473,  45055
            |  -1522,  82564,  73450)
        """.trimMargin()
        val rnd = Random(21048+seedOffset)
        val data = factory(3, 4) { x, y ->
            when (rnd.nextInt(5)) {
                2 -> sparse
                else -> value(rnd.nextInt(-99999,100000))
            }
        }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

}

/**
 * Test base class for non-sparse int matrices
 */
internal abstract class IntMatrixTestBase<V : IntMatrix>(
    seedOffset: Int,
    companion: MatrixCompanion<Int>
) : IntMatrixTestCommon<V>(seedOffset),
    NonsparseTestBase<Int, V> {

    final override val factory: NonSparseMatrixFactory<Int, V> =
        NonSparseMatrixFactory(companion)

    @Test
    override fun testToString() {
        val expected = """|( 16231, -86435, -70206
            |  -2671, -19879,  61075
            | -85092, -58147,  23844
            |  98181, -89011,  76634)
        """.trimMargin()
        val rnd = Random(15461+seedOffset)
        val data = factory(3, 4) { x, y -> rnd.nextInt(-99999,100000) }
        assertEquals(expected, data.toString().removeToStringPrefix())
    }

}

/** Test class for a read-only/immutable non-sparse int matrix. */
internal class IntMatrixTest :
    IntMatrixTestBase<IntMatrix>(0, IntMatrix),
    MatrixTestBase<Int, IntMatrix>

internal class MutableIntMatrixTest : IntMatrixTestBase<MutableIntMatrix>(0, MutableIntMatrix),
    MutableMatrixTestBase<Int, MutableIntMatrix> {

    @Test
    fun testNewMatrixIndependent() {
        testCopyIndependent { IntMatrix(this) }
    }

}

/** Test class for a read-only/immutable sparse int matrix. */
internal class SparseIntMatrixTest : SparseIntMatrixTestBase<SparseIntMatrix>(0, SparseIntMatrix),
    SparseMatrixTestBase<Int, SparseIntMatrix>

/** Test class for a mutable sparse int matrix. */
internal class SparseMutableIntMatrixTest : SparseIntMatrixTestBase<MutableSparseIntMatrix>(0, MutableSparseIntMatrix),
    MutableSparseMatrixTestBase<Int, MutableSparseIntMatrix>
