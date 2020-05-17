package de.themoep.inventorygui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

/**
 * An element that will not appear if there is no previous history,
 * but will go back one step if there is
 */
public class GuiBackElement extends StaticGuiElement {

    /**
     * An element used to go back in history of the gui
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
        super(slotChar, item, text);

        setAction(click -> {
            InventoryGui.goBack(click.getEvent().getWhoClicked());
            return true;
        });
    }

    @Override
    public ItemStack getItem(HumanEntity who, int slot) {
        if (InventoryGui.getHistory(who).isEmpty() || InventoryGui.getHistory(who).size() == 1) {
            return gui.getFiller() != null ? gui.getFiller().getItem(who, slot) : null;
        }

        return super.getItem(who, slot).clone();
    }
}
