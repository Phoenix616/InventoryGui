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

import org.apache.commons.lang.Validate;
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

    /**
     * Add an element to this group after this group element has been assigned to a GUI
     * @param element   The element to add
     * @throws IllegalArgumentException Thrown if trying to add an element before this group element is assigned to a GUI
     */
    public void addElement(GuiElement element) throws IllegalArgumentException {
        Validate.notNull(gui, "You have to add the group element to the GUI before adding elements to it!");
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
