package com.buymore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class OrderService {
    final JdbcTemplate db;
    final ProductController products;
    OrderService(JdbcTemplate db,ProductController products) {this.db=db;this.products=products;}
    public record Item(@NotBlank String product_id,@Min(1) @Max(100) int quantity) {}
    public record Checkout(@NotEmpty @Size(max=100) List<@Valid Item> items) {}
    @Transactional
    public long create(long userId,Checkout request) {
        List<Map<String,Object>> lines=new ArrayList<>();
        BigDecimal total=BigDecimal.ZERO;
        Set<String> ids=new HashSet<>();
        for(Item item:request.items()) {
            if(!ids.add(item.product_id())) throw new ResponseStatusException(BAD_REQUEST,"Duplicate cart item.");
            var product=products.find(item.product_id());
            if(!Boolean.TRUE.equals(product.get("active"))) throw new ResponseStatusException(BAD_REQUEST,"An item is no longer available.");
            BigDecimal price=new BigDecimal(product.get("price").toString()).setScale(2,RoundingMode.HALF_UP);
            total=total.add(price.multiply(BigDecimal.valueOf(item.quantity())));
            lines.add(Map.of("id",item.product_id(),"title",product.getString("title"),"quantity",item.quantity(),"price",price));
        }
        if(total.compareTo(new BigDecimal("9999999999.99"))>0) throw new ResponseStatusException(BAD_REQUEST,"Order total is too large.");
        Long id=db.queryForObject("INSERT INTO orders(user_id,order_date,status,total_amount,updated_at,version) VALUES (?,CURRENT_TIMESTAMP,'PENDING',?,CURRENT_TIMESTAMP,1) RETURNING id",Long.class,userId,total);
        for(var line:lines) db.update("INSERT INTO order_items(order_id,product_id,title,quantity,unit_price) VALUES (?,?,?,?,?)",id,line.get("id"),line.get("title"),line.get("quantity"),line.get("price"));
        return id;
    }
    public Map<String,Object> document(long id) {
        var rows=db.queryForList("SELECT o.*,u.name,u.email FROM orders o JOIN users u ON u.id=o.user_id WHERE o.id=?",id);
        if(rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND,"Order not found.");
        var row=rows.getFirst();
        var result=new LinkedHashMap<String,Object>();
        result.put("order_id",id);result.put("user_id",row.get("user_id"));
        result.put("order_date",((java.sql.Timestamp)row.get("order_date")).toInstant().toString());
        result.put("updated_at",((java.sql.Timestamp)row.get("updated_at")).toInstant().toString());
        result.put("status",row.get("status"));result.put("total_amount",row.get("total_amount"));result.put("version",row.get("version"));
        result.put("customer",Map.of("name",row.get("name"),"email",row.get("email")));
        result.put("items",db.queryForList("SELECT product_id,title,quantity,unit_price FROM order_items WHERE order_id=? ORDER BY id",id));
        return result;
    }
    public List<Long> ids() {return db.queryForList("SELECT id FROM orders ORDER BY id",Long.class);}
    @Transactional public void status(long id,String status) {
        if(!Set.of("PENDING","PROCESSING","SHIPPED").contains(status)) throw new ResponseStatusException(BAD_REQUEST,"Invalid order status.");
        if(db.update("UPDATE orders SET status=?,updated_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=?",status,id)==0) throw new ResponseStatusException(NOT_FOUND,"Order not found.");
    }
}
