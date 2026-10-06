package com.buymore;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class SearchService {
    final String queue;
    final RestClient es;
    final String index;
    final RabbitTemplate rabbit;
    final OrderService orders;
    SearchService(@Value("${buymore.elasticsearch-url}") String url,@Value("${buymore.elasticsearch-index:orders}") String index,@Value("${buymore.index-queue:buymore.java.order-index}") String queue,RabbitTemplate rabbit,OrderService orders) {
        var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.index=index;this.queue=queue;
        es=RestClient.builder().baseUrl(url).requestFactory(factory).build();this.rabbit=rabbit;this.orders=orders;
    }
    @Bean Declarables queues() {return new Declarables(QueueBuilder.durable(queue).deadLetterExchange("").deadLetterRoutingKey(queue+".failed").build(),QueueBuilder.durable(queue+".failed").build());}
    public boolean enqueue(long id) {
        try {
            var correlation=new CorrelationData(UUID.randomUUID().toString());
            rabbit.convertAndSend("",queue,Long.toString(id),correlation);
            return correlation.getFuture().get(5,TimeUnit.SECONDS).isAck() && correlation.getReturned()==null;
        }catch(Exception ex){if(ex instanceof InterruptedException) Thread.currentThread().interrupt();return false;}
    }
    @RabbitListener(queues="${buymore.index-queue:buymore.java.order-index}")
    public void index(String id) {
        var document=orders.document(Long.parseLong(id));
        try {es.put().uri("/"+index+"/_doc/{id}?version={version}&version_type=external_gte",id,document.get("version")).body(document).retrieve().toBodilessEntity();}
        catch(HttpClientErrorException.Conflict ignored) { /* A newer order version is already indexed. */ }
    }
    public String syncStatus(long id,long version) {
        try {var result=es.get().uri("/"+index+"/_doc/{id}",id).retrieve().body(Map.class);var source=(Map<?,?>)result.get("_source");return ((Number)source.get("version")).longValue()==version?"in_sync":"pending";}
        catch(HttpClientErrorException.NotFound ex){return "pending";}catch(Exception ex){return "unavailable";}
    }
    void initialize() {
        try {es.put().uri("/"+index).body(Map.of("mappings",Map.of("properties",Map.of(
            "order_date",Map.of("type","date"),"status",Map.of("type","keyword"),"total_amount",Map.of("type","double"),"version",Map.of("type","long"),
            "customer",Map.of("properties",Map.of("name",Map.of("type","text"),"email",Map.of("type","text"))),
            "items",Map.of("properties",Map.of("title",Map.of("type","text"))))))).retrieve().toBodilessEntity();}
        catch(HttpClientErrorException.BadRequest ex){if(!ex.getResponseBodyAsString().contains("resource_already_exists_exception")) throw ex;}
    }
}
