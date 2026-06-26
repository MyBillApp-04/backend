package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.GlobalSearchResponse;
import com.mybill.MyBill_Backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<GlobalSearchResponse> globalSearch(
            @RequestParam String query,
            Pageable pageable
    ) {
        return ResponseEntity.ok(searchService.globalSearch(query, pageable));
    }
}
