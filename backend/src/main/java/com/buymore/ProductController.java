package com.buymore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    final MongoTemplate mongo;
    ProductController(MongoTemplate mongo) {this.mongo=mongo;}
    public record Product(@NotBlank @Size(max=80) String sku, @NotBlank @Size(max=200) String title,
        @NotNull @Size(max=5000) String description, @NotNull @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer=6,fraction=2) BigDecimal price,
        @NotBlank @Size(max=60) String category, @NotNull List<String> tags, @NotNull Map<String,Object> attributes,
        @NotNull List<Map<String,Object>> variants, boolean active,
        @DecimalMin("0") @DecimalMax("999999.99") BigDecimal compareAtPrice) {}
    @GetMapping Map<String,Object> list(@RequestParam(defaultValue="1") int page, @RequestParam(name="page_size",defaultValue="6") int pageSize,
        @RequestParam(defaultValue="") String query, @RequestParam(defaultValue="") String category,
        @RequestParam(name="max_price",required=false) BigDecimal maxPrice, @RequestParam(name="include_inactive",defaultValue="false") boolean inactive, Authentication auth) {
        if(page<1 || pageSize<1 || pageSize>100 || query.length()>200 || (maxPrice!=null && maxPrice.signum()<0)) throw new ResponseStatusException(BAD_REQUEST,"Invalid pagination or filters.");
        if(inactive && !AuthController.isAdmin(auth)) throw new ResponseStatusException(FORBIDDEN,"Admin access required.");
        List<Criteria> filters=new ArrayList<>();
        if(!inactive) filters.add(Criteria.where("active").is(true));
        if(!query.isBlank()) filters.add(new Criteria().orOperator(Criteria.where("title").regex(Pattern.quote(query.trim()),"i"),Criteria.where("sku").regex(Pattern.quote(query.trim()),"i")));
        if(!category.isBlank()) filters.add(Criteria.where("category").is(category));
        if(maxPrice!=null) filters.add(Criteria.where("price").lte(maxPrice.doubleValue()));
        Query q=filters.isEmpty()?new Query():new Query(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
        long total=mongo.count(q,"products");
        q.with(org.springframework.data.domain.Sort.by("sku")).skip((long)(page-1)*pageSize).limit(pageSize);
        var items=mongo.find(q,Document.class,"products").stream().map(ProductController::json).toList();
        return Map.of("items",items,"total",total,"page",page,"total_pages",(total+pageSize-1)/pageSize,"catalog_counts",Map.of("active",mongo.count(new Query(Criteria.where("active").is(true)),"products"),"inactive",inactive?mongo.count(new Query(Criteria.where("active").is(false)),"products"):0));
    }
    Document find(String id) {
        if(!ObjectId.isValid(id)) throw new ResponseStatusException(NOT_FOUND,"Product not found.");
        Document p=mongo.findById(new ObjectId(id),Document.class,"products");
        if(p==null) throw new ResponseStatusException(NOT_FOUND,"Product not found.");
        return p;
    }
    @GetMapping("/{id}") Map<String,Object> get(@PathVariable String id, Authentication auth) {
        Document p=find(id);
        if(!Boolean.TRUE.equals(p.get("active")) && !AuthController.isAdmin(auth)) throw new ResponseStatusException(NOT_FOUND,"Product not found.");
        return json(p);
    }
    @PostMapping @ResponseStatus(CREATED) Map<String,Object> create(@Valid @RequestBody Product p) {return json(mongo.insert(document(p),"products"));}
    @PutMapping("/{id}") Map<String,Object> update(@PathVariable String id,@Valid @RequestBody Product p) {
        Document old=find(id); Document replacement=document(p); replacement.put("_id",old.get("_id")); preserveCompareAt(replacement,old);
        return json(mongo.save(replacement,"products"));
    }
    static Document document(Product p) {
        var doc=new Document("sku",p.sku().trim()).append("title",p.title().trim()).append("description",p.description()).append("price",p.price().doubleValue()).append("category",p.category()).append("tags",p.tags()).append("attributes",p.attributes()).append("variants",p.variants()).append("active",p.active()).append("updated_at",Instant.now().toString());
        if(p.compareAtPrice()!=null) doc.append("compare_at_price",p.compareAtPrice().doubleValue());
        return doc;
    }
    static Map<String,Object> json(Document p) {var result=new LinkedHashMap<String,Object>(p);result.put("_id",p.get("_id").toString());return result;}
    static void preserveCompareAt(Document replacement, Document old) {
        if(!replacement.containsKey("compare_at_price") && old.get("compare_at_price")!=null) replacement.put("compare_at_price",old.get("compare_at_price"));
    }
}
