/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.facet.icu;

import com.ibm.icu.util.ULocale;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.field.data.FieldDataType;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetCollector;
import org.elasticsearch.search.facet.FacetProcessor;
import org.elasticsearch.search.facet.terms.strings.icu.FieldsTermsStringFacetCollector;
import org.elasticsearch.search.facet.terms.strings.icu.ScriptTermsStringFieldFacetCollector;
import org.elasticsearch.search.facet.terms.strings.icu.TermsStringFacetCollector;
import org.elasticsearch.search.facet.terms.strings.icu.TermsStringOrdinalsFacetCollector;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.facet.icu.ICUTermsFacet.Entry;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 */
public class ICUTermsFacetProcessor extends AbstractComponent implements FacetProcessor {

    @Inject
    public ICUTermsFacetProcessor(Settings settings) {
        super(settings);
        ICUInternalTermsFacet.registerStreams();
    }

    @Override
    public String[] types() {
        return new String[]{ICUTermsFacet.TYPE};
    }

    @Override
    public FacetCollector parse(String facetName, XContentParser parser, SearchContext context) throws IOException {
        String field = null;
        int size = 10;

        String[] fieldsNames = null;
        ImmutableSet<String> excluded = ImmutableSet.of();
        String regex = null;
        String regexFlags = null;
        ULocale locale = ULocale.getDefault();
        Comparator<Entry> comparator = ICUTermsFacetComparator.COUNT;
        String scriptLang = null;
        String script = null;
        Map<String, Object> params = null;
        boolean allTerms = false;
        String executionHint = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if ("params".equals(currentFieldName)) {
                    params = parser.map();
                }
            } else if (token == XContentParser.Token.START_ARRAY) {
                if ("exclude".equals(currentFieldName)) {
                    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        builder.add(parser.text());
                    }
                    excluded = builder.build();
                } else if ("fields".equals(currentFieldName)) {
                    List<String> fields = Lists.newArrayListWithCapacity(4);
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        fields.add(parser.text());
                    }
                    fieldsNames = fields.toArray(new String[fields.size()]);
                }
            } else if (token.isValue()) {
                if ("field".equals(currentFieldName)) {
                    field = parser.text();
                } else if ("script_field".equals(currentFieldName)) {
                    script = parser.text();
                } else if ("size".equals(currentFieldName)) {
                    size = parser.intValue();
                } else if ("all_terms".equals(currentFieldName) || "allTerms".equals(currentFieldName)) {
                    allTerms = parser.booleanValue();
                } else if ("regex".equals(currentFieldName)) {
                    regex = parser.text();
                } else if ("regex_flags".equals(currentFieldName) || "regexFlags".equals(currentFieldName)) {
                    regexFlags = parser.text();
                } else if ("order".equals(currentFieldName) || "comparator".equals(currentFieldName)) {
                    comparator = ICUTermsFacetComparator.fromString(parser.text(), locale);
                    logger.debug("comparator type = " + parser.text() + " locale = " + locale.getName());
                } else if ("script".equals(currentFieldName)) {
                    script = parser.text();
                } else if ("lang".equals(currentFieldName)) {
                    scriptLang = parser.text();
                } else if ("execution_hint".equals(currentFieldName) || "executionHint".equals(currentFieldName)) {
                    executionHint = parser.textOrNull();
                } else if ("locale".equals(currentFieldName)) {
                    locale = new ULocale(parser.text());
                }
            }
        }

        Pattern pattern = null;
        if (regex != null) {
            pattern = Regex.compile(regex, regexFlags);
        }
        if (fieldsNames != null) {
            return new FieldsTermsStringFacetCollector(facetName, fieldsNames, size, locale.getName(), comparator, allTerms, context, excluded, pattern, scriptLang, script, params);
        }
        if (field == null && fieldsNames == null && script != null) {
            return new ScriptTermsStringFieldFacetCollector(facetName, size, locale.getName(), comparator, context, excluded, pattern, scriptLang, script, params);
        }

        FieldMapper fieldMapper = context.smartNameFieldMapper(field);
        if (fieldMapper != null) {
            if (fieldMapper.fieldDataType() == FieldDataType.DefaultTypes.STRING) {
                if (script == null && !"map".equals(executionHint)) {
                    return new TermsStringOrdinalsFacetCollector(facetName, field, size, locale.getName(), comparator, allTerms, context, excluded, pattern);
                }
            }
        }
        return new TermsStringFacetCollector(facetName, field, size, locale.getName(), comparator, allTerms, context, excluded, pattern, scriptLang, script, params);
    }

    @Override
    public Facet reduce(String name, List<Facet> facets) {
        ICUInternalTermsFacet first = (ICUInternalTermsFacet) facets.get(0);
        return first.reduce(name, facets);
    }
}
