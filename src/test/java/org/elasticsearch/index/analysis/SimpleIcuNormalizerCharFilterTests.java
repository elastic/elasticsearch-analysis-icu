/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.elasticsearch.index.analysis;

import com.ibm.icu.text.Normalizer2;
import org.apache.lucene.analysis.CharFilter;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.io.StringReader;

/**
 * Test
 */
public class SimpleIcuNormalizerCharFilterTests extends ElasticsearchTestCase {

    @Test
    public void testDefaultSetting() throws Exception {

        Index index = new Index("test");
        Settings settings = ImmutableSettings.settingsBuilder()
            .put("index.analysis.char_filter.myNormalizerChar.type", "icu_normalizer")
            .build();
        AnalysisService analysisService = createAnalysisService(index, settings);
        CharFilterFactory charFilterFactory = analysisService.charFilter("myNormalizerChar");

        String input = "ʰ㌰゙5℃№㈱㌘，バッファーの正規化のテスト．㋐㋑㋒㋓㋔ｶｷｸｹｺｻﾞｼﾞｽﾞｾﾞｿﾞg̈각/각நிเกषिchkʷक्षि";
        Normalizer2 normalizer = Normalizer2.getInstance(null, "nfkc_cf", Normalizer2.Mode.COMPOSE);
        String expectedOutput = normalizer.normalize(input);
        CharFilter inputReader = (CharFilter) charFilterFactory.create(new StringReader(input));
        char[] tempBuff = new char[10];
        StringBuilder output = new StringBuilder();
        while (true) {
            int length = inputReader.read(tempBuff);
            if (length == -1) break;
            output.append(tempBuff, 0, length);
            assertEquals(output.toString(), normalizer.normalize(input.substring(0, inputReader.correctOffset(output.length()))));
        }
        assertEquals(expectedOutput, output.toString());
    }


    @Test
    public void testNameAndModeSetting() throws Exception {

        Index index = new Index("test");
        Settings settings = ImmutableSettings.settingsBuilder()
            .put("index.analysis.char_filter.myNormalizerChar.type", "icu_normalizer")
            .put("index.analysis.char_filter.myNormalizerChar.name", "nfkc")
            .put("index.analysis.char_filter.myNormalizerChar.mode", "decompose")
            .build();
        AnalysisService analysisService = createAnalysisService(index, settings);
        CharFilterFactory charFilterFactory = analysisService.charFilter("myNormalizerChar");

        String input = "ʰ㌰゙5℃№㈱㌘，バッファーの正規化のテスト．㋐㋑㋒㋓㋔ｶｷｸｹｺｻﾞｼﾞｽﾞｾﾞｿﾞg̈각/각நிเกषिchkʷक्षि";
        Normalizer2 normalizer = Normalizer2.getInstance(null, "nfkc", Normalizer2.Mode.DECOMPOSE);
        String expectedOutput = normalizer.normalize(input);
        CharFilter inputReader = (CharFilter) charFilterFactory.create(new StringReader(input));
        char[] tempBuff = new char[10];
        StringBuilder output = new StringBuilder();
        while (true) {
            int length = inputReader.read(tempBuff);
            if (length == -1) break;
            output.append(tempBuff, 0, length);
            assertEquals(output.toString(), normalizer.normalize(input.substring(0, inputReader.correctOffset(output.length()))));
        }
        assertEquals(expectedOutput, output.toString());
    }

    private AnalysisService createAnalysisService(Index index, Settings settings) {
        Injector parentInjector = new ModulesBuilder().add(new SettingsModule(settings), new EnvironmentModule(new Environment(settings)), new IndicesAnalysisModule()).createInjector();
        Injector injector = new ModulesBuilder().add(
            new IndexSettingsModule(index, settings),
            new IndexNameModule(index),
            new AnalysisModule(settings, parentInjector.getInstance(IndicesAnalysisService.class)).addProcessor(new IcuAnalysisBinderProcessor()))
            .createChildInjector(parentInjector);

        return injector.getInstance(AnalysisService.class);
    }
}
