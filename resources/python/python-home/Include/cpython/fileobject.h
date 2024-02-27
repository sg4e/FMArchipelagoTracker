/* Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 * Copyright (C) 1996-2020 Python Software Foundation
 *
 * Licensed under the PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
 */
#ifndef Py_CPYTHON_FILEOBJECT_H
#  error "this header file must not be included directly"
#endif

PyAPI_FUNC(char *) Py_UniversalNewlineFgets(char *, int, FILE*, PyObject *);

/* The std printer acts as a preliminary sys.stderr until the new io
   infrastructure is in place. */
PyAPI_FUNC(PyObject *) PyFile_NewStdPrinter(int);
PyAPI_DATA(PyTypeObject) PyStdPrinter_Type;

typedef PyObject * (*Py_OpenCodeHookFunction)(PyObject *, void *);

PyAPI_FUNC(PyObject *) PyFile_OpenCode(const char *utf8path);
PyAPI_FUNC(PyObject *) PyFile_OpenCodeObject(PyObject *path);
PyAPI_FUNC(int) PyFile_SetOpenCodeHook(Py_OpenCodeHookFunction hook, void *userData);

PyAPI_FUNC(int) _PyLong_FileDescriptor_Converter(PyObject *, void *);
