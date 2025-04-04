package minicraft.screen;

import minicraft.core.io.InputHandler;
import minicraft.core.io.Localization;
import minicraft.core.io.Sound;
import minicraft.graphic.*;
import minicraft.screen.entry.BlankEntry;
import minicraft.screen.entry.ListEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Menu {

    private int bgSpritePos = 21;

    @NotNull
    private final ArrayList<ListEntry> entries = new ArrayList<>();

    private int spacing = 0;
    private Rectangle bounds = null;
    private Rectangle entryBounds = null;
    private RelPos entryPos = RelPos.CENTER; // the x part of this is re-applied per entry, while the y part is calculated once using the cumulative height of all entries and spacing.

    private String title = "";
    private int titleColor;
    private Point titleLoc = null; // standard point is anchor, with anchor.x + SpriteSheet.boxWidth
    private boolean drawVertically = false;

    private boolean hasFrame;

    private boolean selectable = false;
    boolean shouldRender = true;

    private int displayLength = 0;
    private int padding = 0;
    private boolean wrap = false;

    // menu selection vars
    private int selection = 0;
    private int dispSelection = 0;
    private int offset = 0;

    /**
     * If there's searcher bar in menu
     */
    private boolean useSearcherBar = false;
    public boolean searcherBarActive = false;
    private List<Integer> listSearcher;
    private int listPositionSearcher;
    private int selectionSearcher;

    /**
     * The actual typed word in searcher bar
     */
    private String typingSearcher;

    private Menu() {
    }

    protected Menu(Menu m) {
        entries.addAll(m.entries);
        spacing = m.spacing;
        bounds = m.bounds == null ? null : new Rectangle(m.bounds);
        entryBounds = m.entryBounds == null ? null : new Rectangle(m.entryBounds);
        entryPos = m.entryPos;
        title = m.title;
        titleColor = m.titleColor;
        titleLoc = m.titleLoc;
        drawVertically = m.drawVertically;
        hasFrame = m.hasFrame;
        selectable = m.selectable;
        shouldRender = m.shouldRender;
        displayLength = m.displayLength;
        padding = m.padding;
        wrap = m.wrap;
        selection = m.selection;
        dispSelection = m.dispSelection;
        offset = m.offset;

        useSearcherBar = m.useSearcherBar;
        selectionSearcher = 0;
        listSearcher = new ArrayList<>();
        listPositionSearcher = 0;
        typingSearcher = "";
        bgSpritePos = m.bgSpritePos;
    }

    public void init() {

        if (entries.isEmpty()) {
            selection = 0;
            dispSelection = 0;
            offset = 0;
            return;
        }

        selection = Math.min(selection, entries.size() - 1);
        selection = Math.max(0, selection);

        if (!entries.get(selection).isSelectable()) {
            int previousSelection = selection;
            do {
                selection++;
                if (selection < 0)
                    selection = entries.size() - 1;
                selection = selection % entries.size();
            } while (!entries.get(selection).isSelectable() && selection != previousSelection);
        }

        dispSelection = selection;
        dispSelection = Math.min(dispSelection, displayLength - 1);
        dispSelection = Math.max(0, dispSelection);

        doScroll();
    }

    void setSelection(int index) {
        if (index >= entries.size()) index = entries.size() - 1;
        if (index < 0) index = 0;

        this.selection = index;

        doScroll();
    }

    void setBackground(int bgSpritePos) {
        this.bgSpritePos = bgSpritePos;
    }

    int getSelection() {
        return selection;
    }

    int getDispSelection() {
        return dispSelection;
    }

	ListEntry[] getEntries() {
		return entries.toArray(new ListEntry[0]);
	}

	protected void setEntries(ListEntry[] entries) {
		this.entries.clear();
		this.entries.addAll(0, Arrays.asList(entries));
	}

	protected void setEntries(List<ListEntry> entries) {
		this.entries.clear();
		this.entries.addAll(entries);
	}

    @Nullable
    ListEntry getCurEntry() {
        return entries.isEmpty() ? null : entries.get(selection);
    }

    int getNumOptions() {
        return entries.size();
    }

    Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    String getTitle() {
        return title;
    }

    boolean isSelectable() {
        return selectable;
    }

    boolean shouldRender() {
        return shouldRender;
    }

    /** @noinspection SameParameterValue */
    void translate(int xoff, int yoff) {
        bounds.translate(xoff, yoff);
        entryBounds.translate(xoff, yoff);
        titleLoc.translate(xoff, yoff);
    }

    public void tick(InputHandler input) {
        if (!selectable || entries.isEmpty()) {
            return;
        }

        int previousSelection = selection;
        if (input.getKey("cursor-up").clicked) selection--;
        if (input.getKey("cursor-down").clicked) selection++;
        if (input.getKey("shift-cursor-up").clicked && selectionSearcher == 0) selectionSearcher -= 2;
        if (input.getKey("shift-cursor-down").clicked && selectionSearcher == 0) selectionSearcher += 2;
        if (previousSelection != selection && selectionSearcher != 0) selection = previousSelection;

        if (useSearcherBar) {
            if (input.getKey("searcher-bar").clicked) {
                searcherBarActive = !searcherBarActive;
                input.addKeyTyped("", null); // clear pressed key
            }

            if (!listSearcher.isEmpty() && selectionSearcher == 0) {
                int speed = input.getKey("PAGE-UP").clicked ? -1 : input.getKey("PAGE-DOWN").clicked ? 1 : 0;
                if (speed != 0) {
                    int listPosition = listPositionSearcher + speed;
                    if (listPosition < 0) {
                        listPosition = listSearcher.size() - 1;
                    }
                    listPositionSearcher = listPosition % listSearcher.size();
                    int position = listSearcher.get(listPositionSearcher);

                    int difference = position - selection;
                    selectionSearcher = difference > position ? -difference : difference;
                }
            }

            if (searcherBarActive) {
                String typingSearcher = input.addKeyTyped(this.typingSearcher, null);

                if (!typingSearcher.isEmpty()) {
                    // Convert the first letter on uppercase
                    typingSearcher = typingSearcher.substring(0, 1).toUpperCase() + typingSearcher.substring(1);

                    // Iterate over the remaining characters to convert to lowercase until a space is found
                    for (int i = 1; i < typingSearcher.length(); i++) {
                        if (typingSearcher.charAt(i - 1) == ' ') {
                            typingSearcher = typingSearcher.substring(0, i) + typingSearcher.substring(i, i + 1).toUpperCase() + typingSearcher.substring(i + 1);
                        }
                    }
                }



                for (String pressedKey : input.getAllPressedKeys()) {
                    if (pressedKey.equals("ENTER")) {
                        continue;
                    }

                    input.getKey(pressedKey).clicked = false;
                }

                // check if word was updated
                if (typingSearcher.length() <= ((entryBounds.getWidth() / 8)) && typingSearcher.length() != this.typingSearcher.length()) {
                    this.typingSearcher = typingSearcher;

                    listSearcher.clear();
                    listPositionSearcher = 0;

                    Iterator<ListEntry> entryIt = entries.iterator();
                    boolean shouldSelect = true;
                    for (int i = 0; entryIt.hasNext(); i++) {
                        ListEntry entry = entryIt.next();
                        String stringEntry = entry.toString();

                        if (stringEntry.contains(typingSearcher)) {
                            if (shouldSelect) {
                                int difference = i - selection;
                                selectionSearcher = difference > i ? -difference : difference;

                                shouldSelect = false;
                            }

                            listSearcher.add(i);
                        }
                    }
                }
            }

            if (selectionSearcher != 0) {
                boolean downDirection = selectionSearcher > 0;
                selectionSearcher += downDirection ? -1 : 1;
                selection += downDirection ? 1 : -1;
            }
        }

        int delta = selection - previousSelection;
        selection = previousSelection;
        if (delta == 0) {
            entries.get(selection).tick(input); // only ticks the entry on a frame where the selection cursor has not moved.
            return;
        } else {
            Sound.play("menuSelect");
        }

        do {
            selection += delta;
            if (selection < 0) {
                selection = entries.size() - 1;
            }
            selection = selection % entries.size();
        } while (!entries.get(selection).isSelectable() && selection != previousSelection);

        // update offset and selection displayed
        dispSelection += selection - previousSelection;

        if (dispSelection < 0) dispSelection = 0;
        if (dispSelection >= displayLength) dispSelection = displayLength - 1;

        doScroll();
    }

    private void doScroll() {
        // check if dispSelection is past padding point, and if so, bring it back in

        dispSelection = selection - offset;
        int offset = this.offset;

        // for scrolling up
        while ((dispSelection < padding || !wrap && offset + displayLength > entries.size()) && (wrap || offset > 0)) {
            offset--;
            dispSelection++;
        }

        // for scrolling down
        while ((displayLength - dispSelection <= padding || !wrap && offset < 0) && (wrap || offset + displayLength < entries.size())) {
            offset++;
            dispSelection--;
        }

        // only useful when wrap is true
        if (offset < 0) offset += entries.size();
        if (offset > 0) offset = offset % entries.size();

        this.offset = offset;
    }

    public void render(Screen screen) {
        // render searcher bar
    	if (searcherBarActive && useSearcherBar) {


    	    int leading = Font.textWidth(typingSearcher) * Font.textWidth(" ") / 15;
    	    int xSearcherBar = (titleLoc.x + title.length() * 4) - 16;

    	    if (xSearcherBar - leading < 0) {
    	        leading += xSearcherBar - leading;
    	    }

    	    Font.drawBox(screen, (entryBounds.getCenter().x - entryBounds.getWidth() / 2) - 16, titleLoc.y + 115, 4 + entryBounds.getWidth() / 8, 1);
    	    Font.draw("> " + typingSearcher + " <", screen, xSearcherBar - leading, titleLoc.y + 115, typingSearcher.length() < ((entryBounds.getWidth() / 8)) ? Color.YELLOW : Color.RED);
    	}

        // Render the menu GUI

        renderFrame(screen);

        // Render the title
        if (title.length() > 0) {
            if (drawVertically) {
                for (int i = 0; i < title.length(); i++) {
                    if (hasFrame) {
                        screen.render(titleLoc.x, titleLoc.y + i * Font.textHeight(), 3 + 21 * bgSpritePos, 0, 3);
                    }
                    Font.draw(title.substring(i, i + 1), screen, titleLoc.x, titleLoc.y + i * Font.textHeight(), titleColor);
                }
            } else {
                if (hasFrame) {
                    for (int i = 0; i < title.length(); i++) {
                        screen.render(titleLoc.x + i * 7, titleLoc.y, 3 + (bgSpritePos << 5), 0, 3);
                    }
                }
                Font.draw(title, screen, titleLoc.x, titleLoc.y, titleColor);
            }
        }


        // render the options
        int y = entryBounds.getTop();
        boolean special = wrap && entries.size() < displayLength;
        if (special) {
            int diff = displayLength - entries.size(); // we have to account for this many entry heights.
            int extra = diff * (ListEntry.getHeight() + spacing) / 2;
            y += extra;
        }

        for (int i = offset; i < (wrap ? offset + displayLength : Math.min(offset + displayLength, entries.size())); i++) {
            if (special && i - offset >= entries.size()) {
                break;
            }

            int idx = i % entries.size();
            ListEntry entry = entries.get(idx);

            if (!(entry instanceof BlankEntry)) {
                Point pos = entryPos.positionRect(
            		new Dimension(entry.getWidth(), ListEntry.getHeight()),
            		new Rectangle(entryBounds.getLeft(), y, entryBounds.getWidth(), ListEntry.getHeight(), Rectangle.CORNER_DIMS)
                );

                boolean selected = idx == selection;
                if (searcherBarActive && useSearcherBar) {
                    entry.render(screen, pos.x, pos.y, selected, typingSearcher, Color.YELLOW);
                } else {
                    entry.render(screen, pos.x, pos.y, selected);
                }

                if (selected && entry.isSelectable()) {
                    // draw the arrows
                    Font.draw("> ", screen, pos.x - Font.textWidth("> "), y, ListEntry.COLOR_SELECTED);
                    Font.draw(" <", screen, pos.x + entry.getWidth(), y, ListEntry.COLOR_SELECTED);
                }
            }

            y += ListEntry.getHeight() + spacing;
        }
    }

    void updateSelectedEntry(ListEntry newEntry) {
        updateEntry(selection, newEntry);
    }

    void updateEntry(int idx, ListEntry newEntry) {
        if (idx >= 0 && idx < entries.size()) {
            entries.set(idx, newEntry);
        }
    }

    public void removeSelectedEntry() {
        entries.remove(selection);

        if (selection >= entries.size()) selection = entries.size() - 1;
        if (selection < 0) selection = 0;

        doScroll();
    }

    public void setColors(Menu model) {
        titleColor = model.titleColor;
    }

    private void renderFrame(Screen screen) {
        if (!hasFrame) {
            return;
        }

        int bottom = bounds.getBottom() - SpriteSheet.boxWidth;
        int right = bounds.getRight() - SpriteSheet.boxWidth;

        for (int y = bounds.getTop(); y <= bottom; y += SpriteSheet.boxWidth) { // loop through the height of the bounds
            for (int x = bounds.getLeft(); x <= right; x += SpriteSheet.boxWidth) { // loop through the width of the bounds

                boolean xend = x == bounds.getLeft() || x == right;
                boolean yend = y == bounds.getTop() || y == bottom;
                int spriteoffset = (xend && yend ? 0 : (yend ? 1 : (xend ? 2 : 3))); // determines which sprite to use
                int mirrors = (x == right ? 1 : 0) + (y == bottom ? 2 : 0); // gets mirroring

                screen.render(x, y, spriteoffset + (bgSpritePos << 5), mirrors, 3);

                if (x < right && x + SpriteSheet.boxWidth > right) {
                    x = right - SpriteSheet.boxWidth;
                }
            }

            if (y < bottom && y + SpriteSheet.boxWidth > bottom) {
                y = bottom - SpriteSheet.boxWidth;
            }
        }
    }

    /// This needs to be in the Menu class, to have access to the private
    /// constructor and fields.

    public static class Builder {

        private static final Point center = new Point(Screen.w / 2, Screen.h / 2);

        private Menu menu;

        private boolean setSelectable = false;
        private float padding = 1;

        @NotNull
        private RelPos titlePos = RelPos.TOP;
        private boolean fullTitleColor = false;
        private boolean setTitleColor = false;
        private int titleCol = Color.YELLOW;

        @NotNull
        private Point anchor = center;

        @NotNull
        private RelPos menuPos = RelPos.CENTER;
        private Dimension menuSize = null;

        private boolean searcherBar;

        public Builder(boolean hasFrame, int entrySpacing, RelPos entryPos, ListEntry... entries) {
            this(hasFrame, entrySpacing, entryPos, Arrays.asList(entries));
        }

        public Builder(boolean hasFrame, int entrySpacing, RelPos entryPos, List<ListEntry> entries) {
            menu = new Menu();
            setEntries(entries);
            menu.hasFrame = hasFrame;
            menu.spacing = entrySpacing;
            menu.entryPos = entryPos;
        }

        public Builder setEntries(ListEntry... entries) {
            return setEntries(Arrays.asList(entries));
        }

        public Builder setEntries(List<ListEntry> entries) {
            menu.entries.clear();
            menu.entries.addAll(entries);
            return this;
        }

        public Builder setPositioning(Point anchor, RelPos menuPos) {
            this.anchor = anchor == null ? new Point() : anchor;
            this.menuPos = menuPos == null ? RelPos.BOTTOM_RIGHT : menuPos;
            return this;
        }

        public Builder setSize(int width, int height) {
            menuSize = new Dimension(width, height);
            return this;
        }

        // can be used to set the size to null
        public Builder setMenuSize(Dimension d) {
            menuSize = d;
            return this;
        }

        public Builder setBounds(Rectangle rect) {
            menuSize = rect.getSize();

            // because the anchor represents the center of the rectangle.
            setPositioning(rect.getCenter(), RelPos.CENTER);
            return this;
        }

        public Builder setDisplayLength(int numEntries) {
            menu.displayLength = numEntries;
            return this;
        }

        public Builder setTitlePos(RelPos rp) {
            titlePos = (rp == null ? RelPos.TOP : rp);
            return this;
        }

        public Builder setTitle(String title) {
            menu.title = title;
            return this;
        }

        public Builder setTitle(String title, int color) {
            return setTitle(title, color, false);
        }

        public Builder setTitle(String title, int color, boolean fullColor) {
            menu.title = title;

            fullTitleColor = fullColor;
            setTitleColor = true;

            // This means that the color is the full 4 parts, abcd. Otherwise,
            // it is assumed it is only the main component, the one that matters.
            if (fullColor) {
                menu.titleColor = color;
            } else{
                titleCol = color;
            }

            return this;
        }

        public Builder setFrame(boolean hasFrame) {
            menu.hasFrame = hasFrame;
            return this;
        }

        /**
         * Change the menu backgound and frame sprites
         * @param sy    the Y sprites axys in the GUI spritesheet
         */
        public Builder setBackground(int sy){
            menu.bgSpritePos = sy;
            return this;
        }

        public Builder setScrollPolicies(float padding, boolean wrap) {
            this.padding = padding;
            menu.wrap = wrap;
            return this;
        }

        public Builder setShouldRender(boolean render) {
            menu.shouldRender = render;
            return this;
        }

        public Builder setSelectable(boolean selectable) {
            setSelectable = true;
            menu.selectable = selectable;
            return this;
        }

        public Builder setSelection(int sel) {
            menu.selection = sel;
            return this;
        }

        public Builder setSelection(int sel, int dispSel) {
            menu.selection = sel;
            menu.dispSelection = dispSel;
            return this;
        }

        public Builder setSearcherBar(boolean searcherBar) {
            this.searcherBar = searcherBar;

            return this;
        }

        public Menu createMenu() {
            // this way, I don't have to reference all the variables to a different var.
            return copy().createMenu(this);
        }

        private Menu createMenu(Builder menuBuilder) {
            if (menuBuilder == this) {
                return copy().createMenu(this);
            }

            menu.title = Localization.getLocalized(menu.title);

            // set default selectability
            if (!setSelectable) {
                for (ListEntry entry : menu.entries) {
                    menu.selectable = menu.selectable || entry.isSelectable();
                    if (menu.selectable) break;
                }
            }

            // check the centering of the title, and find the dimensions of the title's display space.

            menu.drawVertically = titlePos == RelPos.LEFT || titlePos == RelPos.RIGHT;

            Dimension titleDim = menu.drawVertically ? new Dimension(Font.textHeight() * 2, Font.textWidth(menu.title)) : new Dimension(Font.textWidth(menu.title), Font.textHeight() * 2);

            // find the area used by the title and/or frame, that can't be used by the
            // entries

            /*
             * Create an Insets instance, and do the following... - if the menu is
             * selectable, add 2 buffer spaces on the left and right, for the selection
             * arrows. - if the menu has a frame, then add one buffer space to all 4 sides -
             * if the menu has a title AND a frame, do nothing. - if the menu has a title
             * and NO frame, add two spaces to whatever side the title is on
             *
             * Remember to set the title pos one space inside the left/right bounds, so it
             * doesn't touch the frame corner.
             *
             * Starting with the entry size figured out, add the insets to get the total
             * size. Starting with the menu size set, subtract the insets to get the entry
             * size.
             */

            Insets border;
            if (menu.hasFrame) {
                border = new Insets(SpriteSheet.boxWidth); // add frame insets
            } else {
                border = new Insets();

                // add title insets
                if (menu.title.length() > 0 && titlePos != RelPos.CENTER) {
                    RelPos c = titlePos;
                    int space = SpriteSheet.boxWidth * 2;

                    if (c.yIndex == 0) {
                        border.top = space;
                    } else if (c.yIndex == 2) {
                        border.bottom = space;
                    } else if (c.xIndex == 0) { // must be center left
                        border.left = space;
                    } else if (c.xIndex == 2) { // must be center right
                        border.right = space;
                    }
                }
            }

            if (menu.isSelectable()) {
                // add spacing for selection cursors
                border.left += SpriteSheet.boxWidth * 2;
                border.right += SpriteSheet.boxWidth * 2;
            }

            if (menu.wrap && menu.displayLength > 0) {
                menu.displayLength = Math.min(menu.displayLength, menu.entries.size());
            }

            // I have anchor and menu's relative position to it, and may or may not have size.
            Dimension entrySize;

            if (menuSize == null) {
                int width = titleDim.width;
                for (ListEntry entry : menu.entries) {
                    int entryWidth = entry.getWidth();
                    if (menu.isSelectable() && !entry.isSelectable()) {
                        entryWidth = Math.max(0, entryWidth - SpriteSheet.boxWidth * 4);
                    }
                    width = Math.max(width, entryWidth);
                }

                if (menu.displayLength > 0) { // has been set; use to determine entry bounds
                    int height = (ListEntry.getHeight() + menu.spacing) * menu.displayLength - menu.spacing;
                    entrySize = new Dimension(width, height);

                } else {

                    // no set size; just keep going to the edges of the screen
                    int maxHeight;
                    if (menuPos.yIndex == 0) { // anchor is lowest down coordinate (highest y value)
                        maxHeight = anchor.y;
                    } else if (menuPos.yIndex == 2) {
                        maxHeight = Screen.h - anchor.y;
                    } else { // is centered; take the lowest value of the other two, and double it
                        maxHeight = Math.min(anchor.y, Screen.h - anchor.y) * 2;
                    }

                    maxHeight -= border.top + border.bottom; // reserve border space

                    int entryHeight = menu.spacing + ListEntry.getHeight();
                    int totalHeight = entryHeight * menu.entries.size() - menu.spacing;
                    maxHeight = ((maxHeight + menu.spacing) / entryHeight) * entryHeight - menu.spacing;

                    entrySize = new Dimension(width, Math.min(maxHeight, totalHeight));
                }

                menuSize = border.addTo(entrySize);
            } else { // menuSize was set manually
                entrySize = border.subtractFrom(menuSize);
            }

            // set default max display length (needs size first)
            if (menu.displayLength <= 0 && menu.entries.size() > 0) {
                menu.displayLength = (entrySize.height + menu.spacing) / (ListEntry.getHeight() + menu.spacing);
            }

            // based on the menu centering, and the anchor, determine the upper-left point from which to draw the menu.
            menu.bounds = menuPos.positionRect(menuSize, anchor, new Rectangle()); // reset to a value that is actually useful to the menu

            menu.entryBounds = border.subtractFrom(menu.bounds);
            menu.titleLoc = titlePos.positionRect(titleDim, menu.bounds);

            if (titlePos.xIndex == 0 && titlePos.yIndex != 1) {
                menu.titleLoc.x += SpriteSheet.boxWidth;
            }

            if (titlePos.xIndex == 2 && titlePos.yIndex != 1) {
                menu.titleLoc.x -= SpriteSheet.boxWidth;
            }

            // set the menu title color
            if (!menu.title.isEmpty()) {
                if (fullTitleColor) {
                    menu.titleColor = titleCol;
                } else {
                    if (!setTitleColor) {
                        titleCol = menu.hasFrame ? Color.YELLOW : Color.WHITE;
                    }
                    menu.titleColor = titleCol; // make it match the frame color, or be transparent
                }
            }

            if (padding < 0) padding = 0;
            if (padding > 1) padding = 1;
            menu.padding = (int) Math.floor(padding * menu.displayLength / 2);

            menu.useSearcherBar = searcherBar;

            // done setting defaults/values; return the new menu

            menu.init(); // any setup the menu does by itself right before being finished.
            return menu;
        }

        // returns a new Builder instance, that can be further modified to creat another menu.
        public Builder copy() {
            Builder b = new Builder(menu.hasFrame, menu.spacing, menu.entryPos, menu.entries);

            b.menu = new Menu(menu);

            b.anchor = anchor == null ? null : new Point(anchor);
            b.menuSize = menuSize == null ? null : new Dimension(menuSize);
            b.menuPos = menuPos;
            b.setSelectable = setSelectable;
            b.padding = padding;
            b.titlePos = titlePos;
            b.fullTitleColor = fullTitleColor;
            b.setTitleColor = setTitleColor;
            b.titleCol = titleCol;
            b.searcherBar = searcherBar;

            return b;
        }

        public Builder setFrame(int i, int j, int k) {
            // TODO Make this method change the sprite of the menu frame
            return null;
        }
    }

    public String toString() {
        return title + "-Menu[" + bounds + "]";
    }
}
