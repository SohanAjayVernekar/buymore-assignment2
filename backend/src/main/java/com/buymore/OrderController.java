package com.buymore;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final SearchService search;
    OrderController(OrderService orders,SearchService search){this.orders=orders;this.search=search;}
    @PostMapping @ResponseStatus(CREATED) Map<String,Object> create(@Valid @RequestBody OrderService.Checkout request,Authentication auth) {
        long id=orders.create(Long.parseLong(auth.getName()),request); // Transaction commits before queue publication.
        var result=orders.document(id);result.put("sync_status",search.enqueue(id)?"queued":"queue_unavailable");return result;
    }
    @GetMapping("/{id}") Map<String,Object> get(@PathVariable long id,Authentication auth) {
        var result=orders.document(id);
        if(!AuthController.isAdmin(auth) && !result.get("user_id").toString().equals(auth.getName())) throw new ResponseStatusException(FORBIDDEN,"This order belongs to another account.");
        result.put("sync_status",search.syncStatus(id,((Number)result.get("version")).longValue())); return result;
    }
    public record Status(@NotBlank String status) {}
    @PatchMapping("/{id}/status") Map<String,Object> status(@PathVariable long id,@Valid @RequestBody Status input) {
        orders.status(id,input.status());var result=orders.document(id);result.put("sync_status",search.enqueue(id)?"queued":"queue_unavailable");return result;
    }
}
