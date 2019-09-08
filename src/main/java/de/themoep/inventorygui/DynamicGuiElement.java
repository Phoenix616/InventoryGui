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

import java.util.function.Supplier;

/**
 * Represents an element in a gui that will query all it's data when drawn.
 */
public class DynamicGuiElement extends GuiElement {
    private Supplier<GuiElement> query;
    private GuiElement cachedElement;
    private long lastCached = 0;
    
    /**
     * Represents an element in a gui that will query all it's data when drawn.
     * @param slotChar  The character to replace in the gui setup string
     * @param query     Query the element data, this should return an element with the information
     */
    public DynamicGuiElement(char slotChar, Supplier<GuiElement> query) {
        super(slotChar);
        this.query = query;
        update();
    }
    
    /**
     * Query this element's state even if it shouldn't be done yet
     */
    public void update() {
        lastCached = System.currentTimeMillis();
        cachedElement = query.get();
        if (cachedElement != null) {
            cachedElement.setGui(gui);
            cachedElement.setSlots(slots);
        }
    }
    
    @Override
    public void setGui(InventoryGui gui) {
        super.setGui(gui);
        if (cachedElement != null) {
            cachedElement.setGui(gui);
        }
    }
    
    @Override
    public ItemStack getItem(int slot) {
        update();
        return cachedElement != null ? cachedElement.getItem(slot) : null;
    }
    
    @Override
    public Action getAction() {
        update();
        return cachedElement != null ? cachedElement.getAction() : null;
    }
    
    /**
     * Get the supplier for this element's content
     * @return The supplier query
     */
    public Supplier<GuiElement> getQuery() {
        return query;
    }
    
    /**
     * Set the supplier for this element's content
     * @param query The supplier query to set
     */
    public void setQuery(Supplier<GuiElement> query) {
        this.query = query;
    }
    
    /**
     * Get the cached element, creates a new one if there is none
     * @return The element that is currently cached
     */
    public GuiElement getCachedElement() {
        if (cachedElement == null) {
            update();
        }
        return cachedElement;
    }
    
    /**
     * Get the time at which this element was last cached
     * @return  The timestamp from when it was last cached
     */
    public long getLastCached() {
        return lastCached;
    }
}
