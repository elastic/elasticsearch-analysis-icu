/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.facet.terms.strings.icu;

import org.elasticsearch.common.collect.ImmutableList;
import com.ibm.icu.util.ULocale;
import org.elasticsearch.common.trove.iterator.TObjectIntIterator;
import org.elasticsearch.common.trove.map.hash.TObjectIntHashMap;
import org.elasticsearch.common.CacheRecycler;
import org.elasticsearch.common.collect.BoundedTreeSet;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.icu.TermsFacet;
import org.elasticsearch.search.facet.icu.InternalTermsFacet;
import org.elasticsearch.search.facet.terms.comparator.icu.AbstractTermsFacetComparator;
import org.elasticsearch.search.facet.terms.comparator.icu.TermsFacetComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class InternalStringTermsFacet extends InternalTermsFacet {

    private static final String STREAM_TYPE = "tTerms";

    public static void registerStream() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static Stream STREAM = new Stream() {
        @Override
        public Facet readFacet(String type, StreamInput in) throws IOException {
            return readTermsFacet(in);
        }
    };

    @Override
    public String streamType() {
        return STREAM_TYPE;
    }

    public static class StringEntry implements Entry {

        private String term;
        private int count;

        public StringEntry(String term, int count) {
            this.term = term;
            this.count = count;
        }

        @Override
        public String term() {
            return term;
        }

        @Override
        public String getTerm() {
            return term;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public int getCount() {
            return count();
        }

        @Override
        public int compareTo(Entry o) {
            int i = term.compareTo(o.term());
            if (i == 0) {
                i = count - o.count();
                if (i == 0) {
                    i = System.identityHashCode(this) - System.identityHashCode(o);
                }
            }
            return i;
        }
    }

    private String name;
    
    int requiredSize;

    long missing;

    long total;

    Collection<StringEntry> entries = ImmutableList.of();

    TermsFacetComparator comparator;

    InternalStringTermsFacet() {
    }

    public InternalStringTermsFacet(String name, TermsFacetComparator comparator, int requiredSize, Collection<StringEntry> entries, long missing, long total) {
        this.name = name;
        this.comparator = comparator;
        this.requiredSize = requiredSize;
        this.entries = entries;
        this.missing = missing;
        this.total = total;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String getType() {
        return type();
    }

    @Override
    public List<StringEntry> entries() {
        if (!(entries instanceof List)) {
            entries = ImmutableList.copyOf(entries);
        }
        return (List<StringEntry>) entries;
    }

    @Override
    public List<StringEntry> getEntries() {
        return entries();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Iterator<Entry> iterator() {
        return (Iterator) entries.iterator();
    }

    @Override
    public long missingCount() {
        return this.missing;
    }

    @Override
    public long getMissingCount() {
        return missingCount();
    }

    @Override
    public long totalCount() {
        return this.total;
    }

    @Override
    public long getTotalCount() {
        return totalCount();
    }

    @Override
    public long otherCount() {
        long other = total;
        for (Entry entry : entries) {
            other -= entry.count();
        }
        return other;
    }

    @Override
    public long getOtherCount() {
        return otherCount();
    }

    public Comparator<TermsFacet.Entry> comparator() {
        return comparator;
    }
    
    @Override
    public Facet reduce(String name, List<Facet> facets) {
        if (facets.size() == 1) {
            return facets.get(0);
        }
        InternalStringTermsFacet first = (InternalStringTermsFacet) facets.get(0);
        TObjectIntHashMap<String> aggregated = CacheRecycler.popObjectIntMap();
        long missing = 0;
        long total = 0;
        for (Facet facet : facets) {
            InternalStringTermsFacet mFacet = (InternalStringTermsFacet) facet;
            missing += mFacet.missingCount();
            total += mFacet.totalCount();
            for (InternalStringTermsFacet.StringEntry entry : mFacet.entries) {
                aggregated.adjustOrPutValue(entry.term(), entry.count(), entry.count());
            }
        }

        BoundedTreeSet<StringEntry> ordered = new BoundedTreeSet<StringEntry>(first.comparator(), first.requiredSize);
        for (TObjectIntIterator<String> it = aggregated.iterator(); it.hasNext(); ) {
            it.advance();
            ordered.add(new StringEntry(it.key(), it.value()));
        }
        first.entries = ordered;
        first.missing = missing;
        first.total = total;

        CacheRecycler.pushObjectIntMap(aggregated);

        return first;
    }

    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString MISSING = new XContentBuilderString("missing");
        static final XContentBuilderString TOTAL = new XContentBuilderString("total");
        static final XContentBuilderString OTHER = new XContentBuilderString("other");
        static final XContentBuilderString TERMS = new XContentBuilderString("terms");
        static final XContentBuilderString TERM = new XContentBuilderString("term");
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(name);
        builder.field(Fields._TYPE, TermsFacet.TYPE);
        builder.field(Fields.MISSING, missing);
        builder.field(Fields.TOTAL, total);
        builder.field(Fields.OTHER, otherCount());
        builder.startArray(Fields.TERMS);
        for (Entry entry : entries) {
            builder.startObject();
            builder.field(Fields.TERM, entry.term());
            builder.field(Fields.COUNT, entry.count());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public static InternalStringTermsFacet readTermsFacet(StreamInput in) throws IOException {
        InternalStringTermsFacet facet = new InternalStringTermsFacet();
        facet.readFrom(in);
        return facet;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        name = in.readUTF();
        String type = in.readUTF();
        boolean reverse = in.readBoolean();
        ULocale locale =  new ULocale(in.readUTF());
        String rules = in.readOptionalUTF();
        int decomp = in.readInt();
        int strength = in.readInt();
        comparator = AbstractTermsFacetComparator.getInstance(type, reverse, locale, rules, decomp, strength);
        requiredSize = in.readVInt();
        missing = in.readVLong();
        total = in.readVLong();

        int size = in.readVInt();
        entries = new ArrayList<StringEntry>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new StringEntry(in.readUTF(), in.readVInt()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(comparator.getType());
        out.writeBoolean(comparator.getReverse());
        out.writeUTF(comparator.getLocale().toString());
        out.writeOptionalUTF(comparator.getRules());
        out.writeInt(comparator.getDecomposition());
        out.writeInt(comparator.getStrength());
        out.writeVInt(requiredSize);
        out.writeVLong(missing);
        out.writeVLong(total);

        out.writeVInt(entries.size());
        for (Entry entry : entries) {
            out.writeUTF(entry.term());
            out.writeVInt(entry.count());
        }
    }
}