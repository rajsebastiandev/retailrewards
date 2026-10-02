package com.retailer.rewards.controller;

import com.retailer.rewards.dto.CustomerRewards;
import com.retailer.rewards.dto.RewardsSummary;
import com.retailer.rewards.service.RewardsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Reward points per customer, per month and in total.
 * Both endpoints accept optional {@code from} / {@code to} dates (ISO format, inclusive);
 * when omitted the full recorded period is used.
 */
@RestController
@RequestMapping("/api/rewards")
public class RewardsController {

    private final RewardsService rewardsService;

    public RewardsController(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
    }

    @GetMapping
    public RewardsSummary getAllRewards(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return rewardsService.getRewards(from, to);
    }

    @GetMapping("/{customerId}")
    public CustomerRewards getCustomerRewards(
            @PathVariable long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return rewardsService.getRewardsForCustomer(customerId, from, to);
    }
}
