# Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or
# data (collectively the "Software"), free of charge and under any and all
# copyright rights in the Software, and any and all patent rights owned or
# freely licensable by each licensor hereunder covering either (i) the
# unmodified Software as contributed to or provided by such licensor, or (ii)
# the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

def __initialize__(owner_globals):
    delegate_attrs = ['Struct', 'StructError', '_clearcache', 'calcsize', 'error', 'iter_unpack', 'pack',
                          'pack_into', 'unpack', 'unpack_from']
    res = True
    for x in delegate_attrs:
        if x not in owner_globals:
            res = False
            break

    if not res:
        __graalpython__.import_current_as_named_module_with_delegate(
            module_name="_struct",
            delegate_name="_cpython_struct",
            delegate_attributes=delegate_attrs,
            on_import_error={
                # Just enough to make zipfile and six import successfully, but raise the
                # original import error when someone tries to use the Struct object
                "__all__": ["calcsize", "Struct"],
                "_clearcache": lambda: None,
                "__doc__": "Fake _struct",
                "calcsize": lambda _: 8,
                "Struct": type("Struct", (), {
                    "__init__": lambda self, _: None,
                    "__getattr__": lambda self, attr: (lambda *args: __import__("_cpython_struct"))
                })
            },
            owner_globals=owner_globals)

__initialize__(globals())

del __initialize__
