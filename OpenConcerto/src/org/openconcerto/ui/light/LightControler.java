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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONObject;

public class LightControler implements Externalizable, Transferable {
    /**
     * 
     */
    private static final long serialVersionUID = 5894135825924339012L;
    private String type, src, dest;
    public static final String TYPE_ACTIVATION_ON_SELECTION = "activationOnSelection";
    public static final String TYPE_ADD_DEFAULT = "addDefault";
    public static final String TYPE_INSERT_DEFAULT = "insertDefault";
    public static final String TYPE_COPY = "copy";
    public static final String TYPE_REMOVE = "remove";
    public static final String TYPE_UP = "up";
    public static final String TYPE_DOWN = "down";
    public static final String TYPE_CLOSE = "close";
    public static final String TYPE_TILT_PREVIOUS = "tilt.previous";
    public static final String TYPE_TILT_NEXT = "tilt.next";

    public LightControler() {
        // Serialization
    }

    public LightControler(final JSONObject json) {
        this.fromJSON(json);
    }

    public LightControler(final String type, final String src, final String dest) {
        this.type = type;
        this.src = src;
        this.dest = dest;
    }

    public String getType() {
        return this.type;
    }

    public String getSrc() {
        return this.src;
    }

    public String getDest() {
        return this.dest;
    }

    @Override
    public String toString() {
        return super.getClass().getName() + " : " + this.type + " :" + this.src + "," + this.dest;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "LightControler");
        result.put("type", this.type);
        result.put("src", this.src);
        result.put("dest", this.dest);
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.type = (String) JSONConverter.getParameterFromJSON(json, "type", String.class);
        this.src = (String) JSONConverter.getParameterFromJSON(json, "src", String.class);
        this.dest = (String) JSONConverter.getParameterFromJSON(json, "dest", String.class);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.type);
        out.writeUTF(this.src);
        out.writeUTF(this.dest);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.type = in.readUTF();
        this.src = in.readUTF();
        this.dest = in.readUTF();
    }
}
