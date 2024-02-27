/* MIT License
 *
 * Copyright (c) 2023, Oracle and/or its affiliates.
 * Copyright (c) 2019 pyhandle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#include <Python.h>
#include "hpy.h"

#ifndef HPY_ABI_CPYTHON
   // for _h2py and _py2h
#  include "handles.h"
#endif

_HPy_HIDDEN HPy
ctx_Compile_s(HPyContext *ctx, const char *utf8_source, const char *utf8_filename, HPy_SourceKind kind)
{
    int start;
    switch (kind)
    {
    case HPy_SourceKind_Expr: start = Py_eval_input; break;
    case HPy_SourceKind_File: start = Py_file_input; break;
    case HPy_SourceKind_Single: start = Py_single_input; break;
    default:
        PyErr_SetString(PyExc_SystemError, "invalid source kind");
        return HPy_NULL;
    }
    return _py2h(Py_CompileString(utf8_source, utf8_filename, start));
}
