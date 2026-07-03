package com.marketplace.user.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.user.dto.SellerResponse;
import com.marketplace.user.service.UserService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellers")
public class SellerController {

	private final UserService userService;

	public SellerController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{sellerId}")
	public ResponseEntity<ApiResponse<SellerResponse>> getSellerProfile(@PathVariable UUID sellerId) {
		SellerResponse seller = userService.getSellerProfile(sellerId.toString());
		return ResponseEntity.ok(ApiResponse.ok(seller));
	}
}
