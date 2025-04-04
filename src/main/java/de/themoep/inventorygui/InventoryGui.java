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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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

    private final static Map<String, InventoryGui> GUI_MAP = new ConcurrentHashMap<>();
    private final static Map<UUID, ArrayDeque<InventoryGui>> GUI_HISTORY = new ConcurrentHashMap<>();

    private final static Map<String, Pattern> PATTERN_CACHE = new HashMap<>();

    private final static boolean FOLIA;

    private static String DEFAULT_CLICK_SOUND;

    private final Plugin plugin;
    private final GuiListener listener;
    private InventoryCreator creator;
    private BiConsumer<ItemMeta, String> itemNameSetter;
    private BiConsumer<ItemMeta, List<String>> itemLoreSetter;
    private String title;
    private boolean titleUpdated = false;
    private final char[] slots;
    private int width;
    private final GuiElement[] elementSlots;
    private final Map<Character, GuiElement> elements = new ConcurrentHashMap<>();
    private InventoryType inventoryType;
    private final Map<UUID, Inventory> inventories = new ConcurrentHashMap<>();
    private InventoryHolder owner;
    private final Map<UUID, Integer> pageNumbers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pageAmounts = new ConcurrentHashMap<>();
    private GuiElement.Action outsideAction = click -> false;
    private CloseAction closeAction = close -> true;
    private String clickSound = getDefaultClickSound();
    private boolean silent = false;
    
    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;

        // Sound names changed, make it compatible with both versions
        String clickSound = null;
        Map<String, String> clickSounds = new LinkedHashMap<>();
        clickSounds.put("UI_BUTTON_CLICK", "ui.button.click");
        clickSounds.put("CLICK", "random.click");
        for (Map.Entry<String, String> entry : clickSounds.entrySet()) {
            try {
                // Try to get sound enum to see if it exists
                Sound.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                // If it does use the sound key
                clickSound = entry.getValue();
                break;
            } catch (IllegalArgumentException | IncompatibleClassChangeError ignored) {}
        }
        if (clickSound == null) {
            for (Sound sound : Sound.values()) {
                if (sound.name().contains("CLICK")) {
                    // Convert to sound key under the assumption that the enum name is just using underscores in the place of dots
                    clickSound = sound.name().toLowerCase(Locale.ROOT).replace('_', '.');
                    break;
                }
            }
        }
        if (clickSound == null) {
            clickSound = "ui.button.click";
        }
        setDefaultClickSound(clickSound);
    }

    /**
     * Create a new gui with a certain setup and some elements
     * @param plugin            Your plugin
     * @param creator           A creator for the backing inventory
     * @param itemNameSetter    Setter for item display names
     * @param itemLoreSetter    Setter for item lores
     * @param owner             The holder that owns this gui to retrieve it with {@link #get(InventoryHolder)}.
     *                          Can be <code>null</code>.
     * @param title             The name of the GUI. This will be the title of the inventory.
     * @param rows              How your rows are setup. Each element is getting assigned to a character.
     *                          Empty/missing ones get filled with the Filler.
     * @param elements          The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     * @throws IllegalArgumentException Thrown when the provided rows cannot be matched to an InventoryType
     */
    public InventoryGui(Plugin plugin, InventoryCreator creator, BiConsumer<ItemMeta, String> itemNameSetter, BiConsumer<ItemMeta, List<String>> itemLoreSetter, InventoryHolder owner, String title, String[] rows, GuiElement... elements) {
        this.plugin = plugin;
        this.creator = creator;
        this.itemNameSetter = itemNameSetter;
        this.itemLoreSetter = itemLoreSetter;
        this.owner = owner;
        this.title = title;
        this.listener = new GuiListener();

        width = ROW_WIDTHS[0];
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
                double side = (width - row.length()) / 2.0;
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
                slotsBuilder.append(row, 0, width);
            }
        }
        slots = slotsBuilder.toString().toCharArray();
        elementSlots = new GuiElement[slots.length];

        addElements(elements);
    }

    /**
     * Create a new gui with a certain setup and some elements
     * @param plugin    Your plugin
     * @param creator   A creator for the backing inventory
     * @param owner     The holder that owns this gui to retrieve it with {@link #get(InventoryHolder)}.
     *                  Can be <code>null</code>.
     * @param title     The name of the GUI. This will be the title of the inventory.
     * @param rows      How your rows are setup. Each element is getting assigned to a character.
     *                  Empty/missing ones get filled with the Filler.
     * @param elements  The {@link GuiElement}s that the gui should have. You can also use {@link #addElement(GuiElement)} later.
     * @throws IllegalArgumentException Thrown when the provided rows cannot be matched to an InventoryType
     */
    public InventoryGui(Plugin plugin, InventoryCreator creator, InventoryHolder owner, String title, String[] rows, GuiElement... elements) {
        this(plugin, creator, ItemMeta::setDisplayName, ItemMeta::setLore, owner, title, rows, elements);
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
    public InventoryGui(Plugin plugin, InventoryHolder owner, String title, String[] rows, GuiElement... elements) {
        this(plugin, new InventoryCreator(
                (gui, who, type) -> plugin.getServer().createInventory(new Holder(gui), type, gui.replaceVars(who, gui.getTitle())),
                (gui, who, size) -> plugin.getServer().createInventory(new Holder(gui), size, gui.replaceVars(who, gui.getTitle()))),
                owner, title, rows, elements);
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
    public InventoryGui(Plugin plugin, String title, String[] rows, GuiElement... elements) {
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
    public InventoryGui(Plugin plugin, InventoryHolder owner, String title, String[] rows, Collection<GuiElement> elements) {
        this(plugin, owner, title, rows);
        addElements(elements);
    }

    /**
     * Directly set the element in a specific slot
     * @param element   The {@link GuiElement} to add
     * @throws IllegalArgumentException Thrown if the provided slot is below 0 or equal/above the available slot count
     * @throws IllegalStateException    Thrown if the element was already added to a gui
     */
    public void setElement(int slot, GuiElement element) {
        if (slot < 0 || slot >= elementSlots.length) {
            throw new IllegalArgumentException("Provided slots is outside available slots! (" + elementSlots.length + ")");
        }
        if (element.getSlots().length > 0 || element.getGui() != null) {
            throw new IllegalStateException("Element was already added to a gui!");
        }
        element.setSlots(new int[] {slot});
        element.setGui(this);
        elementSlots[slot] = element;
    }

    /**
     * Add an element to the gui with its position directly based on the elements slot char and the gui setup string
     * @param element   The {@link GuiElement} to add
     */
    public void addElement(GuiElement element) {
        if (element.getSlots().length > 0 || element.getGui() != null) {
            throw new IllegalStateException("Element was already added to a gui!");
        }
        elements.put(element.getSlotChar(), element);
        element.setGui(this);
        int[] slots = getSlots(element.getSlotChar());
        element.setSlots(slots);
        for (int slot : slots) {
            elementSlots[slot] = element;
        }
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
     * @param slotChar  The character to specify the elements position based on the gui setup string
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
     * @param slotChar  The character to specify the elements position based on the gui setup string
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
     * @param slotChar  The character to specify the elements position based on the gui setup string
     * @param materialData  The {@link MaterialData} of the item of tihs element
     * @param action        The {@link de.themoep.inventorygui.GuiElement.Action} to run when the player clicks on this element
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
     * @param slotChar  The character to specify the elements position based on the gui setup string
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
     * @param slotChar  The character to specify the elements position based on the gui setup string
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
     * Add multiple elements to the gui with their position based on their slot character
     * @param elements   The {@link GuiElement}s to add
     */
    public void addElements(GuiElement... elements) {
        for (GuiElement element : elements) {
            addElement(element);
        }
    }

    /**
     * Add multiple elements to the gui with their position based on their slot character
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
        boolean removed = elements.remove(element.getSlotChar(), element);
        for (int slot : element.getSlots()) {
            if (elementSlots[slot] == element) {
                elementSlots[slot] = null;
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Remove the element that is currently assigned to a specific slot char from all slots in the gui
     * @param slotChar  The char of the slot
     * @return The element which was in that slot or <code>null</code> if there was none
     */
    public GuiElement removeElement(char slotChar) {
        GuiElement element = getElement(slotChar);
        if (element != null) {
            removeElement(element);
        }
        return element;
    }

    /**
     * Remove the element that is currently in a specific slot. Will not remove that element from other slots
     * @param slot  The slot
     * @return The element which was in that slot or <code>null</code> if there was none
     */
    public GuiElement removeElement(int slot) {
        if (slot < 0 || slot >= elementSlots.length) {
            return null;
        }
        GuiElement element = elementSlots[slot];
        elementSlots[slot] = null;
        return element;
    }

    /**
     * Set the filler element for empty slots
     * @param item  The item for the filler element
     * @return The GUI instance
     */
    public InventoryGui filler(ItemStack item) {
        setFiller(item);
        return this;
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
     * @param player    The Player to query the page number for
     * @return The page number
     */
    public int getPageNumber(@NotNull HumanEntity player) {
        return pageNumbers.getOrDefault(player.getUniqueId(), 0);
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
        pageNumbers.put(player.getUniqueId(), Math.max(pageNumber, 0));
    }

    /**
     * Get the amount of pages that this GUI has for a certain player
     * @param player    The Player to query the page amount for
     * @return The amount of pages
     */
    public int getPageAmount(@NotNull HumanEntity player) {
        return pageAmounts.getOrDefault(player.getUniqueId(), 1);
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
            if (amount > 0 && (pageAmount - 1) * element.getSlots().length < amount && element.getSlots().length > 0) {
                pageAmount = (int) Math.ceil((double) amount / element.getSlots().length);
            }
        }
        setPageAmount(player, pageAmount);
        if (getPageNumber(player) >= pageAmount) {
            setPageNumberInternal(player, Math.min(0, pageAmount - 1));
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
        // Draw the elements into an inventory, if the title was updated then also force-recreate the inventory if it exists
        draw(player, true, titleUpdated);
        if (titleUpdated || !checkOpen || !this.equals(getOpen(player))) {
            InventoryType type = player.getOpenInventory().getType();
            if (type != InventoryType.CRAFTING && type != InventoryType.CREATIVE) {
                // If the player already has a gui open then we assume that the call was from that gui.
                // In order to not close it in a InventoryClickEvent listener (which will lead to errors)
                // we delay the opening for one tick to run after it finished processing the event
                runTask(player, () -> {
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
        // Reset the field that indicates that the title changed
        titleUpdated = false;
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
        listener.registerListeners();
    }

    /**
     * Draw the elements in the inventory. This can be used to manually refresh the gui. Updates any dynamic elements.
     */
    public void draw() {
        for (UUID playerId : inventories.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                runTaskOrNow(player, () -> draw(player));
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
        draw(who, updateDynamic, false);
    }

    /**
     * Draw the elements in the inventory. This can be used to manually refresh the gui.
     * @param who               For who to draw the GUI
     * @param updateDynamic     Update dynamic elements
     * @param recreateInventory Recreate the inventory
     */
    public void draw(HumanEntity who, boolean updateDynamic, boolean recreateInventory) {
        if (updateDynamic) {
            updateElements(who, elements.values());
        }
        calculatePageAmount(who);
        Inventory inventory = getInventory(who);
        if (inventory == null || recreateInventory) {
            build();
            if (slots.length != inventoryType.getDefaultSize()) {
                inventory = getInventoryCreator().getSizeCreator().create(this, who, slots.length);
            } else {
                inventory = getInventoryCreator().getTypeCreator().create(this, who, inventoryType);
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
     * Schedule a task on a {@link HumanEntity}/main thread to run on the next tick
     * @param entity the human entity to schedule a task on
     * @param task the task to be run
     */
    protected void runTask(HumanEntity entity, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, st -> task.run(), null);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedule a task on the global region/main thread to run on the next tick
     * @param task the task to be run
     */
    protected void runTask(Runnable task) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, st -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedule a task on a {@link HumanEntity} to run on the next tick
     * Alternatively if the current thread is already the right thread, execute immediately
     * @param entity the human entity to schedule a task on
     * @param task the task to be run
     */
    protected void runTaskOrNow(HumanEntity entity, Runnable task) {
        if (FOLIA) {
            if (plugin.getServer().isOwnedByCurrentRegion(entity)) {
                task.run();
            } else {
                entity.getScheduler().run(plugin, st -> task.run(), null);
            }
        } else {
            if (plugin.getServer().isPrimaryThread()) {
                task.run();
            } else {
                plugin.getServer().getScheduler().runTask(plugin, task);
            }
        }
    }

    /**
     * Update all dynamic elements in a collection of elements.
     * @param who       The player to update the elements for
     * @param elements  The elements to update
     */
    public static void updateElements(HumanEntity who, Collection<GuiElement> elements) {
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
     * @param clearHistory  Whether to close the GUI completely (by clearing the history)
     */
    public void close(boolean clearHistory) {
        for (Inventory inventory : inventories.values()) {
            for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                close(viewer, clearHistory);
            }
        }
    }

    /**
     * Closes the GUI for a specific viewer it
     * @param viewer    The player viewing it
     */
    public void close(HumanEntity viewer) {
        close(viewer, true);
    }

    /**
     * Closes the GUI for a specific viewer it
     * @param viewer        The player viewing it
     * @param clearHistory  Whether to close the GUI completely (by clearing the history)
     */
    public void close(HumanEntity viewer, boolean clearHistory) {
        if (clearHistory) {
            clearHistory(viewer);
        }
        viewer.closeInventory();
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
        listener.unregisterListeners();
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
        Deque<InventoryGui> previous = GUI_HISTORY.remove(player.getUniqueId());
        return previous != null ? previous : new ArrayDeque<>();
    }

    /**
     * Get the plugin which owns this GUI. Should be the one who created it.
     * @return The plugin which owns this GUI
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Get the helper class which will create the custom inventory for this gui.
     * Simply uses {@link org.bukkit.Bukkit#createInventory(InventoryHolder, int, String)} by default.
     * @return The used inventory creator instance
     */
    public InventoryCreator getInventoryCreator() {
        return creator;
    }

    /**
     * Set the helper class which will create the custom inventory for this gui.
     * Can be used to create more special inventories.
     * Simply uses {@link org.bukkit.Bukkit#createInventory(InventoryHolder, int, String)} by default.
     * Should return a container inventory that can hold the size. Special inventories will break stuff.
     * @param inventoryCreator The new inventory creator instance
     */
    public void setInventoryCreator(InventoryCreator inventoryCreator) {
        this.creator = Objects.requireNonNull(inventoryCreator);
    }

    /**
     * Get the setter for item names.
     * @return The setter instance
     */
    public BiConsumer<ItemMeta, String> getItemNameSetter() {
        return itemNameSetter;
    }

    /**
     * Sets the setter ofr item names.
     * @param itemNameSetter The item name setter BiConsumer taking the ItemMeta to be modified and the string for the name
     */
    public void setItemNameSetter(BiConsumer<ItemMeta, String> itemNameSetter) {
        this.itemNameSetter = Objects.requireNonNull(itemNameSetter);
    }

    /**
     * Get the setter for item lores.
     * @return The setter instance
     */
    public BiConsumer<ItemMeta, List<String>> getItemLoreSetter() {
        return itemLoreSetter;
    }

    /**
     * Sets the setter for item lores.
     * @param itemLoreSetter The item lore setter BiConsumer taking the ItemMeta to be modified and the string list for the lore lines
     */
    public void setItemLoreSetter(BiConsumer<ItemMeta, List<String>> itemLoreSetter) {
        this.itemLoreSetter = Objects.requireNonNull(itemLoreSetter);
    }

    /**
     * Get element in a certain slot
     * @param slot  The slot to get the element from
     * @return      The GuiElement or <code>null</code> if the slot is empty/there wasn't one
     */
    public GuiElement getElement(int slot) {
        return slot < 0 || slot >= elementSlots.length ? null : elementSlots[slot];
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
     * Get the click sound to use for non-silent GUIs that don't have a specific one set
     * @return The default click sound, if set null no sound will play
     */
    public static String getDefaultClickSound() {
        return DEFAULT_CLICK_SOUND;
    }

    /**
     * Set the click sound to use for non-silent GUIs that don't have a specific one set
     * @param defaultClickSound The default click sound, if set to null no sound will play
     */
    public static void setDefaultClickSound(String defaultClickSound) {
        DEFAULT_CLICK_SOUND = defaultClickSound;
    }

    /**
     * Set the sound that plays when a button (that isn't preventing the item from being taken) is clicked in the GUI.
     * Fillers will not play a click sound
     * @return The key of the sound to play
     */
    public String getClickSound() {
        return clickSound;
    }

    /**
     * Set the sound that plays when a button (that isn't preventing the item from being taken) is clicked in the GUI.
     * Fillers will not play a click sound
     * @param soundKey  The key of the sound to play, if null then no sound will play (same effect as {@link #setSilent(boolean)})
     */
    public void setClickSound(String soundKey) {
        clickSound = soundKey;
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
        this.titleUpdated = true;
    }

    /**
     * Play a click sound e.g. when an element acts as a button
     */
    public void playClickSound() {
        if (isSilent() || clickSound == null) return;
        for (Inventory inventory : inventories.values()) {
            for (HumanEntity humanEntity : inventory.getViewers()) {
                if (humanEntity instanceof Player) {
                    ((Player) humanEntity).playSound(humanEntity.getEyeLocation(), getClickSound(), 1, 1);
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

    /**
     * Get the width of the GUI in slots
     * @return The width of the GUI
     */
    int getWidth() {
        return width;
    }

    /**
     * Handle interaction with a slot in this GUI
     * @param event     The event that triggered it
     * @param clickType The type of click
     * @param slot      The slot
     * @param cursor    The item on the cursor
     * @return The resulting click object
     */
    private GuiElement.Click handleInteract(InventoryInteractEvent event, ClickType clickType, int slot, ItemStack cursor) {
        GuiElement.Action action = null;
        GuiElement element = null;
        try {
            if (slot >= 0) {
                element = getElement(slot);
                if (element != null) {
                    action = element.getAction(event.getWhoClicked());
                }
            } else if (slot == -999) {
                action = outsideAction;
            } else {
                if (event instanceof InventoryClickEvent) {
                    // Click was neither for the top inventory nor outside
                    // E.g. click is in the bottom inventory
                    if (((InventoryClickEvent) event).getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                        GuiElement.Click click = new GuiElement.Click(this, slot, clickType, cursor, null, event);
                        simulateCollectToCursor(click);
                        return click;
                    } else if (((InventoryClickEvent) event).getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        // This was an action we can't handle, abort
                        event.setCancelled(true);
                    }
                }
                return null;
            }
            GuiElement.Click click = new GuiElement.Click(this, slot, clickType, cursor, element, event);
            if (action == null || action.onClick(click)) {
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
                return click;
            }
        } catch (Throwable t) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                ((Player) event.getWhoClicked()).updateInventory();
            }
            plugin.getLogger().log(Level.SEVERE, "Exception while trying to run action for click on "
                    + (element != null ? element.getClass().getSimpleName() : "empty/unknown element")
                    + " in slot " + slot + " of " + getTitle() + " GUI!", t);
        }
        return null;
    }

    private abstract class UnregisterableListener implements Listener {
        private final List<UnregisterableListener> listeners;
        private boolean listenersRegistered = false;

        private UnregisterableListener() {
            List<UnregisterableListener> listeners = new ArrayList<>();
            for (Class<?> innerClass : getClass().getDeclaredClasses()) {
                if (UnregisterableListener.class.isAssignableFrom(innerClass)) {
                    try {
                        UnregisterableListener listener = ((Class<? extends UnregisterableListener>) innerClass).getDeclaredConstructor(getClass()).newInstance(this);
                        if (!(listener instanceof OptionalListener) || ((OptionalListener) listener).isCompatible()) {
                            listeners.add(listener);
                        }
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.listeners = Collections.unmodifiableList(listeners);
        }

        protected void registerListeners() {
            if (listenersRegistered) {
                return;
            }
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            for (UnregisterableListener listener : listeners) {
                listener.registerListeners();
            }
            listenersRegistered = true;
        }

        protected void unregisterListeners() {
            HandlerList.unregisterAll(this);
            for (UnregisterableListener listener : listeners) {
                listener.unregisterListeners();
            }
            listenersRegistered = false;
        }
    }

    private abstract class OptionalListener extends UnregisterableListener {
        private boolean isCompatible() {
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
    private class GuiListener extends UnregisterableListener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        private void onInventoryClick(InventoryClickEvent event) {
            if (event.getInventory().equals(getInventory(event.getWhoClicked()))) {

                int slot = -1;
                if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                    slot = event.getRawSlot();
                } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    slot = event.getInventory().firstEmpty();
                }

                // Cache the original cursor
                ItemStack originalCursor = event.getCursor() != null ? event.getCursor().clone() : null;

                // Forward the click
                GuiElement.Click click = handleInteract(event, event.getClick(), slot, event.getCursor());

                // Update the cursor if necessary
                if (click != null && (originalCursor == null || !originalCursor.equals(click.getCursor()))) {
                    event.setCursor(click.getCursor());
                }
            } else if (hasRealOwner() && owner.equals(event.getInventory().getHolder())) {
                // Click into inventory by same owner but not the inventory of the GUI
                // Assume that the underlying inventory changed and redraw the GUI
                runTask(InventoryGui.this::draw);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onInventoryDrag(InventoryDragEvent event) {
            Inventory inventory = getInventory(event.getWhoClicked());
            if (event.getInventory().equals(inventory)) {
                // Check if we only drag over one slot if so then handle that as a click with the element
                if (event.getRawSlots().size() == 1) {
                    int slot = event.getRawSlots().iterator().next();
                    if (slot < event.getView().getTopInventory().getSize()) {
                        GuiElement.Click click = handleInteract(
                                event,
                                // Map drag type to the button that caused it
                                event.getType() == DragType.SINGLE ? ClickType.RIGHT : ClickType.LEFT,
                                slot,
                                event.getOldCursor()
                        );

                        // Update the cursor if necessary
                        if (click != null && !event.getOldCursor().equals(click.getCursor())) {
                            event.setCursor(click.getCursor());
                        }
                    }
                    return;
                }

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
                
                runTask(event.getWhoClicked(), () -> {
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
                if (InventoryGui.this.equals(getOpen(event.getPlayer()))) {
                    if (closeAction == null || closeAction.onClose(new Close(event.getPlayer(), InventoryGui.this, event))) {
                        goBack(event.getPlayer());
                    } else {
                        clearHistory(event.getPlayer());
                    }
                }
                if (inventories.size() <= 1) {
                    destroy(false);
                } else {
                    inventory.clear();
                    for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
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

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onInventoryMoveItem(InventoryMoveItemEvent event) {
            if (hasRealOwner() && (owner.equals(event.getDestination().getHolder()) || owner.equals(event.getSource().getHolder()))) {
                runTask(InventoryGui.this::draw);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDispense(BlockDispenseEvent event) {
            if (hasRealOwner() && owner.equals(event.getBlock().getState())) {
                runTask(InventoryGui.this::draw);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (hasRealOwner() && owner.equals(event.getBlock().getState())) {
                destroy();
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

        /**
         * Event isn't available on older version so just use a separate listener...
         */
        protected class ItemSwapGuiListener extends OptionalListener {

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onInventoryMoveItem(PlayerSwapHandItemsEvent event) {
                Inventory inventory = getInventory(event.getPlayer());
                if (event.getPlayer().getOpenInventory().getTopInventory().equals(inventory)) {
                    event.setCancelled(true);
                }
            }
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

    /**
     * A functional interface for specifying how closing the inventory should be treated
     * by implementing the {@link #onClose(Close)} method.
     * The method should return whether the close should go back or not.
     */
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
                String combined = replaceVars(player, Arrays.stream(text)
                        .map(s -> s == null ? " " : s)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("\n")));
                String[] lines = combined.split("\n");
                if (text[0] != null) {
                    getItemNameSetter().accept(meta, lines[0]);
                }
                if (lines.length > 1) {
                    getItemLoreSetter().accept(meta, Arrays.asList(Arrays.copyOfRange(lines, 1, lines.length)));
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
     * @param player        The player viewing the GUI
     * @param text          The text to replace the placeholders in
     * @param replacements  Additional replacements. i = placeholder, i+1 = replacements
     * @return      The text with all placeholders replaced
     */
    public String replaceVars(@NotNull HumanEntity player, @NotNull String text, String... replacements) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            map.putIfAbsent(replacements[i], replacements[i + 1]);
        }

        map.putIfAbsent("plugin", plugin.getName());
        try {
            map.putIfAbsent("owner", owner instanceof Nameable ? ((Nameable) owner).getCustomName() : "");
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
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
        if (!(click.getRawEvent() instanceof InventoryClickEvent)) {
            // Only a click event can trigger the collection to the cursor
            return;
        }
        InventoryClickEvent event = (InventoryClickEvent) click.getRawEvent();

        ItemStack newCursor = click.getCursor().clone();
    
        boolean itemInGui = false;
        for (int i = 0; i < click.getRawEvent().getView().getTopInventory().getSize(); i++) {
            if (i != event.getRawSlot()) {
                ItemStack viewItem = click.getRawEvent().getView().getTopInventory().getItem(i);
                if (newCursor.isSimilar(viewItem)) {
                    itemInGui = true;
                }
                GuiElement element = getElement(i);
                if (element instanceof GuiStorageElement) {
                    GuiStorageElement storageElement = (GuiStorageElement) element;
                    ItemStack otherStorageItem = storageElement.getStorageItem(click.getWhoClicked(), i);
                    if (storageElement.validateItemTake(i, otherStorageItem)) {
                        int resultSize = addToStack(newCursor, otherStorageItem);
                        if (resultSize > -1) {
                            if (resultSize == 0) {
                                otherStorageItem = null;
                            }
                            storageElement.setStorageItem(click.getWhoClicked(), i, otherStorageItem);
                            if (newCursor.getAmount() == newCursor.getMaxStackSize()) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    
        if (itemInGui) {
            event.setCurrentItem(null);
            event.setCancelled(true);
            if (click.getWhoClicked() instanceof Player) {
                ((Player) click.getWhoClicked()).updateInventory();
            }
        
            if (click.getElement() instanceof GuiStorageElement) {
                ((GuiStorageElement) click.getElement()).setStorageItem(click.getWhoClicked(), click.getSlot(), null);
            }
    
            if (newCursor.getAmount() < newCursor.getMaxStackSize()) {
                Inventory bottomInventory = event.getView().getBottomInventory();
                for (int i = 0; i < bottomInventory.getContents().length; i++) {
                    ItemStack bottomItem = bottomInventory.getItem(i);
                    int resultSize = addToStack(newCursor, bottomItem);
                    if (resultSize > -1) {
                        if (resultSize == 0) {
                            bottomInventory.setItem(i, null);
                        }
                        if (newCursor.getAmount() == newCursor.getMaxStackSize()) {
                            break;
                        }
                    }
                }
            }
            event.setCursor(newCursor);
            draw();
        }
    }
    
    /**
     * Add items to a stack up to the max stack size
     * @param item  The base item
     * @param add   The item stack to add
     * @return the result amount of the <code>add</code> ItemStack; <code>-1</code> if these stacks can't be merged
     */
    private static int addToStack(ItemStack item, ItemStack add) {
        if (item.isSimilar(add)) {
            int newAmount = item.getAmount() + add.getAmount();
            if (newAmount > item.getMaxStackSize()) {
                item.setAmount(item.getMaxStackSize());
                add.setAmount(newAmount - item.getAmount());
                return add.getAmount();
            } else {
                item.setAmount(newAmount);
                add.setAmount(0);
                return 0;
            }
        }
        return -1;
    }

    public static class InventoryCreator {
        private final CreatorImplementation<InventoryType> typeCreator;
        private final CreatorImplementation<Integer> sizeCreator;

        /**
         * A new inventory creator which should be able to create an inventory based on the type and the size.
         * <br><br>
         * By default the creators are implemented as follows:
         * <pre>
         * typeCreator = (gui, who, type) -> plugin.getServer().createInventory(new Holder(gui), type, gui.replaceVars(who, title));
         * sizeCreator = (gui, who, size) -> plugin.getServer().createInventory(new Holder(gui), size, gui.replaceVars(who, title));
         * </pre>
         * @param typeCreator The type creator.
         * @param sizeCreator The size creator
         */
        public InventoryCreator(CreatorImplementation<InventoryType> typeCreator, CreatorImplementation<Integer> sizeCreator) {
            this.typeCreator = typeCreator;
            this.sizeCreator = sizeCreator;
        }

        public CreatorImplementation<InventoryType> getTypeCreator() {
            return typeCreator;
        }

        public CreatorImplementation<Integer> getSizeCreator() {
            return sizeCreator;
        }

        public interface CreatorImplementation<T> {
            /**
             * Creates a new inventory
             * @param gui   The InventoryGui instance
             * @param who   The player to create the inventory for
             * @param t     The size or type of the inventory
             * @return      The created inventory
             */
            Inventory create(InventoryGui gui, HumanEntity who, T t);
        }
    }
}
