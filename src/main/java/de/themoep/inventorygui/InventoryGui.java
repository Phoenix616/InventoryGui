package de.themoep.inventorygui;

/*
 * Copyright 2017 Max Lee (https://github.com/Phoenix616)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.PluginDisableEvent;
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The main library class that lets you create and manage your GUIs
 */
public class InventoryGui implements Listener {

    private final static int[] ROW_WIDTHS = {3, 5, 9};
    private final static InventoryType[] INVENTORY_TYPES = {
            InventoryType.DISPENSER, // 3*3
            InventoryType.HOPPER, // 5*1
            InventoryType.CHEST // 9*x
    };
    private final static Sound CLICK_SOUND;

    private final static Map<String, InventoryGui> GUI_MAP = new HashMap<>();
    private final static Map<UUID, ArrayDeque<InventoryGui>> GUI_HISTORY = new HashMap<>();

    private final static Map<String, Pattern> PATTERN_CACHE = new HashMap<>();

    private final JavaPlugin plugin;
    private final List<UnregisterableListener> listeners = new ArrayList<>();
    private final UnregisterableListener[] optionalListeners = new UnregisterableListener[]{
            new ItemSwapGuiListener(this)
    };
    private String title;
    private final char[] slots;
    private final Map<Character, GuiElement> elements = new HashMap<>();
    private InventoryType inventoryType;
    private Map<UUID, Inventory> inventories = new LinkedHashMap<>();
    private InventoryHolder owner = null;
    private boolean listenersRegistered = false;
    private Map<UUID, Integer> pageNumbers = new LinkedHashMap<>();
    private Map<UUID, Integer> pageAmounts = new LinkedHashMap<>();
    private GuiElement.Action outsideAction = click -> false;
    private CloseAction closeAction = close -> true;
    private boolean silent = false;
    
    static {
        // Sound names changed, make it compatible with both versions
        Sound clickSound = null;
        String[] clickSounds = {"UI_BUTTON_CLICK", "CLICK"};
        for (String s : clickSounds) {
            try {
                clickSound = Sound.valueOf(s.toUpperCase());
                break;
            } catch (IllegalArgumentException ignored) {}
        }
        if (clickSound == null) {
            for (Sound sound : Sound.values()) {
                if (sound.name().contains("CLICK")) {
                    clickSound = sound;
                    break;
                }
            }
        }
        CLICK_SOUND = clickSound;
    }

