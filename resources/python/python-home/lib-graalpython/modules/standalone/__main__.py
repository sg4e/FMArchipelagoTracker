# Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

"""
- Create and a skeleton maven polyglot Java - Python application.

- Create and build a skeleton Python launcher to bundle a Python application
into a single-binary executable.

This tool uses GraalVM Native Image to prepare a self-contained binary from a
script, module, and optionally venv.

"""

import abc
import argparse
import os
import shutil
import stat
import subprocess
import sys
import tempfile
import platform
import socket
import urllib
import urllib.request
import tarfile
import zipfile

assert sys.pycache_prefix is None

MVN_COMPILER_SOURCE = "17"
MVN_COMPILER_TARGET = "17"
MVN_JAR_PLUGIN = "3.1.0"
MVN_NATIVE_IMAGE_MVN_PLUGIN = "0.9.25"

MVN_POM_FILE = "pom.xml"

VFS_PREFIX = "vfs"
VFS_HOME = "home"
VFS_HOME_PREFIX = f"{VFS_PREFIX}/{VFS_HOME}"
VFS_VENV_PREFIX = VFS_PREFIX + "/venv"
VFS_PROJ_PREFIX = VFS_PREFIX + "/proj"

POLYGLOT_APP_JAVA_PKG = "package com.mycompany.javapython;"
NATIVE_EXEC_JAVA_PKG = "package standalone;"

VFS_JAVA_FILE = "VirtualFileSystem.java"
VFS_JAVA_FILE_TEMPLATE = f"templates/{VFS_JAVA_FILE}"

JAVA_BINDING_LAUNCHER = "Py2BinLauncher"
JAVA_BINDING_LAUNCHER_FILE = f"{JAVA_BINDING_LAUNCHER}.java"
JAVA_BINDING_LAUNCHER_TEMPLATE_PATH = f"templates/{JAVA_BINDING_LAUNCHER_FILE}"
JAVA_BINDING_POM_TEMPLATE_PATH = "templates/java_bindings_pom.xml"

NATIVE_EXEC_LAUNCHER = JAVA_BINDING_LAUNCHER

POLYGLOT_APP_LAUNCHER_FILE = "Main.java"
POLYGLOT_APP_LAUNCHER_TEMPLATE_PATH = f"templates/{POLYGLOT_APP_LAUNCHER_FILE}"
POLYGLOT_APP_POM_TEMPLATE_PATH = "templates/polyglot_app_pom.xml"

NATIVE_IMAGE_PROXY_CONF_PATH = "shared/native-image-proxy-configuration.json"
NATIVE_IMAGE_RESOURCES_FILE = "native-image-resources.json"
NATIVE_IMAGE_RESOURCES_PATH = f"shared/{NATIVE_IMAGE_RESOURCES_FILE}"

MODULE_NAME = "standalone"
MODULE_INFO_FILE = "module-info.java"
MODULE_INFO_PATH = f"shared/{MODULE_INFO_FILE}"

GRAALVM_URL_BASE = "https://download.oracle.com/graalvm/"

MVN_REPOSITORY = os.getenv("MVN_REPOSITORY")
MVN_PYTHON_ARTEFACT_ID = "python-community"
MVN_PYTHON_COMMUNITY_VERSION = os.getenv("MVN_GRAALPY_VERSION")

FILES_LIST_NAME = "fileslist.txt"
FILES_LIST_PATH = VFS_PREFIX + "/" + FILES_LIST_NAME

CMD_NATIVE_EXECUTABLE = "native"
CMD_JAVA_PYTHON_APP = "polyglot_app"
ATTR_STANDALONE_CMD = "command"

MVN_CODE_PREFIX = "src/main/java"
MVN_RESOURCE_PREFIX = "src/main/resources"

