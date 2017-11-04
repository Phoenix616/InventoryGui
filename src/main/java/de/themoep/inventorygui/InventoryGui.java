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
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The main library class that lets you create and manage your GUIs
 */
public class InventoryGui implements Listener {

    private final static int[] ROW_WIDTHS = {3, 5, 9};
    private final static InventoryType[] INVENTORY_TYPES = {
            InventoryType.DROPPER, // 3*3
            InventoryType.HOPPER, // 5*1
            InventoryType.CHEST // 9*x
    };

    private final static Map<String, InventoryGui> GUI_MAP = new HashMap<>();
    private final static Map<UUID, ArrayDeque<InventoryGui>> GUI_HISTORY = new HashMap<>();

    private final JavaPlugin plugin;
    private GuiListener listener = new GuiListener(this);
    private String title;
    private final char[] slots;
    private final Map<Character, GuiElement> elements = new HashMap<>();
    private InventoryType inventoryType;
    private Inventory inventory = null;
    private InventoryHolder owner = null;
    private boolean listenersRegistered = false;
    private int pageNumber = 0;
    private int pageAmount = 1;

    /**
     * Create a new gui with a certain setup and some elements
     * @param plugin    Your plugin
     * @param owner     The holder that owns this gui to retrieve it with {@link #get(InventoryHolder)}.
     *                  Can be <tt>null</tt>.
     * @param title     The name of the GUI. This will be the title of the inventory.
     * @param rows      How your rows are setup. Each element is getting assigned to a character.
     *                  Empty/missing ones get filled with the Filler.
     * @param elements  The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     */
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

    /**
     * The simplest way to create a new gui. It has no owner and elements are optional.
     * @param plugin    Your plugin
     * @param title     The name of the GUI. This will be the title of the inventory.
     * @param rows      How your rows are setup. Each element is getting assigned to a character.
     *                  Empty/missing ones get filled with the Filler.
     * @param elements  The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     */
    public InventoryGui(JavaPlugin plugin, String title, String[] rows, GuiElement... elements) {
        this(plugin, null, title, rows, elements);
    }

    /**
     * Create a new gui that has no owner with a certain setup and some elements
     * @param plugin    Your plugin
     * @param owner     The holder that owns this gui to retrieve it with {@link #get(InventoryHolder)}.
     *                  Can be <tt>null</tt>.
     * @param title     The name of the GUI. This will be the title of the inventory.
     * @param rows      How your rows are setup. Each element is getting assigned to a character.
     *                  Empty/missing ones get filled with the Filler.
     * @param elements  The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     */
    public InventoryGui(JavaPlugin plugin, InventoryHolder owner, String title, String[] rows, Collection<GuiElement> elements) {
        this(plugin, owner, title, rows);
        addElements(elements);
    }

