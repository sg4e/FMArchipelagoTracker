/* MIT License
 *
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
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

#ifndef HPY_COMMON_RUNTIME_ARGPARSE_H
#define HPY_COMMON_RUNTIME_ARGPARSE_H
#ifdef __cplusplus
extern "C" {
#endif

#include "hpy.h"

HPyAPI_HELPER int
HPyArg_Parse(HPyContext *ctx, HPyTracker *ht, const HPy *args,
             size_t nargs, const char *fmt, ...);

HPyAPI_HELPER int
HPyArg_ParseKeywords(HPyContext *ctx, HPyTracker *ht, const HPy *args,
                     size_t nargs, HPy kwnames, const char *fmt,
                     const char *keywords[], ...);

HPyAPI_HELPER int
HPyArg_ParseKeywordsDict(HPyContext *ctx, HPyTracker *ht, const HPy *args,
                         HPy_ssize_t nargs, HPy kw, const char *fmt,
                         const char *keywords[], ...);

#ifdef __cplusplus
}
#endif
#endif /* HPY_COMMON_RUNTIME_ARGPARSE_H */
