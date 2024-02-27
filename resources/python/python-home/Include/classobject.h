/* Copyright (c) 2018, 2022, Oracle and/or its affiliates.
 * Copyright (C) 1996-2020 Python Software Foundation
 *
 * Licensed under the PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
 */
/* Former class object interface -- now only bound methods are here  */

/* Revealing some structures (not for general use) */

#ifndef Py_LIMITED_API
#ifndef Py_CLASSOBJECT_H
#define Py_CLASSOBJECT_H
#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    PyObject_HEAD
    PyObject *im_func;   /* The callable object implementing the method */
    PyObject *im_self;   /* The instance it is bound to */
    PyObject *im_weakreflist; /* List of weak references */
    vectorcallfunc vectorcall;
} PyMethodObject;

PyAPI_DATA(PyTypeObject) PyMethod_Type;

#define PyMethod_Check(op) Py_IS_TYPE(op, &PyMethod_Type)

PyAPI_FUNC(PyObject *) PyMethod_New(PyObject *, PyObject *);

PyAPI_FUNC(PyObject *) PyMethod_Function(PyObject *);
PyAPI_FUNC(PyObject *) PyMethod_Self(PyObject *);

/* Macros for direct access to these values. Type checks are *not*
   done, so use with care. */
#define PyMethod_GET_FUNCTION(meth) PyMethod_Function((PyObject*)(meth))
#define PyMethod_GET_SELF(meth) PyMethod_Self((PyObject*)(meth))

typedef struct {
    PyObject_HEAD
    PyObject *func;
} PyInstanceMethodObject;

PyAPI_DATA(PyTypeObject) PyInstanceMethod_Type;

#define PyInstanceMethod_Check(op) Py_IS_TYPE(op, &PyInstanceMethod_Type)

PyAPI_FUNC(PyObject *) PyInstanceMethod_New(PyObject *);
PyAPI_FUNC(PyObject *) PyInstanceMethod_Function(PyObject *);

/* Macros for direct access to these values. Type checks are *not*
   done, so use with care. */
#define PyInstanceMethod_GET_FUNCTION(meth) PyInstanceMethod_Function((PyObject*)(meth))

#ifdef __cplusplus
}
#endif
#endif /* !Py_CLASSOBJECT_H */
#endif /* Py_LIMITED_API */
