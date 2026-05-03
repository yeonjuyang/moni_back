package com.yang.moni.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/ledgers/{ledgerId}/assets")
    public List<AssetResponse> getAssets(@PathVariable Long ledgerId) {
        return assetService.getByLedgerId(ledgerId);
    }

    @PostMapping("/ledgers/{ledgerId}/assets")
    public ResponseEntity<AssetResponse> createAsset(
            @PathVariable Long ledgerId,
            @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assetService.createAsset(ledgerId, request));
    }

    @PutMapping("/ledgers/{ledgerId}/assets/{assetId}")
    public AssetResponse updateAsset(
            @PathVariable Long ledgerId,
            @PathVariable Long assetId,
            @RequestBody AssetRequest request) {
        return assetService.updateAsset(ledgerId, assetId, request);
    }

    @DeleteMapping("/ledgers/{ledgerId}/assets/{assetId}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable Long ledgerId,
            @PathVariable Long assetId) {
        assetService.deleteAsset(ledgerId, assetId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ledgers/{ledgerId}/assets/reorder")
    public ResponseEntity<Void> reorderAssets(
            @PathVariable Long ledgerId,
            @RequestBody List<Long> orderedIds) {
        assetService.reorderAssets(ledgerId, orderedIds);
        return ResponseEntity.noContent().build();
    }
}
