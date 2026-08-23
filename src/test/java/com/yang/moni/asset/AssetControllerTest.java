package com.yang.moni.asset;

import com.yang.moni.ApiTestBase;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssetControllerTest extends ApiTestBase {

    private Long ledgerId;
    private Long defaultAssetId; // 가계부 생성 시 자동 생성된 "내 통장"

    @BeforeEach
    void setup() throws Exception {
        ledgerId = createLedger("자산테스트 가계부");
        // 기본 자산 ID 가져오기
        String res = mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn().getResponse().getContentAsString();
        defaultAssetId = objectMapper.readTree(res).get(0).get("assetId").asLong();
    }

    @Test
    @DisplayName("ASSET-01: 자산 등록 → 목록에 추가됨")
    void createAsset_appearsInList() throws Exception {
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/assets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"assetName\":\"적금통장\",\"assetType\":\"BANK\",\"balance\":500000}"))
)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetName").value("적금통장"))
                .andExpect(jsonPath("$.assetType").value("BANK"))
                .andExpect(jsonPath("$.balance").value(500000));
    }

    @Test
    @DisplayName("ASSET-02: 자산 목록 조회 → sortOrder 오름차순 정렬")
    void getAssets_sortedBySortOrder() throws Exception {
        createAsset(ledgerId, "현금", "CASH", 0);
        createAsset(ledgerId, "카드", "CARD", 0);

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        int prevOrder = -1;
        for (JsonNode a : assets) {
            int order = a.get("sortOrder").asInt();
            assertThat(order).isGreaterThan(prevOrder);
            prevOrder = order;
        }
    }

    @Test
    @DisplayName("ASSET-03: 거래 없는 자산 잔액 = 초기잔액")
    void getAssets_noTransactions_balanceEqualsInitial() throws Exception {
        Long assetId = createAsset(ledgerId, "잔액테스트", "BANK", 100000);

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        JsonNode target = null;
        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) { target = a; break; }
        }
        assertThat(target).isNotNull();
        assertThat(target.get("balance").asLong()).isEqualTo(100000);
    }

    @Test
    @DisplayName("ASSET-04: 수입 거래 후 자산 잔액 = 초기잔액 + 수입금액")
    void getAssets_afterIncome_balanceIncreased() throws Exception {
        Long assetId = createAsset(ledgerId, "수입테스트", "BANK", 100000);

        // 수입 거래 등록
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"transactionType":"INCOME","amount":50000,"memo":"수입",
                     "transactionDate":"2026-01-01","toAssetId":%d}""".formatted(assetId))));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) {
                assertThat(a.get("balance").asLong()).isEqualTo(150000); // 100000 + 50000
                return;
            }
        }
        throw new AssertionError("자산을 찾지 못했습니다");
    }

    @Test
    @DisplayName("ASSET-05: 지출 거래 후 자산 잔액 = 초기잔액 - 지출금액")
    void getAssets_afterExpense_balanceDecreased() throws Exception {
        Long assetId = createAsset(ledgerId, "지출테스트", "BANK", 200000);

        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"transactionType":"EXPENSE","amount":30000,"memo":"지출",
                     "transactionDate":"2026-01-01","fromAssetId":%d}""".formatted(assetId))));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) {
                assertThat(a.get("balance").asLong()).isEqualTo(170000); // 200000 - 30000
                return;
            }
        }
        throw new AssertionError("자산을 찾지 못했습니다");
    }

    @Test
    @DisplayName("ASSET-06: 자산 수정 → 목표잔액 역산 저장, 조회 시 목표잔액 반환")
    void updateAsset_targetBalanceReverseCalculated() throws Exception {
        Long assetId = createAsset(ledgerId, "역산테스트", "BANK", 0);

        // 수입 10만원 등록
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"transactionType":"INCOME","amount":100000,"memo":"수입",
                     "transactionDate":"2026-01-01","toAssetId":%d}""".formatted(assetId))));

        // 현재 잔액은 100000. 목표잔액 200000으로 수정 → 초기잔액 = 200000 - 100000 = 100000 저장
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/assets/" + assetId)
                .contentType(APPLICATION_JSON)
                .content("{\"assetName\":\"역산테스트\",\"assetType\":\"BANK\",\"balance\":200000}")));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) {
                assertThat(a.get("balance").asLong()).isEqualTo(200000);
                return;
            }
        }
        throw new AssertionError("자산을 찾지 못했습니다");
    }

    @Test
    @DisplayName("ASSET-07: 자산 삭제 → 목록에서 제외 (soft delete)")
    void deleteAsset_removedFromList() throws Exception {
        Long assetId = createAsset(ledgerId, "삭제테스트", "CASH", 0);

        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/assets/" + assetId)))
                .andExpect(status().isNoContent());

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        boolean found = false;
        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) { found = true; break; }
        }
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("ASSET-08: 자산 순서 변경 → sortOrder 반영")
    void reorderAssets_sortOrderUpdated() throws Exception {
        Long assetId2 = createAsset(ledgerId, "두번째", "CASH", 0);
        Long assetId3 = createAsset(ledgerId, "세번째", "CARD", 0);

        // [assetId2, defaultAssetId, assetId3] 순서로 변경
        List<Long> newOrder = List.of(assetId2, defaultAssetId, assetId3);
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/assets/reorder")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(newOrder))))
                .andExpect(status().isNoContent());

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        assertThat(assets.get(0).get("assetId").asLong()).isEqualTo(assetId2);
        assertThat(assets.get(1).get("assetId").asLong()).isEqualTo(defaultAssetId);
        assertThat(assets.get(2).get("assetId").asLong()).isEqualTo(assetId3);
    }

    @Test
    @DisplayName("ASSET-09: 존재하지 않는 자산 수정 → 500 에러")
    void updateAsset_notFound_returnsError() throws Exception {
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/assets/999999")
                        .contentType(APPLICATION_JSON)
                        .content("{\"assetName\":\"없는자산\",\"assetType\":\"BANK\",\"balance\":0}")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("ASSET-10: 자산 삭제 후 새 자산 추가 시 sortOrder 중복 없음")
    void createAsset_afterDelete_sortOrderNoDuplicate() throws Exception {
        Long assetId2 = createAsset(ledgerId, "삭제될자산", "CASH", 0);
        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/assets/" + assetId2)));

        // 새 자산 추가
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/assets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"assetName\":\"새자산\",\"assetType\":\"CASH\",\"balance\":0}")))
                .andReturn().getResponse().getContentAsString();

        int newSortOrder = objectMapper.readTree(res).get("sortOrder").asInt();
        assertThat(newSortOrder).isGreaterThan(0);
    }
}
