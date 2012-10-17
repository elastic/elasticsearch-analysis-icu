package org.elasticsearch.search.facet.icu;

import java.util.List;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.test.integration.AbstractNodesTests;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 */
public class SimpleICUTermsFacetTests extends AbstractNodesTests {

    private Client client;

    @BeforeClass
    public void createNodes() throws Exception {
        Settings settings = ImmutableSettings.settingsBuilder().put("index.number_of_shards", numberOfShards()).put("index.number_of_replicas", 0).build();
        for (int i = 0; i < numberOfNodes(); i++) {
            startNode("node" + i, settings);
        }
        client = getClient();
    }

    protected int numberOfShards() {
        return 1;
    }

    protected int numberOfNodes() {
        return 1;
    }

    protected int numberOfRuns() {
        return 1;
    }

    @AfterClass
    public void closeNodes() {
        client.close();
        closeAllNodes();
    }

    protected Client getClient() {
        return client("node0");
    }

    @Test
    public void testFacet() throws Exception {
        try {
            client.admin().indices().prepareDelete("test").execute().actionGet();
        } catch (Exception e) {
            // ignore
        }
        client.admin().indices().prepareCreate("test").execute().actionGet();
        client.admin().indices().preparePutMapping("test").setType("type1")
                .setSource("{ type1 : { properties : { name : { type : \"string\", store : \"yes\" } } } }")
                .execute().actionGet();

        client.admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

        String [] words = new String[] {
          "Göbel", "Goethe", "Goldmann", "Göthe", "Götz"  
        };
        
        for (String word : words) {
            client.prepareIndex("test", "type1").setSource(jsonBuilder().startObject()
                    .field("name", word)
                    .endObject()).execute().actionGet();
        }

        client.admin().indices().prepareFlush().setRefresh(true).execute().actionGet();

        for (int i = 0; i < numberOfRuns(); i++) {
            SearchResponse searchResponse = client.prepareSearch()
                    .setFacets(XContentFactory.jsonBuilder().startObject()
                            .startObject("facet1")
                            .startObject("icu")
                            .field("locale", "de@collation=phonebook")
                            .field("comparator", "term")
                            .field("field", "name")
                            .endObject()
                            .endObject()
                            .endObject().bytes() )
                    .execute().actionGet();

            logger.info(searchResponse.toString());
            assertThat(searchResponse.hits().totalHits(), equalTo(5l));
            ICUTermsFacet facet = searchResponse.facets().facet("facet1");
            assertThat(facet.name(), equalTo("facet1"));
            List<? extends ICUTermsFacet.Entry> facetResult = facet.entries();
            assertThat(facetResult.size(), equalTo(5));
            assertThat(facetResult.get(0).getTerm(), equalTo("göbel"));
            assertThat(facetResult.get(1).getTerm(), equalTo("goethe"));
            assertThat(facetResult.get(2).getTerm(), equalTo("göthe"));
            assertThat(facetResult.get(3).getTerm(), equalTo("götz"));
            assertThat(facetResult.get(4).getTerm(), equalTo("goldmann"));
        }
    }

}
