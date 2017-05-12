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

import org.bukkit.inventory.ItemStack;

public class GuiStateElement extends GuiElement {
    private int currentState = 0;
    private final State[] states;

    public GuiStateElement(char slotChar, State... states) {
        super(slotChar, null);
        if (states.length == 0) {
            throw new IllegalArgumentException("You need to add at least one State!");
        }
        this.states = states;

        setAction(click -> {
            nextState();
            getState().onChange(click);
            return true;
        });
    }

    /**
     * Loop through the states of this element
     */
    public void nextState() {
        currentState = states.length > currentState + 1 ? currentState + 1 : 0;
    }

    /**
     * Loop through the states of this element backwards
     */
    public void previousState() {
        currentState = currentState > 0 ? currentState - 1 : states.length - 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getState().getItem();
    }

    @Override
    public void setText(String... text) {
        getState().setText(text);
    }

    /**
     * Get the current state of this element
     * @return  The current state of this element
     */
    public State getState() {
        return states[currentState];
    }

    public void setState(String key) {
        for (int i = 0; i < states.length; i++) {
            if (states[i].getKey().equals(key)) {
                currentState = i;
                return;
            }
        }
        throw new IllegalArgumentException("This element does not have the state " + key);
    }

    public abstract static class State {
        private final String key;
        private final ItemStack item;
        private String[] text;

        public State(String key, ItemStack item, String... text) {
            this.key = key;
            this.item = item;
            setText(text);
        }

        /**
         * Set this element's display text. If this is an empty array the item's name will be displayed
         * @param text  The text to display on this element
         */
        public void setText(String... text) {
            this.text = text;
            InventoryGui.setItemText(item, text);
        }

        public ItemStack getItem() {
            return item;
        }

        public String getKey() {
            return key;
        }

        public String[] getText() {
            return text;
        }

        /**
         * What should happen when the state changes to this state
         * @param click The click that triggered this change
         */
        public abstract void onChange(Click click);
    }
}
