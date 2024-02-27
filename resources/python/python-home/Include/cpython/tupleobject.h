/* Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 * Copyright (C) 1996-2020 Python Software Foundation
 *
 * Licensed under the PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
 */
#ifndef Py_CPYTHON_TUPLEOBJECT_H
#  error "this header file must not be included directly"
#endif

typedef struct {
    PyObject_VAR_HEAD
    /* ob_item contains space for 'ob_size' elements.
       Items must normally not be NULL, except during construction when
       the tuple is not yet visible outside the function that builds it. */
    // Truffle change: PyObject *ob_item[1] doesn't work for us in Sulong
    PyObject **Py_HIDE_IMPL_FIELD(ob_item);
} PyTupleObject;

PyAPI_FUNC(int) _PyTuple_Resize(PyObject **, Py_ssize_t);
PyAPI_FUNC(void) _PyTuple_MaybeUntrack(PyObject *);

/* Macros trading safety for speed */

/* Cast argument to PyTupleObject* type. */
#define _PyTuple_CAST(op) (assert(PyTuple_Check(op)), (PyTupleObject *)(op))

#define PyTuple_GET_SIZE(op)    Py_SIZE(_PyTuple_CAST(op))

#define PyTuple_GET_ITEM(op, i) PyTuple_GetItem(_PyObject_CAST(op), (i))

/* Macro, *only* to be used to fill in brand new tuples */
PyAPI_FUNC(int) _PyTuple_SET_ITEM(PyObject *, Py_ssize_t, PyObject *);
#define PyTuple_SET_ITEM(op, i, v) _PyTuple_SET_ITEM(_PyObject_CAST(op), (i), (v))

PyAPI_FUNC(void) _PyTuple_DebugMallocStats(FILE *out);
