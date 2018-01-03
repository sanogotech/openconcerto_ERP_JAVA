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

import net.minidev.json.JSONObject;

public class LightUIElement implements Transferable {

    private static final String HORIZONTALLY_RESIZABLE = "horizontally-resizable";
    private static final String VERTICALLY_SCROLLABLE = "vertically-scrollable";
    private static final String VERTICALLY_RESIZABLE = "vertically-resizable";
    private static final String HORIZONTALLY_SCROLLABLE = "horizontally-scrollable";

    public enum GlyphIcon {
        WARNING("#warning"), PLUS("#plus"), PENCIL("#pencil"), SEARCH("#search"), REMOVE("#remove"), STAR("#star"), STAR_EMPTY("#star-empty"), USER("#user"), LOCK("#lock"), UNLOCK(
                "#unlock"), DOWNLOAD("#download"), UPLOAD("#upload");

        private final String id;

        GlyphIcon(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    };

    /**
     * 
     */
    private static final long serialVersionUID = 3272357171610073289L;
    // type
    public static final int TYPE_LABEL = 0;
    public static final int TYPE_TEXT_FIELD = 1;
    public static final int TYPE_DATE = 2;
    public static final int TYPE_COMBOBOX = 3;
    public static final int TYPE_TABLE = 4;
    public static final int TYPE_CHECKBOX = 5;
    public static final int TYPE_TABBED_UI = 6;
    public static final int TYPE_COMBOBOX_ELEMENT = 7;
    public static final int TYPE_PANEL = 8;
    public static final int TYPE_TREE = 9;
    public static final int TYPE_TEXT = 10;
    public static final int TYPE_LIST = 11;
    public static final int TYPE_DROPDOWN_BUTTON = 12;
    public static final int TYPE_FRAME = 13;
    public static final int TYPE_IMAGE = 14;
    public static final int TYPE_FILE_UPLOAD_WITH_SELECTION = 15;
    public static final int TYPE_PANEL_LINE = 16;
    public static final int TYPE_TAB_ELEMENT = 17;
    public static final int TYPE_SLIDER = 18;
    public static final int TYPE_PICTURE_UPLOAD = 19;
    public static final int TYPE_BUTTON = 20;
    public static final int TYPE_BUTTON_WITH_CONTEXT = 21;
    public static final int TYPE_BUTTON_CANCEL = 22;
    public static final int TYPE_BUTTON_UNMANAGED = 23;
    public static final int TYPE_BUTTON_WITH_SELECTION_CONTEXT = 24;
    public static final int TYPE_BUTTON_LINK = 25;
    public static final int TYPE_RAW_HTML = 26;
    public static final int TYPE_TEXT_AREA = 27;
    public static final int TYPE_FILE_UPLOAD = 28;
    public static final int TYPE_LIST_ROW = 29;
    public static final int TYPE_BADGE = 30;
    public static final int TYPE_AUTOCOMPLETE_COMBOBOX = 31;
    public static final int TYPE_COLOR_PICKER = 32;
    public static final int TYPE_HOUR_EDITOR = 33;
    // valueType
    public static final int VALUE_TYPE_STRING = 0;
    public static final int VALUE_TYPE_INTEGER = 1;
    public static final int VALUE_TYPE_DATE = 2;
    public static final int VALUE_TYPE_REF = 3;
    public static final int VALUE_TYPE_LIST = 4;
    public static final int VALUE_TYPE_DECIMAL = 5;
    public static final int VALUE_TYPE_BOOLEAN = 6;
    // actionType
    public static final int ACTION_TYPE_SELECTION = 0;
    public static final int ACTION_TYPE_REMOVE = 1;
    public static final int ACTION_TYPE_REFRESH = 2;

    // commitMode
    public static final int COMMIT_ONCE = 0;
    public static final int COMMIT_INTERACTIVE = 1;
    // horizontalAlignement
    public static final int HALIGN_RIGHT = 0;
    public static final int HALIGN_CENTER = 1;
    public static final int HALIGN_LEFT = 2; // Default
    // verticalAlignement
    public static final int VALIGN_TOP = 0; // Default
    public static final int VALIGN_CENTER = 1;
    public static final int VALIGN_BOTTOM = 2;
    // font size
    public static final int FONT_XXSMALL = 0;
    public static final int FONT_XSMALL = 1;
    public static final int FONT_SMALL = 2; // Default
    public static final int FONT_MEDIUM = 3;
    public static final int FONT_LARGE = 4;
    public static final int FONT_XLARGE = 5;
    public static final int FONT_XXLARGE = 6;

