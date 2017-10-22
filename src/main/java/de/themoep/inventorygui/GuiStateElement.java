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

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * An element that can switch between certain states. It automatically handles the switching
 * of the item in the slot that corresponds to the state that the element is in.
 */
public class GuiStateElement extends GuiElement {
    private int currentState;
    private final State[] states;

    /**
     * An element that can switch between certain states.
     * @param slotChar      The character to replace in the gui setup string.
     * @param defaultState  The index of the default state.
     * @param states        The list of different {@link State}s that this element can have.
     */
    public GuiStateElement(char slotChar, int defaultState, State... states) {
        super(slotChar, null);
        if (states.length == 0) {
            throw new IllegalArgumentException("You need to add at least one State!");
        }
        this.currentState = defaultState;
        this.states = states;

        setAction(click -> {
            nextState();
            click.getEvent().setCurrentItem(getState().getItem());
            if (click.getEvent().getWhoClicked() instanceof Player) {
                Player player = (Player) click.getEvent().getWhoClicked();
                player.playSound(player.getEyeLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
            }
            getState().change.onChange(click);
            return true;
        });
    }

    /**
     * An element that can switch between certain states.
     * @param slotChar      The character to replace in the gui setup string.
     * @param defaultState  The key of the default state.
     * @param states        The list of different {@link State}s that this element can have.
     */
    public GuiStateElement(char slotChar, String defaultState, State... states) {
        this(slotChar, getStateIndex(defaultState, states), states);
    }

    /**
     * An element that can switch between certain states. The first state will be the default one.
     * @param slotChar      The character to replace in the gui setup string.
     * @param states        The list of different {@link State}s that this element can have.
     */
    public GuiStateElement(char slotChar, State... states) {
        this(slotChar, 0, states);
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
    public void setGui(InventoryGui gui) {
        super.setGui(gui);
        for (State state : states) {
            state.setGui(gui);
        }
    }

    /**
     * Get the current state of this element
     * @return  The current state of this element
     */
    public State getState() {
        return states[currentState];
    }

    /**
     * Set the current state with the state's key. Does not trigger the state's change.
     * @param key       The key to search for.
     * @throws IllegalArgumentException Thrown if there is no state with the provided key.
     */
    public void setState(String key) throws IllegalArgumentException {
        currentState = getStateIndex(key, states);
    }

    /**
     * Get the index of a state from a key
     * @param key       The key to search for.
     * @param states    The states to search in.
     * @return          The index of that key in the state array.
     * @throws IllegalArgumentException Thrown if there is no state with the provided key.
     */
    private static int getStateIndex(String key, State[] states) throws IllegalArgumentException {
        for (int i = 0; i < states.length; i++) {
            if (states[i].getKey().equals(key)) {
                return i;
            }
        }
        throw new IllegalArgumentException("This element does not have the state " + key);
    }

    /**
     * A state that the {@link GuiStateElement} can have.
     */
    public static class State {
        private final Change change;
        private final String key;
        private final ItemStack item;
        private String[] text;
        private InventoryGui gui;

        /**
         * A state that the {@link GuiStateElement} can have.
         * @param change    What to do when the state changes
         * @param key       The state's string key
         * @param item      The {@link ItemStack} to represent this state
         * @param text      The text lines that describe this state
         */
        public State(Change change, String key, ItemStack item, String... text) {
            this.change = change;
            this.key = key;
            this.item = item;
            this.text = text;
        }

        /**
         * Set this element's display text. If this is an empty array the item's name will be displayed
         * @param text  The text lines that describe this state
         */
        public void setText(String... text) {
            this.text = text;
        }

        /**
         * Get the {@link ItemStack} that represents this state.
         * @return The {@link ItemStack} that represents this state
         */
        public ItemStack getItem() {
            ItemStack clone = item.clone();
            gui.setItemText(clone, text);
            return clone;
        }

        /**
         * Get the string key of the state.
         * @return The state's string key
         */
        public String getKey() {
            return key;
        }

        /**
         * Get the text lines that describe this state.
         * @return
         */
        public String[] getText() {
            return text;
        }

        private void setGui(InventoryGui gui) {
            this.gui = gui;
        }

        /**
         * Define what should happen when the state of the element' state changes to this state
         */
        public interface Change {

            /**
             * What should happen when the element's state changes to this state
             * @param click The click that triggered this change
             */
            void onChange(Click click);
        }
    }
}
