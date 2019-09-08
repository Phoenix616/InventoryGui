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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an element in a gui that will query all it's data when drawn.
 */
public class DynamicGuiElement extends GuiElement {
    private Function<HumanEntity, GuiElement> query;
    private GuiElement cachedElement;
    private long lastCached = 0;
    
    /**
     * Represents an element in a gui that will query all it's data when drawn.
     * @param slotChar  The character to replace in the gui setup string
     * @param query     Query the element data, this should return an element with the information
     */
    public DynamicGuiElement(char slotChar, Supplier<GuiElement> query) {
        this(slotChar, (h) -> query.get());
    }

    /**
     * Represents an element in a gui that will query all it's data when drawn.
     * @param slotChar  The character to replace in the gui setup string
     * @param query     Query the element data, this should return an element with the information and handle null players properly
     */
    public DynamicGuiElement(char slotChar, Function<HumanEntity, GuiElement> query) {
        super(slotChar);
        this.query = query;
    }
    
    /**
     * Query this element's state even if it shouldn't be done yet
     * @deprecated Use {@link #update(HumanEntity)}
     */
    @Deprecated
    public void update() {
        update(null);
    }

    /**
     * Query this element's state even if it shouldn't be done yet
     */
    public void update(HumanEntity player) {
        lastCached = System.currentTimeMillis();
        cachedElement = query.apply(player);
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
    public ItemStack getItem(HumanEntity who, int slot) {
        update(who);
        return cachedElement != null ? cachedElement.getItem(who, slot) : null;
    }
    
    @Override
    public Action getAction(HumanEntity who) {
        update(who);
        return cachedElement != null ? cachedElement.getAction(who) : null;
    }
    
    /**
     * Get the supplier for this element's content
     * @return The supplier query
     */
    public Function<HumanEntity, GuiElement> getQuery() {
        return query;
    }
    
    /**
     * Set the supplier for this element's content
     * @param query The supplier query to set
     */
    public void setQuery(Function<HumanEntity, GuiElement> query) {
        this.query = query;
    }
    
    /**
     * Get the cached element, creates a new one if there is none
     * @param who The player to get the element for
     * @return The element that is currently cached
     */
    public GuiElement getCachedElement(HumanEntity who) {
        if (cachedElement == null) {
            update(who);
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