POSIX_LAUNCHER_SCRIPT = r"""#!/usr/bin/env bash

source="${BASH_SOURCE[0]}"
while [ -h "$source" ] ; do
    prev_source="$source"
    source="$(readlink "$source")";
    if [[ "$source" != /* ]]; then
        # if the link was relative, it was relative to where it came from
        dir="$( cd -P "$( dirname "$prev_source" )" && pwd )"
        source="$dir/$source"
    fi
done
location="$( cd -P "$( dirname "$source" )" && pwd )"

if [ -z "$JAVA_HOME" ]; then
    JAVA=java
else
    JAVA="${JAVA_HOME}/bin/java"
fi

for var in "$@"; do
    args="${args}$(printf "\v")${var}"
done

curdir=`pwd`
export GRAAL_PYTHON_ARGS="${args}$(printf "\v")"
mvn -f "${location}/pom.xml" exec:exec -Dexec.executable="${JAVA}" -Dexec.workingdir="${curdir}" -Dexec.args="--module-path %classpath '-Dorg.graalvm.launcher.executablename=$0' --module org.graalvm.py.launcher/com.oracle.graal.python.shell.GraalPythonMain"
"""

WIN32_LAUNCHER_SCRIPT = r"""@echo off
REM Invoke the GraalPy launcher through Maven, passing any arguments passed to
REM this script via GRAAL_PYTHON_ARGS. To avoid having to deal with multiple
REM layers of escaping, we store the arguments into GRAAL_PYTHON_ARGS delimited
REM with vertical tabs.

REM Since BAT files cannot easily generate vertical tabs, we create a helper
REM script to do it for us. We're calling Maven soon anyway, so Java must be
REM available.
set JAVA="%JAVA_HOME%/bin/java"
if not defined JAVA_HOME set JAVA=java
echo class VTabCreator { public static void main(String[] args) { System.out.print('\013'); } } > VTabCreator.java
for /f "delims=" %%i in ('%JAVA% VTabCreator.java') do set VTAB=%%i
del VTabCreator.java

REM Store each argument separated by vtab
set GRAAL_PYTHON_ARGS=
:loop
set GRAAL_PYTHON_ARGS=%GRAAL_PYTHON_ARGS%%VTAB%%~1
shift /1
if not "%~1"=="" goto loop

mvn -f "%~dp0pom.xml" exec:exec -Dexec.executable=java -Dexec.args="--module-path %%classpath -Dorg.graalvm.launcher.executablename=%~0 --module org.graalvm.py.launcher/com.oracle.graal.python.shell.GraalPythonMain"
"""

def get_file(*paths):
    return os.path.join(os.path.dirname(__file__), *paths)

class AbstractStandalone:

    def __init__(self, parsed_args):
        self.parsed_args = parsed_args
        self.graalvm_version = MVN_PYTHON_COMMUNITY_VERSION if MVN_PYTHON_COMMUNITY_VERSION else __graalpython__.get_graalvm_version()

    @abc.abstractmethod
    def create(self):
        pass

    def create_virtual_filesystem_file(self, vfs_file, java_pkg=""):
        lines = open(get_file(VFS_JAVA_FILE_TEMPLATE), 'r').readlines()
        with open(vfs_file, 'w') as f:
            for line in lines:
                if "{java-pkg}" in line:
                    line = line.replace("{java-pkg}", java_pkg)
                if "{vfs-prefix}" in line:
                    line = line.replace("{vfs-prefix}", VFS_PREFIX)
                if "{files-list-name}" in line:
                    line = line.replace("{files-list-name}", FILES_LIST_NAME)
                f.write(line)

    def create_pom_file(self, template, pom):
        lines = open(template, 'r').readlines()
        with open(pom, 'w') as f:
            for line in lines:
                if "{mvn-compiler-source}" in line:
                    line = line.replace("{mvn-compiler-source}", MVN_COMPILER_SOURCE)
                if "{mvn-compiler-target}" in line:
                    line = line.replace("{mvn-compiler-target}", MVN_COMPILER_TARGET)
                if "{mvn-jar-plugin}" in line:
                    line = line.replace("{mvn-jar-plugin}", MVN_JAR_PLUGIN)
                if "{native-image-mvn-plugin}" in line:
                    line = line.replace("{native-image-mvn-plugin}", MVN_NATIVE_IMAGE_MVN_PLUGIN)
                if "{vfs-prefix}" in line:
                    line = line.replace("{vfs-prefix}", VFS_PREFIX)
                if "{vfs-home-prefix}" in line:
                    line = line.replace("{vfs-home-prefix}", VFS_HOME_PREFIX)
                if "{vfs-venv-prefix}" in line:
                    line = line.replace("{vfs-venv-prefix}", VFS_VENV_PREFIX)
                if "{files-list-name}" in line:
                    line = line.replace("{files-list-name}", FILES_LIST_NAME)
                if "{graal-version}" in line:
                    line = line.replace("{graal-version}", self.graalvm_version)
                f.write(line)

    def create_launcher_file(self, template, launcher, java_pkg=""):
        lines = open(template, 'r').readlines()
        with open(launcher, 'w') as f:
            for line in lines:
                if "{java-pkg}" in line:
                    line = line.replace("{java-pkg}", java_pkg)
                if "{vfs-home-prefix}" in line:
                    line = line.replace("{vfs-home-prefix}", VFS_HOME_PREFIX)
                if "{vfs-venv-prefix}" in line:
                    line = line.replace("{vfs-venv-prefix}", VFS_VENV_PREFIX)
                if "{vfs-proj-prefix}" in line:
                    line = line.replace("{vfs-proj-prefix}", VFS_PROJ_PREFIX)
                f.write(line)

    def check_output_directory(self):
        if hasattr(self.parsed_args, "module") and os.path.abspath(self.parsed_args.output_directory).startswith(os.path.abspath(self.parsed_args.module)):
            print(
                "Output directory cannot be placed inside of module folder to run.",
                sep="\n",
            )
            exit(1)

        if os.path.exists(os.path.abspath(self.parsed_args.output_directory)):
            print(
                "Output directory already exists.",
                sep="\n",
            )
            exit(1)