    public static final int DEFAULT_GRID_HEIGHT = 1;
    public static final int DEFAULT_GRID_WIDTH = 1;
    public static final int DEFAULT_WEIGHT_X = 0;
    public static final int DEFAULT_WEIGHT_Y = 0;

    private int fontSize = FONT_SMALL;
    private int gridHeight = DEFAULT_GRID_HEIGHT;
    private int gridWidth = DEFAULT_GRID_WIDTH;
    private int horizontalAlignment = HALIGN_LEFT;
    private int verticalAlignment = VALIGN_TOP;
    private int weightX = DEFAULT_WEIGHT_X;
    private int weightY = DEFAULT_WEIGHT_Y;

    private Integer commitMode;
    private Integer height;
    private Integer marginBottom;
    private Integer marginLeft;
    private Integer marginRight;
    private Integer marginTop;
    private Integer maxHeight;
    private Integer maxWidth;
    private Integer minInputSize;
    private Integer minHeight;
    private Integer minWidth;
    private Integer paddingBottom;
    private Integer paddingLeft;
    private Integer paddingRight;
    private Integer paddingTop;
    private Integer type;
    private Integer valueType;
    private Integer width;

    private boolean enabled = true;
    private boolean fillHeight = false;
    private boolean fillWidth = false;
    private boolean foldable = false;
    private boolean folded = false;
    private boolean fontBold = false;
    private boolean fontItalic = false;
    private boolean horizontallyResizable = false;
    private boolean readOnly = false;
    private boolean required = false;
    private boolean verticallyResizable = false;
    private boolean visible = true;
    private boolean notSaved = false;
    private boolean verticallyScrollable = false;
    private boolean horizontallyScrollable = false;

    private String displayPrecision;// "(1,2)" means that 0.159 is shown as 0.16
    private String icon;
    private String id;
    private String label;
    private String toolTip;
    private String UUID;
    // Values
    private String value;
    private String valuePrecision;// "(6,2)" 999999.99 is the max
    private String valueRange; // [-3.14,3.14]

    private Color backgroundColor;
    private Color borderColor;
    private Color cellBackgroundColor;
    private Color foreColor;

    private LightUIElement parent;

    public LightUIElement(final String id) {
        this.id = id;
        this.UUID = java.util.UUID.randomUUID().toString();

        JSONToLightUIConvertorManager.getInstance().put(this.getClassName(), this.getConvertor());
    }

