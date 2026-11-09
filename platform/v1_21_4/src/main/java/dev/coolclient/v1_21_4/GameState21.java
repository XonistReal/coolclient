package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coolclient.core.api.BlockInfo;
import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GameState;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.api.LivingInfo;
import dev.coolclient.core.api.WorldEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import dev.coolclient.core.util.Keys;
import dev.coolclient.v1_21_4.mixin.KeyMappingAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class GameState21 implements GameState {
    private final Map<String, ItemRef> icons = new HashMap<String, ItemRef>();

    private static Minecraft mc() { return Minecraft.getInstance(); }

    private static LocalPlayer player() { return mc().player; }

    @Override public boolean inWorld() { return mc().level != null && mc().player != null; }

    @Override public int fps() { return mc().getFps(); }

    @Override
    public int ping() {
        if (player() == null || mc().getConnection() == null) return -1;
        PlayerInfo info = mc().getConnection().getPlayerInfo(player().getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override
    public String serverAddress() {
        return mc().getCurrentServer() == null || mc().isSingleplayer() ? null : mc().getCurrentServer().ip;
    }

    @Override public String selfName() { return mc().getUser().getName(); }

    @Override
    public UUID selfUuid() {
        // In a world, "self" is the player entity (offline-mode servers assign a different UUID).
        return player() != null ? player().getUUID() : mc().getUser().getProfileId();
    }

    @Override public double x() { return player().getX(); }

    @Override public double y() { return player().getY(); }

    @Override public double z() { return player().getZ(); }

    @Override public float yaw() { return player().getYRot(); }

    @Override public float pitch() { return player().getXRot(); }

    @Override
    public String biome() {
        String path = player().level().getBiome(player().blockPosition()).unwrapKey()
                .map(k -> k.location().getPath()).orElse("unknown");
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    @Override
    public String dimension() {
        return player() == null ? "overworld" : player().level().dimension().location().getPath();
    }

    @Override public long worldTime() { return mc().level == null ? 0 : mc().level.getDayTime(); }

    @Override public float health() { return player().getHealth(); }

    @Override public float maxHealth() { return player().getMaxHealth(); }

    @Override public int food() { return player().getFoodData().getFoodLevel(); }

    @Override public float saturation() { return player().getFoodData().getSaturationLevel(); }

    @Override
    public ItemRef armor(int slot) {
        return player() == null ? ItemRef.EMPTY : ItemRef21.of(player().getInventory().armor.get(slot));
    }

    @Override
    public ItemRef mainHand() { return player() == null ? ItemRef.EMPTY : ItemRef21.of(player().getMainHandItem()); }

    @Override
    public ItemRef offHand() { return player() == null ? ItemRef.EMPTY : ItemRef21.of(player().getOffhandItem()); }

    @Override
    public int countItems(String... ids) {
        if (player() == null) return 0;
        List<Item> items = new ArrayList<Item>();
        for (String id : ids) {
            Item it = ItemRef21.item(id);
            if (it != null) items.add(it);
        }
        int total = 0;
        for (ItemStack s : player().getInventory().items) if (!s.isEmpty() && items.contains(s.getItem())) total += s.getCount();
        for (ItemStack s : player().getInventory().offhand) if (!s.isEmpty() && items.contains(s.getItem())) total += s.getCount();
        return total;
    }

    @Override
    public ItemRef itemOf(String id) {
        ItemRef r = icons.get(id);
        if (r == null) {
            Item it = ItemRef21.item(id);
            r = it == null ? ItemRef.EMPTY : ItemRef21.of(new ItemStack(it));
            icons.put(id, r);
        }
        return r;
    }

    @Override
    public List<EffectInfo> effects() {
        if (player() == null) return Collections.emptyList();
        List<EffectInfo> out = new ArrayList<EffectInfo>();
        for (MobEffectInstance inst : player().getActiveEffects()) {
            if (!inst.isVisible()) continue;
            String id = inst.getEffect().unwrapKey().map(k -> k.location().toString()).orElse("unknown");
            out.add(new EffectInfo(id, inst.getEffect().value().getDisplayName().getString(), inst.getAmplifier(),
                    inst.isInfiniteDuration() ? -1 : inst.getDuration(), inst.getEffect().value().isBeneficial(), inst.getEffect()));
        }
        return out;
    }

    @Override
    public String resourcePack() {
        String name = null;
        for (Pack p : mc().getResourcePackRepository().getSelectedPacks()) {
            if (p.getId().startsWith("file/")) name = p.getTitle().getString();
        }
        if (name != null && name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
        return name;
    }

    @Override
    public float cooldown(String itemId) {
        if (player() == null) return 0f;
        ItemRef ref = itemOf(itemId);
        if (ref.isEmpty()) return 0f;
        return player().getCooldowns().getCooldownPercent((ItemStack) ref.handle(), 0f);
    }

    @Override public boolean sprinting() { return player() != null && player().isSprinting(); }

    @Override public boolean sneaking() { return player() != null && player().isShiftKeyDown(); }

    @Override public boolean thirdPerson() { return !mc().options.getCameraType().isFirstPerson(); }

    @Override public boolean hudHidden() { return mc().options.hideGui; }

    @Override public void setHudHidden(boolean hidden) { mc().options.hideGui = hidden; }

    @Override public boolean debugOpen() { return mc().getDebugOverlay().showDebugScreen(); }

    private static KeyMapping binding(String name) {
        switch (name) {
            case "forward": return mc().options.keyUp;
            case "left": return mc().options.keyLeft;
            case "back": return mc().options.keyDown;
            case "right": return mc().options.keyRight;
            case "jump": return mc().options.keyJump;
            case "sprint": return mc().options.keySprint;
            case "sneak": return mc().options.keyShift;
            case "attack": return mc().options.keyAttack;
            case "use": return mc().options.keyUse;
            default: return null;
        }
    }

    @Override
    public int bindingKey(String name) {
        KeyMapping k = binding(name);
        if (k == null || k.isUnbound()) return Keys.NONE;
        InputConstants.Key key = ((KeyMappingAccessor) k).cool$getKey();
        if (key.getType() == InputConstants.Type.MOUSE) return Keys.mouse(key.getValue());
        if (key.getType() == InputConstants.Type.KEYSYM) return key.getValue();
        return Keys.NONE;
    }

    @Override
    public int perspective() { return mc().options.getCameraType().ordinal(); }

    @Override
    public void setPerspective(int p) {
        CameraType[] types = CameraType.values();
        mc().options.setCameraType(types[Math.max(0, Math.min(types.length - 1, p))]);
    }

    @Override public void setSprintHeld(boolean held) { mc().options.keySprint.setDown(held); }

    @Override public void setSneakHeld(boolean held) { mc().options.keyShift.setDown(held); }

    @Override
    public List<UUID> loadedPlayers() {
        if (mc().level == null) return Collections.emptyList();
        List<UUID> out = new ArrayList<UUID>();
        for (AbstractClientPlayer p : mc().level.players()) out.add(p.getUUID());
        return out;
    }

    // ---------------------------------------------------------------- extra info for the newer modules

    @Override public float absorption() { return player() == null ? 0f : player().getAbsorptionAmount(); }

    @Override public float attackCharge() { return player() == null ? 1f : player().getAttackStrengthScale(0f); }

    @Override public boolean gliding() { return player() != null && player().isFallFlying(); }

    @Override public boolean riding() { return player() != null && player().isPassenger(); }

    @Override
    public double[] mountStats() {
        if (player() == null || !(player().getVehicle() instanceof AbstractHorse)) return null;
        AbstractHorse h = (AbstractHorse) player().getVehicle();
        double speed = h.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.16;
        double j = h.getAttributeValue(Attributes.JUMP_STRENGTH);
        // Fitted curve from jump strength to blocks (the widely used horse-stats formula).
        double jump = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
        return new double[] {speed, Math.max(0, jump), h.getHealth(), h.getMaxHealth()};
    }

    @Override
    public LivingInfo living(int entityId) {
        if (mc().level == null || player() == null) return null;
        Entity e = mc().level.getEntity(entityId);
        if (!(e instanceof LivingEntity)) return null;
        LivingEntity l = (LivingEntity) e;
        return new LivingInfo(l.getName().getString(), l.getHealth(), l.getMaxHealth(), l.getAbsorptionAmount(),
                Math.sqrt(l.distanceToSqr(player())), l instanceof Player, l.getArmorValue());
    }

    @Override
    public BlockInfo targetBlock() {
        HitResult hit = mc().hitResult;
        if (mc().level == null || !(hit instanceof BlockHitResult) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState s = mc().level.getBlockState(pos);
        if (s.isAir()) return null;
        Block b = s.getBlock();
        ItemStack icon = new ItemStack(b.asItem());
        return new BlockInfo(BuiltInRegistries.BLOCK.getKey(b).toString(), b.getName().getString(), ItemRef21.of(icon),
                pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public double[] targetEntityBox(float pt) {
        HitResult hit = mc().hitResult;
        if (!(hit instanceof EntityHitResult)) return null;
        Entity e = ((EntityHitResult) hit).getEntity();
        double dx = Mth.lerp(pt, e.xo, e.getX()) - e.getX();
        double dy = Mth.lerp(pt, e.yo, e.getY()) - e.getY();
        double dz = Mth.lerp(pt, e.zo, e.getZ()) - e.getZ();
        AABB b = e.getBoundingBox().move(dx, dy, dz);
        return new double[] {b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ};
    }

    @Override
    public List<ItemRef> inventory() {
        if (player() == null) return Collections.emptyList();
        List<ItemRef> out = new ArrayList<ItemRef>();
        for (ItemStack s : player().getInventory().items) if (!s.isEmpty()) out.add(ItemRef21.of(s));
        for (ItemStack s : player().getInventory().offhand) if (!s.isEmpty()) out.add(ItemRef21.of(s));
        return out;
    }

    @Override
    public List<WorldEntity> primedTnt(float pt) {
        if (mc().level == null) return Collections.emptyList();
        List<WorldEntity> out = new ArrayList<WorldEntity>();
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!(e instanceof PrimedTnt)) continue;
            out.add(new WorldEntity(Mth.lerp(pt, e.xo, e.getX()), Mth.lerp(pt, e.yo, e.getY()), Mth.lerp(pt, e.zo, e.getZ()),
                    e.getBbHeight(), ItemRef.EMPTY, ((PrimedTnt) e).getFuse()));
        }
        return out;
    }

    @Override
    public List<WorldEntity> droppedItems(float pt, double range) {
        if (mc().level == null || player() == null) return Collections.emptyList();
        List<WorldEntity> out = new ArrayList<WorldEntity>();
        double r2 = range * range;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity) || e.distanceToSqr(player()) > r2) continue;
            out.add(new WorldEntity(Mth.lerp(pt, e.xo, e.getX()), Mth.lerp(pt, e.yo, e.getY()), Mth.lerp(pt, e.zo, e.getZ()),
                    e.getBbHeight(), ItemRef21.of(((ItemEntity) e).getItem()), 0));
        }
        return out;
    }

    @Override
    public int[] spawnSpots(int radius, int height) {
        ClientLevel level = mc().level;
        if (level == null || player() == null) return new int[0];
        BlockPos c = player().blockPosition();
        int[] buf = new int[1024 * 5];
        int n = 0;
        BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos(), air = new BlockPos.MutableBlockPos(),
                head = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int dy = -height; dy <= height; dy++) {
                    floor.set(c.getX() + dx, c.getY() + dy - 1, c.getZ() + dz);
                    BlockState f = level.getBlockState(floor);
                    if (f.isAir() || !f.isValidSpawn(level, floor, EntityType.ZOMBIE)) continue;
                    air.set(floor.getX(), floor.getY() + 1, floor.getZ());
                    head.set(floor.getX(), floor.getY() + 2, floor.getZ());
                    BlockState a = level.getBlockState(air), h = level.getBlockState(head);
                    if (!NaturalSpawner.isValidEmptySpawnBlock(level, air, a, a.getFluidState(), EntityType.ZOMBIE)) continue;
                    if (!NaturalSpawner.isValidEmptySpawnBlock(level, head, h, h.getFluidState(), EntityType.ZOMBIE)) continue;
                    if (n + 5 > buf.length) buf = java.util.Arrays.copyOf(buf, buf.length * 2);
                    buf[n++] = air.getX();
                    buf[n++] = air.getY();
                    buf[n++] = air.getZ();
                    buf[n++] = level.getBrightness(LightLayer.BLOCK, air);
                    buf[n++] = level.getBrightness(LightLayer.SKY, air);
                }
        return java.util.Arrays.copyOf(buf, n);
    }
}
