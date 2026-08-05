package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.service.QuotationPublicResponseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Controller
@RequestMapping("/q")
@Validated
public class QuotationPublicController {

    private final QuotationPublicResponseService publicResponseService;

    public QuotationPublicController(QuotationPublicResponseService publicResponseService) {
        this.publicResponseService = publicResponseService;
    }

    @GetMapping("/{token}")
    public String showPublicQuotationPage(@PathVariable("token") @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String token, Model model) {
        QuotationPublicResponseService.PublicQuotationView view = publicResponseService.getPublicQuotationView(token);
        model.addAttribute("view", view);

        if (!view.isValid()) {
            model.addAttribute("errorMsg", view.errorMessage());
        }

        return "quotation-response";
    }

    @PostMapping("/{token}/respond")
    public String submitResponse(
            @PathVariable("token") @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String token,
            @RequestParam("action") @Pattern(regexp = "(?i)ACCEPT(?:ED)?|DECLINE(?:D)?|REJECTED|DISCUSS(?:ION(?:_REQUESTED)?)?|REVISE|REVISION(?:_REQUESTED)?|MODIFICATION") String action,
            @RequestParam(value = "message", required = false) @Size(max = 2000) String message,
            HttpServletRequest request,
            Model model) {

        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        QuotationPublicResponseService.ResponseSubmissionResult result =
                publicResponseService.processClientResponse(token, action, message, ipAddress, userAgent);

        QuotationPublicResponseService.PublicQuotationView updatedView =
                publicResponseService.getPublicQuotationView(token);

        model.addAttribute("view", updatedView);

        if (result.success()) {
            model.addAttribute("successMsg", result.message());
        } else if (result.alreadyResponded()) {
            model.addAttribute("successMsg", result.message());
        } else {
            model.addAttribute("errorMsg", result.message());
        }

        return "quotation-response";
    }
}
