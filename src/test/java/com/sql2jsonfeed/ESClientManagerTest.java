package com.sql2jsonfeed;

import com.sql2jsonfeed.channel.ChannelManager;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregator;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by catalin on 2015-06-04.
 */
public class ESClientManagerTest {


    private String clusterName = "Olympus152";

    @Test
    public void testGet() throws Exception {
        ESClientManager.get(clusterName);
    }

    @Test
    public void testClose() throws Exception {
        ESClientManager.close(clusterName);
    }

    @Test
    public void testCloseAll() throws Exception {
        ESClientManager.closeAll();
    }

    @Test
    public void testAggregateSearch() throws Exception {
        Client esClient = ESClientManager.get(clusterName);
        // Create search request
        final String maxRef = "maxRef";
        SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(/*index*/ "op");
        searchRequestBuilder.setTypes("order");
        // REPLACE WITH TERM
        searchRequestBuilder.setQuery(QueryBuilders.matchQuery(ChannelManager.FIELD_CHANNEL, "ordersFromDB"));
        searchRequestBuilder.addAggregation(AggregationBuilders.max(maxRef).field(
                "localPlacedDate"));
        searchRequestBuilder.setSize(0);

        SearchResponse searchResponse = null;

        try {
            searchResponse = searchRequestBuilder.execute().actionGet();
        } catch (IndexMissingException ime) {
            System.out.println(ime);
            return; // not created yet
        } catch (RuntimeException re) {
            // TODO remove
            System.err.println(re);
            throw re;
        }

        System.out.println(searchResponse.getHits().totalHits());
        Max maxAgg = searchResponse.getAggregations().get(maxRef);
        // How to cast to a date
        System.out.println(maxAgg.getValue());
        System.out.println((long) maxAgg.getValue());
        Date maxDate = new Date((long) maxAgg.getValue());
        System.out.println(maxDate);
    }
}