ICU Analysis for ElasticSearch
==================================

The ICU Analysis plugin integrates Lucene ICU module into elasticsearch, adding ICU relates analysis components.

In order to install the plugin, simply run: `bin/plugin -install elasticsearch/elasticsearch-analysis-icu/1.7.0`.

    ----------------------------------------
    | ICU Analysis Plugin | ElasticSearch  |
    ----------------------------------------
    | master              | 0.19 -> master |
    ----------------------------------------
    | 1.7.0               | 0.19 -> master |
    ----------------------------------------
    | 1.6.0               | 0.19 -> master |
    ----------------------------------------
    | 1.5.0               | 0.19 -> master |
    ----------------------------------------
    | 1.4.0               | 0.19 -> master |
    ----------------------------------------
    | 1.3.0               | 0.19 -> master |
    ----------------------------------------
    | 1.2.0               | 0.19 -> master |
    ----------------------------------------
    | 1.1.0               | 0.18           |
    ----------------------------------------
    | 1.0.0               | 0.18           |
    ----------------------------------------


ICU Normalization
-----------------

Normalizes characters as explained "here":http://userguide.icu-project.org/transforms/normalization. It registers itself by default under @icu_normalizer@ or @icuNormalizer@ using the default settings. Allows for the name parameter to be provided which can include the following values: @nfc@, @nfkc@, and @nfkc_cf@. Here is a sample settings:

    {
        "index" : {
            "analysis" : {
                "analyzer" : {
                    "collation" : {
                        "tokenizer" : "keyword",
                        "filter" : ["icu_normalizer"]
                    }
                }
            }
        }
    }

ICU Folding
-----------

Folding of unicode characters based on @UTR#30@. It registers itself under @icu_folding@ and @icuFolding@ names. Sample setting:

    {
        "index" : {
            "analysis" : {
                "analyzer" : {
                    "collation" : {
                        "tokenizer" : "keyword",
                        "filter" : ["icu_folding"]
                    }
                }
            }
        }
    }

ICU Collation
-------------

Uses collation token filter. Allows to either specify the rules for collation (defined "here":http://www.icu-project.org/userguide/Collate_Customization.html) using the @rules@ parameter (can point to a location or expressed in the settings, location can be relative to config location), or using the @language@ parameter (further specialized by country and variant). By default registers under @icu_collation@ or @icuCollation@ and uses the default locale.

Here is a sample settings:

    {
        "index" : {
            "analysis" : {
                "analyzer" : {
                    "collation" : {
                        "tokenizer" : "keyword",
                        "filter" : ["icu_collation"]
                    }
                }
            }
        }
    }

And here is a sample of custom collation:

    {
        "index" : {
            "analysis" : {
                "analyzer" : {
                    "collation" : {
                        "tokenizer" : "keyword",
                        "filter" : ["myCollator"]
                    }
                },
                "filter" : {
                    "myCollator" : {
                        "type" : "icu_collation",
                        "language" : "en"
                    }
                }
            }
        }
    }

Optional options:
* `strength` - The strength property determines the minimum level of difference considered significant during comparison.
 The default strength for the Collator is `tertiary`, unless specified otherwise by the locale used to create the Collator.
 Possible values: `primary`, `secondary`, `tertiary`, `quaternary` or `identical`.
 See ICU Collation:http://icu-project.org/apiref/icu4j/com/ibm/icu/text/Collator.html documentation for a more detailed
 explanation for the specific values.
* `decomposition` - Possible values: `no` or `canonical`. Defaults to `no`. Setting this decomposition property with
`canonical` allows the Collator to handle un-normalized text properly, producing the same results as if the text were
normalized. If `no` is set, it is the user's responsibility to insure that all text is already in the appropriate form
before a comparison or before getting a CollationKey. Adjusting decomposition mode allows the user to select between
faster and more complete collation behavior. Since a great many of the world's languages do not require text
normalization, most locales set `no` as the default decomposition mode.

Expert options:
* `alternate` - Possible values: `shifted` or `non-ignorable`. Sets the alternate handling for strength `quaternary`
 to be either shifted or non-ignorable. What boils down to ignoring punctuation and whitespace.
* `caseLevel` - Possible values: `true` or `false`. Default is `false`. Whether case level sorting is required. When
 strength is set to `primary` this will ignore accent differences.
* `caseFirst` - Possible values: `lower` or `upper`. Useful to control which case is sorted first when case is not ignored
 for strength `tertiary`.
* `numeric` - Possible values: `true` or `false`. Whether digits are sorted according to numeric representation. For
 example the value `egg-9` is sorted before the value `egg-21`. Defaults to `false`.
* `variableTop` - Single character or contraction. Controls what is variable for `alternate`.
* `hiraganaQuaternaryMode` - Possible values: `true` or `false`. Defaults to `false`. Distinguishing between Katakana
 and Hiragana characters in `quaternary` strength .

ICU Tokenizer
-------------

Breaks text into words according to UAX #29: Unicode Text Segmentation ((http://www.unicode.org/reports/tr29/)).

    {
        "index" : {
            "analysis" : {
                "analyzer" : {
                    "collation" : {
                        "tokenizer" : "icu_tokenizer",
                    }
                }
            }
        }
    }

ICU Facets
----------

The ICU terms facet allows to sort by an ICU locale setting. 

Be aware, the order of the attributes is sensitive, first the attribute "locale" needs to be declared, followed by the attribute "collator". The "collator" may have one of the values "term", "reverse\_term", or (for convenience) "count" and "reverse\_count".

For example, this facet sorts a name field by german phonebook order (DIN 5007-2).

    {
    	"facets" : {
    		"facet1" : {
    			"icu" : {
    				"locale" : "de@collation=phonebook",
    				"collator" : "term",
    				"field" : "name"
    			}
    		}
    	}
    }
	
The result looks like

    {
      "took" : 241,
      "timed_out" : false,
      "_shards" : {
        "total" : 1,
        "successful" : 1,
        "failed" : 0
      },
      "hits" : {
        "total" : 5,
        "max_score" : 1.0,
        "hits" : [...]
      },
          "facets" : {
            "facet1" : {
              "_type" : "icu",
              "missing" : 0,
              "total" : 5,
              "other" : 0,
              "terms" : [ {
                "term" : "göbel",
                "count" : 1
              }, {
                "term" : "goethe",
                "count" : 1
              }, {
                "term" : "göthe",
                    "count" : 1
              }, {
                "term" : "götz",
                "count" : 1
              }, {
                "term" : "goldmann",
                "count" : 1
              } ]
            }
          }
        }
	}


License
-------

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2012 Shay Banon and ElasticSearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
