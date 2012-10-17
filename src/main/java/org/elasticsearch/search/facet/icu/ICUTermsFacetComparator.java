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
package org.elasticsearch.search.facet.icu;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import java.util.Comparator;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.search.facet.icu.ICUTermsFacet.Entry;

public class ICUTermsFacetComparator implements Comparator<Entry> {

    private final Collator collator;
    
    private final String type;
    
    private final String locale;

    public ICUTermsFacetComparator(String type, String locale) {
        this(type, new ULocale(locale));
    }

    public ICUTermsFacetComparator(String type, ULocale locale) {
        collator = Collator.getInstance(locale);
        this.type = type;
        this.locale = locale.getName();
    }
    
    public String getType() {
        return type;
    }
    
    public String getLocale() {
        return locale;
    }
    
    public final static Comparator<Entry> COUNT = new Comparator<Entry>() {
        @Override
        public int compare(Entry o1, Entry o2) {
            int i = o2.count() - o1.count();
            if (i == 0) {
                i = o2.compareTo(o1);
                if (i == 0) {
                    i = System.identityHashCode(o2) - System.identityHashCode(o1);
                }
            }
            return i;
        }
    };
    public final static Comparator<Entry> REVERSE_COUNT = new Comparator<Entry>() {
        @Override
        public int compare(Entry o1, Entry o2) {
            return -COUNT.compare(o1, o2);
        }
    };

    public static Comparator<Entry> fromString(String type, String locale) {
        return fromString(type, new ULocale(locale));
    }
    
    public static Comparator<Entry> fromString(String type, ULocale locale) {
        if ("count".equals(type)) {
            return COUNT;
        } else if ("term".equals(type)) {
            return new ICUTermsFacetComparator(type, locale);
        } else if ("reverse_count".equals(type) || "reverseCount".equals(type)) {
            return REVERSE_COUNT;
        } else if ("reverse_term".equals(type) || "reverseTerm".equals(type)) {
            return new ICUTermsFacetReverseComparator(type, locale);
        }
        throw new ElasticSearchIllegalArgumentException("No type argument match for terms facet comparator [" + type + "]");
    }

    @Override
    public int compare(Entry t0, Entry t1) {
        return collator.compare(t0.getTerm(), t1.getTerm());
    }
}
