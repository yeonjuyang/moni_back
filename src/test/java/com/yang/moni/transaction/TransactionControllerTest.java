package com.yang.moni.transaction;

import com.yang.moni.ApiTestBase;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionControllerTest extends ApiTestBase {

    private Long ledgerId;
    private Long bankAssetId;  // 초기잔액 0
    private Long cashAssetId;  // 초기잔액 0

    @BeforeEach
    void setup() throws Exception {
        ledgerId = createLedger("거래테스트 가계부");

        // 기본 자산 ID 가져오기 (내 통장)
        String assetRes = mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn().getResponse().getContentAsString();
        bankAssetId = objectMapper.readTree(assetRes).get(0).get("assetId").asLong();

        // 이체 테스트용 두 번째 자산
        cashAssetId = createAsset(ledgerId, "현금", "CASH", 0);
    }

    // ── 기본 CRUD ──────────────────────────────────────────────────

    @Test
    @DisplayName("TXN-01: 지출 거래 등록 (fromAssetId)")
    void createExpense_success() throws Exception {
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"transactionType":"EXPENSE","amount":15000,"memo":"점심",
                             "transactionDate":"2026-01-15","fromAssetId":%d}""".formatted(bankAssetId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(15000))
                .andExpect(jsonPath("$.fromAssetId").value(bankAssetId));
    }

    @Test
    @DisplayName("TXN-02: 수입 거래 등록 (toAssetId)")
    void createIncome_success() throws Exception {
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"transactionType":"INCOME","amount":3000000,"memo":"월급",
                             "transactionDate":"2026-01-05","toAssetId":%d}""".formatted(bankAssetId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("INCOME"))
                .andExpect(jsonPath("$.toAssetId").value(bankAssetId));
    }

    @Test
    @DisplayName("TXN-03: paidByUserId 미입력 → currentUser로 자동 설정")
    void createTransaction_noPaidBy_setsCurrentUser() throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"transactionType":"EXPENSE","amount":5000,"memo":"자동설정",
                             "transactionDate":"2026-01-01","fromAssetId":%d}""".formatted(bankAssetId))))
                .andReturn().getResponse().getContentAsString();

        Long paidBy = objectMapper.readTree(res).get("paidByUserId").asLong();
        assertThat(paidBy).isEqualTo(userId);
    }

    @Test
    @DisplayName("TXN-04: 카테고리명으로 카테고리 연결")
    void createTransaction_withCategoryName_linked() throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"transactionType":"EXPENSE","amount":8000,"memo":"식사",
                             "categoryName":"식비","transactionDate":"2026-01-01","fromAssetId":%d}""".formatted(bankAssetId))))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(res).get("categoryName").asText()).isEqualTo("식비");
    }

    @Test
    @DisplayName("TXN-05: 없는 카테고리명으로 거래 등록 → categoryName null 저장")
    void createTransaction_unknownCategory_storedAsNull() throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"transactionType":"EXPENSE","amount":1000,"memo":"없는카테고리",
                             "categoryName":"존재안함","transactionDate":"2026-01-01","fromAssetId":%d}""".formatted(bankAssetId))))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(res).get("categoryName").isNull()).isTrue();
    }

    @Test
    @DisplayName("TXN-06: 거래 목록 조회 → 날짜 내림차순 정렬")
    void getTransactions_sortedByDateDesc() throws Exception {
        // 날짜 다른 거래 2개 등록
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"memo\":\"오래된\"," +
                         "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + "}")));
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("{\"transactionType\":\"EXPENSE\",\"amount\":2000,\"memo\":\"최신\"," +
                         "\"transactionDate\":\"2026-06-01\",\"fromAssetId\":" + bankAssetId + "}")));

        JsonNode txns = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/transactions")))
                .andReturn());

        assertThat(txns.get(0).get("memo").asText()).isEqualTo("최신");
        assertThat(txns.get(1).get("memo").asText()).isEqualTo("오래된");
    }

    @Test
    @DisplayName("TXN-07: 거래 수정 → 금액, 메모, 카테고리 변경")
    void updateTransaction_changesFields() throws Exception {
        String createRes = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":5000,\"memo\":\"수정전\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + "}")))
                .andReturn().getResponse().getContentAsString();
        Long txnId = objectMapper.readTree(createRes).get("id").asLong();

        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/transactions/" + txnId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":9999,\"memo\":\"수정후\"," +
                                 "\"categoryName\":\"식비\",\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + "}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(9999))
                .andExpect(jsonPath("$.memo").value("수정후"))
                .andExpect(jsonPath("$.categoryName").value("식비"));
    }

    @Test
    @DisplayName("TXN-08: 거래 삭제 → 목록에서 제외")
    void deleteTransaction_removedFromList() throws Exception {
        String createRes = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"memo\":\"삭제대상\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + "}")))
                .andReturn().getResponse().getContentAsString();
        Long txnId = objectMapper.readTree(createRes).get("id").asLong();

        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/transactions/" + txnId)))
                .andExpect(status().isNoContent());

        JsonNode txns = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/transactions")))
                .andReturn());

        boolean found = false;
        for (JsonNode t : txns) {
            if (t.get("id").asLong() == txnId) { found = true; break; }
        }
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("TXN-09: 다른 가계부의 거래 수정 시도 → 500 에러")
    void updateTransaction_wrongLedger_returnsError() throws Exception {
        Long otherLedgerId = createLedger("다른 가계부");
        Long otherAssetId = createAsset(otherLedgerId, "다른자산", "BANK", 0);

        String createRes = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"memo\":\"원거래\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + "}")))
                .andReturn().getResponse().getContentAsString();
        Long txnId = objectMapper.readTree(createRes).get("id").asLong();

        // 다른 가계부 ID로 수정 시도
        mockMvc.perform(withAuth(put("/api/ledgers/" + otherLedgerId + "/transactions/" + txnId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":9999,\"memo\":\"해킹\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + otherAssetId + "}")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("TXN-10: 거래 삭제 후 자산 잔액 원복")
    void deleteTransaction_assetBalanceRestored() throws Exception {
        Long assetId = createAsset(ledgerId, "복구테스트", "BANK", 100000);

        String createRes = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":30000,\"memo\":\"삭제예정\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + assetId + "}")))
                .andReturn().getResponse().getContentAsString();
        Long txnId = objectMapper.readTree(createRes).get("id").asLong();

        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/transactions/" + txnId)));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());
        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) {
                assertThat(a.get("balance").asLong()).isEqualTo(100000); // 원복
                return;
            }
        }
        throw new AssertionError("자산을 찾지 못했습니다");
    }

    @Test
    @DisplayName("TXN-11: 잘못된 날짜 형식 → 400 Bad Request")
    void createTransaction_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"memo\":\"날짜오류\"," +
                                 "\"transactionDate\":\"2026/01/01\",\"fromAssetId\":" + bankAssetId + "}")))
                .andExpect(status().isBadRequest());
    }

    // ── 이체 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TXN-12: 이체 등록 → fromAsset 잔액 감소, toAsset 잔액 증가")
    void createTransfer_bothAssetsUpdated() throws Exception {
        // bankId 초기잔액 100000, cashId 초기잔액 0
        Long bankId = createAsset(ledgerId, "이체출금", "BANK", 100000);
        Long cashId = createAsset(ledgerId, "이체입금", "CASH", 0);

        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"transactionType":"TRANSFER","amount":40000,"memo":"이체",
                             "transactionDate":"2026-01-01",
                             "fromAssetId":%d,"toAssetId":%d}""".formatted(bankId, cashId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.fromAssetId").value(bankId))
                .andExpect(jsonPath("$.toAssetId").value(cashId));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        long bankBalance = -1, cashBalance = -1;
        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == bankId) bankBalance = a.get("balance").asLong();
            if (a.get("assetId").asLong() == cashId) cashBalance = a.get("balance").asLong();
        }
        assertThat(bankBalance).isEqualTo(60000);  // 100000 - 40000
        assertThat(cashBalance).isEqualTo(40000);  // 0 + 40000
    }

    @Test
    @DisplayName("TXN-13: 이체 후 총 순자산 합계 변화 없음")
    void createTransfer_totalBalanceUnchanged() throws Exception {
        Long bankId = createAsset(ledgerId, "이체출금합계", "BANK", 200000);
        Long cashId = createAsset(ledgerId, "이체입금합계", "CASH", 50000);

        long totalBefore = 200000 + 50000;

        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("{\"transactionType\":\"TRANSFER\",\"amount\":70000,\"memo\":\"이체합계\"," +
                         "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankId + ",\"toAssetId\":" + cashId + "}")));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        long totalAfter = 0;
        for (JsonNode a : assets) {
            long id = a.get("assetId").asLong();
            if (id == bankId || id == cashId) totalAfter += a.get("balance").asLong();
        }
        assertThat(totalAfter).isEqualTo(totalBefore);
    }

    @Test
    @DisplayName("TXN-14: 이체 삭제 → 양쪽 자산 잔액 원복")
    void deleteTransfer_bothAssetsRestored() throws Exception {
        Long bankId = createAsset(ledgerId, "이체삭제출금", "BANK", 100000);
        Long cashId = createAsset(ledgerId, "이체삭제입금", "CASH", 0);

        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"TRANSFER\",\"amount\":50000,\"memo\":\"이체삭제\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankId + ",\"toAssetId\":" + cashId + "}")))
                .andReturn().getResponse().getContentAsString();
        Long txnId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId + "/transactions/" + txnId)));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        for (JsonNode a : assets) {
            long id = a.get("assetId").asLong();
            if (id == bankId) assertThat(a.get("balance").asLong()).isEqualTo(100000);
            if (id == cashId) assertThat(a.get("balance").asLong()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("TXN-15: 이체 수정 (금액 변경) → 양쪽 잔액 재계산")
    void updateTransfer_amountChanged_balancesRecalculated() throws Exception {
        Long bankId = createAsset(ledgerId, "이체수정출금", "BANK", 100000);
        Long cashId = createAsset(ledgerId, "이체수정입금", "CASH", 0);

        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"TRANSFER\",\"amount\":30000,\"memo\":\"이체수정전\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankId + ",\"toAssetId\":" + cashId + "}")))
                .andReturn().getResponse().getContentAsString();
        Long txnId = objectMapper.readTree(res).get("id").asLong();

        // 금액 30000 → 60000으로 수정
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/transactions/" + txnId)
                .contentType(APPLICATION_JSON)
                .content("{\"transactionType\":\"TRANSFER\",\"amount\":60000,\"memo\":\"이체수정후\"," +
                         "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankId + ",\"toAssetId\":" + cashId + "}")));

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        for (JsonNode a : assets) {
            long id = a.get("assetId").asLong();
            if (id == bankId) assertThat(a.get("balance").asLong()).isEqualTo(40000);  // 100000 - 60000
            if (id == cashId) assertThat(a.get("balance").asLong()).isEqualTo(60000);  // 0 + 60000
        }
    }

    @Test
    @DisplayName("TXN-16: 같은 자산으로 이체 (from == to) → 잔액 변화 확인")
    void createTransfer_sameAsset_balanceEffect() throws Exception {
        Long assetId = createAsset(ledgerId, "자기이체", "BANK", 100000);

        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"TRANSFER\",\"amount\":50000,\"memo\":\"자기이체\"," +
                                 "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + assetId + ",\"toAssetId\":" + assetId + "}")))
                .andExpect(status().isCreated());

        JsonNode assets = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andReturn());

        for (JsonNode a : assets) {
            if (a.get("assetId").asLong() == assetId) {
                // from 출금 50000, to 입금 50000 → 상쇄되어 잔액 변화 없어야 함
                assertThat(a.get("balance").asLong()).isEqualTo(100000);
                return;
            }
        }
    }

    @Test
    @DisplayName("TXN-17: 이체 거래 목록 조회 → TRANSFER 타입으로 반환")
    void getTransactions_includesTransfer() throws Exception {
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("{\"transactionType\":\"TRANSFER\",\"amount\":10000,\"memo\":\"이체조회\"," +
                         "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + ",\"toAssetId\":" + cashAssetId + "}")));

        JsonNode txns = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/transactions")))
                .andReturn());

        boolean found = false;
        for (JsonNode t : txns) {
            if ("TRANSFER".equals(t.get("transactionType").asText())) { found = true; break; }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("TXN-18: EXPENSE 등록 시 fromAssetId 없음 → H2에서는 constraint 미적용으로 통과 (PostgreSQL에서는 실패)")
    void createExpense_noFromAssetId_passesInH2() throws Exception {
        // H2에는 ck_transaction_asset_rule constraint 없음 → 200 응답
        // PostgreSQL 운영환경에서는 DB constraint로 실패해야 함
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"memo\":\"constraint없음\"," +
                                 "\"transactionDate\":\"2026-01-01\"}")))
                .andExpect(status().isCreated()); // H2에서는 통과
    }

    @Test
    @DisplayName("TXN-19: 가계부별 데이터 격리 → A 가계부 거래가 B 가계부 조회에 노출 안 됨")
    void getTransactions_isolatedByLedger() throws Exception {
        Long otherLedgerId = createLedger("격리테스트 가계부B");

        // A 가계부에 거래 등록
        mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/transactions")
                .contentType(APPLICATION_JSON)
                .content("{\"transactionType\":\"EXPENSE\",\"amount\":9999,\"memo\":\"A가계부거래\"," +
                         "\"transactionDate\":\"2026-01-01\",\"fromAssetId\":" + bankAssetId + "}")));

        // B 가계부 조회 → A 거래 없어야 함
        JsonNode txns = parse(mockMvc.perform(withAuth(get("/api/ledgers/" + otherLedgerId + "/transactions")))
                .andReturn());

        boolean found = false;
        for (JsonNode t : txns) {
            if ("A가계부거래".equals(t.get("memo").asText())) { found = true; break; }
        }
        assertThat(found).isFalse();
    }
}
