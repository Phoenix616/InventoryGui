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
 * Represents a group of multiple elements. Will be left-aligned by default.
 */
public class GuiElementGroup extends GuiElement {
    private List<GuiElement> elements = new ArrayList<>();
    private GuiElement filler = null;
    private Alignment alignment = Alignment.LEFT;
    
    /**
     * A group of elements
     * @param slotChar  The character to replace in the gui setup string
     * @param elements  The elements in this group
     */
    public GuiElementGroup(char slotChar, GuiElement... elements) {
        super(slotChar, null);
        setAction(click -> {
            GuiElement element = getElement(click.getSlot(), click.getGui().getPageNumber(click.getWhoClicked()));
            if (element != null && element.getAction(click.getEvent().getWhoClicked()) != null) {
                return element.getAction(click.getEvent().getWhoClicked()).onClick(click);
            }
            return true;
        });
        Collections.addAll(this.elements, elements);
    }

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        GuiElement element = getElement(slot, gui.getPageNumber(who));
        if (element != null) {
            return element.getItem(who, slot);
        }
        return null;
    }
    
    @Override
    public void setGui(InventoryGui gui) {
        super.setGui(gui);
        for (GuiElement element : elements) {
            if (element != null) {
                element.setGui(gui);
            }
        }
        if (filler != null) {
            filler.setGui(gui);
        }
    }
    
    @Override
    public void setSlots(int[] slots) {
        super.setSlots(slots);
        for (GuiElement element : elements) {
            if (element != null) {
                element.setSlots(slots);
            }
        }
    }

    /**
     * Add an element to this group
     * @param element   The element to add
     */
    public void addElement(GuiElement element){
        elements.add(element);
        if (element != null) {
            element.setGui(gui);
            element.setSlots(slots);
        }
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
     * @return      The GuiElement in that slot or <code>null</code>
     */
    public GuiElement getElement(int slot) {
        return getElement(slot, 0);
    }

    /**
     * Get the element in a certain slot on a certain page
     * @param slot          The slot to get the element for
     * @param pageNumber    The number of the page that the gui is on
     * @return              The GuiElement in that slot or <code>null</code>
     */
    public GuiElement getElement(int slot, int pageNumber) {
        if (elements.isEmpty()) {
            return null;
        }
        int index = getSlotIndex(slot, slots.length < elements.size() ? pageNumber : 0);
        if (index > -1) {
            if (alignment == Alignment.LEFT) {
                if (index < elements.size()) {
                    return elements.get(index);
                }
            } else {
                int lineWidth = getLineWidth(slot);
                int linePosition = getLinePosition(slot);
                if (elements.size() - index > lineWidth - linePosition) {
                    return elements.get(index);
                }
                int rest = elements.size() - (index - linePosition);
                int blankBefore = alignment == Alignment.CENTER ? (lineWidth - rest) / 2 : lineWidth - rest;
                if (linePosition < blankBefore || index - blankBefore >= elements.size()) {
                    return filler;
                }
                return elements.get(index - blankBefore);
            }
        }
        return filler;
    }

    /**
     * Get the width of the line the slot is in
     * @param slot The slot
     * @return The width of the line in the GUI setup of this group
     */
    private int getLineWidth(int slot) {
        int width = gui.getWidth();
        int row = slot / width;

        int amount = 0;
        for (int s : slots) {
            if (s >= row * width && s < (row + 1) * width) {
                amount++;
            }
        }
        return amount;
    }

    /**
     * Get the position of the slot in its line
     * @param slot The slot ID
     * @return The line position or -1 if not in its line. wat
     */
    private int getLinePosition(int slot) {
        int width = gui.getWidth();
        int row = slot / width;

        int position = -1;
        for (int s : slots) {
            if (s >= row * width && s < (row + 1) * width) {
                position++;
                if (s == slot) {
                    return position;
                }
            }
        }
        return position;
    }

    /**
     * Get all elements of this group. This list is immutable, use {@link #addElement(GuiElement)}
     * and {@link #clearElements()} to modify the elements in this group.
     * @return An immutable list of all elements in this group
     */
    public List<GuiElement> getElements() {
        return Collections.unmodifiableList(elements);
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
        if (filler != null) {
            filler.setGui(gui);
        }
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

    /**
     * Set the alignment of the elements in this group
     * @param alignment The alignment
     */
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    /**
     * Get the alignment of the elements in this group
     * @return The alignment
     */
    public Alignment getAlignment() {
        return alignment;
    }

    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT;
    }
}
