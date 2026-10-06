package com.buymore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    final SearchService search;
    final OrderService orders;
    SearchController(SearchService search,OrderService orders){this.search=search;this.orders=orders;}
    public record Filters(@Size(max=200) String query, List<@Pattern(regexp="PENDING|PROCESSING|SHIPPED") String> statuses,
        LocalDate date_from,LocalDate date_to,@DecimalMin("0") BigDecimal min_price,@DecimalMin("0") BigDecimal max_price,@Min(1) @Max(1000) Integer page) {}
    @PostMapping("/orders") Map<String,Object> find(@Valid @RequestBody Filters input) {
        if(input.date_from()!=null && input.date_to()!=null && input.date_from().isAfter(input.date_to())) throw new ResponseStatusException(BAD_REQUEST,"Invalid date range.");
        if(input.min_price()!=null && input.max_price()!=null && input.min_price().compareTo(input.max_price())>0) throw new ResponseStatusException(BAD_REQUEST,"Invalid price range.");
        List<Object> filters=new ArrayList<>();
        if(input.statuses()!=null && !input.statuses().isEmpty()) filters.add(Map.of("terms",Map.of("status",input.statuses())));
        Map<String,Object> dates=new HashMap<>(),prices=new HashMap<>();
        if(input.date_from()!=null) dates.put("gte",input.date_from().toString());
        if(input.date_to()!=null) dates.put("lt",input.date_to().plusDays(1).toString());
        if(!dates.isEmpty()) filters.add(Map.of("range",Map.of("order_date",dates)));
        if(input.min_price()!=null) prices.put("gte",input.min_price());
        if(input.max_price()!=null) prices.put("lte",input.max_price());
        if(!prices.isEmpty()) filters.add(Map.of("range",Map.of("total_amount",prices)));
        Object must=input.query()==null || input.query().isBlank() ? Map.of("match_all",Map.of()):Map.of("multi_match",Map.of("query",input.query().trim(),"type","bool_prefix","fields",List.of("customer.name","customer.email","items.title")));
        int page=input.page()==null?1:input.page();
        Map<String,Object> body=Map.of("query",Map.of("bool",Map.of("must",must,"filter",filters)),"from",(page-1)*10,"size",10,"track_total_hits",true,"sort",List.of(Map.of("order_date","desc")),"aggs",Map.of("revenue",Map.of("sum",Map.of("field","total_amount")),"by_status",Map.of("terms",Map.of("field","status"))));
        try {
            Map<?,?> result=search.es.post().uri("/"+search.index+"/_search").body(body).retrieve().body(Map.class);
            Map<?,?> hits=(Map<?,?>)result.get("hits"), aggs=(Map<?,?>)result.get("aggregations");
            List<?> rows=((List<?>)hits.get("hits")).stream().map(x->((Map<?,?>)x).get("_source")).toList();
            Map<String,Object> counts=new HashMap<>();
            for(Object bucket:(List<?>)((Map<?,?>)aggs.get("by_status")).get("buckets")) {Map<?,?> b=(Map<?,?>)bucket; counts.put(b.get("key").toString(),b.get("doc_count"));}
            return Map.of("orders",rows,"total",((Map<?,?>)hits.get("total")).get("value"),"revenue",((Map<?,?>)aggs.get("revenue")).get("value"),"by_status",counts);
        }catch(Exception ex){throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Order search is unavailable. Please try again.");}
    }
    @PostMapping("/reindex") Map<String,Object> reindex() {
        int queued=0,failed=0;
        for(Long id:orders.ids()) {if(search.enqueue(id)) queued++;else failed++;}
        return Map.of("queued",queued,"failed",failed);
    }
}
