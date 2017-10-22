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
 * This is an element that allows for controlling the pagination of the gui.
 * <b>Untested und potentially unfinished.</b>
 */
public class GuiPageElement extends GuiElement {
    private final ItemStack item;
    private String[] text;
    private PageAction pageAction;

    /**
     * An element that allows for controlling the pagination of the gui.
     * @param slotChar      The character to replace in the gui setup string
     * @param item          The {@link ItemStack} representing this element
     * @param pageAction    What kind of page action you want to happen when interacting with the element.
     * @param text          The text lines describing the element and its actions.
     */
    public GuiPageElement(char slotChar, ItemStack item, PageAction pageAction, String... text) {
        super(slotChar, click -> {
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
            if (click.getEvent().getWhoClicked() instanceof Player) {
                Player player = (Player) click.getEvent().getWhoClicked();
                player.playSound(player.getEyeLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
            }
            return true;
        });
        this.item = item;
        this.pageAction = pageAction;
        this.text = text;
    }

    @Override
    public ItemStack getItem(int slot) {
        if ((pageAction == PageAction.NEXT && gui.getPageNumber() + 1 >= gui.getPageAmount())
                || (pageAction == PageAction.PREVIOUS && gui.getPageNumber() == 0)) {
            return gui.getFiller().getItem(slot);
        }
        ItemStack clone = item.clone();
        gui.setItemText(clone, text);
        return clone;
    }

    /**
     * Set this element's display text. If this is an empty array the item's name will be displayed
     * @param text  The text to display on this element
     */
    public void setText(String... text) {
        this.text = text;
    }

    /**
     * Get the text that this element displays
     * @return  The text that is displayed on this element
     */
    public String[] getText() {
        return text;
    }

    private enum PageAction {
        NEXT,
        PREVIOUS,
        FIRST,
        LAST;
    }
}
