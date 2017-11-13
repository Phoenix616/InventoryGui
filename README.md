# InventoryGui
A library that simplifies the creation of chest GUIs for Bukkit/Spigot plugins and 
allows assigning of the GUI to a specific InventoryHolder. If you are in need of a GUI for text inputs then take a look at [WesJD](https://github.com/WesJD)'s [AnvilGUI](https://github.com/WesJD/AnvilGUI) library.

Please note that this is **not a plugin!**

Requires Java 8.

## Using InventoryGui
Take a look at the examples below to learn how to create a GUI with this library or use the [InventoryGui Javadocs](https://docs.minebench.de/inventorygui/).

### Defining the GUI setup
Every line in the array define the line in the inventory chest interface. 

The characters are getting assigned to certain elements laters, similar to how recipes are defined.
Any empty slot will be filled with a filler character that you can also define yourself.

```java
String[] guiSetup = {
    "         ",
    "  s i z  ",
    "         "
};
```

The GUI supports 3\*3, 5\*1 and 9\*x inventory sizes.
Sizes that do not match these are getting expanded to the next bigger one.

### Creating the GUI
You create GUIs assigned to an InventoryHolder like a Container or a LivingEntity. 

If the holder is `null` then you are not able to retrieve the GUI by the holder via `InventoryGui.get(holder)`.

```java
InventoryGui gui = new InventoryGui(yourPlugin, theInventoryHolder, guiTitle, guiSetup);
gui.setFiller(new ItemStack(Material.STAINED_GLASS_PANE, 1, 15)); // fill the empty slots with this
```

### Adding to the GUI
You can add certain GUI elements to the GUI which will be assigned to a certain character from the setup.
With these elements you define the actions that should happen when the player interacts with the element.
E.g. you can run some code when the player clicks it. Some elements (like the state one) have predefined
actions that happen when they are clicked. (e.g. toggling between the possible states)

```java
// A simple, static element that runs some action when it is clicked
gui.addElement(new GuiStaticElement('s',
        new ItemStack(Material.REDSTONE),
        click -> {
            if (click.getEvent().getWhoClicked().getName().equals("Redstone") {
                click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                return true; // returning true will cancel the click event and stop taking the item
            }
            return false; // returning false will not cancel the initial click event to the gui
        },
        "You can add lines describing this element here!",
        "The first line is displayed as the displayname,",
        "any additional ones as the lore!",
        "Any text the ItemStack had will be overwritten."
)); 

// Element that directly accesses the holder inventory
// If the element is displayed only in one slot it will show the first item in the inventory.
// In two slots the first two and so one.
gui.addElement(new GuiStorageElement('i', theInventoryHolder.getInventory()));

// An element that can have certain states that trigger some code when changed to
// and automatically changes the ItemStack icon
gui.addElement(new GuiStateElement('z', 
        new GuiStateElement.State(
                change -> {
                    change.getEvent().getWhoClicked().setFlying(true);
                    change.getEvent().getWhoClicked().sendMessage("You are now flying!");
                },
                new ItemStack(Material.WOOL, 1, 5)), // the item to display as an icon
                "flyingEnabled", // a key to identify this state by
                ChatColor.GREEN + "Enable flying!", // explanation text what this element does
                "By clicking here you will start flying"
        ),
        new GuiStateElement.State(
                change -> {
                    change.getEvent().getWhoClicked().setFlying(false);
                    change.getEvent().getWhoClicked().sendMessage("You are no longer flying!");
                },
                new ItemStack(Material.WOOL, 1, 14)),
                "flyingDisabled",
                ChatColor.RED + "Disable flying!",
                "By clicking here you will stop flying"
        )
        // ... you can define as many states as you want, they will cycle through on each click
        // you can also set the state directly via setState(String key)
));            
```
The library includes some additional element types: `GuiElementGroup` and `GuiPageElement`.
These are currently in testing and their API subject to change.

### Retrieving and showing the GUI
After you have created the GUI you can retrieve it with the original holder and show it to a player.
```java
InventoryGui gui = InventoryGui.get(InventoryHolder holder);
gui.show(player);
```
Obviously you can also show the GUI directly after creating it.

## Depending on InventoryGui with maven
You can easily depend on the library with maven.
```xml
<repositories>
    <repository>
        <id>minebench-repo</id>
        <url>https://repo.minebench.de/</url>
    </repository>
</repositories>
```
```xml
<dependencies>
    <dependency>
        <groupId>de.themoep</groupId>
        <artifactId>inventorygui</artifactId>
        <version>1.1-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```
As this is not a stadalone plugin you have to shade it into your plugin!
E.g. with the maven-shade-plugin [like this](https://github.com/Minebench/Pipes/blob/048337e7594684353e7360411b1ef6ba8e7223c4/pom.xml#L63-L82).

You can also get development builds directly from the [Minebench Jenkins ci server](https://ci.minebench.de/job/InventoryGui/)
if you want to manually add it to your project but I strongly advise using a dependency management tool like maven or gradle!

## License
```
Copyright 2017 Max Lee (https://github.com/Phoenix616/)

This program is free software: you can redistribute it and/or modify
it under the terms of the Mozilla Public License as published by
the Mozilla Foundation, version 2.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
Mozilla Public License v2.0 for more details.

You should have received a copy of the Mozilla Public License v2.0
along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
```
