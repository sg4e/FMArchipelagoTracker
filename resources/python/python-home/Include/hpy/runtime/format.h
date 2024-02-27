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

/**
 * String formatting helpers. These functions are not part of HPy ABI. The implementation will be linked into HPy extensions.
 */
#ifndef HPY_COMMON_RUNTIME_FORMAT_H
#define HPY_COMMON_RUNTIME_FORMAT_H

#include "hpy.h"

HPyAPI_HELPER HPy
HPyUnicode_FromFormat(HPyContext *ctx, const char *fmt, ...);

HPyAPI_HELPER HPy
HPyUnicode_FromFormatV(HPyContext *ctx, const char *format, va_list vargs);

HPyAPI_HELPER HPy
HPyErr_Format(HPyContext *ctx, HPy h_type, const char *fmt, ...);

#endif /* HPY_COMMON_RUNTIME_FORMAT_H */
