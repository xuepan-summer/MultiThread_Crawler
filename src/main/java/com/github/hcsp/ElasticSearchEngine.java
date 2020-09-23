package com.github.hcsp;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ElasticSearchEngine {
    public static void main(String[] args) throws IOException {
        while (true) {
            System.out.println("Please input the keyword:");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String keyWord = bufferedReader.readLine();

            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
                SearchRequest searchRequest = new SearchRequest("news");
                searchRequest.source(new SearchSourceBuilder().query(new MultiMatchQueryBuilder(keyWord, "title", "content")));
                SearchResponse result = client.search(searchRequest, RequestOptions.DEFAULT);
                result.getHits().forEach(hit -> System.out.println(hit.getSourceAsMap()));
            }
        }
    }
}
