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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

/**
 * This element is used to access an {@link Inventory}. The slots in the inventory are selected
 * by searching through the whole gui the element is in and getting the number of the spot
 * in the character group that this element is in. <br>
 * E.g. if you have five characters called "s" in the gui setup and the second element is
 * accessed by the player then it will translate to the second slot in the inventory.
 */
public class GuiStorageElement extends GuiElement {
    private final Inventory storage;
    private final int invSlot;
    private Runnable applyStorage;
    private Function<ValidatorInfo, Boolean> itemValidator;
    
    /**
     * An element used to access an {@link Inventory}.
     * @param slotChar  The character to replace in the gui setup string.
     * @param storage   The {@link Inventory} that this element is linked to.
     */
    public GuiStorageElement(char slotChar, Inventory storage) {
        this(slotChar, storage, -1);
    }
    
    /**
     * An element used to access a specific slot in an {@link Inventory}.
     * @param slotChar  The character to replace in the gui setup string.
     * @param storage   The {@link Inventory} that this element is linked to.
     * @param invSlot   The index of the slot to access in the {@link Inventory}.
     */
    public GuiStorageElement(char slotChar, Inventory storage, int invSlot) {
        this(slotChar, storage, invSlot, null, null);
    }
    
    /**
     * An element used to access a specific slot in an {@link Inventory}.
     * @param slotChar      The character to replace in the gui setup string.
     * @param storage       The {@link Inventory} that this element is linked to.
     * @param invSlot       The index of the slot to access in the {@link Inventory}.
     * @param applyStorage  Apply the storage that this element represents.
     * @param itemValidator Should return <code>false</code> for items that should not work in that slot
     *                      Can be null if the storage is directly linked.
     */
    public GuiStorageElement(char slotChar, Inventory storage, int invSlot, Runnable applyStorage, Function<ValidatorInfo, Boolean> itemValidator) {
        super(slotChar, null);
        this.invSlot = invSlot;
        this.applyStorage = applyStorage;
        this.itemValidator = itemValidator;
        setAction(click -> {
            if (getStorageSlot(click.getWhoClicked(), click.getSlot()) < 0) {
                return true;
            }
            ItemStack storageItem = getStorageItem(click.getWhoClicked(), click.getSlot());
            ItemStack slotItem = click.getRawEvent().getView().getTopInventory().getItem(click.getSlot());
            if (slotItem == null && storageItem != null && storageItem.getType() != Material.AIR
                    || storageItem == null && slotItem != null && slotItem.getType() != Material.AIR
                    || storageItem != null && !storageItem.equals(slotItem)) {
                gui.draw(click.getWhoClicked(), false);
                return true;
            }

            if (!(click.getRawEvent() instanceof InventoryClickEvent)) {
                // Only the click event will be handled here, drag event is handled separately
                return true;
            }

            InventoryClickEvent event = (InventoryClickEvent) click.getRawEvent();

            ItemStack movedItem = null;
            switch (event.getAction()) {
                case NOTHING:
                case CLONE_STACK:
                    return false;
                case MOVE_TO_OTHER_INVENTORY:
                    if (event.getRawSlot() < click.getRawEvent().getView().getTopInventory().getSize()) {
                        // Moved from storage

                        // Check if there is actually space (more advanced checks can unfortunately not be supported right now)
                        if (click.getRawEvent().getView().getBottomInventory().firstEmpty() == -1) {
                            // No empty slot, cancel
                            return true;
                        }
                        movedItem = null;
                    } else {
                        // Moved to storage

                        // Check if there is actually space (more advanced checks can unfortunately not be supported right now)
                        if (click.getRawEvent().getView().getTopInventory().firstEmpty() == -1) {
                            // No empty slot, cancel
                            return true;
                        }
                        movedItem = event.getCurrentItem();
                    }
                    // Update GUI to avoid display glitches
                    gui.runTask(gui::draw);
                    break;
                case HOTBAR_MOVE_AND_READD:
                case HOTBAR_SWAP:
                    int button = event.getHotbarButton();
                    if (button < 0) {
                        return true;
                    }
                    ItemStack hotbarItem = click.getRawEvent().getView().getBottomInventory().getItem(button);
                    if (hotbarItem != null) {
                        movedItem = hotbarItem.clone();
                    }
                    break;
                case PICKUP_ONE:
                case DROP_ONE_SLOT:
                    if (event.getCurrentItem() != null) {
                        movedItem = event.getCurrentItem().clone();
                        movedItem.setAmount(movedItem.getAmount() - 1);
                    }
                    break;
                case DROP_ALL_SLOT:
                    movedItem = null;
                    break;
                case PICKUP_HALF:
                    if (event.getCurrentItem() != null) {
                        movedItem = event.getCurrentItem().clone();
                        movedItem.setAmount(movedItem.getAmount() / 2);
                    }
                    break;
                case PLACE_SOME:
                    if (event.getCurrentItem() == null) {
                        if (event.getCursor() != null) {
                            movedItem = event.getCursor().clone();
                        }
                    } else {
                        movedItem = event.getCurrentItem().clone();
                        int newAmount = movedItem.getAmount() + (event.getCursor() != null ? event.getCursor().getAmount() : 0);
                        if (newAmount < movedItem.getMaxStackSize()) {
                            movedItem.setAmount(newAmount);
                        } else {
                            movedItem.setAmount(movedItem.getMaxStackSize());
                        }
                    }
                    break;
                case PLACE_ONE:
                    if (event.getCursor() != null) {
                        if (event.getCurrentItem() == null) {
                            movedItem = event.getCursor().clone();
                            movedItem.setAmount(1);
                        } else {
                            movedItem = event.getCursor().clone();
                            movedItem.setAmount(event.getCurrentItem().getAmount() + 1);
                        }
                    }
                    break;
                case PLACE_ALL:
                    if (event.getCursor() != null) {
                        movedItem = event.getCursor().clone();
                        if (event.getCurrentItem() != null && event.getCurrentItem().getAmount() > 0) {
                            movedItem.setAmount(event.getCurrentItem().getAmount() + movedItem.getAmount());
                        }
                    }
                    break;
                case PICKUP_ALL:
                case SWAP_WITH_CURSOR:
                    if (event.getCursor() != null) {
                        movedItem = event.getCursor().clone();
                    };
                    break;
                case COLLECT_TO_CURSOR:
                    if (event.getCursor() == null
                            || event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                        return true;
                    }
                    gui.simulateCollectToCursor(click);
                    return false;
                default:
                    click.getRawEvent().getWhoClicked().sendMessage(ChatColor.RED + "The action " + event.getAction() + " is not supported! Sorry about that :(");
                    return true;
            }
            return !setStorageItem(click.getWhoClicked(), click.getSlot(), movedItem);
        });
        this.storage = storage;
    }
    
    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        int index = getStorageSlot(who, slot);
        if (index > -1 && index < storage.getSize()) {
            return storage.getItem(index);
        }
        return null;
    }

    /**
     * Get the {@link Inventory} that this element is linked to.
     * @return  The {@link Inventory} that this element is linked to.
     */
    public Inventory getStorage() {
        return storage;
    }
    
    /**
     * Get the storage slot index that corresponds to the InventoryGui slot
     * @param player    The player which is using the GUI view
     * @param slot      The slot in the GUI
     * @return      The index of the storage slot or <code>-1</code> if it's outside the storage
     */
    private int getStorageSlot(HumanEntity player, int slot) {
        int index = invSlot != -1 ? invSlot : getSlotIndex(slot, gui.getPageNumber(player));
        if (index < 0 || index >= storage.getSize()) {
            return -1;
        }
        return index;
    }
    
    /**
     * Get the item in the storage that corresponds to the InventoryGui slot
     * @param slot      The slot in the GUI
     * @return      The {@link ItemStack} or <code>null</code> if the slot is outside of the item's size
     * @deprecated Use {@link #getStorageItem(HumanEntity, int)}
     */
    @Deprecated
    public ItemStack getStorageItem(int slot) {
        return getStorageItem(null, slot);
    }

    /**
     * Get the item in the storage that corresponds to the InventoryGui slot
     * @param player    The player which is using the GUI view
     * @param slot      The slot in the GUI
     * @return      The {@link ItemStack} or <code>null</code> if the slot is outside of the item's size
     */
    public ItemStack getStorageItem(HumanEntity player, int slot) {
        int index = getStorageSlot(player, slot);
        if (index == -1) {
            return null;
        }
        return storage.getItem(index);
    }
    
    /**
     * Set the item in the storage that corresponds to the InventoryGui slot.
     * @param slot  The slot in the GUI
     * @param item  The {@link ItemStack} to set
     * @return      <code>true</code> if the item was set; <code>false</code> if the slot was outside of this storage
     * @deprecated Use {@link #setStorageItem(HumanEntity, int, ItemStack)}
     */
    @Deprecated
    public boolean setStorageItem(int slot, ItemStack item) {
        return setStorageItem(null, slot, item);
    }

    /**
     * Set the item in the storage that corresponds to the InventoryGui slot.
     * @param player    The player using the GUI view
     * @param slot      The slot in the GUI
     * @param item      The {@link ItemStack} to set
     * @return      <code>true</code> if the item was set; <code>false</code> if the slot was outside of this storage
     */
    public boolean setStorageItem(HumanEntity player, int slot, ItemStack item) {
        int index = getStorageSlot(player, slot);
        if (index == -1) {
            return false;
        }
        if (!validateItem(slot, item)) {
            return false;
        }
        storage.setItem(index, item);
        if (applyStorage != null) {
            applyStorage.run();
        }
        return true;
    }
    
    /**
     * Get the runnable that applies the storage
     * @return The storage applying runnable; might be null
     */
    public Runnable getApplyStorage() {
        return applyStorage;
    }
    
    /**
     * Set what should be done to apply the storage.
     * Not necessary if the storage is directly backed by a real inventory.
     * @param applyStorage  How to apply the storage; can be null if nothing should be done
     */
    public void setApplyStorage(Runnable applyStorage) {
        this.applyStorage = applyStorage;
    }
    
    /**
     * Get the item validator
     * @return  The item validator
     */
    public Function<ValidatorInfo, Boolean> getItemValidator() {
        return itemValidator;
    }
    
    /**
     * Set a function that can validate whether or not an item can fit in the slot
     * @param itemValidator The item validator that takes a {@link ValidatorInfo} and returns <code>true</code> for items that
     *                      should and <code>false</code> for items that should not work in that slot
     */
    public void setItemValidator(Function<ValidatorInfo, Boolean> itemValidator) {
        this.itemValidator = itemValidator;
    }
    
    /**
     * Validate whether or not an item can be put in a slot with the item validator set in {@link #setItemValidator(Function)}
     * @param slot  The slot the item should be tested for
     * @param item  The item to test
     * @return      <code>true</code> for items that should and <code>false</code> for items that should not work in that slot
     */
    public boolean validateItem(int slot, ItemStack item) {
        return itemValidator == null || itemValidator.apply(new ValidatorInfo(this, slot, item));
    }
    
    public static class ValidatorInfo {
        private final GuiElement element;
        private final int slot;
        private final ItemStack item;
    
        public ValidatorInfo(GuiElement element, int slot, ItemStack item) {
            this.item = item;
            this.slot = slot;
            this.element = element;
        }
    
        public GuiElement getElement() {
            return element;
        }
    
        public int getSlot() {
            return slot;
        }
    
        public ItemStack getItem() {
            return item;
        }
    }
}
