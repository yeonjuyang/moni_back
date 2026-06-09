package com.yang.moni.ledger;

import com.yang.moni.asset.Asset;
import com.yang.moni.asset.AssetRepository;
import com.yang.moni.category.Category;
import com.yang.moni.category.CategoryRepository;
import com.yang.moni.user.User;
import com.yang.moni.user.UserRepository;
import com.yang.moni.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final LedgerMemberRepository ledgerMemberRepository;
    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    public List<UserResponse> getMembers(Long ledgerId) {
        List<LedgerMember> members = ledgerMemberRepository.findByLedgerId(ledgerId);
        Map<Long, String> userNicknames = userRepository.findAllById(
                members.stream().map(LedgerMember::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getUserId, User::getNickname));

        return members.stream()
                .map(m -> new UserResponse(
                        m.getUserId(),
                        m.getNickname() != null ? m.getNickname() : userNicknames.getOrDefault(m.getUserId(), "알 수 없음")
                ))
                .toList();
    }

    public List<LedgerResponse> getMyLedgers() {
        List<Long> ledgerIds = ledgerMemberRepository.findByUserId(1L).stream() // TODO: JWT
                .map(LedgerMember::getLedgerId)
                .toList();
        return ledgerRepository.findAllById(ledgerIds).stream()
                .filter(Ledger::isActive)
                .map(this::toResponse)
                .toList();
    }

    public LedgerResponse getLedger(Long ledgerId) {
        return toResponse(findActive(ledgerId));
    }

    @Transactional
    public LedgerResponse updateLedger(Long ledgerId, LedgerRequest request) {
        Ledger ledger = findActive(ledgerId);
        ledger.updateName(request.ledgerName());
        return toResponse(ledger);
    }

    @Transactional
    public LedgerResponse createLedger(LedgerRequest request) {
        String type = (request.ledgerType() != null) ? request.ledgerType() : "PERSONAL";
        Ledger ledger = Ledger.builder()
                .ledgerName(request.ledgerName())
                .ledgerType(type)
                .createdBy(1L) // TODO: JWT
                .build();
        Ledger saved = ledgerRepository.save(ledger);

        ledgerMemberRepository.save(LedgerMember.builder()
                .ledgerId(saved.getLedgerId())
                .userId(1L) // TODO: JWT
                .role("OWNER")
                .build());

        createDefaultAssets(saved.getLedgerId());
        createDefaultCategories(saved.getLedgerId());

        return toResponse(saved);
    }

    @Transactional
    public void deleteLedger(Long ledgerId) {
        findActive(ledgerId).deactivate();
    }

    @Transactional
    public LedgerResponse generateInviteCode(Long ledgerId) {
        Ledger ledger = findActive(ledgerId);
        ledger.updateInviteCode(createCode());
        return toResponse(ledger);
    }

    @Transactional
    public LedgerResponse joinLedger(String inviteCode) {
        Ledger ledger = ledgerRepository.findByInviteCode(inviteCode)
                .filter(Ledger::isActive)
                .orElseThrow(() -> new NoSuchElementException("유효하지 않은 초대 코드입니다"));
        Long userId = 1L; // TODO: JWT
        if (!ledgerMemberRepository.existsByLedgerIdAndUserId(ledger.getLedgerId(), userId)) {
            ledgerMemberRepository.save(LedgerMember.builder()
                    .ledgerId(ledger.getLedgerId())
                    .userId(userId)
                    .role("MEMBER")
                    .build());
        }
        return toResponse(ledger);
    }

    private Ledger findActive(Long ledgerId) {
        return ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new RuntimeException("Ledger not found: " + ledgerId));
    }

    @Transactional
    public LedgerResponse updateMyNickname(Long ledgerId, String nickname) {
        LedgerMember member = ledgerMemberRepository
                .findByLedgerIdAndUserId(ledgerId, 1L) // TODO: JWT
                .orElseThrow(() -> new NoSuchElementException("가계부 멤버가 아닙니다"));
        member.updateNickname(nickname.isBlank() ? null : nickname.trim());
        return toResponse(findActive(ledgerId));
    }

    private LedgerResponse toResponse(Ledger l) {
        String myNickname = ledgerMemberRepository
                .findByLedgerIdAndUserId(l.getLedgerId(), 1L) // TODO: JWT
                .map(LedgerMember::getNickname)
                .orElse(null);
        return new LedgerResponse(l.getLedgerId(), l.getLedgerName(), l.getLedgerType(), l.getInviteCode(), myNickname);
    }

    private String createCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void createDefaultAssets(Long ledgerId) {
        assetRepository.save(Asset.builder()
                .ledgerId(ledgerId)
                .assetName("내 통장")
                .assetType("BANK")
                .balance(0L)
                .sortOrder(1)
                .build());
    }

    private void createDefaultCategories(Long ledgerId) {
        categoryRepository.saveAll(List.of(
                category(ledgerId, "식비",  "EXPENSE", "restaurant", "#FF6B6B", 1),
                category(ledgerId, "교통",  "EXPENSE", "bus",        "#6BCB77", 2),
                category(ledgerId, "쇼핑",  "EXPENSE", "shopping",   "#4D96FF", 3),
                category(ledgerId, "의료",  "EXPENSE", "hospital",   "#FF6BAA", 4),
                category(ledgerId, "기타",  "EXPENSE", "other",      "#9E9E9E", 5),
                category(ledgerId, "급여",  "INCOME",  "salary",     "#26A69A", 1),
                category(ledgerId, "기타",  "INCOME",  "other",      "#9E9E9E", 2)
        ));
    }

    private Category category(Long ledgerId, String name, String type,
                               String iconName, String iconColor, int sortOrder) {
        return Category.builder()
                .ledgerId(ledgerId)
                .categoryName(name)
                .categoryType(type)
                .iconName(iconName)
                .iconColor(iconColor)
                .sortOrder(sortOrder)
                .build();
    }
}
