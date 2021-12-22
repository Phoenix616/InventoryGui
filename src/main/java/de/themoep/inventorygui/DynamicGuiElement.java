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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an element in a gui that will query all it's data when drawn.
 */
public class DynamicGuiElement extends GuiElement {
    private Function<HumanEntity, GuiElement> query;

    private Map<UUID, CacheEntry> cachedElements = new HashMap<>();

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
     * Query this element's state for every player who had it cached
     */
    public void update() {
        for (UUID playerId : new ArrayList<>(cachedElements.keySet())) {
            Player p = gui.getPlugin().getServer().getPlayer(playerId);
            if (p != null && p.isOnline()) {
                update(p);
            } else {
                cachedElements.remove(playerId);
            }
        }
    }

    /**
     * Query this element's state for a certain player
     * @param player The player for whom to update the element
     */
    public CacheEntry update(HumanEntity player) {
        CacheEntry cacheEntry = new CacheEntry(queryElement(player));
        if (cacheEntry.element instanceof DynamicGuiElement) {
            ((DynamicGuiElement) cacheEntry.element).update(player);
        } else if (cacheEntry.element instanceof GuiElementGroup) {
            InventoryGui.updateElements(player, ((GuiElementGroup) cacheEntry.element).getElements());
        }
        cachedElements.put(player.getUniqueId(), cacheEntry);
        return cacheEntry;
    }
    
    @Override
    public void setGui(InventoryGui gui) {
        super.setGui(gui);
    }
    
    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        GuiElement element = getCachedElement(who);
        return element != null ? element.getItem(who, slot) : null;
    }
    
    @Override
    public Action getAction(HumanEntity who) {
        GuiElement element = getCachedElement(who);
        return element != null ? element.getAction(who) : null;
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
     * Query the element for a player
     * @param who The player
     * @return The GuiElement or null
     */
    public GuiElement queryElement(HumanEntity who) {
        GuiElement element = getQuery().apply(who);
        if (element != null) {
            element.setGui(gui);
            element.setSlots(slots);
        }
        return element;
    }
    
    /**
     * Get the cached element, creates a new one if there is none for that player.
     * Use {@link #getLastCached(HumanEntity)} to check if a player has something cached.
     * @param who The player to get the element for
     * @return The element that is currently cached
     */
    public GuiElement getCachedElement(HumanEntity who) {
        CacheEntry cached = cachedElements.get(who.getUniqueId());
        if (cached == null) {
            cached = update(who);
        }
        return cached.getElement();
    }

    /**
     * Remove the cached element if the player has one.
     * @param who The player to remove the cached element for
     * @return The element that was cached or null if none was cached
     */
    public GuiElement removeCachedElement(HumanEntity who) {
        CacheEntry cached = cachedElements.remove(who.getUniqueId());
        return cached != null ? cached.getElement() : null;
    }
    
    /**
     * Get the time at which this element was last cached for a certain player
     * @param who The player to get the last cache time for
     * @return  The timestamp from when it was last cached or -1 if it wasn't cached
     */
    public long getLastCached(HumanEntity who) {
        CacheEntry cached = cachedElements.get(who.getUniqueId());
        return cached != null ? cached.getCreated() : -1;
    }

    public class CacheEntry {
        private final GuiElement element;
        private final long created = System.currentTimeMillis();

        CacheEntry(GuiElement element) {
            this.element = element;
        }

        public GuiElement getElement() {
            return element;
        }

        public long getCreated() {
            return created;
        }
    }
}
