package com.buymore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"buymore.admin-invitation=integration-test-invitation", "buymore.seed=true", "buymore.index-queue=buymore.java.test-order-index", "buymore.elasticsearch-index=buymore-integration-orders", "spring.datasource.url=jdbc:postgresql://localhost:${TEST_POSTGRES_PORT:15434}/buymore_test", "spring.data.mongodb.uri=mongodb://localhost:${TEST_MONGO_PORT:27020}/buymore_test"})
@AutoConfigureMockMvc
class BackendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    JsonNode data(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}
    MockHttpSession account(String role)throws Exception {
        String email="integration-"+UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name","Integration Tester","email",email,"password","Test-password-42","role",role,"invitationCode","integration-test-invitation")))).andExpect(status().isCreated());
        var login=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",email,"password","Test-password-42","role",role)))).andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value(role)).andReturn();
        assertTrue(db.queryForObject("SELECT password_hash FROM users WHERE email=?",String.class,email).startsWith("$2a$"));
        return (MockHttpSession)login.getRequest().getSession(false);
    }
    @Test void authenticationAndPermissions()throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name","Admin","email","blocked@example.test","password","Valid-pass-42","role","ADMIN","invitationCode","wrong")))).andExpect(status().isForbidden());
        var customer=account("CUSTOMER");
        mvc.perform(get("/api/auth/me").session(customer)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CUSTOMER"));
        mvc.perform(get("/api/products?include_inactive=true").session(customer)).andExpect(status().isForbidden());
        mvc.perform(post("/api/search/orders").session(customer).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(customer).with(csrf())).andExpect(status().isOk());
        assertTrue(customer.isInvalid());
    }
    @Test void catalogCheckoutAndSearch()throws Exception {
        var admin=account("ADMIN");var customer=account("CUSTOMER");var stranger=account("CUSTOMER");
        mvc.perform(get("/api/products?page_size=0")).andExpect(status().isBadRequest());
        String sku="TEST-"+UUID.randomUUID();
        var product=Map.of("sku",sku,"title","Integration Wireless Mouse","description","Test product","price",12.50,"category","peripherals","tags",List.of("test"),"attributes",Map.of("brand","Test","color","blue"),"variants",List.of(),"active",true);
        mvc.perform(post("/api/products").session(customer).with(csrf()).contentType("application/json").content(body(product))).andExpect(status().isForbidden());
        String id=data(mvc.perform(post("/api/products").session(admin).with(csrf()).contentType("application/json").content(body(product))).andExpect(status().isCreated()).andReturn()).get("_id").asText();
        mvc.perform(get("/api/products").param("query",sku)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        var invalid=Map.of("items",List.of(Map.of("product_id",id,"quantity",0)));
        mvc.perform(post("/api/orders").session(customer).with(csrf()).contentType("application/json").content(body(invalid))).andExpect(status().isBadRequest());
        var checkout=Map.of("user_id",999999,"items",List.of(Map.of("product_id",id,"quantity",2)));
        var order=data(mvc.perform(post("/api/orders").session(customer).with(csrf()).contentType("application/json").content(body(checkout))).andExpect(status().isCreated()).andExpect(jsonPath("$.total_amount").value(25.0)).andReturn());
        long orderId=order.get("order_id").asLong();
        long actualUser=data(mvc.perform(get("/api/auth/me").session(customer)).andReturn()).get("id").asLong();
        assertEquals(actualUser,order.get("user_id").asLong());
        mvc.perform(get("/api/orders/"+orderId).session(stranger)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/"+orderId+"/status").session(customer).with(csrf()).contentType("application/json").content("{\"status\":\"SHIPPED\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/"+orderId+"/status").session(admin).with(csrf()).contentType("application/json").content("{\"status\":\"SHIPPED\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/orders/"+orderId).session(customer)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        // Wait for the real RabbitMQ consumer and Elasticsearch refresh, rather than indexing directly.
        boolean indexed=false;
        for(int i=0;i<30;i++) {
            var result=data(mvc.perform(post("/api/search/orders").session(admin).with(csrf()).contentType("application/json").content("{\"query\":\"Integration Wireless\",\"statuses\":[\"SHIPPED\"],\"page\":1}")).andExpect(status().isOk()).andReturn());
            for(JsonNode row:result.get("orders")) if(row.get("order_id").asLong()==orderId) indexed=true;
            if(indexed) break;Thread.sleep(500);
        }
        assertTrue(indexed,"New order should arrive in search through RabbitMQ");
    }
    @Test void browserCsrfFlowAndSessionRotation()throws Exception {
        String email="browser-"+UUID.randomUUID()+"@example.test";
        var tokenResult=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)tokenResult.getRequest().getSession(false);
        var token=data(tokenResult);
        mvc.perform(post("/api/auth/register").session(session).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(body(Map.of("name","Browser Test","email",email,"password","Test-password-42","role","CUSTOMER")))).andExpect(status().isCreated());
        String previousId=session.getId();
        var login=mvc.perform(post("/api/auth/login").session(session).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(body(Map.of("email",email,"password","Test-password-42","role","CUSTOMER")))).andExpect(status().isOk()).andReturn();
        var authenticated=(MockHttpSession)login.getRequest().getSession(false);
        assertNotEquals(previousId,authenticated.getId());
        mvc.perform(get("/api/auth/me").session(authenticated)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(authenticated).header(token.get("headerName").asText(),token.get("token").asText())).andExpect(status().isForbidden());
        var fresh=data(mvc.perform(get("/api/auth/csrf").session(authenticated)).andReturn());
        mvc.perform(post("/api/auth/logout").session(authenticated).header(fresh.get("headerName").asText(),fresh.get("token").asText())).andExpect(status().isOk());
    }
    @Test void duplicateAndWrongPassword()throws Exception {
        String email="duplicate-"+UUID.randomUUID()+"@example.test";
        String registration=body(Map.of("name","Customer","email",email,"password","Test-password-42","role","CUSTOMER"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration)).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration)).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",email,"password","wrong-password","role","CUSTOMER")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",email,"password","Test-password-42","role","ADMIN")))).andExpect(status().isUnauthorized());
    }
    @Test void customerDashboardOwnershipFavouritesAndCurrentPrices()throws Exception {
        var customer=account("CUSTOMER"); var other=account("CUSTOMER"); var admin=account("ADMIN");
        long user=data(mvc.perform(get("/api/auth/me").session(customer)).andReturn()).get("id").asLong();
        var product=new HashMap<String,Object>(Map.of("sku","DASH-"+UUID.randomUUID(),"title","Dashboard Mouse","description","Saved favourite","price",10.0,"category","peripherals","tags",List.of(),"attributes",Map.of(),"variants",List.of(),"active",true));
        String id=data(mvc.perform(post("/api/products").session(admin).with(csrf()).contentType("application/json").content(body(product))).andExpect(status().isCreated()).andReturn()).get("_id").asText();
        mvc.perform(get("/api/account/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/orders?page=0").session(customer)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/account/favourites/"+id).session(customer)).andExpect(status().isForbidden());
        for(int i=0;i<2;i++) mvc.perform(put("/api/account/favourites/"+id).session(customer).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isOk());
        mvc.perform(get("/api/account/favourites").session(customer)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0]._id").value(id));
        mvc.perform(get("/api/account/favourites").session(other)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(delete("/api/account/favourites/"+id).session(other).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/account/favourites").session(customer)).andExpect(jsonPath("$.items.length()").value(1));
        long order=db.queryForObject("INSERT INTO orders(user_id,order_date,status,total_amount,updated_at,version) VALUES (?,CURRENT_TIMESTAMP,'PENDING',20,CURRENT_TIMESTAMP,1) RETURNING id",Long.class,user);
        db.update("INSERT INTO order_items(order_id,product_id,title,quantity,unit_price) VALUES (?,?,?,2,10)",order,id,"Dashboard Mouse");
        mvc.perform(get("/api/account/orders").session(customer)).andExpect(jsonPath("$.summary.total").value(1)).andExpect(jsonPath("$.summary.active").value(1)).andExpect(jsonPath("$.items[0].order_id").value(order));
        mvc.perform(get("/api/account/orders").session(other)).andExpect(jsonPath("$.summary.total").value(0));
        mvc.perform(get("/api/account/orders/"+order+"/buy-again").session(other)).andExpect(status().isForbidden());
        product.put("price",17.50);
        mvc.perform(put("/api/products/"+id).session(admin).with(csrf()).contentType("application/json").content(body(product))).andExpect(status().isOk());
        mvc.perform(get("/api/account/orders/"+order+"/buy-again").session(customer)).andExpect(jsonPath("$.items[0].product.price").value(17.50)).andExpect(jsonPath("$.items[0].quantity").value(2));
        mvc.perform(get("/api/orders/"+order).session(customer)).andExpect(jsonPath("$.items[0].unit_price").value(10));
        product.put("active",false);
        mvc.perform(put("/api/products/"+id).session(admin).with(csrf()).contentType("application/json").content(body(product))).andExpect(status().isOk());
        mvc.perform(get("/api/account/orders/"+order+"/buy-again").session(customer)).andExpect(jsonPath("$.items.length()").value(0)).andExpect(jsonPath("$.unavailable[0]").value("Dashboard Mouse"));
        mvc.perform(patch("/api/account/profile").session(customer).with(csrf()).contentType("application/json").content("{\"name\":\"New Display Name\",\"role\":\"ADMIN\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New Display Name")).andExpect(jsonPath("$.role").value("CUSTOMER"));
        mvc.perform(get("/api/auth/me").session(customer)).andExpect(jsonPath("$.name").value("New Display Name"));
        mvc.perform(patch("/api/account/profile").session(customer).with(csrf()).contentType("application/json").content("{\"name\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/account/favourites/"+id).session(customer).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/account/favourites").session(customer)).andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test void adminDashboardPermissionsMetricsInboxAndSynchronization()throws Exception {
        var customer=account("CUSTOMER");var admin=account("ADMIN");
        for(String endpoint:List.of("overview","orders","customers","messages","sync")) {
            mvc.perform(get("/api/admin/"+endpoint)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/admin/"+endpoint).session(customer)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/"+endpoint).session(admin)).andExpect(status().isOk());
        }
        long expected=db.queryForObject("SELECT count(*) FROM orders",Long.class);
        mvc.perform(get("/api/admin/overview").session(admin)).andExpect(jsonPath("$.totals.orders").value(expected)).andExpect(jsonPath("$.trend.length()").value(7));
        String email=data(mvc.perform(get("/api/auth/me").session(customer)).andReturn()).get("email").asText();
        mvc.perform(get("/api/admin/customers").session(admin).param("query",email)).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].email").value(email));
        mvc.perform(get("/api/admin/customers?query=%25").session(admin)).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/admin/orders?status=UNKNOWN").session(admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/messages?page=0").session(admin)).andExpect(status().isBadRequest());
        long message=data(mvc.perform(post("/api/contact").with(csrf()).contentType("application/json").content(body(Map.of("name","Dashboard Test","email",email,"subject","Inbox test","message","Please check my order.")))).andExpect(status().isCreated()).andReturn()).get("submissionId").asLong();
        mvc.perform(patch("/api/admin/messages/"+message).session(customer).with(csrf()).contentType("application/json").content("{\"resolved\":true}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/messages/"+message).session(admin).with(csrf()).contentType("application/json").content("{\"resolved\":true}")).andExpect(status().isOk());
        assertTrue(db.queryForObject("SELECT resolved FROM contact_submissions WHERE id=?",Boolean.class,message));
        mvc.perform(get("/api/admin/messages?resolved=true").session(admin)).andExpect(jsonPath("$.items[0].id").value(message));
        mvc.perform(patch("/api/admin/messages/"+message).session(admin).with(csrf()).contentType("application/json").content("{\"resolved\":false}")).andExpect(status().isOk());
        assertFalse(db.queryForObject("SELECT resolved FROM contact_submissions WHERE id=?",Boolean.class,message));
        mvc.perform(post("/api/admin/sync/1").session(customer).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        var sync=data(mvc.perform(get("/api/admin/sync").session(admin)).andExpect(status().isOk()).andReturn());
        assertEquals("available",sync.get("state").asText());
        if(sync.get("items").size()>0) {
            long id=sync.get("items").get(0).get("order_id").asLong();
            mvc.perform(post("/api/admin/sync/"+id).session(admin).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.queued").value(true));
        }
    }
    @Test void contactSubmissionsAreSavedDurably()throws Exception {
        String email="contact-"+UUID.randomUUID()+"@example.test";
        var submission=Map.of("name","Contact Tester","email",email,"subject","Question about an order","message","Hello, can you tell me more about shipping?");
        mvc.perform(post("/api/contact").with(csrf()).contentType("application/json").content(body(submission))).andExpect(status().isCreated()).andExpect(jsonPath("$.ok").value(true));
        assertEquals(1,db.queryForObject("SELECT count(*) FROM contact_submissions WHERE email=?",Long.class,email));
        // Missing fields are rejected before anything is written.
        mvc.perform(post("/api/contact").with(csrf()).contentType("application/json").content(body(Map.of("name","x","email","not-an-email","subject","","message","")))).andExpect(status().isBadRequest());
        // The anonymous endpoint still requires a valid session CSRF token.
        mvc.perform(post("/api/contact").contentType("application/json").content(body(submission))).andExpect(status().isForbidden());
    }
}
