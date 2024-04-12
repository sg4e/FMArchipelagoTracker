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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import moe.maika.fmaptracker.TrackerController.Duelist;

public class Farm {
    
    private final Duelist duelist;
    private List<Duelist> multipleDuelists;
    private final String duelRank;
    private final int totalProbability;
    private final int missingDrops;
    private final int totalDrops;

    public Farm(Duelist duelist, String duelRank, int totalProbability, int missingDrops, int totalDrops) {
        this.duelist = duelist;
        this.duelRank = duelRank;
        this.totalProbability = totalProbability;
        this.missingDrops = missingDrops;
        this.totalDrops = totalDrops;
        this.multipleDuelists = null;
    }
    
    public Farm(Collection<Duelist> multipleDuelists, String duelRank, int totalProbability, int missingDrops, int totalDrops) {
        this((Duelist) null, duelRank, totalProbability, missingDrops, totalDrops);
        this.multipleDuelists = new ArrayList<>(multipleDuelists);
        this.multipleDuelists.sort((d1, d2) -> d1.id() - d2.id());
    }

    public Duelist duelist() {
        if(duelist != null)
            return duelist;
        else
            return multipleDuelists.getFirst();
    }

    public String duelRank() {
        return duelRank;
    }

    public int totalProbability() {
        return totalProbability;
    }

    public int missingDrops() {
        return missingDrops;
    }

    public int totalDrops() {
        return totalDrops;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(duelist, multipleDuelists, duelRank, totalProbability, missingDrops, totalDrops);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Farm other = (Farm) obj;
        if (this.totalProbability != other.totalProbability) {
            return false;
        }
        if (this.missingDrops != other.missingDrops) {
            return false;
        }
        if (this.totalDrops != other.totalDrops) {
            return false;
        }
        if (!Objects.equals(this.duelRank, other.duelRank)) {
            return false;
        }
        if (!Objects.equals(this.duelist, other.duelist)) {
            return false;
        }
        return Objects.equals(this.multipleDuelists, other.multipleDuelists);
    }
}
