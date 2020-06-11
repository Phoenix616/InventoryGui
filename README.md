# InventoryGui
A library that simplifies the creation of chest GUIs for Bukkit/Spigot plugins and 
allows assigning of the GUI to a specific InventoryHolder. If you are in need of a GUI for text inputs then take a look at [WesJD](https://github.com/WesJD)'s [AnvilGUI](https://github.com/WesJD/AnvilGUI) library.

Please note that this is **not a plugin!**

Requires Java 8.

## Using InventoryGui
Take a look at the examples below to learn how to create a GUI with this library or use the [InventoryGui Javadocs](https://docs.phoenix616.dev/inventorygui/).

### Defining the GUI setup
Every line in the array define the line in the inventory chest interface. 

The characters are getting assigned to certain elements laters, similar to how recipes are defined.
Any empty slot will be filled with a filler character that you can also define yourself.

```java
String[] guiSetup = {
    "  s i z  ",
    "  ggggg  ",
    "  fpdnl  "
};
```

The GUI supports 3\*3, 5\*1 and 9\*x inventory sizes.
Sizes that do not match these are getting expanded to the next bigger one.

### Creating the GUI
You create GUIs assigned to an InventoryHolder like a Container or a LivingEntity. 

If the holder is `null` then you are not able to retrieve the GUI by the holder via `InventoryGui.get(holder)`.

```java
InventoryGui gui = new InventoryGui(yourPlugin, theInventoryHolder, guiTitle, guiSetup);
gui.setFiller(new ItemStack(Material.GRAY_STAINED_GLASS, 1)); // fill the empty slots with this
```

### Adding to the GUI
You can add certain GUI elements to the GUI which will be assigned to a certain character from the setup.
With these elements you define the actions that should happen when the player interacts with the element.
E.g. you can run some code when the player clicks it. Some elements (like the state one) have predefined
actions that happen when they are clicked. (e.g. toggling between the possible states)

#### Static Element
A simple, static element that runs some action when it is clicked.
```java
gui.addElement(new StaticGuiElement('s',
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
```
#### Storage Element
An Element that directly accesses the holder inventory.
If the element is displayed only in one slot it will show the first item in the inventory.
In two slots the first two and so on.
```java
gui.addElement(new GuiStorageElement('i', theInventoryHolder.getInventory()));
```
#### State Element
An element that can have certain states that trigger some code when changed to.
and automatically changes the ItemStack icon.
```java
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
));
```
... you can define as many states as you want, they will cycle through on each click
you can also set the state directly via `GuiStateElement#setState(String key)`

#### Dynamic Element
You can also dynamically load elements each time the GUI is re-drawn. E.g. when you want to cache GUIs but not the 
text of some buttons or dynamically change them while they are open without closing and reopening them.
Dynamic elements just return one of the other elements that should be displayed each time `InventoryGui#draw` is called.
The slot character for the returned element doesn't really play a role, it is recommended to set it to
the DynamicGuiElement's slot character though.
```java
gui.addElement(new DynamicGuiElement('d', () -> {
    return new StaticGuiElement('d', new ItemStack (Material.CLOCK), "Update time: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
}));
```
If you want to change the content of a DynamicGuiElement after a player click on it just call `InventoryGui#draw` in the action of the supplied element.

#### Element Group
A group can contain multiple different elements and if there are more elements in the group than display slot you can use the GuiPageElement to switch between pages.

```java
GuiElementGroup group = new GuiElementGroup('g');
for (String text : texts) {
    // Add an element to the group
    // Elements are in the order they got added to the group and don't need to have the same type.
    group.addElement((new StaticGuiElement('e', new ItemStack(Material.DIRT), text);
}
gui.addElement(group);
```
##### Pagination
It will automatically detect GuiElementGroup elements with more elements in them than available slots with that character in the GUI and go to the according page on click. (depending on type)
There are also some pagination specific placeholders available for the element descriptions.

```java
// First page
gui.addElement(new GuiPageElement('f', new ItemStack(Material.ARROW), PageAction.FIRST, "Go to first page (current: %page%)"));

// Previous page
gui.addElement(new GuiPageElement('p', new ItemStack(Material.SIGN), PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

// Next page
gui.addElement(new GuiPageElement('n', new ItemStack(Material.SIGN), PageAction.NEXT, "Go to next page (%nextpage%)"));

// Last page
gui.addElement(new GuiPageElement('l', new ItemStack(Material.ARROW), PageAction.LAST, "Go to last page (%pages%)"));
```

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
        <id>phoenix-repo</id>
        <url>https://repo.phoenix616.dev/</url>
    </repository>
</repositories>
```
```xml
<dependencies>
    <dependency>
        <groupId>de.themoep</groupId>
        <artifactId>inventorygui</artifactId>
        <version>1.4.2-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```
As this is not a stadalone plugin you have to shade it into your plugin!
E.g. with the maven-shade-plugin [like this](https://github.com/Minebench/Pipes/blob/048337e7594684353e7360411b1ef6ba8e7223c4/pom.xml#L63-L82).

You can also get development builds directly from the [Minebench Jenkins ci server](https://ci.minebench.de/job/InventoryGui/)
if you want to manually add it to your project but I strongly advise using a dependency management tool like maven or gradle!

## License
InventoryGui is licensed under the following, MIT license:

```
Copyright 2017 Max Lee (https://github.com/Phoenix616)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
