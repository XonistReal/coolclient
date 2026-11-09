package dev.coolclient.core.api;

/** The block under the crosshair. */
public final class BlockInfo {
    /** Namespaced id, e.g. {@code minecraft:oak_log}. */
    public final String id;
    public final String name;
    /** The block's item form for its icon, or {@link ItemRef#EMPTY}. */
    public final ItemRef item;
    public final int x, y, z;
    /** Name of the mod (namespace) that adds it, e.g. "Minecraft". */
    public final String source;

    public BlockInfo(String id, String name, ItemRef item, int x, int y, int z) {
        this.id = id;
        this.name = name;
        this.item = item == null ? ItemRef.EMPTY : item;
        this.x = x;
        this.y = y;
        this.z = z;
        String ns = id.indexOf(':') > 0 ? id.substring(0, id.indexOf(':')) : "minecraft";
        this.source = ns.isEmpty() ? "" : Character.toUpperCase(ns.charAt(0)) + ns.substring(1);
    }
}
