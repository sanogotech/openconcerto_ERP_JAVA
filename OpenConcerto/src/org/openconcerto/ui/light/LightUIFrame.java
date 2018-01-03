/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.ui.light;

import org.openconcerto.utils.io.JSONConverter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUIFrame extends LightUIContainer {
    private static final String ACTIVE = "active";
    private static final String TITLE_PANEL = "title-panel";
    private static final String FOOTER_PANEL = "footer-panel";
    private static final String CHILDREN_FRAME = "children-frame";

    private Boolean active = false;
    private LightUIPanel titlePanel = null;
    private LightUIPanel footerPanel = new LightUIPanel(this.getId() + ".footer.panel");

    private List<LightUIFrame> childrenFrame;

    // Init from json constructor
    public LightUIFrame(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIFrame(final LightUIFrame frame) {
        super(frame);
        this.active = frame.active;
        this.titlePanel = frame.titlePanel;
        this.childrenFrame = frame.childrenFrame;

        this.setFooterPanel(frame.footerPanel);
    }

    /**
     * Creation of an instance of a frame, this one is initialized with an empty main panel
     * 
     * @param id Id of the frame
     */
    public LightUIFrame(final String id) {
        super(id);
        this.setType(TYPE_FRAME);

        this.childrenFrame = new ArrayList<LightUIFrame>();
        this.addChild(new LightUIPanel(this.getId() + ".main.panel"));
        this.footerPanel.setParent(this);
        this.footerPanel.setFillHeight(false);
        this.footerPanel.setHeight(50);

        this.createTitlePanel();
    }

    public LightUIPanel createTitlePanel(final String title) {
        this.createTitlePanel();
        final LightUILabel titleLabel = new LightUILabel(this.titlePanel.getId() + ".label", title, true);
        titleLabel.setVerticalAlignement(VALIGN_CENTER);
        this.titlePanel.getLastLine().addChild(titleLabel);
        return this.titlePanel;
    }

    public LightUIPanel createTitlePanel() {
        this.titlePanel = new LightUIPanel(this.getId() + ".title.panel");
        this.titlePanel.setFillHeight(false);
        this.titlePanel.setHeight(50);
        return this.titlePanel;
    }

    public LightUIPanel getTitlePanel() {
        return this.titlePanel;
    }

    public LightUIPanel getFooterPanel() {
        return this.footerPanel;
    }

    /**
     * Set the footer panel of the frame, be careful the ID is automatically replace by
     * <code>this.getId() + ".footer.panel"</code>.
     * 
     * @param footerPanel - The new footer panel of this frame.
     */
    public void setFooterPanel(final LightUIPanel footerPanel) {
        footerPanel.setId(this.getId() + ".footer.panel");
        this.footerPanel = footerPanel;
        this.footerPanel.setFillHeight(false);
        this.footerPanel.setParent(this);
        this.footerPanel.setHeight(50);
    }

    public void updateFooterPanel(final LightUIPanel footerPanel) {
        if (footerPanel != null) {
            this.footerPanel.copy(footerPanel);
        } else {
            this.footerPanel.clear();
        }

    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getPanelId() {
        return this.getId() + ".main.panel";
    }

    public void removeChildFrame(final LightUIFrame childFrame) {
        this.childrenFrame.remove(childFrame);
    }

    public void removeChildFrame(final int index) {
        this.childrenFrame.remove(index);
    }

    public void clearChildrenFrame() {
        this.childrenFrame.clear();
    }

    @Override
    /**
     * Only one panel is accepted into a frame. And it's Id is always : frame.getId() +
     * ".main.panel"
     * 
     * @param parent The parent frame of this one.
     * @throws InvalidClassException
     */
    public void setParent(final LightUIElement parent) {
        if (!(parent instanceof LightUIFrame)) {
            throw new InvalidClassException(LightUIFrame.class.getName(), parent.getClassName(), parent.getId());
        }
        super.setParent(parent);

        ((LightUIFrame) parent).childrenFrame.add(this);
    }

    @Override
    public <T extends LightUIElement> T findChild(String searchParam, boolean byUUID, Class<T> childClass) {
        final T result = super.findChild(searchParam, byUUID, childClass);
        if (result != null) {
            return result;
        } else {
            return this.footerPanel.findChild(searchParam, byUUID, childClass);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.footerPanel.setReadOnly(readOnly);
    }

    @Override
    /**
     * Only one panel is accepted into a frame. And it's Id is always : frame.getId() +
     * ".main.panel"
     * 
     * @param child The panel which will replace the main panel
     * @throws InvalidClassException
     */
    public void addChild(final LightUIElement child) throws InvalidClassException {
        if (!(child instanceof LightUIPanel)) {
            throw new InvalidClassException(LightUIPanel.class.getName(), child.getClassName(), child.getId());
        }
        child.setId(this.getPanelId());
        this.clear();
        super.addChild(child);
    }

    @Override
    /**
     * Only one panel is accepted into a frame. And it's Id is always : frame.getId() +
     * ".main.panel"
     * 
     * @param index No importance
     * @param child The panel which will replace the main panel
     * @throws InvalidClassException
     */
    public void insertChild(int index, LightUIElement child) throws InvalidClassException {
        if (!(child instanceof LightUIPanel)) {
            throw new InvalidClassException(LightUIPanel.class.getName(), child.getClassName(), child.getId());
        }
        child.setId(this.getPanelId());
        this.clear();
        super.insertChild(index, child);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIFrame(json);
            }
        };
    }

    @Override
    public void dump(final PrintStream out, final int depth) {
        out.println("------------- LightUIFrame -------------");
        super.dump(out, 0);

        out.println("footer-panel: ");
        if (this.footerPanel != null) {
            this.footerPanel.dump(out, 0);
        } else {
            out.println("null");
        }

        out.println("--------------------------");
    }

    @Override
    public LightUIElement clone() {
        final LightUIFrame clone = new LightUIFrame(this);
        clone.getFooterPanel().setParent(clone);
        return clone;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.active) {
            json.put(ACTIVE, true);
        }
        if (this.titlePanel != null) {
            json.put(TITLE_PANEL, this.titlePanel.toJSON());
        }
        json.put(FOOTER_PANEL, this.footerPanel.toJSON());
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.active = JSONConverter.getParameterFromJSON(json, ACTIVE, Boolean.class, false);

        final JSONObject jsonTitlePanel = JSONConverter.getParameterFromJSON(json, TITLE_PANEL, JSONObject.class);
        if (jsonTitlePanel != null) {
            this.titlePanel.fromJSON(jsonTitlePanel);
        }

        final JSONObject jsonFooterPanel = (JSONObject) JSONConverter.getParameterFromJSON(json, FOOTER_PANEL, JSONObject.class, null);
        if (jsonFooterPanel != null) {
            this.footerPanel.fromJSON(jsonFooterPanel);
        }

        final JSONArray jsonChildrenFrame = (JSONArray) JSONConverter.getParameterFromJSON(json, CHILDREN_FRAME, JSONArray.class, null);
        this.childrenFrame = new ArrayList<LightUIFrame>();
        if (jsonChildrenFrame != null) {
            for (final Object objJsonFrame : jsonChildrenFrame) {
                final JSONObject jsonFrame = JSONConverter.getObjectFromJSON(objJsonFrame, JSONObject.class);
                final LightUIFrame childFrame = new LightUIFrame(jsonFrame);
                this.childrenFrame.add(childFrame);
            }
        }
    }
}
