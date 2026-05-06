package com.yang.moni.ledger;

import com.yang.moni.asset.Asset;
import com.yang.moni.asset.AssetRepository;
import com.yang.moni.category.Category;
import com.yang.moni.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    public LedgerResponse getLedger(Long ledgerId) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new RuntimeException("Ledger not found: " + ledgerId));
        return new LedgerResponse(ledger.getLedgerId(), ledger.getLedgerName(), ledger.getLedgerType());
    }

    @Transactional
    public LedgerResponse updateLedger(Long ledgerId, LedgerRequest request) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new RuntimeException("Ledger not found: " + ledgerId));
        ledger.updateName(request.ledgerName());
        return new LedgerResponse(ledger.getLedgerId(), ledger.getLedgerName(), ledger.getLedgerType());
    }

    @Transactional
    public LedgerResponse createLedger(LedgerRequest request) {
        String type = (request.ledgerType() != null) ? request.ledgerType() : "PERSONAL";
        Ledger ledger = Ledger.builder()
                .ledgerName(request.ledgerName())
                .ledgerType(type)
                .createdBy(1L) // TODO: replace with JWT claim
                .build();
        Ledger saved = ledgerRepository.save(ledger);

        createDefaultAssets(saved.getLedgerId());
        createDefaultCategories(saved.getLedgerId());

        return new LedgerResponse(saved.getLedgerId(), saved.getLedgerName(), saved.getLedgerType());
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
