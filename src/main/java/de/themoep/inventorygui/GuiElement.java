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

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an element in a gui
 */
public class GuiElement {
    private final char slotChar;
    private ItemStack item;
    private Action action;
    private int number = 1;
    protected int[] slots = new int[0];
    private String[] text;

    /**
     * Represents an element in a gui
     * @param slotChar The character to replace in the gui setup string
     * @param item     The item this element displays
     * @param action   The action to run when the player clicks on this element
     * @param text     The text to display on this element, if it's not set/empty the item's default name will be used
     */
    public GuiElement(char slotChar, ItemStack item, Action action, String... text) {
        this.slotChar = slotChar;
        setItem(item);
        setAction(action);
        setText(text);
    }

    /**
     * Represents an element in a gui that doesn't have any action when clicked
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item this element displays
     * @param text      The text to display on this element, if it's not set/empty the item's default name will be used
     */
    public GuiElement(char slotChar, ItemStack item, String... text) {
        this(slotChar, item, null, text);
    }

    /**
     * Get the character in the gui setup that corresponds with this element
     * @return  The character
     */
    public char getSlotChar() {
        return slotChar;
    }

    /**
     * Set the item that is displayed by this element
     * @param item  The item that should be displayed by this element
     */
    public void setItem(ItemStack item) {
        this.item = item;
        setText(getText());
    }

    /**
     * Get the item that is displayed by this element
     * @param slot  The slot to get the item for
     * @return  The ItemStack that is displayed as this element
     */
    public ItemStack getItem(int slot) {
        return item;
    }

    /**
     * Get the action that is executed when clicking on this element
     * @return      The action to run
     */
    public Action getAction() {
        return action;
    }

    /**
     * Set the action that is executed when clicking on this element
     * @param action    The action to run
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * Set this element's display text. If this is an empty array the item's name will be displayed
     * @param text  The text to display on this element
     */
    public void setText(String... text) {
        this.text = text;
        InventoryGui.setItemText(item, text);
    }

    /**
     * Get the text that this element displays
     * @return  The text that is displayed on this element
     */
    public String[] getText() {
        return text;
    }

    /**
     * Set the number that this element should display (via the Item's amount)
     * @param number    The number, 1 will not display the number
     * @throws IllegalArgumentException If the number is below 1 or above the max stack count (currently 64)
     */
    public void setNumber(int number) throws IllegalArgumentException {
        if (number < 1 || number > 64) {
            throw new IllegalArgumentException("Only numbers from 1 to 64 are allowed. (" + number + ")");
        }
        this.number = number;
        this.item.setAmount(number);
    }

    /**
     * Get the number that this element should display
     * @return The number (item amount) that this element currently has
     */
    public int getNumber() {
        return number;
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
     * @return      The index in the list of slots that this id has or <tt>-1</tt> if it isn't in that list
     */
    public int getSlotIndex(int slot) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return i;
            }
        }
        return -1;
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
        private final GuiElement element;
        private final ClickType type;
        private final InventoryClickEvent event;

        public Click(InventoryGui gui, int slot, GuiElement element, ClickType type, InventoryClickEvent event) {
            this.gui = gui;
            this.slot = slot;
            this.element = element;
            this.type = type;
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
            return type;
        }

        /**
         * Get the event of the click
         * @return  The InventoryClickEvent associated with this Click
         */
        public InventoryClickEvent getEvent() {
            return event;
        }

        public InventoryGui getGui() {
            return gui;
        }
    }
}