class PolyglotJavaPython(AbstractStandalone):

    def create(self):
        self.check_output_directory()

        target_dir = self.parsed_args.output_directory

        if self.parsed_args.verbose:
            print(f"Creating polyglot java python application in directory {target_dir}")

        # java sources
        shutil.copytree(os.path.join(os.path.dirname(__file__), "app"), os.path.join(target_dir))

        # create launcher scripts to run graalpy
        posix_launcher = os.path.join(target_dir, "graalpy.sh")
        with open(posix_launcher, "w") as f:
            f.write(POSIX_LAUNCHER_SCRIPT)
        os.chmod(posix_launcher, os.stat(posix_launcher).st_mode | stat.S_IEXEC)
        with open(os.path.join(target_dir, "graalpy.cmd"), "w") as f:
            f.write(WIN32_LAUNCHER_SCRIPT)

        virtual_filesystem_java_file = os.path.join(target_dir, MVN_CODE_PREFIX, "com", "mycompany", "javapython", VFS_JAVA_FILE)
        self.create_virtual_filesystem_file(virtual_filesystem_java_file, POLYGLOT_APP_JAVA_PKG)

        launcher_java_file = os.path.join(target_dir, MVN_CODE_PREFIX, "com", "mycompany", "javapython", POLYGLOT_APP_LAUNCHER_FILE)
        self.create_launcher_file(get_file(POLYGLOT_APP_LAUNCHER_TEMPLATE_PATH), launcher_java_file)

        # std lib
        vfs_home = os.path.join(target_dir, MVN_RESOURCE_PREFIX, VFS_PREFIX, VFS_HOME)
        os.makedirs(vfs_home, exist_ok=True)
        shutil.copytree(__graalpython__.capi_home, os.path.join(vfs_home, "lib-graalpython"))
        shutil.copytree(__graalpython__.stdlib_home, os.path.join(vfs_home, "lib-python", "3"))

        # misc
        shutil.copy(get_file(NATIVE_IMAGE_RESOURCES_PATH), target_dir)
        shutil.copy(get_file(NATIVE_IMAGE_PROXY_CONF_PATH), target_dir)
        self.create_pom_file(get_file(POLYGLOT_APP_POM_TEMPLATE_PATH), os.path.join(target_dir, MVN_POM_FILE))

