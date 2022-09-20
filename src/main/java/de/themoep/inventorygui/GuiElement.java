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

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an element in a gui
 */
public abstract class GuiElement {
    private final char slotChar;
    private Action action;
    protected int[] slots = new int[0];
    protected InventoryGui gui;

    /**
     * Represents an element in a gui
     * @param slotChar The character to replace in the gui setup string
     * @param action   The action to run when the player clicks on this element
     */
    public GuiElement(char slotChar, Action action) {
        this.slotChar = slotChar;
        setAction(action);
    }

    /**
     * Represents an element in a gui that doesn't have any action when clicked
     * @param slotChar  The character to replace in the gui setup string
     */
    public GuiElement(char slotChar) {
        this(slotChar, null);
    }

    /**
     * Get the character in the gui setup that corresponds with this element
     * @return  The character
     */
    public char getSlotChar() {
        return slotChar;
    }

    /**
     * Get the item that is displayed by this element on a certain page
     * @param who   The player who views the page
     * @param slot  The slot to get the item for
     * @return      The ItemStack that is displayed as this element
     */
    public abstract ItemStack getItem(HumanEntity who, int slot);

    /**
     * Get the action that is executed when clicking on this element
     * @param who   The player who views the page
     * @return      The action to run
     */
    public Action getAction(HumanEntity who) {
        return action;
    }

    /**
     * Set the action that is executed when clicking on this element
     * @param action    The action to run. The {@link Action#onClick} method should
     *                  return whether or not the click event should be cancelled
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Get the indexes of the lots that this element is displayed in
     * @return An array of the lost indexes
     */
    public int[] getSlots() {
        return slots;
    }

    /**
     * Set the ids of the slots where this element is assigned to
     * @param slots An array of the slot ids where this element is displayed
     */
    public void setSlots(int[] slots) {
        this.slots = slots;
    }

    /**
     * Get the index that this slot has in the list of slots that this element is displayed in
     * @param slot  The id of the slot
     * @return      The index in the list of slots that this id has or <code>-1</code> if it isn't in that list
     */
    public int getSlotIndex(int slot) {
        return getSlotIndex(slot, 0);
    }

    /**
     * Get the index that this slot has in the list of slots that this element is displayed in
     * @param slot          The id of the slot
     * @param pageNumber    The number of the page that the gui is on
     * @return              The index in the list of slots that this id has or <code>-1</code> if it isn't in that list
     */
    public int getSlotIndex(int slot, int pageNumber) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return i + slots.length * pageNumber;
            }
        }
        return -1;
    }

    /**
     * Set the gui this element belongs to
     * @param gui   The GUI that this element is in
     */
    public void setGui(InventoryGui gui) {
        this.gui = gui;
    }

    /**
     * Get the gui this element belongs to
     * @return The GUI that this element is in
     */
    public InventoryGui getGui() {
        return gui;
    }

    public static interface Action {

        /**
         * Executed when a player clicks on an element
         * @param click The Click class containing information about the click
         * @return Whether or not the click event should be cancelled
         */
        boolean onClick(Click click);

    }

    public static class Click {
        private final InventoryGui gui;
        private final int slot;
        private final ClickType clickType;
        private ItemStack cursor;
        private final GuiElement element;
        private final InventoryInteractEvent event;

        public Click(InventoryGui gui, int slot, ClickType clickType, ItemStack cursor, GuiElement element, InventoryInteractEvent event) {
            this.gui = gui;
            this.slot = slot;
            this.clickType = clickType;
            this.cursor = cursor;
            this.element = element;
            this.event = event;
        }

        /**
         * Get the slot of the GUI that was clicked
         * @return  The clicked slot
         */
        public int getSlot() {
            return slot;
        }

        /**
         * Get the element that was clicked
         * @return  The clicked GuiElement
         */
        public GuiElement getElement() {
            return element;
        }

        /**
         * Get the type of the click
         * @return  The type of the click
         */
        public ClickType getType() {
            return clickType;
        }

        /**
         * Get the item on the cursor
         * @return The item on the cursor when this click occurred
         */
        public ItemStack getCursor() {
            return cursor;
        }

        /**
         * Set the item on the cursor after the click
         * @param cursor The new item on the cursor
         */
        public void setCursor(ItemStack cursor) {
            this.cursor = cursor;
        }

        /**
         * Get who clicked the element
         * @return  The player that clicked
         */
        public HumanEntity getWhoClicked() {
            return event.getWhoClicked();
        }

        /**
         * Get the event of the inventory interaction
         * @return  The InventoryInteractEvent associated with this Click
         */
        public InventoryInteractEvent getRawEvent() {
            return event;
        }

        public InventoryGui getGui() {
            return gui;
        }
    }
}
