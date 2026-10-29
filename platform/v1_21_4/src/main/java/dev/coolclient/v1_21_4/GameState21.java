package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GameState;
import dev.coolclient.core.api.ItemRef;
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
}
