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

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of multiple elements
 */
public class GuiElementGroup extends GuiElement {
    private List<GuiElement> elements = new ArrayList<>();

    /**
     * A group of elements
     * @param slotChar  The character to replace in the gui setup string
     * @param elements  The elements in this group
     */
    public GuiElementGroup(char slotChar, GuiElement... elements) {
        super(slotChar, null);
        setAction(click -> {
            GuiElement element = getElement(click.getSlot(), click.getGui().getPageNumber());
            if (element != null && element.getAction() != null) {
                return element.getAction().onClick(click);
            }
            return true;
        });
        Collections.addAll(this.elements, elements);
    }

    @Override
    public ItemStack getItem(int slot) {
        GuiElement element = getElement(slot, gui.getPageNumber());
        if (element != null) {
            return element.getItem(slot);
        }
        return null;
    }
    
    @Override
    public void setGui(InventoryGui gui) {
        super.setGui(gui);
        for (GuiElement element : elements) {
            element.setGui(gui);
        }
    }
    
    @Override
    public void setSlots(int[] slots) {
        super.setSlots(slots);
        for (GuiElement element : elements) {
            element.setSlots(slots);
        }
    }

    /**
     * Add an element to this group
     * @param element   The element to add
     */
    public void addElement(GuiElement element){
        elements.add(element);
        element.setGui(gui);
        element.setSlots(slots);
    }

    /**
     * Get the element in a certain slot
     * @param slot  The slot to get the element for
     * @return      The GuiElement in that slot or <tt>null</tt>
     */
    public GuiElement getElement(int slot) {
        return getElement(slot, 0);
    }

    /**
     * Get the element in a certain slot on a certain page
     * @param slot          The slot to get the element for
     * @param pageNumber    The number of the page that the gui is on
     * @return              The GuiElement in that slot or <tt>null</tt>
     */
    public GuiElement getElement(int slot, int pageNumber) {
        if (elements.isEmpty()) {
            return null;
        }
        int index = getSlotIndex(slot, pageNumber);
        if (index > -1 && index < elements.size()) {
            return elements.get(index);
        }
        GuiElement last = elements.get(elements.size() - 1);
        if (last.getSlotChar() == ' ') { // Check if last element in elements array is filler
            return last;
        }
        return null;
    }

    /**
     * Get the size of this group
     * @return  The amount of elements that this group has
     */
    public int size() {
        return elements.size();
    }
}