    /**
     * Create a new gui with a certain setup and some elements
     * @param plugin    Your plugin
     * @param owner     The holder that owns this gui to retrieve it with {@link #get(InventoryHolder)}.
     *                  Can be <code>null</code>.
     * @param title     The name of the GUI. This will be the title of the inventory.
     * @param rows      How your rows are setup. Each element is getting assigned to a character.
     *                  Empty/missing ones get filled with the Filler.
     * @param elements  The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     * @throws IllegalArgumentException Thrown when the provided rows cannot be matched to an InventoryType
     */
    public InventoryGui(JavaPlugin plugin, InventoryHolder owner, String title, String[] rows, GuiElement... elements) {
        this.plugin = plugin;
        this.owner = owner;
        this.title = title;
        listeners.add(new GuiListener(this));
        for (UnregisterableListener listener : optionalListeners) {
            if (listener.isCompatible()) {
                listeners.add(listener);
            }
        }

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
        if (inventoryType == null) {
            throw new IllegalArgumentException("Could not match row setup to an inventory type!");
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
     * @throws IllegalArgumentException Thrown when the provided rows cannot be matched to an InventoryType
     */
    public InventoryGui(JavaPlugin plugin, String title, String[] rows, GuiElement... elements) {
        this(plugin, null, title, rows, elements);
    }

    /**
     * Create a new gui that has no owner with a certain setup and some elements
     * @param plugin    Your plugin
     * @param owner     The holder that owns this gui to retrieve it with {@link #get(InventoryHolder)}.
     *                  Can be <code>null</code>.
     * @param title     The name of the GUI. This will be the title of the inventory.
     * @param rows      How your rows are setup. Each element is getting assigned to a character.
     *                  Empty/missing ones get filled with the Filler.
     * @param elements  The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     * @throws IllegalArgumentException Thrown when the provided rows cannot be matched to an InventoryType
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
     * Create and add a {@link StaticGuiElement} in one quick method.
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item that should be displayed
     * @param action    The {@link de.themoep.inventorygui.GuiElement.Action} to run when the player clicks on this element
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public void addElement(char slotChar, ItemStack item, GuiElement.Action action, String... text) {
        addElement(new StaticGuiElement(slotChar, item, action, text));
    }

    /**
     * Create and add a {@link StaticGuiElement} that has no action.
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item that should be displayed
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public void addElement(char slotChar, ItemStack item, String... text) {
        addElement(new StaticGuiElement(slotChar, item, null, text));
    }

    /**
     * Create and add a {@link StaticGuiElement} in one quick method.
     * @param slotChar      The character to replace in the gui setup string
     * @param materialData  The {@link MaterialData} of the item of tihs element
     * @param action         The {@link de.themoep.inventorygui.GuiElement.Action} to run when the player clicks on this element
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public void addElement(char slotChar, MaterialData materialData, GuiElement.Action action, String... text) {
        addElement(slotChar, materialData.toItemStack(1), action, text);
    }

    /**
     * Create and add a {@link StaticGuiElement}
     * @param slotChar  The character to replace in the gui setup string
     * @param material  The {@link Material} that the item should have
     * @param data      The <code>byte</code> representation of the material data of this element
     * @param action    The {@link GuiElement.Action} to run when the player clicks on this element
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public void addElement(char slotChar, Material material, byte data, GuiElement.Action action, String... text) {
        addElement(slotChar, new MaterialData(material, data), action, text);
    }

    /**
     * Create and add a {@link StaticGuiElement}
     * @param slotChar  The character to replace in the gui setup string
     * @param material  The {@link Material} that the item should have
     * @param action    The {@link GuiElement.Action} to run when the player clicks on this element
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public void addElement(char slotChar, Material material, GuiElement.Action action, String... text) {
        addElement(slotChar, material, (byte) 0, action, text);
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
     * Remove a specific element from this gui.
     * @param element   The element to remove
     * @return Whether or not the gui contained this element and if it was removed
     */
    public boolean removeElement(GuiElement element) {
        return elements.remove(element.getSlotChar(), element);
    }

    /**
     * Remove the element that is currently assigned to a specific slot char
     * @param slotChar  The char of the slot
     * @return The element which was in that slot or <code>null</code> if there was none
     */
    public GuiElement removeElement(char slotChar) {
        return elements.remove(slotChar);
    }

    /**
     * Set the filler element for empty slots
     * @param item  The item for the filler element
     */
    public void setFiller(ItemStack item) {
        addElement(new StaticGuiElement(' ', item, " "));
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
     * @deprecated Use {@link #getPageNumber(HumanEntity)}
     */
    @Deprecated
    public int getPageNumber() {
        return getPageNumber(null);
    }

    /**
     * Get the number of the page that this gui is on. zero indexed. Only affects group elements.
     * @param player    The Player to query the page number for
     * @return The page number
     */
    public int getPageNumber(HumanEntity player) {
        return player != null
                ? pageNumbers.getOrDefault(player.getUniqueId(), 0)
                : pageNumbers.isEmpty() ? 0 : pageNumbers.values().iterator().next();
    }

    /**
     * Set the number of the page that this gui is on for all players. zero indexed. Only affects group elements.
     * @param pageNumber The page number to set
     */
    public void setPageNumber(int pageNumber) {
        for (UUID playerId : inventories.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                setPageNumber(player, pageNumber);
            }
        }
    }

    /**
     * Set the number of the page that this gui is on for a player. zero indexed. Only affects group elements.
     * @param player        The player to set the page number for
     * @param pageNumber    The page number to set
     */
    public void setPageNumber(HumanEntity player, int pageNumber) {
        setPageNumberInternal(player, pageNumber);
        draw(player, false);
    }

    private void setPageNumberInternal(HumanEntity player, int pageNumber) {
        pageNumbers.put(player.getUniqueId(), pageNumber > 0 ? pageNumber : 0);
    }

    /**
     * Get the amount of pages that this GUI has
     * @return The amount of pages
     * @deprecated Use {@link #getPageAmount(HumanEntity)}
     */
    @Deprecated
    public int getPageAmount() {
        return getPageAmount(null);
    }

    /**
     * Get the amount of pages that this GUI has for a certain player
     * @param player    The Player to query the page amount for
     * @return The amount of pages
     */
    public int getPageAmount(HumanEntity player) {
        return player != null
                ? pageAmounts.getOrDefault(player.getUniqueId(), 1)
                : pageAmounts.isEmpty() ? 1 : pageAmounts.values().iterator().next();
    }

    /**
     * Set the amount of pages that this GUI has for a certain player
     * @param player        The Player to query the page amount for
     * @param pageAmount    The page amount
     */
    private void setPageAmount(HumanEntity player, int pageAmount) {
        pageAmounts.put(player.getUniqueId(), pageAmount);
    }

    private void calculatePageAmount(HumanEntity player) {
        int pageAmount = 0;
        for (GuiElement element : elements.values()) {
            int amount = calculateElementSize(player, element);
            if (amount > 0 && (pageAmount - 1) * element.getSlots().length < amount) {
                pageAmount = (int) Math.ceil((double) amount / element.getSlots().length);
            }
        }
        setPageAmount(player, pageAmount);
        if (getPageNumber(player) >= pageAmount) {
            setPageNumberInternal(player, pageAmount - 1);
        }
    }

    private int calculateElementSize(HumanEntity player, GuiElement element) {
        if (element instanceof GuiElementGroup) {
            return ((GuiElementGroup) element).size();
        } else if (element instanceof GuiStorageElement) {
            return ((GuiStorageElement) element).getStorage().getSize();
        } else if (element instanceof DynamicGuiElement) {
            return calculateElementSize(player, ((DynamicGuiElement) element).getCachedElement(player));
        }
        return 0;
    }

    private void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        for (UnregisterableListener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
        listenersRegistered = true;
    }

    private void unregisterListeners() {
        for (UnregisterableListener listener : listeners) {
            listener.unregister();
        }
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
        draw(player);
        if (!checkOpen || !this.equals(getOpen(player))) {
            if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                // If the player already has a gui open then we assume that the call was from that gui.
                // In order to not close it in a InventoryClickEvent listener (which will lead to errors)
                // we delay the opening for one tick to run after it finished processing the event
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Inventory inventory = getInventory(player);
                    if (inventory != null) {
                        addHistory(player, this);
                        player.openInventory(inventory);
                    }
                });
            } else {
                Inventory inventory = getInventory(player);
                if (inventory != null) {
                    clearHistory(player);
                    addHistory(player, this);
                    player.openInventory(inventory);
                }
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
        setOwner(owner);
        registerListeners();
    }

    /**
     * Draw the elements in the inventory. This can be used to manually refresh the gui. Updates any dynamic elements.
     */
    public void draw() {
        for (UUID playerId : inventories.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                draw(player);
            }
        }
    }

    /**
     * Draw the elements in the inventory. This can be used to manually refresh the gui. Updates any dynamic elements.
     * @param who For who to draw the GUI
     */
    public void draw(HumanEntity who) {
        draw(who, true);
    }

    /**
     * Draw the elements in the inventory. This can be used to manually refresh the gui.
     * @param who           For who to draw the GUI
     * @param updateDynamic Update dynamic elements
     */
    public void draw(HumanEntity who, boolean updateDynamic) {
        if (updateDynamic) {
            updateElements(who, elements.values());
        }
        calculatePageAmount(who);
        Inventory inventory = getInventory(who);
        if (inventory == null) {
            build();
            if (slots.length != inventoryType.getDefaultSize()) {
                inventory = plugin.getServer().createInventory(new Holder(this), slots.length, replaceVars(who, title));
            } else {
                inventory = plugin.getServer().createInventory(new Holder(this), inventoryType, replaceVars(who, title));
            }
            inventories.put(who != null ? who.getUniqueId() : null, inventory);
        } else {
            inventory.clear();
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            GuiElement element = getElement(i);
            if (element == null) {
                element = getFiller();
            }
            if (element != null) {
                inventory.setItem(i, element.getItem(who, i));
            }
        }
    }

    /**
     * Update all dynamic elements in a collection of elements.
     * @param who       The player to update the elements for
     * @param elements  The elements to update
     */
    private void updateElements(HumanEntity who, Collection<GuiElement> elements) {
        for (GuiElement element : elements) {
            if (element instanceof DynamicGuiElement) {
                ((DynamicGuiElement) element).update(who);
            } else if (element instanceof GuiElementGroup) {
                updateElements(who, ((GuiElementGroup) element).getElements());
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
        for (Inventory inventory : inventories.values()) {
            for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                if (clearHistory) {
                    clearHistory(viewer);
                }
                viewer.closeInventory();
            }
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
        for (Inventory inventory : inventories.values()) {
            inventory.clear();
        }
        inventories.clear();
        pageNumbers.clear();
        pageAmounts.clear();
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
     *                  returns an empty one and not <code>null</code>!
     */
    public static Deque<InventoryGui> getHistory(HumanEntity player) {
        return GUI_HISTORY.getOrDefault(player.getUniqueId(), new ArrayDeque<>());
    }

    /**
     * Go back one entry in the history
     * @param player    The player to show the previous gui to
     * @return          <code>true</code> if there was a gui to show; <code>false</code> if not
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
     * Get the plugin which owns this GUI. Should be the one who created it.
     * @return The plugin which owns this GUI
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Get element in a certain slot
     * @param slot  The slot to get the element from
     * @return      The GuiElement or <code>null</code> if the slot is empty/there wasn't one
     */
    public GuiElement getElement(int slot) {
        return slot < 0 || slot >= slots.length ? null : elements.get(slots[slot]);
    }

    /**
     * Get an element by its character
     * @param c The character to get the element by
     * @return  The GuiElement or <code>null</code> if there is no element for that character
     */
    public GuiElement getElement(char c) {
        return elements.get(c);
    }

    /**
     * Get all elements of this gui. This collection is immutable, use the addElement and removeElement methods
     * to modify the elements in this gui.
     * @return An immutable collection of all elements in this group
     */
    public Collection<GuiElement> getElements() {
        return Collections.unmodifiableCollection(elements.values());
    }
    
    /**
     * Set the owner of this GUI. Will remove the previous assignment.
     * @param owner The owner of the GUI
     */
    public void setOwner(InventoryHolder owner) {
        removeFromMap();
        this.owner = owner;
        if (owner instanceof Entity) {
            GUI_MAP.put(((Entity) owner).getUniqueId().toString(), this);
        } else if (owner instanceof BlockState) {
            GUI_MAP.put(((BlockState) owner).getLocation().toString(), this);
        }
    }

    /**
     * Get the owner of this GUI. Will be null if th GUI doesn't have one
     * @return The InventoryHolder of this GUI
     */
    public InventoryHolder getOwner() {
        return owner;
    }
    
    /**
     * Check whether or not the Owner of this GUI is real or fake
     * @return <code>true</code> if the owner is a real world InventoryHolder; <code>false</code> if it is null
     */
    public boolean hasRealOwner() {
        return owner != null;
    }
    
    /**
     * Get the Action that is run when clicked outside of the inventory
     * @return  The Action for when the player clicks outside the inventory; can be null
     */
    public GuiElement.Action getOutsideAction() {
        return outsideAction;
    }
    
    /**
     * Set the Action that is run when clicked outside of the inventory
     * @param outsideAction The Action for when the player clicks outside the inventory; can be null
     */
    public void setOutsideAction(GuiElement.Action outsideAction) {
        this.outsideAction = outsideAction;
    }
    
    /**
     * Get the action that is run when this GUI is closed
     * @return The action for when the player closes this inventory; can be null
     */
    public CloseAction getCloseAction() {
        return closeAction;
    }
    
    /**
     * Set the action that is run when this GUI is closed; it should return true if the GUI should go back
     * @param closeAction The action for when the player closes this inventory; can be null
     */
    public void setCloseAction(CloseAction closeAction) {
        this.closeAction = closeAction;
    }

    /**
     * Get whether or not this GUI should make a sound when interacting with elements that make sound
     * @return  Whether or not to make a sound when interacted with
     */
    public boolean isSilent() {
        return silent;
    }

    /**
     * Set whether or not this GUI should make a sound when interacting with elements that make sound
     * @param silent Whether or not to make a sound when interacted with
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }
    
    private void removeFromMap() {
        if (owner instanceof Entity) {
            GUI_MAP.remove(((Entity) owner).getUniqueId().toString(), this);
        } else if (owner instanceof BlockState) {
            GUI_MAP.remove(((BlockState) owner).getLocation().toString(), this);
        }
    }

    /**
     * Get the GUI registered to an InventoryHolder
     * @param holder    The InventoryHolder to get the GUI for
     * @return          The InventoryGui registered to it or <code>null</code> if none was registered to it
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
        if (isSilent()) return;
        for (Inventory inventory : inventories.values()) {
            for (HumanEntity humanEntity : inventory.getViewers()) {
                if (humanEntity instanceof Player) {
                    ((Player) humanEntity).playSound(humanEntity.getEyeLocation(), CLICK_SOUND, 1, 1);
                }
            }
        }
    }
    
    /**
     * Get the inventory. Package scope as it should only be used by InventoryGui.Holder
     * @return The GUI's generated inventory
     */
    Inventory getInventory() {
        return getInventory(null);
    }

    /**
     * Get the inventory of a certain player
     * @param who The player, if null it will try to return the inventory created first or null if none was created
     * @return The GUI's generated inventory, null if none was found
     */
    private Inventory getInventory(HumanEntity who) {
        return who != null ? inventories.get(who.getUniqueId()) : (inventories.isEmpty() ? null : inventories.values().iterator().next());
    }

    private interface UnregisterableListener extends Listener {
        void unregister();

        default boolean isCompatible() {
            try {
                getClass().getMethods();
                getClass().getDeclaredMethods();
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }
    }

    /**
     * All the listeners that InventoryGui needs to work
     */
    public class GuiListener implements UnregisterableListener {
        private final InventoryGui gui;

        private GuiListener(InventoryGui gui) {
            this.gui = gui;
        }

        @EventHandler
        private void onInventoryClick(InventoryClickEvent event) {
            if (event.getInventory().equals(getInventory(event.getWhoClicked()))) {

                int slot = -1;
                if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                    slot = event.getRawSlot();
                } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    slot = event.getInventory().firstEmpty();
                }
    
                GuiElement.Action action = null;
                GuiElement element = null;
                if (slot >= 0) {
                    element = getElement(slot);
                    if (element != null) {
                        action = element.getAction(event.getWhoClicked());
                    }
                } else if (slot == -999) {
                    action = outsideAction;
                } else {
                    // Click was neither for the top inventory or outside
                    // E.g. click is in the bottom inventory
                    if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                        simulateCollectToCursor(new GuiElement.Click(gui, slot, null, event));
                    }
                    return;
                }
                try {
                    if (action == null || action.onClick(new GuiElement.Click(gui, slot, element, event))) {
                        event.setCancelled(true);
                        if (event.getWhoClicked() instanceof Player) {
                            ((Player) event.getWhoClicked()).updateInventory();
                        }
                    }
                    if (action != null) {
                        // Let's assume something changed and re-draw all currently shown inventories
                        for (UUID playerId : inventories.keySet()) {
                            if (!event.getWhoClicked().getUniqueId().equals(playerId)) {
                                Player player = plugin.getServer().getPlayer(playerId);
                                if (player != null) {
                                    draw(player, false);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        ((Player) event.getWhoClicked()).updateInventory();
                    }
                    plugin.getLogger().log(Level.SEVERE, "Exception while trying to run action for click on "
                            + (element != null ? element.getClass().getSimpleName() : "empty element")
                            + " in slot " + event.getRawSlot() + " of " + gui.getTitle() + " GUI!");
                    t.printStackTrace();
                }
            } else if (hasRealOwner() && owner.equals(event.getInventory().getHolder())) {
                // Click into inventory by same owner but not the inventory of the GUI
                // Assume that the underlying inventory changed and redraw the GUI
                plugin.getServer().getScheduler().runTask(plugin, (Runnable) gui::draw);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInventoryDrag(InventoryDragEvent event) {
            Inventory inventory = getInventory(event.getWhoClicked());
            if (event.getInventory().equals(inventory)) {
                int rest = 0;
                Map<Integer, ItemStack> resetSlots = new HashMap<>();
                for (Map.Entry<Integer, ItemStack> items : event.getNewItems().entrySet()) {
                    if (items.getKey() < inventory.getSize()) {
                        GuiElement element = getElement(items.getKey());
                        if (!(element instanceof GuiStorageElement)
                                || !((GuiStorageElement) element).setStorageItem(event.getWhoClicked(), items.getKey(), items.getValue())) {
                            ItemStack slotItem = event.getInventory().getItem(items.getKey());
                            if (!items.getValue().isSimilar(slotItem)) {
                                rest += items.getValue().getAmount();
                            } else if (slotItem != null) {
                                rest += items.getValue().getAmount() - slotItem.getAmount();
                            }
                            //items.getValue().setAmount(0); // can't change resulting items :/
                            resetSlots.put(items.getKey(), event.getInventory().getItem(items.getKey())); // reset them manually
                        }
                    }
                }
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Map.Entry<Integer, ItemStack> items : resetSlots.entrySet()) {
                        event.getView().getTopInventory().setItem(items.getKey(), items.getValue());
                    }
                });
                
                if (rest > 0) {
                    int cursorAmount = event.getCursor() != null ? event.getCursor().getAmount() : 0;
                    if (!event.getOldCursor().isSimilar(event.getCursor())) {
                        event.setCursor(event.getOldCursor());
                        cursorAmount = 0;
                    }
                    int newCursorAmount = cursorAmount + rest;
                    if (newCursorAmount <= event.getCursor().getMaxStackSize()) {
                        event.getCursor().setAmount(newCursorAmount);
                    } else {
                        event.getCursor().setAmount(event.getCursor().getMaxStackSize());
                        ItemStack add = event.getCursor().clone();
                        int addAmount = newCursorAmount - event.getCursor().getMaxStackSize();
                        if (addAmount > 0) {
                            add.setAmount(addAmount);
                            for (ItemStack drop : event.getWhoClicked().getInventory().addItem(add).values()) {
                                event.getWhoClicked().getLocation().getWorld().dropItem(event.getWhoClicked().getLocation(), drop);
                            }
                        }
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onInventoryClose(InventoryCloseEvent event) {
            Inventory inventory = getInventory(event.getPlayer());
            if (event.getInventory().equals(inventory)) {
                // go back. that checks if the player is in gui and has history
                if (gui.equals(getOpen(event.getPlayer()))) {
                    if (closeAction == null || closeAction.onClose(new Close(event.getPlayer(), gui, event))) {
                        goBack(event.getPlayer());
                    } else {
                        clearHistory(event.getPlayer());
                    }
                }
                if (inventories.size() <= 1) {
                    destroy(false);
                } else {
                    inventory.clear();
                    for (HumanEntity viewer : inventory.getViewers()) {
                        if (viewer != event.getPlayer()) {
                            viewer.closeInventory();
                        }
                    }
                    inventories.remove(event.getPlayer().getUniqueId());
                    pageAmounts.remove(event.getPlayer().getUniqueId());
                    pageNumbers.remove(event.getPlayer().getUniqueId());
                    for (GuiElement element : getElements()) {
                        if (element instanceof DynamicGuiElement) {
                            ((DynamicGuiElement) element).removeCachedElement(event.getPlayer());
                        }
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onInventoryMoveItem(InventoryMoveItemEvent event) {
            if (hasRealOwner() && (owner.equals(event.getDestination().getHolder()) || owner.equals(event.getSource().getHolder()))) {
                plugin.getServer().getScheduler().runTask(plugin, (Runnable) gui::draw);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDispense(BlockDispenseEvent event) {
            if (hasRealOwner() && owner.equals(event.getBlock().getState())) {
                plugin.getServer().getScheduler().runTask(plugin, (Runnable) gui::draw);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (hasRealOwner() && owner.equals(event.getBlock().getState())) {
                destroy();
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDeath(EntityDeathEvent event) {
            if (hasRealOwner() && owner.equals(event.getEntity())) {
                destroy();
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent event) {
            if (event.getPlugin() == plugin) {
                destroy();
            }
        }
    
        public void unregister() {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryDragEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
            InventoryMoveItemEvent.getHandlerList().unregister(this);
            BlockDispenseEvent.getHandlerList().unregister(this);
            BlockBreakEvent.getHandlerList().unregister(this);
            EntityDeathEvent.getHandlerList().unregister(this);
            PluginDisableEvent.getHandlerList().unregister(this);
        }
    }

    /**
     * Event isn't available on older version so just use a separate listener...
     */
    public class ItemSwapGuiListener implements UnregisterableListener {

        private final InventoryGui gui;

        private ItemSwapGuiListener(InventoryGui gui) {
            this.gui = gui;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onInventoryMoveItem(PlayerSwapHandItemsEvent event) {
            Inventory inventory = getInventory(event.getPlayer());
            if (event.getPlayer().getOpenInventory().getTopInventory().equals(inventory)) {
                event.setCancelled(true);
            }
        }

        public void unregister() {
            PlayerSwapHandItemsEvent.getHandlerList().unregister(this);
        }
    }
    
    /**
     * Fake InventoryHolder for the GUIs
     */
    public static class Holder implements InventoryHolder {
        private InventoryGui gui;
    
        public Holder(InventoryGui gui) {
            this.gui = gui;
        }
        
        @Override
        public Inventory getInventory() {
            return gui.getInventory();
        }
        
        public InventoryGui getGui() {
            return gui;
        }
    }
    
    public static interface CloseAction {
        
        /**
         * Executed when a player closes a GUI inventory
         * @param close The close object holding information about this close
         * @return Whether or not the close should go back or not
         */
        boolean onClose(Close close);
        
    }
    
    public static class Close {
        private final HumanEntity player;
        private final InventoryGui gui;
        private final InventoryCloseEvent event;
    
        public Close(HumanEntity player, InventoryGui gui, InventoryCloseEvent event) {
            this.player = player;
            this.gui = gui;
            this.event = event;
        }
    
        public HumanEntity getPlayer() {
            return player;
        }
    
        public InventoryGui getGui() {
            return gui;
        }
    
        public InventoryCloseEvent getEvent() {
            return event;
        }
    }
    
    /**
     * Set the text of an item using the display name and the lore.
     * Also replaces any placeholders in the text and filters out empty lines.
     * Use a single space to create an emtpy line.
     * @param item      The {@link ItemStack} to set the text for
     * @param text      The text lines to set
     * @deprecated Use {@link #setItemText(HumanEntity, ItemStack, String...)}
     */
    @Deprecated
    public void setItemText(ItemStack item, String... text) {
        setItemText(null, item, text);
    }

    /**
     * Set the text of an item using the display name and the lore.
     * Also replaces any placeholders in the text and filters out empty lines.
     * Use a single space to create an emtpy line.
     * @param player    The player viewing the GUI
     * @param item      The {@link ItemStack} to set the text for
     * @param text      The text lines to set
     */
    public void setItemText(HumanEntity player, ItemStack item, String... text) {
        if (item != null && text != null && text.length > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String combined = replaceVars(player, Arrays.stream(text).collect(Collectors.joining("\n")));
                String[] lines = combined.split("\n");
                if(text[0] != null) meta.setDisplayName(lines[0]);
                if (lines.length > 1) {
                    meta.setLore(Arrays.asList(Arrays.copyOfRange(lines, 1, lines.length)));
                } else {
                    meta.setLore(null);
                }
                item.setItemMeta(meta);
            }
        }
    }

    /**
     * Replace some placeholders in the with values regarding the gui's state. Replaced color codes.<br>
     * The placeholders are:<br>
     * <code>%plugin%</code>    - The name of the plugin that this gui is from.<br>
     * <code>%owner%</code>     - The name of the owner of this gui. Will be an empty string when the owner is null.<br>
     * <code>%title%</code>     - The title of this GUI.<br>
     * <code>%page%</code>      - The current page that this gui is on.<br>
     * <code>%nextpage%</code>  - The next page. "none" if there is no next page.<br>
     * <code>%prevpage%</code>  - The previous page. "none" if there is no previous page.<br>
     * <code>%pages%</code>     - The amount of pages that this gui has.
     * @param text          The text to replace the placeholders in
     * @param replacements  Additional repplacements. i = placeholder, i+1 = replacements
     * @return      The text with all placeholders replaced
     * @deprecated Use {@link #replaceVars(HumanEntity, String, String...)}
     */
    @Deprecated
    public String replaceVars(String text, String... replacements) {
        return replaceVars(null, text, replacements);
    }

    /**
     * Replace some placeholders in the with values regarding the gui's state. Replaced color codes.<br>
     * The placeholders are:<br>
     * <code>%plugin%</code>    - The name of the plugin that this gui is from.<br>
     * <code>%owner%</code>     - The name of the owner of this gui. Will be an empty string when the owner is null.<br>
     * <code>%title%</code>     - The title of this GUI.<br>
     * <code>%page%</code>      - The current page that this gui is on.<br>
     * <code>%nextpage%</code>  - The next page. "none" if there is no next page.<br>
     * <code>%prevpage%</code>  - The previous page. "none" if there is no previous page.<br>
     * <code>%pages%</code>     - The amount of pages that this gui has.
     * @param player        The player viewing the GUI
     * @param text          The text to replace the placeholders in
     * @param replacements  Additional repplacements. i = placeholder, i+1 = replacements
     * @return      The text with all placeholders replaced
     */
    public String replaceVars(HumanEntity player, String text, String... replacements) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            map.putIfAbsent(replacements[i], replacements[i + 1]);
        }

        map.putIfAbsent("plugin", plugin.getName());
        try {
            map.putIfAbsent("owner", owner instanceof Nameable ? ((Nameable) owner).getCustomName() : "");
        } catch (NoSuchMethodError e) {
            map.putIfAbsent("owner", owner instanceof Entity ? ((Entity) owner).getCustomName() : "");
        }
        map.putIfAbsent("title", title);
        map.putIfAbsent("page", String.valueOf(getPageNumber(player) + 1));
        map.putIfAbsent("nextpage", getPageNumber(player) + 1 < getPageAmount(player) ? String.valueOf(getPageNumber(player) + 2) : "none");
        map.putIfAbsent("prevpage", getPageNumber(player) > 0 ? String.valueOf(getPageNumber(player)) : "none");
        map.putIfAbsent("pages", String.valueOf(getPageAmount(player)));

        return ChatColor.translateAlternateColorCodes('&', replace(text, map));
    }

    /**
     * Replace placeholders in a string
     * @param string        The string to replace in
     * @param replacements  What to replace the placeholders with. The n-th index is the placeholder, the n+1-th the value.
     * @return The string with all placeholders replaced (using the configured placeholder prefix and suffix)
     */
    private String replace(String string, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String placeholder = "%" + entry.getKey() + "%";
            Pattern pattern = PATTERN_CACHE.get(placeholder);
            if (pattern == null) {
                PATTERN_CACHE.put(placeholder, pattern = Pattern.compile(placeholder, Pattern.LITERAL));
            }
            string = pattern.matcher(string).replaceAll(Matcher.quoteReplacement(entry.getValue() != null ? entry.getValue() : "null"));
        }
        return string;
    }
    
    /**
     * Simulate the collecting to the cursor while respecting elements that can't be modified
     * @param click The click that startet it all
     */
    void simulateCollectToCursor(GuiElement.Click click) {
        ItemStack newCursor = click.getEvent().getCursor().clone();
    
        boolean itemInGui = false;
        for (int i = 0; i < click.getEvent().getView().getTopInventory().getSize(); i++) {
            if (i != click.getEvent().getRawSlot()) {
                ItemStack viewItem = click.getEvent().getView().getTopInventory().getItem(i);
                if (newCursor.isSimilar(viewItem)) {
                    itemInGui = true;
                }
                GuiElement element = getElement(i);
                if (element instanceof GuiStorageElement) {
                    GuiStorageElement storageElement = (GuiStorageElement) element;
                    ItemStack otherStorageItem = storageElement.getStorageItem(click.getWhoClicked(), i);
                    if (addToStack(newCursor, otherStorageItem)) {
                        if (otherStorageItem.getAmount() == 0) {
                            otherStorageItem = null;
                        }
                        storageElement.setStorageItem(i, otherStorageItem);
                        if (newCursor.getAmount() == newCursor.getMaxStackSize()) {
                            break;
                        }
                    }
                }
            }
        }
    
        if (itemInGui) {
            click.getEvent().setCurrentItem(null);
            click.getEvent().setCancelled(true);
            if (click.getEvent().getWhoClicked() instanceof Player) {
                ((Player) click.getEvent().getWhoClicked()).updateInventory();
            }
        
            if (click.getElement() instanceof GuiStorageElement) {
                ((GuiStorageElement) click.getElement()).setStorageItem(click.getWhoClicked(), click.getSlot(), null);
            }
    
            if (newCursor.getAmount() < newCursor.getMaxStackSize()) {
                Inventory bottomInventory = click.getEvent().getView().getBottomInventory();
                for (ItemStack bottomIem : bottomInventory) {
                    if (addToStack(newCursor, bottomIem)) {
                        if (newCursor.getAmount() == newCursor.getMaxStackSize()) {
                            break;
                        }
                    }
                }
            }
            click.getEvent().setCursor(newCursor);
            draw();
        }
    }
    
    /**
     * Add items to a stack up to the max stack size
     * @param item  The base item
     * @param add   The item stack to add
     * @return <code>true</code> if the stack is finished; <code>false</code> if these stacks can't be merged
     */
    private static boolean addToStack(ItemStack item, ItemStack add) {
        if (item.isSimilar(add)) {
            int newAmount = item.getAmount() + add.getAmount();
            if (newAmount >= item.getMaxStackSize()) {
                item.setAmount(item.getMaxStackSize());
                add.setAmount(newAmount - item.getAmount());
            } else {
                item.setAmount(newAmount);
                add.setAmount(0);
            }
            return true;
        }
        return false;
    }
}
