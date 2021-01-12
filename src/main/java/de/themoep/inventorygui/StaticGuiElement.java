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
import org.bukkit.inventory.ItemStack;

/**
 * Represents a simple element in a gui to which an action can be assigned.
 * If you want the item to change on click you have to do that yourself.
 */
public class StaticGuiElement extends GuiElement {
    private ItemStack item;
    private int number;
    private String[] text;
    
    /**
     * Represents an element in a gui
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item this element displays
     * @param number    The number, 1 will not display the number
     * @param action    The action to run when the player clicks on this element
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replace} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     * @throws IllegalArgumentException If the number is below 1 or above the max stack count (currently 64)
     */
    public StaticGuiElement(char slotChar, ItemStack item, int number, Action action, String... text) throws IllegalArgumentException {
        super(slotChar, action);
        this.item = item;
        this.text = text;
        setNumber(number);
    }
    
    /**
     * Represents an element in a gui
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item this element displays
     * @param action    The action to run when the player clicks on this element
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public StaticGuiElement(char slotChar, ItemStack item, Action action, String... text) {
        this(slotChar, item, item != null ? item.getAmount() : 1, action, text);
    }

    /**
     * Represents an element in a gui that doesn't have any action when clicked
     * @param slotChar  The character to replace in the gui setup string
     * @param item      The item this element displays
     * @param text      The text to display on this element, placeholders are automatically
     *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                  placeholder variables. Empty text strings are also filter out, use
     *                  a single space if you want to add an empty line!<br>
     *                  If it's not set/empty the item's default name will be used
     */
    public StaticGuiElement(char slotChar, ItemStack item, String... text) {
        this(slotChar, item, item != null ? item.getAmount() : 1, null, text);
    }


    /**
     * Set the item that is displayed by this element
     * @param item  The item that should be displayed by this element
     */
    public void setItem(ItemStack item) {
        this.item = item;
    }

    /**
     * Get the raw item displayed by this element which was passed to the constructor or set with {@link #setItem(ItemStack)}.
     * This item will not have the amount or text applied! Use {@link #getItem(HumanEntity, int)} for that!
     * @return  The raw item
     */
    public ItemStack getRawItem() {
        return item;
    }

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        if (item == null) {
            return null;
        }
        ItemStack clone = item.clone();
        gui.setItemText(who, clone, getText());
        if (number > 0 && number <= 64) {
            clone.setAmount(number);
        }
        return clone;
    }

    /**
     * Set this element's display text. If this is an empty array the item's name will be displayed
     * @param text  The text to display on this element, placeholders are automatically
     *              replaced, see {@link InventoryGui#replaceVars} for a list of the
     *              placeholder variables. Empty text strings are also filter out, use
     *              a single space if you want to add an empty line!<br>
     *              If it's not set/empty the item's default name will be used
     */
    public void setText(String... text) {
        this.text = text;
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
     * @return          <code>true</code> if the number was set; <code>false</code> if it was below 1 or above 64
     */
    public boolean setNumber(int number) {
        if (number < 1 || number > 64) {
            this.number = 1;
            return false;
        }
        this.number = number;
        return true;
    }

    /**
     * Get the number that this element should display
     * @return The number (item amount) that this element currently has
     */
    public int getNumber() {
        return number;
    }

}