class Standalone(AbstractStandalone):
    def __init__(self, parsed_args):
        super().__init__(parsed_args)

    def create_target_directory(self):
        if self.parsed_args.verbose:
            print(f"Bundling Python resources into {self.target_dir}")

        self.bundle_python_resources(
            os.path.join(self.target_dir, self.mvn_resource_prefix),
            self.parsed_args.module,
            self.parsed_args.venv,
        )

        os.makedirs(os.path.dirname(self.launcher_file), exist_ok=True)
        self.create_launcher_file(get_file(JAVA_BINDING_LAUNCHER_TEMPLATE_PATH), self.launcher_file, NATIVE_EXEC_JAVA_PKG)

        virtual_filesystem_java_file = os.path.join(self.target_dir, self.mvn_code_prefix, MODULE_NAME, VFS_JAVA_FILE)
        self.create_virtual_filesystem_file(virtual_filesystem_java_file, NATIVE_EXEC_JAVA_PKG)

        shutil.copy(get_file(NATIVE_IMAGE_RESOURCES_PATH), os.path.join(self.target_dir, NATIVE_IMAGE_RESOURCES_FILE))
        shutil.copy(get_file(MODULE_INFO_PATH), os.path.join(self.target_dir, MODULE_INFO_FILE))

    def bundle_python_resources(self, target_dir, project, venv=None):
        """
        Copy the Python core, stdlib, venv, and module into one folder.
        """

        os.makedirs(os.path.dirname(target_dir), exist_ok=True)

        lib_source = __graalpython__.capi_home
        self.copy_folder_to_target(
            target_dir,
            lib_source,
            f"{VFS_HOME_PREFIX}/lib-graalpython",
            path_filter=lambda file=None, dir=None: file and file.endswith(".py"),
        )

        lib_source = __graalpython__.stdlib_home
        self.copy_folder_to_target(
            target_dir,
            lib_source,
            f"{VFS_HOME_PREFIX}/lib-python/3",
            path_filter=lambda file=None, dir=None: dir
            and dir in ["idlelib", "ensurepip", "tkinter", "turtledemo"],
        )

        if venv:
            self.copy_folder_to_target(target_dir, venv, VFS_VENV_PREFIX)

        if project and os.path.isdir(project):
            self.copy_folder_to_target(target_dir, project, VFS_PROJ_PREFIX)
        else:
            with tempfile.TemporaryDirectory() as tmpdir:
                name = os.path.join(tmpdir, "__main__.py")
                shutil.copy(project, name)
                self.copy_folder_to_target(target_dir, tmpdir, VFS_PROJ_PREFIX)
                os.unlink(name)

    def copy_folder_to_target(self, resource_root, folder, prefix, path_filter=lambda file=None, dir=None: False):
        """
        Store a folder with Python modules.
        """
        folder = folder.rstrip("/\\")
        for root, dirs, files in os.walk(folder):
            dirs[:] = filter(lambda d: not path_filter(dir=d) and d != "__pycache__", dirs)

            for file in files:
                if path_filter(file=file):
                    continue
                fullname = os.path.join(root, file)
                arcname = os.path.join(prefix, fullname[len(folder) + 1 :])

                resource_parent_path = os.path.dirname(os.path.join(resource_root, arcname))
                os.makedirs(resource_parent_path, exist_ok=True)
                shutil.copy(fullname, os.path.join(resource_root, arcname))

class JavaBinding(Standalone):
    def __init__(self, parsed_args):
        super().__init__(parsed_args)

        self.target_dir = parsed_args.output_directory
        self.mvn_code_prefix = MVN_CODE_PREFIX
        self.mvn_resource_prefix = MVN_RESOURCE_PREFIX
        self.launcher_file = os.path.join(self.target_dir, self.mvn_code_prefix, JAVA_BINDING_LAUNCHER_FILE)

    def create(self):
        self.check_output_directory()

        os.makedirs(self.target_dir, exist_ok=True)
        self.create_target_directory()
        self.create_pom_file(get_file(JAVA_BINDING_POM_TEMPLATE_PATH), os.path.join(self.target_dir, MVN_POM_FILE))

