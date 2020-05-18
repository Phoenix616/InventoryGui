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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of multiple elements
 */
public class GuiElementGroup extends GuiElement {
    private List<GuiElement> elements = new ArrayList<>();
    private GuiElement filler = null;
    
    /**
     * A group of elements
     * @param slotChar  The character to replace in the gui setup string
     * @param elements  The elements in this group
     */
    public GuiElementGroup(char slotChar, GuiElement... elements) {
        super(slotChar, null);
        setAction(click -> {
            GuiElement element = getElement(click.getSlot(), click.getGui().getPageNumber());
            if (element != null && element.getAction(click.getEvent().getWhoClicked()) != null) {
                return element.getAction(click.getEvent().getWhoClicked()).onClick(click);
            }
            return true;
        });
        Collections.addAll(this.elements, elements);
    }

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        GuiElement element = getElement(slot, gui.getPageNumber());
        if (element != null) {
            return element.getItem(who, slot);
        }
        return null;
    }
    
    @Override
    public void setGui(InventoryGui gui) {
        super.setGui(gui);
        for (GuiElement element : elements) {
            element.setGui(gui);
        }
        if (filler != null) {
            filler.setGui(gui);
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
     * Add elements to this group
     * @param elements  The elements to add
     */
    public void addElements(GuiElement... elements){
        for (GuiElement element : elements) {
            addElement(element);
        }
    }
    
    /**
     * Add elements to this group
     * @param elements  The elements to add
     */
    public void addElements(Collection<GuiElement> elements){
        for (GuiElement element : elements) {
            addElement(element);
        }
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
        return filler;
    }

    /**
     * Removes all elements in the group
     */
    public void clearElements() {
        elements.clear();
    }
    
    /**
     * Set the filler element for empty slots
     * @param item The item for the filler element
     */
    public void setFiller(ItemStack item) {
        filler = new StaticGuiElement(' ', item, " ");
        filler.setGui(gui);
    }
    
    /**
     * Set the filler element for empty slots
     * @param filler The item for the filler element
     */
    public void setFiller(GuiElement filler) {
        this.filler = filler;
    }
    
    /**
     * Get the filler element
     * @return The filler element
     */
    public GuiElement getFiller() {
        return filler;
    }

    /**
     * Get the size of this group
     * @return  The amount of elements that this group has
     */
    public int size() {
        return elements.size();
    }
}