    // Init from json constructor
    public LightUIElement(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUIElement(final LightUIElement element) {
        this.id = element.id;
        this.parent = element.parent;
        this.UUID = element.UUID;
        this.type = element.type;

        this.copy(element);
    }

    public int getFontSize() {
        return this.fontSize;
    }

    public void setFontSize(final int fontSize) {
        this.fontSize = fontSize;
    }

    public int getGridWidth() {
        return this.gridWidth;
    }

    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    public Integer getHeight() {
        return this.height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public final int getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    public final void setHorizontalAlignement(int horizontalAlignement) {
        this.horizontalAlignment = horizontalAlignement;
    }

    public int getValueType() {
        return this.valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public final int getVerticalAlignment() {
        return this.verticalAlignment;
    }

    public void setVerticalAlignement(int verticalAlignement) {
        this.verticalAlignment = verticalAlignement;
    }

    public int getWeightX() {
        return this.weightX;
    }

    public void setWeightX(final int weightX) {
        this.weightX = weightX;
    }

    public int getWeightY() {
        return this.weightY;
    }

    public void setWeightY(final int weightY) {
        this.weightY = weightY;
    }

    public Integer getCommitMode() {
        return this.commitMode;
    }

    public void setCommitMode(int commitMode) {
        this.commitMode = commitMode;
    }

    public Integer getMarginTop() {
        return this.marginTop;
    }

    public void setMarginTop(Integer marginTop) {
        this.marginTop = marginTop;
    }

    public Integer getMarginBottom() {
        return this.marginBottom;
    }

    public void setMarginBottom(Integer marginBottom) {
        this.marginBottom = marginBottom;
    }

    public Integer getMarginLeft() {
        return this.marginLeft;
    }

    public void setMarginLeft(final Integer marginLeft) {
        this.marginLeft = marginLeft;
    }

    public Integer getMarginRight() {
        return this.marginRight;
    }

    public void setMarginRight(final Integer marginRight) {
        this.marginRight = marginRight;
    }

    public Integer getMinInputSize() {
        return this.minInputSize;
    }

    public Integer getMaxHeight() {
        return this.maxHeight;
    }

    public void setMaxHeight(Integer maxHeight) {
        this.maxHeight = maxHeight;
    }

    public Integer getMinHeight() {
        return this.minHeight;
    }

    public void setMinHeight(Integer minHeight) {
        this.minHeight = minHeight;
    }

    public Integer getMaxWidth() {
        return this.maxWidth;
    }

    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    public Integer getMinWidth() {
        return this.minWidth;
    }

    public void setMinWidth(Integer minWidth) {
        this.minWidth = minWidth;
    }

    public void setMinInputSize(Integer minInputSize) {
        this.minInputSize = minInputSize;
    }

    public Integer getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(Integer paddingTop) {
        this.paddingTop = paddingTop;
    }

    public Integer getPaddingBottom() {
        return this.paddingBottom;
    }

    public void setPaddingBottom(Integer paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public Integer getPaddingLeft() {
        return this.paddingLeft;
    }

    public void setPaddingLeft(Integer paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public Integer getPaddingRight() {
        return this.paddingRight;
    }

    public void setPaddingRight(Integer paddingRight) {
        this.paddingRight = paddingRight;
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Integer getWidth() {
        return this.width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFoldable() {
        return this.foldable;
    }

    public void setFoldable(boolean foldable) {
        this.foldable = foldable;
    }

    public boolean isFolded() {
        return this.folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isFillHeight() {
        return this.fillHeight;
    }

    public void setFillHeight(boolean fillHeight) {
        this.fillHeight = fillHeight;
    }

    public boolean isFillWidth() {
        return this.fillWidth;
    }

    public void setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isNotSaved() {
        return this.notSaved;
    }

    public void setNotSaved(boolean notSaved) {
        this.notSaved = notSaved;
    }

    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getBorderColor() {
        return this.borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getCellBackgroundColor() {
        return this.cellBackgroundColor;
    }

    public void setCellBackgroundColor(Color cellBackgroundColor) {
        this.cellBackgroundColor = cellBackgroundColor;
    }

    public Color getForeColor() {
        return this.foreColor;
    }

    public void setForeColor(Color foreColor) {
        this.foreColor = foreColor;
    }

    public boolean isFontItalic() {
        return this.fontItalic;
    }

    public void setFontBold(boolean fontBold) {
        this.fontBold = fontBold;
    }

    public boolean isFontBold() {
        return this.fontBold;
    }

    public void setFontItalic(boolean fontItalic) {
        this.fontItalic = fontItalic;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUUID() {
        return this.UUID;
    }

    public void changeUUID() {
        this.UUID = java.util.UUID.randomUUID().toString();
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return this.value;
    }

    public String getDisplayPrecision() {
        return this.displayPrecision;
    }

    public void setDisplayPrecision(String displayPrecision) {
        this.displayPrecision = displayPrecision;
    }

    public String getValuePrecision() {
        return this.valuePrecision;
    }

    public void setValuePrecision(String valuePrecision) {
        this.valuePrecision = valuePrecision;
    }

    public String getValueRange() {
        return this.valueRange;
    }

    public void setValueRange(String valueRange) {
        this.valueRange = valueRange;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getToolTip() {
        return this.toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public final boolean isVerticallyResizable() {
        return this.verticallyResizable;
    }

    public final void setVerticallyResizable(boolean verticallyResizable) {
        this.verticallyResizable = verticallyResizable;
    }

    public final boolean isHorizontallyResizable() {
        return this.horizontallyResizable;
    }

    public final void setHorizontallyResizable(boolean horizontallyResizable) {
        this.horizontallyResizable = horizontallyResizable;
    }

    public final boolean isHorizontallyScrollable() {
        return this.horizontallyScrollable;
    }

    public final void setHorizontallyScrollable(boolean horizontallyScrollable) {
        this.horizontallyScrollable = horizontallyScrollable;
    }

    public final boolean isVerticallyScrollable() {
        return this.verticallyScrollable;
    }

    public final void setVerticallyScrollable(boolean verticallyScrollable) {
        this.verticallyScrollable = verticallyScrollable;
    }

    public final LightUIElement getParent() {
        return this.parent;
    }

    public final <T extends LightUIElement> T getParent(final Class<T> expectedClass) {
        LightUIElement parent = this.parent;
        while (parent != null && !expectedClass.isAssignableFrom(parent.getClass())) {
            parent = parent.getParent();
        }
        return expectedClass.cast(parent);
    }

    public void setParent(LightUIElement parent) {
        this.parent = parent;
    }

    public void dump(PrintStream out, final int depth) {
        final String type = this.getTypeAsString();
        String valueType = "?";
        if (this.valueType != null) {
            if (this.valueType == VALUE_TYPE_STRING) {
                valueType = "string";
            } else if (this.valueType == VALUE_TYPE_INTEGER) {
                valueType = "int";
            } else if (this.valueType == VALUE_TYPE_REF) {
                valueType = "ref";
            } else if (this.valueType == VALUE_TYPE_LIST) {
                valueType = "list";
            } else if (this.valueType == VALUE_TYPE_DECIMAL) {
                valueType = "decimal";
            }
        }

        String str = "LightUIElement" + "class:" + this.getClassName() + " type:" + type + " id:" + this.id + " uuid:" + this.UUID + " w:" + this.gridWidth + " fill:" + this.fillWidth;
        str += " value:" + this.value + "(" + valueType + ")";
        if (this.valueRange != null) {
            str += "range: " + this.valueRange;
        }
        if (this.valuePrecision != null) {
            str += "precision: " + this.valuePrecision;
        }
        if (this.displayPrecision != null) {
            str += "display prec.: " + this.displayPrecision;
        }
        if (this.label != null) {
            str += " label:" + this.label;
        }
        if (this.horizontallyResizable) {
            str += "|- H ->";
        }
        if (this.verticallyResizable) {
            str += "|- V ->";
        }

        switch (this.fontSize) {
        case FONT_XXSMALL:
            str += " font: xx-small";
            break;
        case FONT_XSMALL:
            str += " font: x-small";
            break;
        case FONT_SMALL:
            str += " font: small";
            break;
        case FONT_MEDIUM:
            str += " font: medium";
            break;
        case FONT_LARGE:
            str += " font: large";
            break;
        case FONT_XLARGE:
            str += " font: x-large";
            break;
        case FONT_XXLARGE:
            str += " font: xx-large";
            break;
        }

        switch (this.horizontalAlignment) {
        case HALIGN_RIGHT:
            str += " horiz-align: right";
            break;
        case HALIGN_CENTER:
            str += " horiz-align: center";
            break;
        case HALIGN_LEFT:
            str += " horiz-align: left";
            break;
        }

        switch (this.verticalAlignment) {
        case HALIGN_RIGHT:
            str += " vert-align: top";
            break;
        case HALIGN_CENTER:
            str += " vert-align: center";
            break;
        case HALIGN_LEFT:
            str += " vert-align: bottom";
            break;
        }
        addSpacer(out, depth);
        out.println(str);

    }

    protected final void addSpacer(PrintStream out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.print("  ");
        }
    }

    public final String getTypeAsString() {
        String type = "?";
        if (this.type == TYPE_CHECKBOX) {
            type = "checkbox";
        } else if (this.type == TYPE_COMBOBOX) {
            type = "combobox";
        } else if (this.type == TYPE_LABEL) {
            type = "label";
        } else if (this.type == TYPE_TEXT_AREA) {
            type = "textarea";
        } else if (this.type == TYPE_TEXT_FIELD) {
            type = "textfield";
        } else if (this.type == TYPE_TABLE) {
            type = "table";
        } else if (this.type == TYPE_TABBED_UI) {
            type = "tabs";
        } else if (this.type == TYPE_TREE) {
            type = "tree";
        } else if (this.type == TYPE_BUTTON) {
            type = "button";
        } else if (this.type == TYPE_BUTTON_WITH_CONTEXT) {
            type = "button with context";
        } else if (this.type == TYPE_BUTTON_CANCEL) {
            type = "cancel button";
        } else if (this.type == TYPE_COMBOBOX_ELEMENT) {
            type = "combo element";
        } else if (this.type == TYPE_BUTTON_WITH_SELECTION_CONTEXT) {
            type = "button with selection context";
        } else if (this.type == TYPE_FILE_UPLOAD_WITH_SELECTION) {
            type = "file upload with selection";
        } else if (this.type == TYPE_FRAME) {
            type = "frame";
        } else if (this.type == TYPE_DROPDOWN_BUTTON) {
            type = "drop down button";
        } else if (this.type == TYPE_IMAGE) {
            type = "image";
        } else if (this.type == TYPE_LIST) {
            type = "list";
        } else if (this.type == TYPE_RAW_HTML) {
            type = "raw html";
        } else if (this.type == TYPE_SLIDER) {
            type = "slider";
        } else if (this.type == TYPE_PICTURE_UPLOAD) {
            type = "picture upload";
        } else if (this.type == TYPE_FILE_UPLOAD) {
            type = "file upload";
        }

        return type;
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIElement(json);
            }
        };
    }

    protected void copy(final LightUIElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Try to copy attributes of null element in " + this.getId());
        }
        this.fontSize = element.fontSize;
        this.gridHeight = element.gridHeight;
        this.gridWidth = element.gridWidth;
        this.horizontalAlignment = element.horizontalAlignment;
        this.verticalAlignment = element.verticalAlignment;
        this.weightX = element.weightX;

        this.commitMode = element.commitMode;
        this.height = element.height;
        this.marginBottom = element.marginBottom;
        this.marginLeft = element.marginLeft;
        this.marginRight = element.marginRight;
        this.marginTop = element.marginTop;
        this.maxHeight = element.maxHeight;
        this.maxWidth = element.maxWidth;
        this.minInputSize = element.minInputSize;
        this.minHeight = element.minHeight;
        this.minWidth = element.minWidth;
        this.paddingBottom = element.paddingBottom;
        this.paddingLeft = element.paddingLeft;
        this.paddingRight = element.paddingRight;
        this.paddingTop = element.paddingTop;
        this.valueType = element.valueType;
        this.width = element.width;

        this.enabled = element.enabled;
        this.fillWidth = element.fillWidth;
        this.fillHeight = element.fillHeight;
        this.foldable = element.foldable;
        this.folded = element.folded;
        this.fontBold = element.fontBold;
        this.fontItalic = element.fontItalic;
        this.horizontallyResizable = element.horizontallyResizable;
        this.horizontallyScrollable = element.horizontallyScrollable;
        this.required = element.required;
        this.readOnly = element.readOnly;
        this.verticallyResizable = element.verticallyResizable;
        this.verticallyScrollable = element.verticallyScrollable;
        this.visible = element.visible;
        this.notSaved = element.notSaved;

        this.displayPrecision = element.displayPrecision;
        this.icon = element.icon;
        this.label = element.label;
        this.toolTip = element.toolTip;
        this.value = element.value;
        this.valuePrecision = element.valuePrecision;
        this.valueRange = element.valueRange;

        this.backgroundColor = element.backgroundColor;
        this.borderColor = element.borderColor;
        this.cellBackgroundColor = element.cellBackgroundColor;
        this.foreColor = element.foreColor;
    }

    @Override
    public LightUIElement clone() {
        return new LightUIElement(this);
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.id;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();

        result.put("class-name", this.getClassName());

        if (this.fontSize != FONT_SMALL) {
            result.put("font-size", this.fontSize);
        }
        if (this.gridWidth != DEFAULT_GRID_WIDTH) {
            result.put("grid-width", this.gridWidth);
        }
        if (this.gridHeight != DEFAULT_GRID_HEIGHT) {
            result.put("grid-height", this.gridHeight);
        }
        if (this.horizontalAlignment != HALIGN_LEFT) {
            result.put("horizontal-alignment", this.horizontalAlignment);
        }
        if (this.verticalAlignment != VALIGN_TOP) {
            result.put("vertical-alignment", this.verticalAlignment);
        }
        if (this.weightX != DEFAULT_WEIGHT_X) {
            result.put("weight-x", this.weightX);
        }

        if (this.commitMode != null) {
            result.put("commit-mode", this.commitMode);
        }
        if (this.height != null) {
            result.put("height", this.height);
        }
        if (this.marginBottom != null) {
            result.put("m-bottom", this.marginBottom);
        }
        if (this.marginLeft != null) {
            result.put("m-left", this.marginLeft);
        }
        if (this.marginRight != null) {
            result.put("m-right", this.marginRight);
        }
        if (this.marginTop != null) {
            result.put("m-top", this.marginTop);
        }
        if (this.maxHeight != null) {
            result.put("max-height", this.maxHeight);
        }
        if (this.maxWidth != null) {
            result.put("max-width", this.maxWidth);
        }
        if (this.minInputSize != null) {
            result.put("min-input-size", this.minInputSize);
        }
        if (this.minHeight != null) {
            result.put("min-height", this.minHeight);
        }
        if (this.minWidth != null) {
            result.put("min-width", this.minWidth);
        }
        if (this.paddingBottom != null) {
            result.put("p-bottom", this.paddingBottom);
        }
        if (this.paddingLeft != null) {
            result.put("p-left", this.paddingLeft);
        }
        if (this.paddingRight != null) {
            result.put("p-right", this.paddingRight);
        }
        if (this.paddingTop != null) {
            result.put("p-top", this.paddingTop);
        }
        result.put("type", this.type);
        if (this.valueType != null) {
            result.put("value-type", this.valueType);
        }
        if (this.width != null) {
            result.put("width", this.width);
        }

        if (!this.enabled) {
            result.put("enabled", false);
        }
        if (this.fillWidth) {
            result.put("fill-width", true);
        }
        if (this.fillHeight) {
            result.put("fill-height", true);
        }
        if (this.foldable) {
            result.put("foldable", true);
        }
        if (this.folded) {
            result.put("folded", true);
        }
        if (this.fontBold) {
            result.put("bold", true);
        }
        if (this.fontItalic) {
            result.put("italic", true);
        }
        if (this.horizontallyResizable) {
            result.put(HORIZONTALLY_RESIZABLE, true);
        }
        if (this.horizontallyScrollable) {
            result.put(HORIZONTALLY_SCROLLABLE, true);
        }
        if (this.required) {
            result.put("required", true);
        }
        if (this.readOnly) {
            result.put("read-only", true);
        }
        if (this.verticallyResizable) {
            result.put(VERTICALLY_RESIZABLE, true);
        }
        if (this.verticallyScrollable) {
            result.put(VERTICALLY_SCROLLABLE, true);
        }
        if (!this.visible) {
            result.put("visible", false);
        }
        if (this.notSaved) {
            result.put("not-saved", true);
        }

        if (this.displayPrecision != null) {
            result.put("display-precision", this.displayPrecision);
        }
        if (this.icon != null) {
            result.put("icon", this.icon);
        }
        result.put("id", this.id);
        if (this.label != null) {
            result.put("label", this.label);
        }
        if (this.toolTip != null) {
            result.put("tool-tip", this.toolTip);
        }
        result.put("uuid", this.UUID);
        if (this.value != null) {
            result.put("value", this.value);
        }
        if (this.valuePrecision != null) {
            result.put("value-precision", this.valuePrecision);
        }
        if (this.valueRange != null) {
            result.put("value-range", this.valueRange);
        }

        if (this.backgroundColor != null) {
            result.put("background-color", JSONConverter.getJSON(this.backgroundColor));
        }
        if (this.borderColor != null) {
            result.put("border-color", JSONConverter.getJSON(this.borderColor));
        }
        if (this.cellBackgroundColor != null) {
            result.put("cell-background-color", JSONConverter.getJSON(this.cellBackgroundColor));
        }
        if (this.foreColor != null) {
            result.put("fore-color", JSONConverter.getJSON(this.foreColor));
        }

        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = JSONConverter.getParameterFromJSON(json, "id", String.class);
        this.UUID = JSONConverter.getParameterFromJSON(json, "uuid", String.class);
        this.displayPrecision = JSONConverter.getParameterFromJSON(json, "display-precision", String.class);
        this.icon = JSONConverter.getParameterFromJSON(json, "icon", String.class);
        this.label = JSONConverter.getParameterFromJSON(json, "label", String.class);

        this.toolTip = JSONConverter.getParameterFromJSON(json, "tool-tip", String.class);
        this.value = JSONConverter.getParameterFromJSON(json, "value", String.class);
        this.valuePrecision = JSONConverter.getParameterFromJSON(json, "value-precision", String.class);
        this.valueRange = JSONConverter.getParameterFromJSON(json, "value-range", String.class);

        this.commitMode = JSONConverter.getParameterFromJSON(json, "commit-mode", Integer.class);
        this.fontSize = JSONConverter.getParameterFromJSON(json, "font-size", Integer.class, FONT_SMALL);
        this.gridWidth = JSONConverter.getParameterFromJSON(json, "grid-width", Integer.class, DEFAULT_GRID_WIDTH);
        this.gridHeight = JSONConverter.getParameterFromJSON(json, "grid-height", Integer.class, DEFAULT_GRID_HEIGHT);
        this.horizontalAlignment = JSONConverter.getParameterFromJSON(json, "horizontal-alignment", Integer.class, HALIGN_LEFT);
        this.verticalAlignment = JSONConverter.getParameterFromJSON(json, "vertical-alignment", Integer.class, VALIGN_TOP);
        this.weightX = JSONConverter.getParameterFromJSON(json, "weight-x", Integer.class, DEFAULT_WEIGHT_X);
        this.minInputSize = JSONConverter.getParameterFromJSON(json, "min-input-size", Integer.class);
        this.type = JSONConverter.getParameterFromJSON(json, "type", Integer.class);
        this.valueType = JSONConverter.getParameterFromJSON(json, "value-type", Integer.class);
        this.width = JSONConverter.getParameterFromJSON(json, "width", Integer.class);
        this.marginTop = JSONConverter.getParameterFromJSON(json, "m-top", Integer.class);
        this.marginBottom = JSONConverter.getParameterFromJSON(json, "m-bottom", Integer.class);
        this.marginLeft = JSONConverter.getParameterFromJSON(json, "m-left", Integer.class);
        this.marginRight = JSONConverter.getParameterFromJSON(json, "m-right", Integer.class);
        this.maxWidth = JSONConverter.getParameterFromJSON(json, "max-width", Integer.class);
        this.minWidth = JSONConverter.getParameterFromJSON(json, "min-width", Integer.class);
        this.height = JSONConverter.getParameterFromJSON(json, "height", Integer.class);
        this.maxHeight = JSONConverter.getParameterFromJSON(json, "max-height", Integer.class);
        this.minHeight = JSONConverter.getParameterFromJSON(json, "min-height", Integer.class);
        this.paddingTop = JSONConverter.getParameterFromJSON(json, "p-top", Integer.class);
        this.paddingBottom = JSONConverter.getParameterFromJSON(json, "p-bottom", Integer.class);
        this.paddingLeft = JSONConverter.getParameterFromJSON(json, "p-left", Integer.class);
        this.paddingRight = JSONConverter.getParameterFromJSON(json, "p-right", Integer.class);

        this.enabled = JSONConverter.getParameterFromJSON(json, "enabled", Boolean.class, true);
        this.foldable = JSONConverter.getParameterFromJSON(json, "foldable", Boolean.class, false);
        this.folded = JSONConverter.getParameterFromJSON(json, "folded", Boolean.class, false);
        this.fillWidth = JSONConverter.getParameterFromJSON(json, "fill-width", Boolean.class, false);
        this.fontBold = JSONConverter.getParameterFromJSON(json, "bold", Boolean.class, false);
        this.fontItalic = JSONConverter.getParameterFromJSON(json, "italic", Boolean.class, false);
        this.horizontallyResizable = JSONConverter.getParameterFromJSON(json, HORIZONTALLY_RESIZABLE, Boolean.class, false);
        this.horizontallyScrollable = JSONConverter.getParameterFromJSON(json, HORIZONTALLY_SCROLLABLE, Boolean.class, false);
        this.verticallyResizable = JSONConverter.getParameterFromJSON(json, VERTICALLY_RESIZABLE, Boolean.class, false);
        this.verticallyScrollable = JSONConverter.getParameterFromJSON(json, VERTICALLY_SCROLLABLE, Boolean.class, false);
        this.required = JSONConverter.getParameterFromJSON(json, "required", Boolean.class, false);
        this.readOnly = JSONConverter.getParameterFromJSON(json, "read-only", Boolean.class, false);
        this.visible = JSONConverter.getParameterFromJSON(json, "visible", Boolean.class, true);
        this.notSaved = JSONConverter.getParameterFromJSON(json, "not-saved", Boolean.class, false);

        this.backgroundColor = JSONConverter.getParameterFromJSON(json, "background-color", Color.class);
        this.borderColor = JSONConverter.getParameterFromJSON(json, "border-color", Color.class);
        this.cellBackgroundColor = JSONConverter.getParameterFromJSON(json, "cell-background-color", Color.class);
        this.foreColor = JSONConverter.getParameterFromJSON(json, "fore-color", Color.class);
    }

    public void destroy() {
        this.setValue(null);
    }
}
