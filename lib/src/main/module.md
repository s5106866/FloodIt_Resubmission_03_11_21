# Package uk.ac.bournemouth.ap.lib.matrix
This package contains general Matrix classes that store 2-dimensional data. It offers classes
in sparse (some coordinates are invalid), non-sparse (all coordinates in the matrix are valid)
versions, either mutable or immutable.

The primary entry points for this module are:
- [Matrix] for read-only matrices
- [MutableMatrix] for updatable matrices
- [SparseMatrix] for read-only sparse matrices
- [MutableSparseMatrix] for updatable sparse matrices

These interfaces have companion objects with factory methods (`operator invoke`) that will
provide instances of appropriate implementation types.

# Package uk.ac.bournemouth.ap.lib.matrix.int
This package specialised Matrix classes that have optimized support for [Int][Integers]

# Package uk.ac.bournemouth.ap.lib.matrix.boolean
This package specialised Matrix classes that have optimized support for [Boolean][Booleans]

# Package uk.ac.bournemouth.ap.lib.matrix.ext
This package contains the additional types that refine the matrix classes. There is no need
to use it, but it can be useful.

# Package uk.ac.bournemouth.ap.floodit.logic
This package contains the interface of the Floodit game for which an implementation needs to be
implemented. The tests use this package to test against.