    /**
     * Add an element to the gui
     * @param element   The {@link GuiElement} to add
     */
    public void addElement(GuiElement element) {
        elements.put(element.getSlotChar(), element);
        element.setGui(this);
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

    /**
     * Create and add a {@link GuiStaticElement} in one quick method.
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item that should be displayed
     * @param action    The {@link de.themoep.inventorygui.GuiElement.Action} to run when the player clicks on this element
     */
    public void addElement(char slotChar, ItemStack item, GuiElement.Action action) {
        addElement(new GuiStaticElement(slotChar, item, action));
    }

    /**
     * Create and add a {@link GuiStaticElement} that has no action.
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item that should be displayed
     */
    public void addElement(char slotChar, ItemStack item) {
        addElement(new GuiStaticElement(slotChar, item, null));
    }

    /**
     * Create and add a {@link GuiStaticElement} in one quick method.
     * @param slotChar      The character to replace in the gui setup string
     * @param materialData  The {@link MaterialData} of the item of tihs element
     * @param action         The {@link de.themoep.inventorygui.GuiElement.Action} to run when the player clicks on this element
     */
    public void addElement(char slotChar, MaterialData materialData, GuiElement.Action action) {
        addElement(slotChar, materialData.toItemStack(1), action);
    }

    /**
     * Create and add a {@link GuiStaticElement}
     * @param slotChar  The character to replace in the gui setup string
     * @param material  The {@link Material} that the item should have
     * @param data      The <tt>byte</tt> representation of the material data of this element
     * @param action    The {@link GuiElement.Action} to run when the player clicks on this element
     */
    public void addElement(char slotChar, Material material, byte data, GuiElement.Action action) {
        addElement(slotChar, new MaterialData(material, data), action);
    }

    /**
     * Create and add a {@link GuiStaticElement}
     * @param slotChar  The character to replace in the gui setup string
     * @param material  The {@link Material} that the item should have
     * @param action    The {@link GuiElement.Action} to run when the player clicks on this element
     */
    public void addElement(char slotChar, Material material, GuiElement.Action action) {
        addElement(slotChar, material, (byte) 0, action);
    }

    /**
     * Add multiple elements to the gui
     * @param elements   The {@link GuiElement}s to add
     */
    public void addElements(GuiElement... elements) {
        for (GuiElement element : elements) {
            addElement(element);
        }
    }

    /**
     * Add multiple elements to the gui
     * @param elements   The {@link GuiElement}s to add
     */
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

    /**
     * Get the number of the page that this gui is on. zero indexed. Only affects group elements.
     * @return The page number
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Set the number of the page that this gui is on. zero indexed. Only affects group elements.
     */
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        draw();
    }

    /**
     * Get the amount of pages that this GUI has
     * @return The amount of pages
     */
    public int getPageAmount() {
        return pageAmount;
    }

    private void calculatePageAmount() {
        for (GuiElement element : elements.values()) {
            int amount = 0;
            if (element instanceof GuiElementGroup) {
                amount = ((GuiElementGroup) element).size();
            } else if (element instanceof GuiStorageElement) {
                amount = ((GuiStorageElement) element).getStorage().getSize();
            }
            if (amount > 0 && (pageAmount - 1) * element.slots.length < amount) {
                pageAmount = (int) Math.ceil((double) amount / element.slots.length);
            }
        }
    }

