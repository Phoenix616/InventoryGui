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

/**
 * This is an element that allows for controlling the pagination of the gui.
 * <b>Untested und potentially unfinished.</b>
 */
public class GuiPageElement extends GuiStaticElement {
    private PageAction pageAction;

    /**
     * An element that allows for controlling the pagination of the gui.
     * @param slotChar      The character to replace in the gui setup string
     * @param item          The {@link ItemStack} representing this element
     * @param pageAction    What kind of page action you want to happen when interacting with the element.
     * @param text          The text to display on this element, placeholders are automatically
     *                      replaced, see {@link InventoryGui#replaceVars(String)} for a list of
     *                      the placeholder variables. Empty text strings are also filter out, use
     *                      a single space if you want to add an empty line!<br>
     *                      If it's not set/empty the item's default name will be used
     */
    public GuiPageElement(char slotChar, ItemStack item, PageAction pageAction, String... text) {
        super(slotChar, item, click -> {
            switch (pageAction) {
                case NEXT:
                    if (click.getGui().getPageNumber() + 1 < click.getGui().getPageAmount()) {
                        click.getGui().setPageNumber(click.getGui().getPageNumber() + 1);
                    }
                    break;
                case PREVIOUS:
                    if (click.getGui().getPageNumber() > 0) {
                        click.getGui().setPageNumber(click.getGui().getPageNumber() - 1);
                    }
                    break;
                case FIRST:
                    click.getGui().setPageNumber(0);
                    break;
                case LAST:
                    click.getGui().setPageNumber(click.getGui().getPageAmount() - 1);
                    break;
            }
            click.getGui().playClickSound();
            return true;
        }, text);
        this.pageAction = pageAction;
    }

    @Override
    public ItemStack getItem(int slot) {
        if ((pageAction == PageAction.NEXT && gui.getPageNumber() + 1 >= gui.getPageAmount())
                || (pageAction == PageAction.PREVIOUS && gui.getPageNumber() == 0)) {
            return gui.getFiller().getItem(slot);
        }
        try {
            if (pageAction == PageAction.PREVIOUS) {
                setNumber(gui.getPageNumber());
            } else if (pageAction == PageAction.NEXT) {
                setNumber(gui.getPageNumber() + 2);
            } else if (pageAction == PageAction.LAST) {
                setNumber(gui.getPageAmount() + 1);
            }
        } catch (IllegalArgumentException e) {
            // cannot set that item amount/number as it isn't supported in Minecraft
        }
        return super.getItem(slot).clone();
    }

    public enum PageAction {
        NEXT,
        PREVIOUS,
        FIRST,
        LAST;
    }
}
