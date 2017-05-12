package de.themoep.inventorygui;

/*
 * Copyright 2017 Max Lee (https://github.com/Phoenix616/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InventoryGui implements Listener {

    private final static int[] ROW_WIDTHS = {3, 5, 9};
    private final static InventoryType[] INVENTORY_TYPES = {
            InventoryType.DROPPER, // 3*3
            InventoryType.ANVIL, // 5*1
            InventoryType.CHEST // 9*x
    };

    private final static Map<String, InventoryGui> GUI_MAP = new HashMap<>();

    private final JavaPlugin plugin;
    private String title;
    private final char[] slots;
    private final Map<Character, GuiElement> elements = new HashMap<>();
    private InventoryType inventoryType;
    private Inventory inventory = null;
    private InventoryHolder owner;
    private boolean listenersRegistered = false;

    public InventoryGui(JavaPlugin plugin, InventoryHolder owner, String title, String[] rows, GuiElement... elements) {
        this.plugin = plugin;
        this.owner = owner;
        this.title = title;

        int width = ROW_WIDTHS[0];
        for (String row : rows) {
            if (row.length() > width) {
                width = row.length();
            }
        }
        for (int i = 0; i < ROW_WIDTHS.length && i < INVENTORY_TYPES.length; i++) {
            if (width < ROW_WIDTHS[i]) {
                width = ROW_WIDTHS[i];
            }
            if (width == ROW_WIDTHS[i]) {
                inventoryType = INVENTORY_TYPES[i];
                break;
            }
        }

        StringBuilder slotsBuilder = new StringBuilder();
        for (String row : rows) {
            if (row.length() < width) {
                double side = (width - row.length()) / 2;
                for (int i = 0; i < Math.floor(side); i++) {
                    slotsBuilder.append(" ");
                }
                slotsBuilder.append(row);
                for (int i = 0; i < Math.ceil(side); i++) {
                    slotsBuilder.append(" ");
                }
            } else if (row.length() == width) {
                slotsBuilder.append(row);
            } else {
                slotsBuilder.append(row.substring(0, width));
            }
        }
        slots = slotsBuilder.toString().toCharArray();

        addElements(elements);
    }

    public InventoryGui(JavaPlugin plugin, String title, String[] rows, GuiElement... elements) {
        this(plugin, null, title, rows, elements);
    }

    public InventoryGui(JavaPlugin plugin, InventoryHolder owner, String title, String[] rows, Collection<GuiElement> elements) {
        this(plugin, owner, title, rows);
        addElements(elements);
    }

    public void addElement(GuiElement element) {
        elements.put(element.getSlotChar(), element);
        element.setSlots(getSlots(element.getSlotChar()));
    }

    private int[] getSlots(char slotChar) {
        ArrayList<Integer> slotList = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slotChar) {
                slotList.add(i);
            }
        }
        return slotList.stream().mapToInt(Integer::intValue).toArray();
    }

    public void addElement(char slotChar, ItemStack item, GuiElement.Action action) {
        addElement(new GuiStaticElement(slotChar, item, action));
    }

    public void addElement(char slotChar, ItemStack item) {
        addElement(new GuiStaticElement(slotChar, item, null));
    }

    private void addElement(char slotChar, MaterialData materialData, GuiElement.Action action) {
        addElement(slotChar, materialData.toItemStack(1), action);
    }

    public void addElement(char slotChar, Material material, byte data, GuiElement.Action action) {
        addElement(slotChar, new MaterialData(material, data), action);
    }

    public void addElement(char slotChar, Material material, GuiElement.Action action) {
        addElement(slotChar, material, (byte) 0, action);
    }

    public void addElements(GuiElement... elements) {
        for (GuiElement element : elements) {
            addElement(element);
        }
    }

    public void addElements(Collection<GuiElement> elements) {
        for (GuiElement element : elements) {
            addElement(element);
        }
    }

    /**
     * Set the filler element for empty slots
     * @param item  The item for the filler element
     */
    public void setFiller(ItemStack item) {
        addElement(new GuiStaticElement(' ', item, " "));
    }

    /**
     * Get the filler element
     * @return  The filler element for empty slots
     */
    public GuiElement getFiller() {
        return elements.get(' ');
    }

    private void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        listenersRegistered = true;
    }

    private void unregisterListeners() {
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
    }

    /**
     * Show this GUI to a player
     * @param player    The Player to show the GUI to
     */
    public void show(Player player) {
        if (inventory == null) {
            build();
        }
        draw();
        player.openInventory(inventory);
    }

    public void build() {
        build(owner);
    }

    public void build(InventoryHolder owner) {
        if (slots.length > inventoryType.getDefaultSize()) {
            inventory = plugin.getServer().createInventory(owner, slots.length, title);
        } else {
            inventory = plugin.getServer().createInventory(owner, inventoryType, title);
        }
        setOwner(owner);
        registerListeners();
    }

    /**
     * Draw the elements in the inventory
     */
    public void draw() {
        for (int i = 0; i < inventory.getSize(); i++) {
            GuiElement element = getElement(i);
            if (element == null) {
                element = getFiller();
            }
            if (element != null) {
                inventory.setItem(i, element.getItem(i));
            }
        }
    }

    /**
     * Destroy this GUI. This unregisters all listeners and removes it from the GUI_MAP
     */
    public void destroy() {
        unregisterListeners();
        removeFromMap();
    }

    /**
     * Get element in a certain slot
     * @param slot  The slot to get the element from
     * @return      The GuiElement or <tt>null</tt> if the slot is empty/there wasn't one
     */
    private GuiElement getElement(int slot) {
        return slot < 0 || slot >= slots.length ? null : elements.get(slots[slot]);
    }

    private void setOwner(InventoryHolder owner) {
        removeFromMap();
        this.owner = owner;
        if (owner instanceof Entity) {
            GUI_MAP.put(((Entity) owner).getUniqueId().toString(), this);
        } else if (owner instanceof BlockState) {
            GUI_MAP.put(((BlockState) owner).getLocation().toString(), this);
        }
    }

    private void removeFromMap() {
        if (owner == null) {
            return;
        }
        if (owner instanceof Entity) {
            GUI_MAP.remove(((Entity) owner).getUniqueId().toString(), this);
        } else if (owner instanceof BlockState) {
            GUI_MAP.remove(((BlockState) owner).getLocation().toString(), this);
        }
    }

    /**
     * Get the GUI registered to an InventoryHolder
     * @param holder    The InventoryHolder to get the GUI for
     * @return          The InventoryGui registered to it or <tt>null</tt> if none was registered to it
     */
    public static InventoryGui get(InventoryHolder holder) {
        if (holder instanceof Entity) {
            return GUI_MAP.get(((Entity) holder).getUniqueId().toString());
        } else if (holder instanceof BlockState) {
            return GUI_MAP.get(((BlockState) holder).getLocation().toString());
        }
        return null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (inventory.getViewers().contains(event.getWhoClicked())) {
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
                return;
            }
            GuiElement element = getElement(event.getRawSlot());
            GuiElement.Action action = null;
            if (element != null) {
                action = element.getAction();
            }
            if (action == null || action.onClick(new GuiElement.Click(this, event.getRawSlot(), element, event.getClick(), event))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (inventory.getViewers().contains(event.getWhoClicked()) && containsBelow(event.getRawSlots(), inventory.getSize())) {
            event.setCancelled(true);
        }
    }

    private boolean containsBelow(Set<Integer> rawSlots, int maxSlot) {
        for (int i : rawSlots) {
            if (i < maxSlot) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (inventory.getViewers().contains(event.getPlayer())) {
            if (event.getViewers().size() <= 1) {
                destroy();
            }
        }
    }

    public static void setItemText(ItemStack item, String... text) {
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (text != null && text.length > 0) {
                meta.setDisplayName(text[0]);
                if (text.length > 1) {
                    meta.setLore(Arrays.asList(Arrays.copyOfRange(text, 1, text.length)));
                } else {
                    meta.setLore(null);
                }
            } else {
                meta.setDisplayName(null);
            }
            item.setItemMeta(meta);
        }
    }
}
