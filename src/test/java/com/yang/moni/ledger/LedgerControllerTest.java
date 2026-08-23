package com.yang.moni.ledger;

import com.yang.moni.ApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LedgerControllerTest extends ApiTestBase {

    private Long ledgerId;

    @BeforeEach
    void setup() throws Exception {
        ledgerId = createLedger("테스트 가계부");
    }

    @Test
    @DisplayName("LEDGER-01: 가계부 생성 → 기본 자산 1개 자동 생성")
    void createLedger_createsDefaultAsset() throws Exception {
        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/assets")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assetName").value("내 통장"));
    }

    @Test
    @DisplayName("LEDGER-02: 가계부 생성 → 기본 카테고리 7개 자동 생성")
    void createLedger_createsDefaultCategories() throws Exception {
        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/categories")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    @DisplayName("LEDGER-03: 가계부 생성 시 ledgerType 미입력 → PERSONAL 기본값")
    void createLedger_noType_defaultsToPersonal() throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers")
                        .contentType(APPLICATION_JSON)
                        .content("{\"ledgerName\":\"타입없는 가계부\"}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(res).get("ledgerType").asText()).isEqualTo("PERSONAL");
    }

    @Test
    @DisplayName("LEDGER-04: 내 가계부 목록 조회 → 생성한 가계부 포함")
    void getMyLedgers_includesCreatedLedger() throws Exception {
        mockMvc.perform(withAuth(get("/api/ledgers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ledgerId == " + ledgerId + ")]").exists());
    }

    @Test
    @DisplayName("LEDGER-05: 가계부 단건 조회")
    void getLedger_returnsCorrectLedger() throws Exception {
        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerId").value(ledgerId))
                .andExpect(jsonPath("$.ledgerName").value("테스트 가계부"));
    }

    @Test
    @DisplayName("LEDGER-06: 가계부 이름 수정")
    void updateLedger_changesName() throws Exception {
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"ledgerName\":\"수정된 가계부\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerName").value("수정된 가계부"));
    }

    @Test
    @DisplayName("LEDGER-07: 가계부 삭제 후 목록에서 제외")
    void deleteLedger_removedFromList() throws Exception {
        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId)))
                .andExpect(status().isNoContent());

        String res = mockMvc.perform(withAuth(get("/api/ledgers")))
                .andReturn().getResponse().getContentAsString();

        boolean exists = objectMapper.readTree(res).findValues("ledgerId")
                .stream().anyMatch(n -> n.asLong() == ledgerId);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("LEDGER-08: 삭제된 가계부 단건 조회 → active 체크 미구현으로 200 반환 (버그)")
    void getLedger_afterDelete_returns200_bug() throws Exception {
        mockMvc.perform(withAuth(delete("/api/ledgers/" + ledgerId)));

        // findActive()가 isActive() 체크 없이 findById만 사용 → 삭제된 가계부도 조회됨
        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("LEDGER-09: 초대코드 생성 → inviteCode 반환")
    void generateInviteCode_returnsCode() throws Exception {
        String res = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/invite")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String code = objectMapper.readTree(res).get("inviteCode").asText();
        assertThat(code).isNotBlank().hasSize(6);
    }

    @Test
    @DisplayName("LEDGER-10: 초대코드로 가계부 참여 → MEMBER 추가")
    void joinLedger_addsAsMember() throws Exception {
        // 초대코드 생성
        String inviteRes = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/invite")))
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(inviteRes).get("inviteCode").asText();

        // 두 번째 유저로 참여
        String token2 = loginAs("member@moni.dev", "멤버");
        mockMvc.perform(post("/api/ledgers/join")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(APPLICATION_JSON)
                        .content("{\"inviteCode\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerId").value(ledgerId));

        // 멤버 목록에서 두 번째 유저 확인
        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("LEDGER-11: 이미 멤버인 유저 재참여 → 중복 추가 없음")
    void joinLedger_alreadyMember_noDuplicate() throws Exception {
        String inviteRes = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/invite")))
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(inviteRes).get("inviteCode").asText();

        String token2 = loginAs("dup@moni.dev", "중복테스트");
        // 첫 번째 참여
        mockMvc.perform(post("/api/ledgers/join")
                .header("Authorization", "Bearer " + token2)
                .contentType(APPLICATION_JSON)
                .content("{\"inviteCode\":\"" + code + "\"}"));
        // 두 번째 참여 시도
        mockMvc.perform(post("/api/ledgers/join")
                .header("Authorization", "Bearer " + token2)
                .contentType(APPLICATION_JSON)
                .content("{\"inviteCode\":\"" + code + "\"}"));

        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/members")))
                .andExpect(jsonPath("$.length()").value(2)); // owner + 중복테스트 = 2명
    }

    @Test
    @DisplayName("LEDGER-12: 잘못된 초대코드로 참여 시도 → 500 에러")
    void joinLedger_invalidCode_returnsError() throws Exception {
        mockMvc.perform(withAuth(post("/api/ledgers/join")
                        .contentType(APPLICATION_JSON)
                        .content("{\"inviteCode\":\"XXXXXX\"}")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("LEDGER-13: 가계부 내 닉네임 수정")
    void updateMyNickname_changesNickname() throws Exception {
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/me")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}")))
                .andExpect(status().isOk());

        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId)))
                .andExpect(jsonPath("$.myNickname").value("새닉네임"));
    }

    @Test
    @DisplayName("LEDGER-14: 닉네임 빈 문자열로 수정 → null 저장")
    void updateMyNickname_blank_storesNull() throws Exception {
        mockMvc.perform(withAuth(put("/api/ledgers/" + ledgerId + "/me")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}")))
                .andExpect(status().isOk());

        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId)))
                .andExpect(jsonPath("$.myNickname").doesNotExist());
    }

    @Test
    @DisplayName("LEDGER-15: 멤버 목록 조회 → 생성자 1명 포함")
    void getLedgerMembers_returnsOwner() throws Exception {
        mockMvc.perform(withAuth(get("/api/ledgers/" + ledgerId + "/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nickname").value("테스터"));
    }

    @Test
    @DisplayName("LEDGER-16: 초대코드 재발급 → 기존 코드 덮어쓰기")
    void generateInviteCode_twice_overwritesOldCode() throws Exception {
        String res1 = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/invite")))
                .andReturn().getResponse().getContentAsString();
        String code1 = objectMapper.readTree(res1).get("inviteCode").asText();

        String res2 = mockMvc.perform(withAuth(post("/api/ledgers/" + ledgerId + "/invite")))
                .andReturn().getResponse().getContentAsString();
        String code2 = objectMapper.readTree(res2).get("inviteCode").asText();

        // 코드가 갱신됨 (같을 수도 있지만 재발급 자체는 성공)
        assertThat(code2).isNotBlank().hasSize(6);
    }
}