class NativeExecutable(Standalone):

    def __init__(self, parsed_args):
        super().__init__(parsed_args)
        self.target_dir = tempfile.mkdtemp()
        self.modules_path = os.path.join(self.target_dir, "modules")
        self.mvn_code_prefix = ""
        self.mvn_resource_prefix = ""
        self.launcher_file = os.path.join(self.target_dir, MODULE_NAME, JAVA_BINDING_LAUNCHER_FILE)
        self.jdk_version = __graalpython__.get_jdk_version()

    def create(self):
        try:
            ni, jc = self.get_tools()
            self.download_python()
            self.create_target_directory()
            files_list_path = os.path.join(self.target_dir, FILES_LIST_PATH)
            dir_to_list = os.path.join(self.target_dir, VFS_PREFIX)
            __graalpython__.list_files(dir_to_list, files_list_path)
            self.build_binary(ni, jc)
        finally:
            if not self.parsed_args.keep_temp:
                shutil.rmtree(self.target_dir)

    @staticmethod
    def get_executable(file):
        if os.path.isfile(file):
            return file
        exe = f"{file}.exe"
        if os.path.isfile(exe):
            return exe
        exe = f"{file}.cmd"
        if os.path.isfile(exe):
            return exe
        return None

    def get_graalvm_url(self):
        if "." in self.jdk_version:
            major_version = self.jdk_version[:self.jdk_version.index(".")]
        else:
            major_version = self.jdk_version

        system = platform.system()
        sufix = 'tar.gz'
        if system == 'Darwin':
            system = 'macos'
        elif system == 'Linux':
            system = 'linux'
        elif system == 'win32':
            sufix = 'zip'
            system = 'windows'
        else:
            raise RuntimeError("Unknown platform system", system)

        machine = platform.machine()
        if machine == 'x86_64':
            machine = 'x64'
        elif machine == 'arm64' or machine == 'aarch64':
            machine = 'aarch64'
        else:
            raise RuntimeError("Unknown platform machine", machine)

        return f"{GRAALVM_URL_BASE}{major_version}/archive/graalvm-jdk-{self.jdk_version}_{system}-{machine}_bin.{sufix}"

    def get_tools(self):
        if os.getenv("JAVA_HOME"):
            graalvm_home = os.getenv("JAVA_HOME")
        else:
            self.modules_path = os.path.join(self.target_dir, "lib")
            graalvm_url = self.get_graalvm_url()
            os.makedirs(self.modules_path, exist_ok=True)
            graalvm_file = os.path.join(self.modules_path, graalvm_url[graalvm_url.rindex("/") + 1:])
            if self.parsed_args.verbose:
                print(f"downloading {graalvm_url} to {graalvm_file}")
            try:
                urllib.request.urlretrieve(graalvm_url, graalvm_file)
            except (socket.gaierror, urllib.error.URLError) as err:
                raise ConnectionError(f"failed to download from {graalvm_url}: {err}")

            if platform.system() == 'Darwin' or platform.system() == 'Linux':
                with tarfile.open(graalvm_file) as tar_file:
                    first_member = tar_file.next().path
                    tar_file.extractall(self.modules_path)
            else:
                with zipfile.ZipFile(graalvm_file) as zip_file:
                    first_member = zip_file.namelist()[0]
                    zip_file.extractall(self.modules_path)

            graalvm_dir = os.path.join(self.modules_path, first_member[:first_member.index("/")])
            if platform.system() == 'Darwin':
                graalvm_home = os.path.join(graalvm_dir, "Contents", "Home")
            else:
                graalvm_home = graalvm_dir

        ni = NativeExecutable.get_executable(os.path.join(graalvm_home, "bin", "native-image"))
        jc = NativeExecutable.get_executable(os.path.join(graalvm_home, "bin", "javac"))
        if self.parsed_args.verbose:
            print(f"using GRAALVM: {graalvm_home}")
            print(f"  native_image: {ni}")
            print(f"  javac: {jc}")

        if not ni or not os.path.exists(ni):
            if not self.parsed_args.verbose:
                print(f"using GRAALVM: {graalvm_home}")
                print(f"  native_image: {ni}")
                print(f"  javac: {jc}")
            if os.getenv("JAVA_HOME"):
                print("If using JAVA_HOME env variable, please point it to a GraalVM installation with native image and javac")
            else:
                graalvm_url = self.get_graalvm_url()
                print(f"GraalVM downloaded from {graalvm_url} has no native image or javac")
            sys.exit(1)

        return ni, jc

    def download_python(self):
        mvnd = NativeExecutable.get_executable(os.path.join(__graalpython__.home, "libexec", "graalpy-polyglot-get"))
        cmd = [mvnd]

        if MVN_REPOSITORY:
            cmd += ["-r", MVN_REPOSITORY]
        cmd += ["-a", MVN_PYTHON_ARTEFACT_ID]
        cmd += ["-v", self.graalvm_version]
        cmd += ["-o", self.modules_path]
        if self.parsed_args.verbose:
            print(f"downloading graalpython maven artefacts: {' '.join(cmd)}")

        if self.parsed_args.verbose:
            p = subprocess.run(cmd)
        else:
            p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        if p.returncode != 0:
            if not self.parsed_args.verbose:
                print(p.stdout.decode())
                print(p.stderr.decode())
            exit(1)

    def build_binary(self, ni, jc):
        cwd = os.getcwd()
        output = os.path.abspath(self.parsed_args.output)
        os.chdir(self.target_dir)

        try:
            cmd = [jc, "--module-path", self.modules_path, os.path.join(self.target_dir,MODULE_INFO_FILE), os.path.join(self.target_dir, MODULE_NAME, "VirtualFileSystem.java"), self.launcher_file]
            if self.parsed_args.verbose:
                print(f"Compiling code for Python standalone entry point: {' '.join(cmd)}")
            p = subprocess.run(cmd, cwd=self.target_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if p.returncode != 0:
                print(p.stdout.decode())
                print(p.stderr.decode())
                exit(1)

            ni_modules = os.pathsep.join([os.path.join(self.modules_path, f) for f in os.listdir(self.modules_path) if f.endswith(".jar")] + [self.target_dir])
            cmd = [ni, "--module-path", ni_modules] + self.parsed_args.ni_args[:]

            if self.parsed_args.Os:
                cmd +=[
                    "-Dtruffle.TruffleRuntime=com.oracle.truffle.api.impl.DefaultTruffleRuntime",
                    "-Dpolyglot.engine.WarnInterpreterOnly=false",
                ]
                # Remove once GR-48563 is fixed
                if "Oracle GraalVM" in subprocess.check_output([ni, "--version"], text=True):
                    cmd.append("-H:-OptConditionalMoves")
            cmd += [
                "--no-fallback",
                "-H:-CopyLanguageResources",
                f"-H:ResourceConfigurationFiles={self.target_dir}/native-image-resources.json",
                "-o",
                output,
                f"{MODULE_NAME}.{NATIVE_EXEC_LAUNCHER}",
            ]
            if self.parsed_args.verbose:
                print(f"Building Python standalone binary: {' '.join(cmd)}")
            subprocess.check_call(cmd, cwd=self.target_dir)
        finally:
            os.chdir(cwd)

def main(args):
    parser = argparse.ArgumentParser(prog=f"{sys.executable} -m standalone")
    parser.add_argument(
        "--verbose", action="store_true", help="Print actions as they are performed"
    )
    parser.add_argument(
        "--keep-temp", action="store_true", help="Keep temporary files for debugging."
    )

    subparsers = parser.add_subparsers(required=True, dest=ATTR_STANDALONE_CMD)

    parser_bin = subparsers.add_parser(
        CMD_NATIVE_EXECUTABLE, help="Create a standalone binary from the Python code directly."
    )
    parser_bin.add_argument(
        "-m", "--module", help="Python file or module folder to run", required=True
    )
    parser_bin.add_argument("--venv", help="Python venv to bundle")
    parser_bin.add_argument(
        "-o", "--output", help="Output filename for the binary", required=True
    )
    parser_bin.add_argument(
        "-Os", action="store_true", help="Optimize the binary for size, not speed"
    )
    parser_bin.add_argument(
        "-N",
        action="append",
        dest="ni_args",
        help="extra arguments to pass to the GraalVM Native Image build command",
        metavar="<arg>",
        default=[],
    )

    parser_app = subparsers.add_parser(
        CMD_JAVA_PYTHON_APP,
        help="Create a skeleton Java project. This gives the most flexibility, as the project can be used to build both standalone Jar files or native binaries using Maven.",
    )

    parser_app.add_argument(
        "-o",
        "--output-directory",
        help="The directory to write the Java project to.",
        required=True,
    )

    parsed_args = parser.parse_args(args)

    if parsed_args.command == CMD_JAVA_PYTHON_APP:
        standalone = PolyglotJavaPython(parsed_args)
    else :
        standalone = NativeExecutable(parsed_args)

    standalone.create()

if __name__ == "__main__":
    main(sys.argv[1:])
