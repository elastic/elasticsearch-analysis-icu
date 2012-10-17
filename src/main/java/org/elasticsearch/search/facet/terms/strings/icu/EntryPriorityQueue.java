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

import org.apache.lucene.util.PriorityQueue;
import org.elasticsearch.search.facet.icu.ICUTermsFacet;

import java.util.Comparator;

public class EntryPriorityQueue extends PriorityQueue<ICUTermsFacet.Entry> {

    public static final int LIMIT = 5000;

    private final Comparator<ICUTermsFacet.Entry> comparator;

    public EntryPriorityQueue(int size, Comparator<ICUTermsFacet.Entry> comparator) {
        initialize(size);
        this.comparator = comparator;
    }

    @Override
    protected boolean lessThan(ICUTermsFacet.Entry a, ICUTermsFacet.Entry b) {
        return comparator.compare(a, b) > 0; // reverse, since we reverse again when adding to a list
    }
}
