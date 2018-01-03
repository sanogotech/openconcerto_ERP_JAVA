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

public class LightUIContainer extends LightUIElement {
    private List<LightUIElement> children;

    public LightUIContainer(final String id) {
        super(id);
        this.children = new ArrayList<LightUIElement>();
    }

    public LightUIContainer(final JSONObject json) {
        super(json);
    }

    public LightUIContainer(final LightUIContainer container) {
        super(container);
    }

    public void addChild(final LightUIElement child) {
        if (child == null) {
            throw new IllegalArgumentException("Attempt to put null child in container, id:" + this.getId());
        }
        child.setReadOnly(this.isReadOnly());
        child.setParent(this);
        this.children.add(child);
    }
    
    public void addChildren(final List<LightUIElement> children) {
        if (children == null) {
            throw new IllegalArgumentException("List null, id:" + this.getId());
        }
        for(final LightUIElement child: children){
            this.addChild(child);
        }
    }

    public void insertChild(final int index, final LightUIElement child) {
        if (child == null) {
            throw new IllegalArgumentException("Attempt to put null child in container, id:" + this.getId());
        }
        child.setReadOnly(this.isReadOnly());
        child.setParent(this);
        this.children.add(index, child);
    }

    public void removeChild(final LightUIElement child) {
        if (this.children != null)
            this.children.remove(child);
    }

    public void removeChild(final int index) {
        if (this.children != null)
            this.children.remove(index);
    }

    public LightUIElement getChild(final int index) {
        return this.getChild(index, LightUIElement.class);
    }

    public <T extends LightUIElement> T getChild(final int index, final Class<T> expectedClass) {
        if (this.children != null)
            return expectedClass.cast(this.children.get(index));
        else
            return null;
    }

    public <T extends LightUIElement> T getFirstChild(final Class<T> expectedClass) {
        if (this.getChildrenCount() > 0)
            return expectedClass.cast(this.children.get(0));
        else
            return null;
    }

    public int getChildrenCount() {
        if (this.children != null) {
            return this.children.size();
        } else {
            return 0;
        }
    }

    public void clear() {
        if (this.children != null)
            this.children.clear();
    }

    public boolean replaceChild(final LightUIElement pChild) {
        final int childCount = this.getChildrenCount();
        pChild.setReadOnly(this.isReadOnly());
        for (int i = 0; i < childCount; i++) {
            final LightUIElement child = this.children.get(i);
            if (child.getId().equals(pChild.getId())) {
                this.children.set(i, pChild);
                pChild.setParent(this);
                return true;
            }
            if (child instanceof LightUIContainer) {
                if (((LightUIContainer) child).replaceChild(pChild)) {
                    return true;
                }
            }
            if (child instanceof LightUITable) {
                if (((LightUITable) child).replaceChild(pChild)) {
                    return true;
                }
            }
        }
        return false;
    }

    public LightUIElement findChild(final String searchParam, final boolean byUUID) {
        return this.findChild(searchParam, byUUID, LightUIElement.class);
    }

    public <T extends LightUIElement> T findChild(final String searchParam, final boolean byUUID, final Class<T> expectedClass) {
        final int childCount = this.getChildrenCount();
        LightUIElement result = null;
        for (int i = 0; i < childCount; i++) {
            final LightUIElement child = this.getChild(i);
            if (byUUID) {
                if (child.getUUID().equals(searchParam)) {
                    result = child;
                    break;
                }
            } else {
                if (child.getId().equals(searchParam)) {
                    result = child;
                    break;
                }
            }

            if (child instanceof LightUIContainer) {
                result = ((LightUIContainer) child).findChild(searchParam, byUUID, expectedClass);
                if (result != null) {
                    break;
                }
            }

            if (child instanceof LightUITable) {
                result = ((LightUITable) child).findElement(searchParam, byUUID, expectedClass);
                if (result != null) {
                    break;
                }
            }
        }

        if (result != null) {
            if (expectedClass.isAssignableFrom(result.getClass())) {
                return expectedClass.cast(result);
            } else {
                throw new InvalidClassException(expectedClass.getName(), result.getClass().getName(), result.getId());
            }
        }
        return null;
    }

    public <T extends LightUIElement> List<T> findChildren(final Class<T> expectedClass, final boolean recursively) {
        final List<T> result = new ArrayList<T>();
        final int childCount = this.getChildrenCount();
        for (int i = 0; i < childCount; i++) {
            final LightUIElement child = this.getChild(i);

            if (recursively) {
                if (child instanceof LightUIContainer) {
                    result.addAll(((LightUIContainer) child).findChildren(expectedClass, true));
                }

                if (child instanceof LightUITable) {
                    result.addAll(((LightUITable) child).findChildren(expectedClass, true));
                }
            }

            if (expectedClass.isAssignableFrom(child.getClass())) {
                result.add(expectedClass.cast(child));
            }
        }

        return result;
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);
        if (!(element instanceof LightUIContainer)) {
            throw new InvalidClassException(LightUIContainer.class.getName(), element.getClassName(), element.getId());
        }

        this.clear();
        final LightUIContainer container = (LightUIContainer) element;
        this.children = new ArrayList<LightUIElement>(container.children);

        for (final LightUIElement child : this.children) {
            child.setParent(this);
        }
    }

    @Override
    public void dump(final PrintStream out, final int depth) {
        super.dump(out, depth);
        for (final LightUIElement child : this.children) {
            child.dump(out, depth + 1);
        }
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);

        final int childCount = this.getChildrenCount();
        for (int i = 0; i < childCount; i++) {
            final LightUIElement child = this.getChild(i);
            child.setReadOnly(readOnly);
        }
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = super.toJSON();
        if (this.children == null || !this.children.isEmpty()) {
            result.put("childs", JSONConverter.getJSON(this.children));
        }

        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.clear();

        final JSONArray jsonElements = (JSONArray) JSONConverter.getParameterFromJSON(json, "childs", JSONArray.class);
        this.children = new ArrayList<LightUIElement>();
        if (jsonElements != null) {
            for (final Object o : jsonElements) {
                final JSONObject jsonElement = (JSONObject) JSONConverter.getObjectFromJSON(o, JSONObject.class);
                if (jsonElement == null) {
                    throw new IllegalArgumentException("null element in json parameter");
                }
                final LightUIElement lightElement = JSONToLightUIConvertorManager.getInstance().createUIElementFromJSON(jsonElement);
                this.children.add(lightElement);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (final LightUIElement child : this.children) {
            child.destroy();
        }
    }
}
