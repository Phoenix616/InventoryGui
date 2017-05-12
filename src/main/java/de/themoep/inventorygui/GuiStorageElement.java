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

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiStorageElement extends GuiElement {
    private final Inventory storage;

    public GuiStorageElement(char slotChar, Inventory storage) {
        super(slotChar, null);
        setAction(click -> {
            int index = getSlotIndex(click.getSlot());
            if (index == -1 || index >= storage.getSize()) {
                return true;
            }
            ItemStack storageItem = storage.getItem(index);
            if (click.getEvent().getCurrentItem() == null && storageItem != null || storageItem != null && !storageItem.equals(click.getEvent().getCurrentItem())) {
                click.getEvent().setCancelled(true);
                click.getGui().draw();
                return false;
            }
            storage.setItem(index, click.getEvent().getCursor());
            return false;
        });
        this.storage = storage;
    }

    @Override
    public ItemStack getItem(int slot) {
        int index = getSlotIndex(slot);
        if (index > -1 && index < storage.getSize()) {
            return storage.getItem(index);
        }
        return null;
    }
}
