/*
 * The MIT License (MIT)
 * Copyright (c) 2024 sg4e
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package moe.maika.fmaptracker;

public class Version {
    private static final String VERSION_STRING = "v2.2.0";

    private static final int MAJOR, MINOR, PATCH;

    static {
        String[] components = VERSION_STRING.split("\\.");
        MAJOR = Integer.parseInt(getVless(components[0]));
        MINOR = Integer.parseInt(components[1]);
        PATCH = Integer.parseInt(components[2].split("-")[0]);
    }

    public static String getAsString() {
        return VERSION_STRING;
    }

    public static int getMajor() {
        return MAJOR;
    }

    public static int getMinor() {
        return MINOR;
    }

    public static int getPatch() {
        return PATCH;
    }

    public static String getMajorFromString(String version) {
        return getVless(version).split("\\.")[0];
    }

    private static String getVless(String version) {
        return version.startsWith("v") || version.startsWith("V") ?
                version.substring(1) : version;
    }

    public static boolean isCompatible(String apworldVersion) {
        if(apworldVersion == null || apworldVersion.isEmpty())
            return false;
        String vless = getVless(apworldVersion);
        try {
            int apworldMajor = Integer.parseInt(vless.split("\\.")[0]);
            return MAJOR == apworldMajor;
        }
        catch(NumberFormatException ex) {
            return false;
        }
    }
}
