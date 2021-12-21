package de.themoep.inventorygui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

/**
 * An element that will not appear if there is no previous history,
 * but will go back one step if there is
 */
public class GuiBackElement extends StaticGuiElement {

    private boolean close;

    /**
     * An element used to go back in history of the gui if there is something to go back to.
     * Will not display when there is nothing to go back to.
     *
     * @param slotChar The character to replace in the gui setup string
     * @param item     The {@link ItemStack} representing this element
     * @param text     The text to display on this element, placeholders are automatically
     *                 replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                 placeholder variables. Empty text strings are also filter out, use
     *                 a single space if you want to add an empty line!<br>
     *                 If it's not set/empty the item's default name will be used
     */
    public GuiBackElement(char slotChar, ItemStack item, String... text) {
        this(slotChar, item, false, text);
    }

    /**
     * An element used to go back in history of the gui
     *
     * @param slotChar The character to replace in the gui setup string
     * @param item     The {@link ItemStack} representing this element
     * @param close    Whether to close the GUI if there is nothing to go back to.
     *                 Will not display item if set to false and nothing to go back to.
     * @param text     The text to display on this element, placeholders are automatically
     *                 replaced, see {@link InventoryGui#replaceVars} for a list of the
     *                 placeholder variables. Empty text strings are also filter out, use
     *                 a single space if you want to add an empty line!<br>
     *                 If it's not set/empty the item's default name will be used
     */
    public GuiBackElement(char slotChar, ItemStack item, boolean close, String... text) {
        super(slotChar, item, text);
        this.close = close;

        setAction(click -> {
            if (canGoBack(click.getWhoClicked())) {
                InventoryGui.goBack(click.getWhoClicked());
            } else if (close) {
                click.getGui().close();
            }
            return true;
        });
    }

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        if (!canGoBack(who) && !close) {
            return gui.getFiller() != null ? gui.getFiller().getItem(who, slot) : null;
        }

        return super.getItem(who, slot);
    }

    /**
     * Whether this element can close the GUI when nothing to go back to
     * @return Close the GUI when nothing to go back
     */
    public boolean canClose() {
        return close;
    }

    private boolean canGoBack(HumanEntity who) {
        return InventoryGui.getHistory(who).size() > 1 || (InventoryGui.getHistory(who).size() == 1 && InventoryGui.getHistory(who).peekLast() != gui);
    }
}