    private void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listenersRegistered = true;
    }

    private void unregisterListeners() {
        InventoryClickEvent.getHandlerList().unregister(listener);
        InventoryCloseEvent.getHandlerList().unregister(listener);
        InventoryDragEvent.getHandlerList().unregister(listener);
        InventoryMoveItemEvent.getHandlerList().unregister(listener);
        BlockDispenseEvent.getHandlerList().unregister(listener);
        BlockBreakEvent.getHandlerList().unregister(listener);
        EntityDeathEvent.getHandlerList().unregister(listener);
        listenersRegistered = false;
    }

    /**
     * Show this GUI to a player
     * @param player    The Player to show the GUI to
     */
    public void show(HumanEntity player) {
        show(player, true);
    }
    
    /**
     * Show this GUI to a player
     * @param player    The Player to show the GUI to
     * @param checkOpen Whether or not it should check if this gui is already open
     */
    public void show(HumanEntity player, boolean checkOpen) {
        draw();
        if (!checkOpen || !this.equals(getOpen(player))) {
            if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                // If the player already has a gui open then we assume that the call was from that gui.
                // In order to not close it in a InventoryClickEvent listener (which will lead to errors)
                // we delay the opening for one tick to run after it finished processing the event
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    addHistory(player, this);
                    player.openInventory(inventory);
                });
            } else {
                clearHistory(player);
                addHistory(player, this);
                player.openInventory(inventory);
            }
        }
    }

    /**
     * Build the gui
     */
    public void build() {
        build(owner);
    }

    /**
     * Set the gui's owner and build it
     * @param owner     The {@link InventoryHolder} that owns the gui
     */
    public void build(InventoryHolder owner) {
        if (slots.length > inventoryType.getDefaultSize()) {
            inventory = plugin.getServer().createInventory(owner, slots.length, replaceVars(title));
        } else {
            inventory = plugin.getServer().createInventory(owner, inventoryType, replaceVars(title));
        }
        setOwner(owner);
        registerListeners();
        calculatePageAmount();
    }

    /**
     * Draw the elements in the inventory. This can be used to manually refresh the gui.
     */
    public void draw() {
        if (inventory == null) {
            build();
        } else {
            inventory.clear();
        }
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
     * Closes the GUI for everyone viewing it
     */
    public void close() {
        close(true);
    }
    
    /**
     * Close the GUI for everyone viewing it
     * @param clearHistory  Whether or not to close the GUI completely (by clearing the history)
     */
    public void close(boolean clearHistory) {
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
            if (clearHistory) {
                clearHistory(viewer);
            }
            viewer.closeInventory();
        }
    }

    /**
     * Destroy this GUI. This unregisters all listeners and removes it from the GUI_MAP
     */
    public void destroy() {
        destroy(true);
    }

    private void destroy(boolean closeInventories) {
        if (closeInventories) {
            close();
        }
        inventory.clear();
        inventory = null;
        unregisterListeners();
        removeFromMap();
    }

    /**
     * Add a new history entry to the end of the history
     * @param player    The player to add the history entry for
     * @param gui       The GUI to add to the history
     */
    public static void addHistory(HumanEntity player, InventoryGui gui) {
        GUI_HISTORY.putIfAbsent(player.getUniqueId(), new ArrayDeque<>());
        Deque<InventoryGui> history = getHistory(player);
        if (history.peekLast() != gui) {
            history.add(gui);
        }
    }

    /**
     * Get the history of a player
     * @param player    The player to get the history for
     * @return          The history as a deque of InventoryGuis;
     *                  returns an empty one and not <tt>null</tt>!
     */
    public static Deque<InventoryGui> getHistory(HumanEntity player) {
        return GUI_HISTORY.getOrDefault(player.getUniqueId(), new ArrayDeque<>());
    }

    /**
     * Go back one entry in the history
     * @param player    The player to show the previous gui to
     * @return          <tt>true</tt> if there was a gui to show; <tt>false</tt> if not
     */
    public static boolean goBack(HumanEntity player) {
        Deque<InventoryGui> history = getHistory(player);
        history.pollLast();
        if (history.isEmpty()) {
            return false;
        }
        InventoryGui previous = history.peekLast();
        if (previous != null) {
            previous.show(player, false);
        }
        return true;
    }

    /**
     * Clear the history of a player
     * @param player    The player to clear the history for
     * @return          The history
     */
    public static Deque<InventoryGui> clearHistory(HumanEntity player) {
        if (GUI_HISTORY.containsKey(player.getUniqueId())) {
            return GUI_HISTORY.remove(player.getUniqueId());
        }
        return new ArrayDeque<>();
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

    /**
     * Get the GUI that a player has currently open
     * @param player    The Player to get the GUI for
     * @return          The InventoryGui that the player has open
     */
    public static InventoryGui getOpen(HumanEntity player) {
        return getHistory(player).peekLast();
    }

    /**
     * Get the title of the gui
     * @return  The title of the gui
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of the gui
     * @param title The {@link String} that should be the title of the gui
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Play a click sound e.g. when an element acts as a button
     */
    public void playClickSound() {
        for (HumanEntity humanEntity : inventory.getViewers()) {
            if (humanEntity instanceof Player) {
                ((Player) humanEntity).playSound(humanEntity.getEyeLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
            }
        }
    }

    /**
     * All the listeners that InventoryGui needs to work
     */
    public class GuiListener implements Listener {
        private final InventoryGui gui;

        public GuiListener(InventoryGui gui) {
            this.gui = gui;
        }

        @EventHandler
        private void onInventoryClick(InventoryClickEvent event) {
            if (inventory.getViewers().contains(event.getWhoClicked())) {
                if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                    event.setCancelled(true);
                    return;
                }

                int slot = -1;
                if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                    slot = event.getRawSlot();
                } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    slot = event.getInventory().firstEmpty();
                }

                if (slot >= 0) {
                    GuiElement element = getElement(slot);
                    GuiElement.Action action = null;
                    if (element != null) {
                        action = element.getAction();
                    }
                    if (action == null || action.onClick(new GuiElement.Click(gui, slot, element, event.getClick(), event))) {
                        event.setCancelled(true);
                    }
                }
            } else if (owner != null && owner.equals(event.getInventory().getHolder())) {
                // Click into inventory by same owner but not the inventory of the GUI
                // Assume that the underlying inventory changed and redraw the GUI
                plugin.getServer().getScheduler().runTask(plugin, gui::draw);
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

        @EventHandler(priority = EventPriority.MONITOR)
        public void onInventoryClose(InventoryCloseEvent event) {
            if (event.getInventory().equals(gui.inventory)) {
                // go back. that checks if the player is in gui and has history
                if (gui.equals(getOpen(event.getPlayer()))) {
                    goBack(event.getPlayer());
                }
                if (inventory.getViewers().size() <= 1) {
                    destroy(false);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onInventoryMoveItem(InventoryMoveItemEvent event) {
            if (owner != null && (owner.equals(event.getDestination().getHolder()) || owner.equals(event.getSource().getHolder()))) {
                plugin.getServer().getScheduler().runTask(plugin, gui::draw);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDispense(BlockDispenseEvent event) {
            if (owner != null && owner.equals(event.getBlock().getState())) {
                plugin.getServer().getScheduler().runTask(plugin, gui::draw);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (owner != null && owner.equals(event.getBlock().getState())) {
                destroy();
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDeath(EntityDeathEvent event) {
            if (owner != null && owner.equals(event.getEntity())) {
                destroy();
            }
        }
    }

    /**
     * Set the text of an item using the display name and the lore.
     * Also replaces any placeholders in the text and filters out empty lines.
     * Use a single space to create an emtpy line.
     * @param item  The {@link ItemStack} to set the text for
     * @param text  The text lines to set
     */
    public void setItemText(ItemStack item, String... text) {
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (text != null && text.length > 0) {
                String combined = replaceVars(Arrays.stream(text)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("\n")));
                String[] lines = combined.split("\n");
                meta.setDisplayName(lines[0]);
                if (lines.length > 1) {
                    meta.setLore(Arrays.asList(Arrays.copyOfRange(lines, 1, lines.length)));
                } else {
                    meta.setLore(null);
                }
            } else {
                meta.setDisplayName(null);
            }
            item.setItemMeta(meta);
        }
    }

    /**
     * Replace some placeholders in the with values regarding the gui's state.<br>
     * The placeholders are:<br>
     * <tt>%plugin%</tt>    - The name of the plugin that this gui is from.<br>
     * <tt>%owner%</tt>     - The name of the owner of this gui. Will be an empty string when the owner is null.<br>
     * <tt>%page%</tt>      - The current page that this gui is on.<br>
     * <tt>%nextpage%</tt>  - The next page. "none" if there is no next page.<br>
     * <tt>%prevpage%</tt>  - The previous page. "none" if there is no previous page.<br>
     * <tt>%pages%</tt>     - The amount of pages that this gui has.
     * @param text  The text to replace the placeholders in
     * @return      The text with all placeholders replaced
     */
    public String replaceVars(String text) {
        String[] repl = {
                "plugin", plugin.getName(),
                "owner", owner != null ? owner.getInventory().getName() : "",
                "page", String.valueOf(getPageNumber() + 1),
                "nextpage", getPageNumber() + 1 < getPageAmount() ? String.valueOf(getPageNumber() + 2) : "none",
                "prevpage", getPageNumber() > 0 ? String.valueOf(getPageNumber()) : "none",
                "pages", String.valueOf(getPageAmount())
        };
        for (int i = 0; i + 1 < repl.length; i+=2) {
            text = text.replace("%" + repl[i] + "%", repl[i + 1]);
        }
        return text;
    }
}
