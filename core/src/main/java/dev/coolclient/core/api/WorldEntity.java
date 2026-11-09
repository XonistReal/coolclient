package dev.coolclient.core.api;

/** An entity position (interpolated for the current frame) plus whatever the query asked for. */
public final class WorldEntity {
    public final double x, y, z;
    /** Entity height, for drawing above it. */
    public final double height;
    /** Item for dropped items, otherwise {@link ItemRef#EMPTY}. */
    public final ItemRef item;
    /** Fuse ticks left for primed TNT, otherwise 0. */
    public final int fuse;

    public WorldEntity(double x, double y, double z, double height, ItemRef item, int fuse) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.height = height;
        this.item = item == null ? ItemRef.EMPTY : item;
        this.fuse = fuse;
    }
}
