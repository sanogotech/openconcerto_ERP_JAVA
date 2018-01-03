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
 
 package org.openconcerto.ui.component;

import java.awt.Component;
import java.awt.TextComponent;

import javax.swing.text.JTextComponent;

public enum InteractionMode {

    /** All interactions are disabled */
    DISABLED(false, false),
    /** Content can be read, only read-only interaction enabled */
    READ_ONLY(true, false),
    /** Content can be modified, all interactions enabled */
    READ_WRITE(true, true);

    private final boolean enabled, editable;

    private InteractionMode(final boolean enabled, final boolean editable) {
        this.enabled = enabled;
        this.editable = editable;
    }

    /**
     * Whether some interactions are allowed.
     * 
     * @return <code>true</code> if at least some interactions are allowed.
     */
    public final boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Whether the content can be modified.
     * 
     * @return <code>true</code> if the content can be modified.
     */
    public final boolean isEditable() {
        return this.editable;
    }

    /**
     * Try to apply this mode to the passed component. This method has special cases for known
     * component (.e.g {@link JTextComponent}) otherwise generic components are presumed to not have
     * a {@link #READ_ONLY} mode. For complete control, implement {@link InteractionComponent}.
     * 
     * @param comp a component.
     * @return the passed component.
     * @see #from(Component)
     */
    public Component applyTo(final Component comp) {
        if (comp instanceof InteractionComponent) {
            ((InteractionComponent) comp).setInteractionMode(this);
        } else if (comp instanceof JTextComponent) {
            comp.setEnabled(this.isEnabled());
            ((JTextComponent) comp).setEditable(this.isEditable());
        } else if (comp instanceof TextComponent) {
            comp.setEnabled(this.isEnabled());
            ((TextComponent) comp).setEditable(this.isEditable());
        } else {
            comp.setEnabled(this.isEditable());
        }
        return comp;
    }

    /**
     * Try to infer the mode of the passed component. This method has special cases for known
     * component (.e.g {@link JTextComponent}) otherwise generic components are presumed to not have
     * a {@link #READ_ONLY} mode. For complete control, implement {@link InteractionComponent}.
     * 
     * @param comp a component.
     * @return the inferred mode.
     * @see #applyTo(Component)
     */
    public static InteractionMode from(final Component comp) {
        if (comp instanceof InteractionComponent) {
            return ((InteractionComponent) comp).getInteractionMode();
        } else if (comp instanceof TextComponent || comp instanceof JTextComponent) {
            final boolean enabled = comp.isEnabled();
            // optimization to avoid casting and calling isEditable()
            if (!enabled)
                return DISABLED;
            final boolean editable = comp instanceof TextComponent && ((TextComponent) comp).isEditable() || comp instanceof JTextComponent && ((JTextComponent) comp).isEditable();
            return from(enabled, editable);
        } else {
            // most components do not have the concept or R/O
            return comp.isEnabled() ? READ_WRITE : DISABLED;
        }
    }

    public static InteractionMode from(final boolean enabled, final boolean editable) {
        if (!enabled)
            return DISABLED;
        else if (editable)
            return READ_WRITE;
        else
            return READ_ONLY;
    }

    public static interface InteractionComponent {
        public void setInteractionMode(InteractionMode mode);

        public InteractionMode getInteractionMode();
    }
}
