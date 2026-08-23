package com.yang.moni;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    protected String token;
    protected Long userId;

    @BeforeEach
    void authenticate() throws Exception {
        String res = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tester@moni.dev\",\"nickname\":\"테스터\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(res);
        token = json.get("token").asText();
        userId = json.get("userId").asLong();
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + token);
    }

    protected JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String toJson(Object obj) throws JacksonException {
        return objectMapper.writeValueAsString(obj);
    }

    /** 다른 유저로 로그인해서 토큰 반환 */
    protected String loginAs(String email, String nickname) throws Exception {
        String res = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"nickname\":\"" + nickname + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("token").asText();
    }

    /** 가계부 생성 후 ledgerId 반환 */
    protected Long createLedger(String name) throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ledgerName\":\"" + name + "\"}")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("ledgerId").asLong();
    }

    /** 자산 생성 후 assetId 반환 */
    protected Long createAsset(Long ledgerId, String name, String type, long balance) throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetName\":\"" + name + "\",\"assetType\":\"" + type + "\",\"balance\":" + balance + "}")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("assetId").asLong();
    }

    /** 카테고리 생성 후 categoryId 반환 */
    protected Long createCategory(Long ledgerId, String name, String type) throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"" + name + "\",\"categoryType\":\"" + type + "\",\"iconName\":\"other\",\"iconColor\":\"#9E9E9E\"}")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("categoryId").asLong();
    }
}
