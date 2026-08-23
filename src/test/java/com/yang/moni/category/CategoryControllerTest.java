package com.yang.moni.category;

import com.yang.moni.ApiTestBase;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryControllerTest extends ApiTestBase {

    private Long ledgerId;

    @BeforeEach
    void setup() throws Exception {
        ledgerId = createLedger("카테고리테스트 가계부");
    }

    @Test
    @DisplayName("CAT-01: 카테고리 등록 → 목록에 추가, sortOrder 자동 증가")
    void createCategory_appearsInListWithSortOrder() throws Exception {
        // 가계부 생성 시 기본 카테고리 7개 (sortOrder 1~7)
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/categories")
                        .contentType(APPLICATION_JSON)
                        .content("{\"categoryName\":\"여가\",\"categoryType\":\"EXPENSE\",\"iconName\":\"sports\",\"iconColor\":\"#4D96FF\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("여가"))
                .andExpect(jsonPath("$.sortOrder").value(8)); // 기본 7개 다음
    }

    @Test
    @DisplayName("CAT-02: 카테고리 목록 조회 → sortOrder 오름차순, active=true만")
    void getCategories_sortedAndActiveOnly() throws Exception {
        JsonNode cats = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/categories")))
                .andReturn());

        int prevOrder = -1;
        for (JsonNode c : cats) {
            int order = c.get("sortOrder").asInt();
            assertThat(order).isGreaterThanOrEqualTo(prevOrder);
            prevOrder = order;
        }
        assertThat(cats.size()).isEqualTo(7); // 기본 7개
    }

    @Test
    @DisplayName("CAT-03: 카테고리 수정 → 이름, 아이콘, 색상 변경")
    void updateCategory_changesFields() throws Exception {
        Long catId = createCategory(ledgerId, "수정전", "EXPENSE");

        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/categories/" + catId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"categoryName\":\"수정후\",\"categoryType\":\"EXPENSE\",\"iconName\":\"new_icon\",\"iconColor\":\"#FF0000\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("수정후"))
                .andExpect(jsonPath("$.iconName").value("new_icon"))
                .andExpect(jsonPath("$.iconColor").value("#FF0000"));
    }

    @Test
    @DisplayName("CAT-04: 카테고리 삭제 → 목록에서 제외 (soft delete)")
    void deleteCategory_removedFromList() throws Exception {
        Long catId = createCategory(ledgerId, "삭제대상", "EXPENSE");

        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/categories/" + catId)))
                .andExpect(status().isNoContent());

        JsonNode cats = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/categories")))
                .andReturn());

        boolean found = false;
        for (JsonNode c : cats) {
            if (c.get("categoryId").asLong() == catId) { found = true; break; }
        }
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("CAT-05: 삭제된 카테고리 수정 시도 → 500 에러")
    void updateDeletedCategory_returnsError() throws Exception {
        Long catId = createCategory(ledgerId, "삭제후수정", "EXPENSE");
        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/categories/" + catId)));

        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/categories/" + catId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"categoryName\":\"수정불가\",\"categoryType\":\"EXPENSE\",\"iconName\":\"x\",\"iconColor\":\"#000\"}")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("CAT-06: 존재하지 않는 카테고리 삭제 → 500 에러")
    void deleteCategory_notFound_returnsError() throws Exception {
        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/categories/999999")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("CAT-07: 카테고리 삭제 후 새 카테고리 추가 → sortOrder = 남은개수+1")
    void createCategory_afterDelete_sortOrderBasedOnActiveCount() throws Exception {
        Long catId = createCategory(ledgerId, "삭제될것", "EXPENSE");
        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/categories/" + catId)));

        // 기본 7개 + 위에서 추가한 1개 = 8, 삭제 1개 → active = 7
        // 새 카테고리 sortOrder = 7 + 1 = 8
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/categories")
                        .contentType(APPLICATION_JSON)
                        .content("{\"categoryName\":\"새카테고리\",\"categoryType\":\"EXPENSE\",\"iconName\":\"new\",\"iconColor\":\"#000\"}")))
                .andReturn().getResponse().getContentAsString();

        int sortOrder = objectMapper.readTree(res).get("sortOrder").asInt();
        assertThat(sortOrder).isEqualTo(8); // active count(7) + 1
    }

    @Test
    @DisplayName("CAT-08: 다른 가계부 카테고리 수정 시도 → 500 에러")
    void updateCategory_differentLedger_returnsError() throws Exception {
        Long otherLedgerId = createLedger("다른 가계부");
        Long catId = createCategory(ledgerId, "내 카테고리", "EXPENSE");

        // 다른 가계부 ID로 수정 시도
        mockMvc.perform(withAuth(put("/api/ledgers/" + otherLedgerId + "/categories/" + catId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"categoryName\":\"해킹시도\",\"categoryType\":\"EXPENSE\",\"iconName\":\"x\",\"iconColor\":\"#000\"}")))
                .andExpect(status().is5xxServerError());
    }
}
