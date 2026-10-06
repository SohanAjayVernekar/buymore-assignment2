package com.buymore;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    final JdbcTemplate db;
    final SearchService search;
    final OrderService orders;
    AdminDashboardController(JdbcTemplate db,SearchService search,OrderService orders) {this.db=db;this.search=search;this.orders=orders;}
    @GetMapping("/overview") Map<String,Object> overview() {
        var totals=db.queryForMap("SELECT count(*) AS orders,COALESCE(sum(total_amount),0) AS revenue,count(*) FILTER (WHERE status='PENDING') AS pending,count(*) FILTER (WHERE status='PROCESSING') AS processing,count(*) FILTER (WHERE status='SHIPPED') AS shipped FROM orders");
        totals.put("customers",db.queryForObject("SELECT count(*) FROM users WHERE role='CUSTOMER'",Long.class));
        totals.put("messages",db.queryForObject("SELECT count(*) FROM contact_submissions WHERE resolved=false",Long.class));
        var trend=db.queryForList("SELECT days.day::date AS day,COALESCE(sum(o.total_amount),0) AS revenue,count(o.id) AS orders FROM generate_series(CURRENT_DATE-6,CURRENT_DATE,'1 day') AS days(day) LEFT JOIN orders o ON o.order_date>=days.day AND o.order_date<days.day+interval '1 day' GROUP BY days.day ORDER BY days.day");
        return Map.of("totals",totals,"trend",trend,"recent",db.queryForList("SELECT o.id AS order_id,o.status,o.order_date,o.total_amount,u.name,u.email FROM orders o JOIN users u ON u.id=o.user_id ORDER BY o.order_date DESC,o.id DESC LIMIT 5"));
    }
    @GetMapping("/orders") Map<String,Object> queue(@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="1") int page) {
        AccountController.page(page);
        if(!Set.of("PENDING","PROCESSING","SHIPPED").contains(status)) throw new ResponseStatusException(BAD_REQUEST,"Invalid order status.");
        Long count=db.queryForObject("SELECT count(*) FROM orders WHERE status=?",Long.class,status);
        var items=db.queryForList("SELECT o.id AS order_id,o.order_date,o.status,o.total_amount,u.name,u.email FROM orders o JOIN users u ON u.id=o.user_id WHERE o.status=? ORDER BY o.order_date,o.id LIMIT 10 OFFSET ?",status,(page-1)*10);
        return Map.of("items",items,"total",count,"page",page,"total_pages",(count+9)/10);
    }
    @GetMapping("/customers") Map<String,Object> customers(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="1") int page) {
        AccountController.page(page);
        if(query.length()>200) throw new ResponseStatusException(BAD_REQUEST,"Search is too long.");
        String term=query.trim().toLowerCase(Locale.ROOT);
        String filter=" WHERE u.role='CUSTOMER' AND (strpos(lower(u.name),?)>0 OR strpos(lower(u.email),?)>0)";
        Long total=db.queryForObject("SELECT count(*) FROM users u"+filter,Long.class,term,term);
        var rows=db.queryForList("SELECT u.id,u.name,u.email,count(o.id) AS orders,COALESCE(sum(o.total_amount),0) AS spent,max(o.order_date) AS last_order FROM users u LEFT JOIN orders o ON o.user_id=u.id"+filter+" GROUP BY u.id ORDER BY u.id DESC LIMIT 10 OFFSET ?",term,term,(page-1)*10);
        return Map.of("items",rows,"total",total,"page",page,"total_pages",(total+9)/10);
    }
    @GetMapping("/messages") Map<String,Object> messages(@RequestParam(defaultValue="false") boolean resolved,@RequestParam(defaultValue="1") int page) {
        AccountController.page(page);
        Long total=db.queryForObject("SELECT count(*) FROM contact_submissions WHERE resolved=?",Long.class,resolved);
        var items=db.queryForList("SELECT id,name,email,subject,message,created_at,resolved FROM contact_submissions WHERE resolved=? ORDER BY created_at DESC,id DESC LIMIT 10 OFFSET ?",resolved,(page-1)*10);
        return Map.of("items",items,"total",total,"page",page,"total_pages",(total+9)/10);
    }
    public record Resolution(@NotNull Boolean resolved) {}
    @PatchMapping("/messages/{id}") Map<String,Boolean> resolve(@PathVariable long id,@Valid @RequestBody Resolution input) {
        if(db.update("UPDATE contact_submissions SET resolved=? WHERE id=?",input.resolved(),id)==0) throw new ResponseStatusException(NOT_FOUND,"Message not found.");
        return Map.of("ok",true);
    }
    @GetMapping("/sync") Map<String,Object> sync() {
        var rows=db.queryForList("SELECT id AS order_id,version,status,updated_at FROM orders ORDER BY updated_at DESC,id DESC LIMIT 50");
        List<Map<String,Object>> checked=new ArrayList<>();
        String state="available";
        try {
            var ids=rows.stream().map(r->r.get("order_id").toString()).toList();
            Map<?,?> result=ids.isEmpty()?Map.of("docs",List.of()):search.es.post().uri("/"+search.index+"/_mget").body(Map.of("ids",ids)).retrieve().body(Map.class);
            var docs=(List<?>)result.get("docs");
            for(int i=0;i<rows.size();i++) {
                var row=new LinkedHashMap<>(rows.get(i));
                var doc=(Map<?,?>)docs.get(i);
                var source=(Map<?,?>)doc.get("_source");
                boolean current=source!=null && Objects.toString(source.get("version"),"").equals(row.get("version").toString());
                row.put("sync_status",doc.containsKey("error")?"unavailable":current?"in_sync":"pending");
                checked.add(row);
            }
        } catch(Exception ex) {state="unavailable";checked.clear();for(var row:rows){row.put("sync_status","unavailable");checked.add(row);}}
        return Map.of("state",state,"items",checked,"checked",rows.size(),"total",db.queryForObject("SELECT count(*) FROM orders",Long.class));
    }
    @PostMapping("/sync/{id}") Map<String,Object> retry(@PathVariable long id) {
        orders.document(id);
        return Map.of("queued",search.enqueue(id));
    }
}
