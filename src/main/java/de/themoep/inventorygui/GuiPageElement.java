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

/**
 * This is an element that allows for controlling the pagination of the gui.
 * <b>Untested und potentially unfinished.</b>
 */
public class GuiPageElement extends StaticGuiElement {
    private PageAction pageAction;
    private boolean silent = false;

    /**
     * An element that allows for controlling the pagination of the gui.
     * @param slotChar      The character to replace in the gui setup string
     * @param item          The {@link ItemStack} representing this element
     * @param pageAction    What kind of page action you want to happen when interacting with the element.
     * @param text          The text to display on this element, placeholders are automatically
     *                      replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                      placeholder variables. Empty text strings are also filter out, use
     *                      a single space if you want to add an empty line!<br>
     *                      If it's not set/empty the item's default name will be used
     */
    public GuiPageElement(char slotChar, ItemStack item, PageAction pageAction, String... text) {
        super(slotChar, item, text);
        setAction(click -> {
            switch (pageAction) {
                case NEXT:
                    if (click.getGui().getPageNumber(click.getWhoClicked()) + 1 < click.getGui().getPageAmount(click.getWhoClicked())) {
                        if (!isSilent()) {
                            click.getGui().playClickSound();
                        }
                        click.getGui().setPageNumber(click.getWhoClicked(), click.getGui().getPageNumber(click.getWhoClicked()) + 1);
                    }
                    break;
                case PREVIOUS:
                    if (click.getGui().getPageNumber(click.getWhoClicked()) > 0) {
                        if (!isSilent()) {
                            click.getGui().playClickSound();
                        }
                        click.getGui().setPageNumber(click.getWhoClicked(), click.getGui().getPageNumber(click.getWhoClicked()) - 1);
                    }
                    break;
                case FIRST:
                    if (!isSilent()) {
                        click.getGui().playClickSound();
                    }
                    click.getGui().setPageNumber(click.getWhoClicked(), 0);
                    break;
                case LAST:
                    if (!isSilent()) {
                        click.getGui().playClickSound();
                    }
                    click.getGui().setPageNumber(click.getWhoClicked(), click.getGui().getPageAmount(click.getWhoClicked()) - 1);
                    break;
            }
            return true;
        });
        this.pageAction = pageAction;
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

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        if (((pageAction == PageAction.FIRST || pageAction == PageAction.LAST) && gui.getPageAmount(who) < 3)
                || (pageAction == PageAction.NEXT && gui.getPageNumber(who) + 1 >= gui.getPageAmount(who))
                || (pageAction == PageAction.PREVIOUS && gui.getPageNumber(who) == 0)) {
            return gui.getFiller() != null ? gui.getFiller().getItem(who, slot) : null;
        }
        if (pageAction == PageAction.PREVIOUS) {
            setNumber(gui.getPageNumber(who));
        } else if (pageAction == PageAction.NEXT) {
            setNumber(gui.getPageNumber(who) + 2);
        } else if (pageAction == PageAction.LAST) {
            setNumber(gui.getPageAmount(who));
        }
        return super.getItem(who, slot).clone();
    }

    public enum PageAction {
        NEXT,
        PREVIOUS,
        FIRST,
        LAST;
    }
}
