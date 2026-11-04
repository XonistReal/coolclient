package dev.coolclient.v1_8_9;

import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GameState;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.util.Keys;
import dev.coolclient.v1_8_9.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.ResourcePackLoader;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class GameState189 implements GameState {
    private final Map<String, ItemRef> iconCache = new HashMap<String, ItemRef>();

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    private static ClientPlayerEntity player() { return mc().player; }

    @Override
    public boolean inWorld() { return mc().world != null && mc().player != null; }

    @Override
    public int fps() { return MinecraftClient.getCurrentFps(); }

    @Override
    public int ping() {
        ClientPlayerEntity p = player();
        if (p == null || mc().getNetworkHandler() == null) return -1;
        PlayerListEntry e = mc().getNetworkHandler().getPlayerListEntry(p.getUuid());
        return e == null ? -1 : e.getLatency();
    }

    @Override
    public String serverAddress() {
        return mc().getCurrentServerEntry() == null ? null : mc().getCurrentServerEntry().address;
    }

    @Override
    public String selfName() { return mc().getSession().getUsername(); }

    @Override
    public UUID selfUuid() {
        if (player() != null) return player().getUuid();
        try {
            return mc().getSession().getProfile().getId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override public double x() { return player().x; }

    @Override public double y() { return player().y; }

    @Override public double z() { return player().z; }

    @Override public float yaw() { return player().yaw; }

    @Override public float pitch() { return player().pitch; }

    @Override
    public String biome() {
        try {
            return mc().world.getBiome(new BlockPos(player())).name;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public String dimension() {
        int d = player() == null ? 0 : player().dimension;
        return d == -1 ? "the_nether" : (d == 1 ? "the_end" : "overworld");
    }

    @Override
    public long worldTime() { return mc().world == null ? 0 : mc().world.getTimeOfDay(); }

    @Override public float health() { return player().getHealth(); }

    @Override public float maxHealth() { return player().getMaxHealth(); }

    @Override public int food() { return player().getHungerManager().getFoodLevel(); }

    @Override public float saturation() { return player().getHungerManager().getSaturationLevel(); }

    @Override
    public ItemRef armor(int slot) {
        if (player() == null) return ItemRef.EMPTY;
        return ItemRef189.of(player().inventory.armor[slot]);
    }

    @Override
    public ItemRef mainHand() {
        return player() == null ? ItemRef.EMPTY : ItemRef189.of(player().inventory.getMainHandStack());
    }

    @Override
    public ItemRef offHand() { return ItemRef.EMPTY; }

    @Override
    public int countItems(String... ids) {
        PlayerEntity p = player();
        if (p == null) return 0;
        List<Item> items = new ArrayList<Item>();
        for (String id : ids) {
            Item it = ItemRef189.item(id);
            if (it != null) items.add(it);
        }
        int total = 0;
        for (ItemStack s : p.inventory.main) {
            if (s != null && items.contains(s.getItem())) total += s.count;
        }
        return total;
    }

    @Override
    public ItemRef itemOf(String id) {
        ItemRef r = iconCache.get(id);
        if (r == null) {
            Item it = ItemRef189.item(id);
            r = it == null ? ItemRef.EMPTY : ItemRef189.of(new ItemStack(it));
            iconCache.put(id, r);
        }
        return r;
    }

    @Override
    public List<EffectInfo> effects() {
        PlayerEntity p = player();
        if (p == null) return Collections.emptyList();
        List<EffectInfo> out = new ArrayList<EffectInfo>();
        for (StatusEffectInstance inst : p.getStatusEffectInstances()) {
            StatusEffect e = StatusEffect.STATUS_EFFECTS[inst.getEffectId()];
            if (e == null) continue;
            Integer icon = e.hasIcon() ? e.getIconLevel() : null;
            out.add(new EffectInfo(e.getTranslationKey(), I18n.translate(e.getTranslationKey()), inst.getAmplifier(),
                    inst.getDuration(), !e.isNegative(), icon));
        }
        return out;
    }

    @Override
    public String resourcePack() {
        List<ResourcePackLoader.Entry> packs = mc().getResourcePackLoader().getSelectedResourcePacks();
        if (packs.isEmpty()) return null;
        String n = packs.get(0).getName();
        return n.endsWith(".zip") ? n.substring(0, n.length() - 4) : n;
    }

    @Override public float cooldown(String itemId) { return 0f; }

    @Override public boolean sprinting() { return player() != null && player().isSprinting(); }

    @Override public boolean sneaking() { return player() != null && player().isSneaking(); }

    @Override public boolean thirdPerson() { return mc().options.perspective != 0; }

    @Override public boolean hudHidden() { return mc().options.hudHidden; }

    @Override public boolean debugOpen() { return mc().options.debugEnabled; }

    private static KeyBinding binding(String name) {
        switch (name) {
            case "forward": return mc().options.forwardKey;
            case "left": return mc().options.leftKey;
            case "back": return mc().options.backKey;
            case "right": return mc().options.rightKey;
            case "jump": return mc().options.jumpKey;
            case "sprint": return mc().options.sprintKey;
            case "sneak": return mc().options.sneakKey;
            case "attack": return mc().options.attackKey;
            case "use": return mc().options.useKey;
            default: return null;
        }
    }

    @Override
    public int bindingKey(String name) {
        KeyBinding k = binding(name);
        return k == null || k.getCode() == 0 ? Keys.NONE : KeyMap.bindingToCore(k.getCode());
    }

    @Override public int perspective() { return mc().options.perspective; }

    @Override public void setPerspective(int p) { mc().options.perspective = p; }

    @Override
    public void setSprintHeld(boolean held) { ((KeyBindingAccessor) mc().options.sprintKey).cool$setPressed(held); }

    @Override
    public void setSneakHeld(boolean held) { ((KeyBindingAccessor) mc().options.sneakKey).cool$setPressed(held); }

    @Override
    public void setUseHeld(boolean held) { ((KeyBindingAccessor) mc().options.useKey).cool$setPressed(held); }

    @Override
    public void setAttackHeld(boolean held) { ((KeyBindingAccessor) mc().options.attackKey).cool$setPressed(held); }

    @Override
    public List<UUID> loadedPlayers() {
        if (mc().world == null) return Collections.emptyList();
        List<UUID> out = new ArrayList<UUID>();
        for (PlayerEntity p : mc().world.playerEntities) out.add(p.getUuid());
        return out;
    }
}
