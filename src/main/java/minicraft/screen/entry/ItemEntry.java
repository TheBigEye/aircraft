package minicraft.screen.entry;

import minicraft.core.io.InputHandler;
import minicraft.graphic.Screen;
import minicraft.item.Item;

import java.util.List;

public class ItemEntry extends ListEntry {

    public static ItemEntry[] useItems(List<Item> items) {
        ItemEntry[] entries = new ItemEntry[items.size()];
        int itemStackSize = items.size();

        for (int i = 0; i < itemStackSize; i++) {
            entries[i] = new ItemEntry(items.get(i));
        }
        return entries;
    }

    private Item item;

    public ItemEntry(Item i) {
        this.item = i;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public void tick(InputHandler input) {
    }

    @Override
    public void render(Screen screen, int x, int y, boolean isSelected) {
        super.render(screen, x, y, true);
        item.sprite.render(screen, x - 2, y);
    }

    // if you add to the length of the string, and therefore the width of the entry,
    // then it will actually move the entry RIGHT in the inventory, instead of the
    // intended left, because it is auto-positioned to the left side.
    @Override
    public String toString() {
        return item.getDisplayName();
    }
}
