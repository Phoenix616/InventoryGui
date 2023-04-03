# InventoryGui
A library that simplifies the creation of chest GUIs for Bukkit/Spigot plugins and 
allows assigning of the GUI to a specific InventoryHolder. If you are in need of a GUI for text inputs then take a look at [WesJD](https://github.com/WesJD)'s [AnvilGUI](https://github.com/WesJD/AnvilGUI) library.

Please note that this is **not a plugin!**

Requires Java 8.

## Using InventoryGui
Take a look at [the examples in the wiki](https://wiki.phoenix616.dev/library:inventorygui:usage) to learn how to create a GUI with this library or use the [InventoryGui Javadocs](https://docs.phoenix616.dev/inventorygui/).

## Maven information
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
        <!--The following version may not be the latest. Check it before using.-->
        <version>1.6.1-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```
As this is not a standalone plugin you have to shade it into your plugin!
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
