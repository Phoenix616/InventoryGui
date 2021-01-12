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

import java.util.function.Supplier;

/**
 * An element that can switch between certain states. It automatically handles the switching
 * of the item in the slot that corresponds to the state that the element is in.
 */
public class GuiStateElement extends GuiElement {
    private Supplier<Integer> queryState = null;
    private boolean silent = false;
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
            State next = nextState();
            click.getEvent().setCurrentItem(next.getItem(click.getWhoClicked()));
            next.change.onChange(click);
            if (!isSilent()) {
                click.getGui().playClickSound();
            }
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
     * An element that can switch between certain states.
     * @param slotChar      The character to replace in the gui setup string.
     * @param queryState    Supplier for the current state.
     * @param states        The list of different {@link State}s that this element can have.
     */
    public GuiStateElement(char slotChar, Supplier<String> queryState, State... states) {
        this(slotChar, queryState.get(), states);
        this.queryState = () -> getStateIndex(queryState.get(), states);
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
     * @return The new state (next one to the old)
     */
    public State nextState() {
        queryCurrentState();
        currentState = states.length > currentState + 1 ? currentState + 1 : 0;
        return states[currentState];
    }

    /**
     * Loop through the states of this element backwards
     * @return The new state (previous one to the old)
     */
    public State previousState() {
        queryCurrentState();
        currentState = currentState > 0 ? currentState - 1 : states.length - 1;
        return states[currentState];
    }

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        return getState().getItem(who);
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
        queryCurrentState();
        return states[currentState];
    }

    /**
     * Get whether or not this element should make a sound when interacted with
     * @return  Whether or not to make a sound when interacted with
     */
    public boolean isSilent() {
        return silent;
    }

    /**
     * Set whether or not this element should make a sound when interacted with
     * @param silent Whether or not to make a sound when interacted with
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }
    
    /**
     * Try to query the current state if there is a query
     */
    private void queryCurrentState() {
        if (queryState != null) {
            currentState = queryState.get();
        }
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
         * @param text      The text to display on this element, placeholders are automatically
         *                  replaced, see {@link InventoryGui#replaceVars} for a list of the
         *                  placeholder variables. Empty text strings are also filter out, use
         *                  a single space if you want to add an empty line!<br>
         *                  If it's not set/empty the item's default name will be used
         */
        public State(Change change, String key, ItemStack item, String... text) {
            this.change = change;
            this.key = key;
            this.item = item;
            this.text = text;
        }

        /**
         * Set this element's display text. If this is an empty array the item's name will be displayed
         * @param text  The text to display on this element, placeholders are automatically
         *              replaced, see {@link InventoryGui#replaceVars} for a list of the
         *              placeholder variables. Empty text strings are also filter out, use
         *              a single space if you want to add an empty line!<br>
         *              If it's not set/empty the item's default name will be used
         */
        public void setText(String... text) {
            this.text = text;
        }

        /**
         * Get the {@link ItemStack} that represents this state.
         * @return The {@link ItemStack} that represents this state
         * @deprecated Use {@link #getItem(HumanEntity)}
         */
        @Deprecated
        public ItemStack getItem() {
            return getItem(null);
        }

        /**
         * Get the {@link ItemStack} that represents this state.
         * @param who The player viewing the GUI
         * @return The {@link ItemStack} that represents this state
         */
        public ItemStack getItem(HumanEntity who) {
            ItemStack clone = item.clone();
            gui.setItemText(who, clone, getText());
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
         * @return The text lines for this state
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
