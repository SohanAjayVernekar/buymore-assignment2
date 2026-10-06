package com.buymore;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    final JdbcTemplate db;
    final OrderService orders;
    final ProductController products;
    AccountController(JdbcTemplate db, OrderService orders, ProductController products) {
        this.db=db; this.orders=orders; this.products=products;
    }
    static long userId(Authentication auth) { return Long.parseLong(auth.getName()); }
    static void page(int page) {
        if(page<1 || page>100000) throw new ResponseStatusException(BAD_REQUEST,"Invalid page.");
    }
    static final java.util.Set<String> ORDER_STATUSES=java.util.Set.of("PENDING","PROCESSING","SHIPPED");
    @GetMapping("/orders") Map<String,Object> list(Authentication auth,@RequestParam(defaultValue="1") int page,@RequestParam(required=false) String status) {
        page(page); long user=userId(auth);
        boolean filtered=status!=null && !status.isBlank();
        if(filtered && !ORDER_STATUSES.contains(status)) throw new ResponseStatusException(BAD_REQUEST,"Unknown order status.");
        var summary=db.queryForMap("SELECT count(*) AS total, COALESCE(sum(total_amount),0) AS spent, count(*) FILTER (WHERE status <> 'SHIPPED') AS active FROM orders WHERE user_id=?",user);
        // The dashboard's status chips page against the filtered set, while the
        // summary keeps describing the whole account.
        long total=filtered?db.queryForObject("SELECT count(*) FROM orders WHERE user_id=? AND status=?",Long.class,user,status):((Number)summary.get("total")).longValue();
        var rows=filtered
            ?db.queryForList("SELECT o.id AS order_id,o.order_date,o.status,o.total_amount,COALESCE(sum(i.quantity),0) AS item_count FROM orders o LEFT JOIN order_items i ON i.order_id=o.id WHERE o.user_id=? AND o.status=? GROUP BY o.id ORDER BY o.order_date DESC,o.id DESC LIMIT 8 OFFSET ?",user,status,(page-1)*8)
            :db.queryForList("SELECT o.id AS order_id,o.order_date,o.status,o.total_amount,COALESCE(sum(i.quantity),0) AS item_count FROM orders o LEFT JOIN order_items i ON i.order_id=o.id WHERE o.user_id=? GROUP BY o.id ORDER BY o.order_date DESC,o.id DESC LIMIT 8 OFFSET ?",user,(page-1)*8);
        return Map.of("items",rows,"summary",summary,"page",page,"total_pages",(total+7)/8);
    }
    public record Profile(@NotBlank @Size(max=120) String name) {}
    @PatchMapping("/profile") Map<String,Object> profile(Authentication auth,@Valid @RequestBody Profile input) {
        db.update("UPDATE users SET name=? WHERE id=?",input.name().trim(),userId(auth));
        return AuthController.publicUser(db.queryForMap("SELECT * FROM users WHERE id=?",userId(auth)));
    }
    @GetMapping("/favourites") Map<String,Object> favourites(Authentication auth) {
        List<Map<String,Object>> items=new ArrayList<>();
        for(String id:db.queryForList("SELECT product_id FROM favourites WHERE user_id=? ORDER BY created_at DESC,product_id",String.class,userId(auth))) {
            try {items.add(ProductController.json(products.find(id)));}
            catch(ResponseStatusException ex) {
                if(ex.getStatusCode()!=NOT_FOUND) throw ex;
                items.add(Map.of("_id",id,"title","Unavailable product","active",false,"price",0,"category","","attributes",Map.of()));
            }
        }
        return Map.of("items",items);
    }
    @PutMapping("/favourites/{id}") Map<String,Boolean> saveFavourite(Authentication auth,@PathVariable String id) {
        var product=products.find(id);
        if(!Boolean.TRUE.equals(product.get("active"))) throw new ResponseStatusException(BAD_REQUEST,"This product is no longer available.");
        db.update("INSERT INTO favourites(user_id,product_id) VALUES (?,?) ON CONFLICT DO NOTHING",userId(auth),id);
        return Map.of("ok",true);
    }
    @DeleteMapping("/favourites/{id}") Map<String,Boolean> removeFavourite(Authentication auth,@PathVariable String id) {
        db.update("DELETE FROM favourites WHERE user_id=? AND product_id=?",userId(auth),id);
        return Map.of("ok",true);
    }
    @GetMapping("/orders/{id}/buy-again") Map<String,Object> buyAgain(Authentication auth,@PathVariable long id) {
        var order=orders.document(id);
        if(!order.get("user_id").toString().equals(auth.getName())) throw new ResponseStatusException(FORBIDDEN,"This order belongs to another account.");
        List<Map<String,Object>> available=new ArrayList<>();
        List<String> unavailable=new ArrayList<>();
        for(var line:db.queryForList("SELECT product_id,title,quantity FROM order_items WHERE order_id=? ORDER BY id",id)) {
            try {
                var product=products.find(line.get("product_id").toString());
                if(Boolean.TRUE.equals(product.get("active"))) available.add(Map.of("product",ProductController.json(product),"quantity",line.get("quantity")));
                else unavailable.add(line.get("title").toString());
            } catch(ResponseStatusException ex) {
                if(ex.getStatusCode()!=NOT_FOUND) throw ex;
                unavailable.add(line.get("title").toString());
            }
        }
        return Map.of("items",available,"unavailable",unavailable);
    }
}
