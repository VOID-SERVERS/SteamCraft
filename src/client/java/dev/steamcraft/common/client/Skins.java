package dev.steamcraft.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;
//? if >=1.21.9 {
/*import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
*///?}

public final class Skins {
	private Skins() {
	}

	public static PlayerSkin of(ResourceLocation body, ResourceLocation cape, ResourceLocation elytra, boolean slim) {
		//? if >=1.21.9 {
		/*return new PlayerSkin(texture(body), texture(cape), texture(elytra), slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, false);
		*///?} else {
		return new PlayerSkin(body, null, cape, elytra, slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, false);
		//?}
	}

	public static PlayerSkin override(PlayerSkin vanilla, ResourceLocation skin, boolean slim, ResourceLocation cape, ResourceLocation elytra) {
		//? if >=1.21.9 {
		/*return new PlayerSkin(
				skin != null ? texture(skin) : vanilla.body(),
				cape != null ? texture(cape) : vanilla.cape(),
				cape != null ? texture(elytra) : vanilla.elytra(),
				skin != null ? (slim ? PlayerModelType.SLIM : PlayerModelType.WIDE) : vanilla.model(),
				true);
		*///?} else {
		return new PlayerSkin(
				skin != null ? skin : vanilla.texture(),
				skin != null ? null : vanilla.textureUrl(),
				cape != null ? cape : vanilla.capeTexture(),
				cape != null ? elytra : vanilla.elytraTexture(),
				skin != null ? (slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE) : vanilla.model(),
				true);
		//?}
	}

	public static ResourceLocation body(PlayerSkin skin) {
		//? if >=1.21.9 {
		/*return skin.body().texturePath();
		*///?} else {
		return skin.texture();
		//?}
	}

	public static ResourceLocation cape(PlayerSkin skin) {
		//? if >=1.21.9 {
		/*return skin.cape() != null ? skin.cape().texturePath() : null;
		*///?} else {
		return skin.capeTexture();
		//?}
	}

	public static ResourceLocation elytra(PlayerSkin skin) {
		//? if >=1.21.9 {
		/*return skin.elytra() != null ? skin.elytra().texturePath() : null;
		*///?} else {
		return skin.elytraTexture();
		//?}
	}

	public static boolean slim(PlayerSkin skin) {
		//? if >=1.21.9 {
		/*return skin.model() == PlayerModelType.SLIM;
		*///?} else {
		return skin.model() == PlayerSkin.Model.SLIM;
		//?}
	}

	public static Supplier<PlayerSkin> account(Minecraft minecraft) {
		//? if >=1.21.9 {
		/*return minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false);
		*///?} else {
		return minecraft.getSkinManager().lookupInsecure(minecraft.getGameProfile());
		//?}
	}

	//? if >=1.21.9 {
	/*private static ClientAsset.Texture texture(ResourceLocation id) {
		return id != null ? new ClientAsset.ResourceTexture(id, id) : null;
	}
	*///?}
}
