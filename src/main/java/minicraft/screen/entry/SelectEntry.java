package minicraft.screen.entry;

import minicraft.core.io.InputHandler;
import minicraft.core.io.Localization;
import minicraft.core.io.Sound;
import minicraft.graphic.Font;
import minicraft.util.Action;

public class SelectEntry extends ListEntry {

    private Action onSelect;
    private String text;
    private boolean localize;

    /**
     * Creates a new entry which acts as a button. Can do an action when it is
     * selected.
     * 
     * @param text     Text displayed on this entry
     * @param onSelect Action which happens when the entry is selected
     */
    public SelectEntry(String text, Action onSelect) {
        this(text, onSelect, true);
    }

    public SelectEntry(String text, Action onSelect, boolean localize) {
        this.onSelect = onSelect;
        this.text = text;
        this.localize = localize;
    }

    /**
     * Changes the text of the entry.
     * 
     * @param text new text
     */
    void setText(String text) {
        this.text = text;
    }
    

	public String getText() {
		return text; 
	}

    @Override
    public void tick(InputHandler input) {
        if (input.getKey("select").clicked && onSelect != null) {
            Sound.play("menuConfirm");
            onSelect.act();
        }
    }

    @Override
    public int getWidth() {
        return Font.textWidth(toString());
    }

    @Override
    public String toString() {
        return localize ? Localization.getLocalized(text) : text;
    }
}
