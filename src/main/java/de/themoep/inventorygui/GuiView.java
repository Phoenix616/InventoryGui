package de.themoep.inventorygui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

@ApiStatus.Internal
public class GuiView {

    private static final MethodHandle GET_TOP_INVENTORY = unreflect("getTopInventory");
    private static final MethodHandle GET_BOTTOM_INVENTORY = unreflect("getBottomInventory");
    private static final MethodHandle GET_PLAYER = unreflect("getPlayer");
    private static final MethodHandle GET_TYPE = unreflect("getType");
    private static final MethodHandle SET_ITEM = unreflect("setItem");
    private static final MethodHandle GET_ITEM = unreflect("getItem");
    private static final MethodHandle SET_CURSOR = unreflect("setCursor");
    private static final MethodHandle GET_CURSOR = unreflect("getCursor");
    private static final MethodHandle GET_INVENTORY = unreflect("getInventory");
    private static final MethodHandle CONVERT_SLOT = unreflect("convertSlot");
    private static final MethodHandle GET_SLOT_TYPE = unreflect("getSlotType");
    private static final MethodHandle CLOSE = unreflect("close");
    private static final MethodHandle COUNT_SLOTS = unreflect("countSlots");
    private static final MethodHandle SET_PROPERTY = unreflect("setProperty");
    private static final MethodHandle GET_TITLE = unreflect("getTitle");

    private static final WeakHashMap<InventoryView, GuiView> VIEWS = new WeakHashMap<>();

    public static GuiView of(InventoryView view) {
        return VIEWS.computeIfAbsent(view, k -> new GuiView(view));
    }

    private final InventoryView view;

    public GuiView(InventoryView view) {
        this.view = view;
    }

    public Inventory getTopInventory() {
        return invoke(GET_TOP_INVENTORY, view);
    }

    public Inventory getBottomInventory() {
        return invoke(GET_BOTTOM_INVENTORY, view);
    }

    public HumanEntity getPlayer() {
        return invoke(GET_PLAYER, view);
    }

    public InventoryType getType() {
        return invoke(GET_TYPE, view);
    }

    public void setItem(int slot, ItemStack item) {
        invokeVoid(SET_ITEM, view, slot, item);
    }

    public ItemStack getItem(int slot) {
        return invoke(GET_ITEM, view, slot);
    }

    public void setCursor(ItemStack item) {
        invokeVoid(SET_CURSOR, view, item);
    }

    public ItemStack getCursor() {
        return invoke(GET_CURSOR, view);
    }

    public Inventory getInventory(int rawSlot) {
        return invoke(GET_INVENTORY, view, rawSlot);
    }

    public int convertSlot(int slot) {
        return invoke(CONVERT_SLOT, view, slot);
    }

    public InventoryType.SlotType getSlotType(int slot) {
        return invoke(GET_SLOT_TYPE, view, slot);
    }

    public void close() {
        invokeVoid(CLOSE, view);
    }

    public int countSlots() {
        return invoke(COUNT_SLOTS, view);
    }

    public boolean setProperty(InventoryView.Property prop, int value) {
        return invoke(SET_PROPERTY, view, prop, value);
    }

    public String getTitle() {
        return invoke(GET_TITLE, view);
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(MethodHandle method, Object... args) {
        try {
            return (T) method.invoke(args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void invokeVoid(MethodHandle method, Object... args) {
        try {
            method.invoke(args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static MethodHandle unreflect(String name) {
        for (Method method : InventoryView.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                try {
                    return MethodHandles.lookup().unreflect(method);
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }
}
