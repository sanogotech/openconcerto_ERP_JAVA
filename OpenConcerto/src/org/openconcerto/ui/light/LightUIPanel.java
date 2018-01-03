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
import org.openconcerto.utils.io.Transferable;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUIPanel extends LightUserControlContainer implements Transferable {
    private static final long serialVersionUID = -3399395824294128572L;

    private String title;

    private Color titleColor;
    private Color titleBackgroundColor;

    private final List<LightControler> controlers = new ArrayList<LightControler>();

    public LightUIPanel(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIPanel(final LightUIPanel panelElement) {
        super(panelElement);
    }

    public LightUIPanel(final String id) {
        super(id);
        this.setType(TYPE_PANEL);
        this.setWeightX(1);
        this.setFillWidth(true);
        this.setFillHeight(true);
    }

    public final LightUILine getLastLine() {
        final int childCount = this.getChildrenCount();
        if (childCount == 0) {
            final LightUILine l = new LightUILine();
            this.addChild(l);

            return l;
        }
        return this.getChild(childCount - 1, LightUILine.class);
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setTitleColor(final Color c) {
        this.titleColor = c;
    }

    public void setTitleBackgoundColor(final Color c) {
        this.titleBackgroundColor = c;
    }

    public void addControler(final LightControler controler) {
        this.controlers.add(controler);
    }

    public List<LightControler> getControlers() {
        return this.controlers;
    }

    public void dumpControllers(final PrintStream out) {
        dumpControllers(out, 0);
    }

    public void dumpControllers(final PrintStream out, final int depth) {
        addSpacer(out, depth);
        out.println("Contollers for id:" + this.getId() + " title: " + this.title);
        for (LightControler controler : this.controlers) {
            addSpacer(out, depth);
            out.println(controler);
        }

        final int lineCount = this.getChildrenCount();
        addSpacer(out, depth);
        out.println(getId() + " : " + this.title);
        addSpacer(out, depth);
        out.println("LightUIPanel " + lineCount + " lines ");
        for (int i = 0; i < lineCount; i++) {
            final LightUILine line = this.getChild(i, LightUILine.class);
            for (int j = 0; j < line.getChildrenCount(); j++) {
                final LightUIElement e = line.getChild(j);
                if (e instanceof LightUIPanel) {
                    ((LightUIPanel) e).dumpControllers(out, depth + 1);
                }
            }
        }
    }

    @Override
    public void copy(final LightUIElement element) {
        super.copy(element);
        if (!(element instanceof LightUIPanel)) {
            throw new InvalidClassException(LightUIPanel.class.getName(), element.getClassName(), element.getId());
        }
        final LightUIPanel panelElement = (LightUIPanel) element;
        this.title = panelElement.title;
        this.titleColor = panelElement.titleColor;
        this.titleBackgroundColor = panelElement.titleBackgroundColor;
        this.controlers.addAll(panelElement.controlers);
    }

    @Override
    public void clear() {
        super.clear();
        this.controlers.clear();
    }

    @Override
    public void addChild(final LightUIElement line) {
        if (!(line instanceof LightUILine)) {
            throw new IllegalArgumentException("Only LightUILine are accepted in LightUIPanel");
        }
        // Ensure uniqueness of line id
        line.setId(LightUILine.createId(this));
        super.addChild(line);
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        final int lineCount = this.getChildrenCount();
        for (int i = 0; i < lineCount; i++) {
            final LightUILine line = this.getChild(i, LightUILine.class);
            line.setReadOnly(readOnly);
        }
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIPanel(json);
            }
        };
    }

    @Override
    public void dump(final PrintStream out, final int depth) {
        this.addSpacer(out, depth);
        out.println("------LightUIPanel-----");
        this.addSpacer(out, depth);
        out.println("Title : " + this.title);
        this.addSpacer(out, depth);
        out.println("v-scroll : " + this.isVerticallyScrollable() + " title-color: " + this.titleColor.toString() + " bg-title-color: " + this.titleBackgroundColor.toString());
        super.dump(out, depth);
        this.addSpacer(out, depth);
        out.println("------------------------");
    }

    @Override
    public void _setValueFromContext(final Object value) {
        final JSONObject jsonContext = (JSONObject) JSONConverter.getObjectFromJSON(value, JSONObject.class);

        if (jsonContext == null) {
            System.err.println("LightUIPanel.setValueFromContext() - json is null for this panel: " + this.getId());
        } else {
            final int childCount = this.getChildrenCount();
            for (int i = 0; i < childCount; i++) {
                final LightUILine line = this.getChild(i, LightUILine.class);
                final int lineChildCount = line.getChildrenCount();
                for (int j = 0; j < lineChildCount; j++) {
                    final LightUIElement lineChild = line.getChild(j);
                    if (lineChild instanceof LightUserControlContainer) {
                        if (!jsonContext.containsKey(lineChild.getUUID())) {
                            System.err.println("LightUIPanel.setValueFromContext() - Impossible to find key " + lineChild.getUUID() + " in context, LightUIElement id: " + lineChild.getId());
                        }
                        ((LightUserControlContainer) lineChild).setValueFromContext(jsonContext.get(lineChild.getUUID()));
                    } else if (lineChild instanceof LightUserControl) {
                        if (!jsonContext.containsKey(lineChild.getUUID())) {
                            System.err.println("LightUIPanel.setValueFromContext() - Impossible to find key " + lineChild.getUUID() + " in context, LightUIElement id: " + lineChild.getId());
                        }
                        ((LightUserControl) lineChild).setValueFromContext(jsonContext.get(lineChild.getUUID()));
                    }
                }
            }
        }
    }

    @Override
    public LightUIElement clone() {
        return new LightUIPanel(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = super.toJSON();
        if (this.title != null) {
            result.put("title", this.title);
        }
        if (this.titleColor != null) {
            result.put("title-color", JSONConverter.getJSON(this.titleColor));
        }
        if (this.titleBackgroundColor != null) {
            result.put("title-bgcolor", JSONConverter.getJSON(this.titleBackgroundColor));
        }
        if (!this.controlers.isEmpty()) {
            result.put("controlers", JSONConverter.getJSON(this.controlers));
        }

        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.title = (String) JSONConverter.getParameterFromJSON(json, "title", String.class, null);
        this.titleColor = (Color) JSONConverter.getParameterFromJSON(json, "title-color", Color.class, null);
        this.titleBackgroundColor = (Color) JSONConverter.getParameterFromJSON(json, "title-bgcolor", Color.class, null);

        final JSONArray jsonControlers = (JSONArray) JSONConverter.getParameterFromJSON(json, "controlers", JSONArray.class);
        if (jsonControlers != null) {
            final int controlersSize = jsonControlers.size();
            for (int i = 0; i < controlersSize; i++) {
                final JSONObject jsonControler = (JSONObject) JSONConverter.getObjectFromJSON(jsonControlers.get(i), JSONObject.class);
                this.controlers.add(new LightControler((JSONObject) jsonControler));
            }
        }
    }

    @Override
    public void setFoldable(boolean foldable) {
        super.setFoldable(foldable);
        if (foldable && this.title == null) {
            this.title = "missing title on panel " + this.getId();
        }
    }
}
