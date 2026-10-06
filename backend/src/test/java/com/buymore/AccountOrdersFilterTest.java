package com.buymore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The dashboard's status chips filter orders server-side, so paging past
 * page 1 keeps working while a filter is active and total_pages follows
 * the filtered count. The summary always describes the whole account.
 */
@SpringBootTest(properties={"buymore.admin-invitation=integration-test-invitation", "buymore.seed=true", "buymore.index-queue=buymore.java.test-order-index", "buymore.elasticsearch-index=buymore-integration-orders", "spring.datasource.url=jdbc:postgresql://localhost:${TEST_POSTGRES_PORT:15434}/buymore_test", "spring.data.mongodb.uri=mongodb://localhost:${TEST_MONGO_PORT:27020}/buymore_test"})
@AutoConfigureMockMvc
class AccountOrdersFilterTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    JsonNode data(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}

    MockHttpSession account()throws Exception {
        String email="filter-"+UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name","Filter Tester","email",email,"password","Test-password-42","role","CUSTOMER","invitationCode","integration-test-invitation")))).andExpect(status().isCreated());
        var login=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",email,"password","Test-password-42","role","CUSTOMER")))).andExpect(status().isOk()).andReturn();
        return (MockHttpSession)login.getRequest().getSession(false);
    }
    long order(long user,String status,double total)throws Exception {
        return db.queryForObject("INSERT INTO orders(user_id,order_date,status,total_amount,updated_at,version) VALUES (?,CURRENT_TIMESTAMP,?,?,CURRENT_TIMESTAMP,1) RETURNING id",Long.class,user,status,total);
    }

    @Test void ordersFilterByStatusWithCorrectPaging()throws Exception {
        var session=account();
        long user=data(mvc.perform(get("/api/auth/me").session(session)).andReturn()).get("id").asLong();
        long pending=order(user,"PENDING",20);
        long processing=order(user,"PROCESSING",30);
        long shipped=order(user,"SHIPPED",40);
        long pending2=order(user,"PENDING",50);

        // No filter: every order, one page, global summary.
        var all=data(mvc.perform(get("/api/account/orders").session(session)).andExpect(status().isOk()).andReturn());
        assertEquals(4,all.get("items").size(),"no filter should return every order");
        assertEquals(4,all.get("summary").get("total").asInt());
        assertEquals(1,all.get("total_pages").asInt());

        // Filtered: only that status; the summary still reports the whole account.
        var shippedRows=data(mvc.perform(get("/api/account/orders").param("status","SHIPPED").session(session)).andExpect(status().isOk()).andReturn());
        assertEquals(1,shippedRows.get("items").size(),"SHIPPED filter should return one order");
        assertEquals(shipped,shippedRows.get("items").get(0).get("order_id").asLong());
        assertEquals(4,shippedRows.get("summary").get("total").asInt(),"summary must stay global");
        assertEquals(140,shippedRows.get("summary").get("spent").intValue(),"spent must stay global");

        var pendingRows=data(mvc.perform(get("/api/account/orders").param("status","PENDING").session(session)).andExpect(status().isOk()).andReturn());
        Set<Long> ids=new HashSet<>();
        pendingRows.get("items").forEach(row->ids.add(row.get("order_id").asLong()));
        assertEquals(Set.of(pending,pending2),ids,"PENDING filter should return exactly the two pending orders");
        assertEquals(4,pendingRows.get("summary").get("total").asInt(),"summary must stay global under a filter");

        // Unknown or wrong-case statuses are client errors, not silent empty lists.
        mvc.perform(get("/api/account/orders").param("status","UNKNOWN").session(session)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/account/orders").param("status","shipped").session(session)).andExpect(status().isBadRequest());

        // Paging follows the filtered count: page 2 of a 2-row set is empty,
        // total_pages stays 1, and no unfiltered rows leak in.
        var page2=data(mvc.perform(get("/api/account/orders").param("status","PENDING").param("page","2").session(session)).andExpect(status().isOk()).andReturn());
        assertEquals(0,page2.get("items").size(),"page 2 of a 2-row filtered list is empty");
        assertEquals(1,page2.get("total_pages").asInt(),"total_pages follows the filtered count");

        // Another account never sees these orders under any filter.
        var stranger=account();
        var theirs=data(mvc.perform(get("/api/account/orders").param("status","SHIPPED").session(stranger)).andExpect(status().isOk()).andReturn());
        assertEquals(0,theirs.get("items").size());
    }
}
