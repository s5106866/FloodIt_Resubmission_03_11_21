package uk.ac.bournemouth.ap.lib.matrix

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.ac.bournemouth.ap.lib.matrix.impl.MatrixCompanion
import uk.ac.bournemouth.ap.lib.matrix.impl.SparseMatrixCompanion
import kotlin.random.Random

/**
 * Common base class for testing all object matrices, it provides shared bits (to create values etc).
 * The seedOffset is used to allow different seeds for different subclasses.
 */
internal abstract class StringMatrixTestCommon<V : SparseMatrix<String>>(
    final override val seedOffset: Int
) : TestBase<String, V> {

    override fun Random.nextValue(): String = nextInt(100).toString()

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    override fun String.toInt(): Int = strToInt(this)

    override fun Int.toT(): String = toString()

}

private fun strToInt(str: String) = str.toInt()

/**
 * Base class for a sparse object matrix
 */
internal abstract class SparseStringMatrixTestBase<V : SparseMatrix<String>>(
    seedOffset: Int,
    companion: SparseMatrixCompanion<*>
) : StringMatrixTestCommon<V>(seedOffset), SparseTestBase<String, V> {

    @Suppress("UNCHECKED_CAST")
    final override val factory: SparseMatrixFactory<String, V> =
        SparseMatrixFactory(companion as SparseMatrixCompanion<String>)


    @Test
    override fun testToString() {
        val expected = """|( 34216,  95378,  48724
            |  64331,  64323, -22137
            |  13235,  -4009,  58969
            |   1622, -84107,  11586)
        """.trimMargin()
        val rnd = Random(8432+seedOffset)
        val data = factory(3, 4) { x, y -> value(rnd.nextInt(-99999,100000).toString()) }
        Assertions.assertEquals(expected, data.toString().removeToStringPrefix())
    }

    @Test
    fun testSparseToString() {
        val expected = """|( 71888,  31624,       
            |  43593,  96815, -62218
            |  26131, -88282, -97962
            | -47955,       ,       )
        """.trimMargin()
        val rnd = Random(21049+seedOffset)
        val data = factory(3, 4) { x, y ->
            when (rnd.nextInt(5)) {
                2 -> sparse
                else -> value(rnd.nextInt(-99999,100000).toString())
            }
        }
        Assertions.assertEquals(expected, data.toString().removeToStringPrefix())
    }

}

/**
 * Test base class for non-sparse object matrices
 */
internal abstract class StringMatrixTestBase<V : Matrix<String>>(
    seedOffset: Int,
    companion: MatrixCompanion<*>
) : StringMatrixTestCommon<V>(seedOffset),
    NonsparseTestBase<String, V> {

    @Suppress("UNCHECKED_CAST")
    final override val factory: NonSparseMatrixFactory<String, V> =
        NonSparseMatrixFactory(companion as MatrixCompanion<String>)

    @Test
    override fun testToString() {
        val expected = """|(-79887,  31413, -84600
            |  35390,  89411,  96483
            | -28831,  82975, -62568
            |  84910,   1664,   5110)
        """.trimMargin()
        val rnd = Random(15462+seedOffset)
        val data = factory(3, 4) { x, y -> rnd.nextInt(-99999,100000).toString() }
        Assertions.assertEquals(expected, data.toString().removeToStringPrefix())
    }

}

/** Test class for a read-only/immutable non-sparse object matrix. */
internal class StringMatrixTest : StringMatrixTestBase<Matrix<String>>(0, Matrix),
    MatrixTestBase<String, Matrix<String>>

/** Test class for a mutable non-sparse object matrix. */
internal class MutableStringMatrixTest :
    StringMatrixTestBase<MutableMatrix<String>>(0, MutableMatrix),
    MutableMatrixTestBase<String, MutableMatrix<String>>

/** Test class for a read-only/immutable sparse object matrix. */
internal class SparseStringMatrixTest : SparseStringMatrixTestBase<SparseMatrix<String>>(0, SparseMatrix),
    SparseMatrixTestBase<String, SparseMatrix<String>>

/** Test class for a mutable sparse object matrix. */
internal class SparseMutableStringMatrixTest :
    SparseStringMatrixTestBase<MutableSparseMatrix<String>>(0, MutableSparseMatrix),
    MutableSparseMatrixTestBase<String, MutableSparseMatrix<String>>
