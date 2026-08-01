package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.service.QuotationPublicResponseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/q")
public class QuotationPublicController {

    private final QuotationPublicResponseService publicResponseService;

    public QuotationPublicController(QuotationPublicResponseService publicResponseService) {
        this.publicResponseService = publicResponseService;
    }

    @GetMapping("/{token}")
    public String showPublicQuotationPage(@PathVariable("token") String token, Model model) {
        QuotationPublicResponseService.PublicQuotationView view = publicResponseService.getPublicQuotationView(token);
        model.addAttribute("view", view);

        if (!view.isValid()) {
            model.addAttribute("errorMsg", view.errorMessage());
        }

        return "quotation-response";
    }

    @PostMapping("/{token}/respond")
    public String submitResponse(
            @PathVariable("token") String token,
            @RequestParam("action") String action,
            @RequestParam(value = "message", required = false) String message,
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
