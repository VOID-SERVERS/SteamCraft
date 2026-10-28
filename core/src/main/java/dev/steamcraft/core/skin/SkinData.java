package dev.steamcraft.core.skin;

public final class SkinData {
	public static final SkinData NONE = new SkinData(false, null, null);

	public final boolean slim;
	public final byte[] skin;
	public final byte[] cape;

	public SkinData(boolean slim, byte[] skin, byte[] cape) {
		this.slim = slim;
		this.skin = skin;
		this.cape = cape;
	}
}
