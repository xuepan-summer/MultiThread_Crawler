package com.github.hcsp;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchNewsGenerator extends Thread {
    public static void main(String[] args) {
        String resource = "db/myBatis/mybatis-config.xml";
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        List<News> newsFromDatabase = getNewsFromDatabase(sqlSessionFactory);

        //开4个线程
        for (int i = 0; i < 4; i++) {
            new Thread(() -> sendRequestToESAndGetResponse(newsFromDatabase)).start();
        }
    }

    private static void sendRequestToESAndGetResponse(List<News> newsFromDatabase) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
            BulkRequest bulkRequest = new BulkRequest("news");
            //每个线程执行10次请求
            for (int i = 0; i < 10; i++) {
                for (News news : newsFromDatabase) {
                    IndexRequest request = new IndexRequest("news");
                    Map<String, Object> map = new HashMap<>();
                    map.put("url", news.getUrl());
                    map.put("title", news.getTitle());
                    map.put("content", news.getContent().substring(0, 10));
                    map.put("createdAt", news.getCreatedAt());
                    map.put("updatedAt", news.getUpdatedAt());

                    request.source(map, XContentType.JSON);
                    bulkRequest.add(request);
                }
                //批量发送请求，获取响应
                BulkResponse bulkresponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                System.out.println("Current Thread:" + Thread.currentThread().getName() + "finishes" + i + "," + bulkresponse.status().getStatus());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<News> getNewsFromDatabase(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList("com.github.hcsp.MockDataMapper.selectNews");
        }
    }
}
