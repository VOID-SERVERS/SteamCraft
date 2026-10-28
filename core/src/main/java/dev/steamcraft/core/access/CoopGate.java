package dev.steamcraft.core.access;

import dev.steamcraft.core.config.CoopAccess;

import java.util.function.LongPredicate;
import java.util.function.Supplier;

public final class CoopGate {
	private final Supplier<CoopAccess> access;
	private final LongPredicate isFriend;
	private final Invites invites;

	public CoopGate(Supplier<CoopAccess> access, LongPredicate isFriend, Invites invites) {
		this.access = access;
		this.isFriend = isFriend;
		this.invites = invites;
	}

	public String rejectReason(long steamId) {
		switch (access.get()) {
			case EVERYONE:
				return null;
			case FRIENDS:
				return isFriend.test(steamId) ? null : "This world is open to the host's Steam friends only.";
			case INVITE:
				return invites.isInvited(steamId) ? null
						: "This world is invite-only. Ask the host for an invite (it is valid for " + Invites.VALID_MINUTES + " minutes).";
			default:
				throw new IllegalStateException();
		}
	}
}
