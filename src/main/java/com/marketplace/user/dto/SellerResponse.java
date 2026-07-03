package com.marketplace.user.dto;

import com.marketplace.user.model.User;

public record SellerResponse(
		String id,
		String displayName,
		String profilePictureUrl,
		String defaultStreet,
		String defaultProvince,
		String defaultDistrict,
		String defaultWard
) {
	public static SellerResponse from(User user) {
		return new SellerResponse(
				user.getId().toString(),
				user.getDisplayName(),
				user.getProfilePictureUrl(),
				user.getDefaultStreet(),
				user.getDefaultProvince(),
				user.getDefaultDistrict(),
				user.getDefaultWard()
		);
	}
}
