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
 
 package org.openconcerto.sql.element;

import static org.openconcerto.sql.TM.getTM;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.element.TreesOfSQLRows.LinkToCut;
import org.openconcerto.sql.model.DBStructureItemNotFound;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowMode;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValues.ForeignCopyMode;
import org.openconcerto.sql.model.SQLRowValuesCluster;
import org.openconcerto.sql.model.SQLRowValuesCluster.DiffResult;
import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.SQLRowValuesCluster.StopRecurseException;
import org.openconcerto.sql.model.SQLRowValuesCluster.StoreMode;
import org.openconcerto.sql.model.SQLRowValuesCluster.WalkOptions;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLSelect.LockStrength;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.FieldGroup;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.model.graph.SQLKey.Type;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.request.SQLCache;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.ui.light.GroupToLightUIConvertor;
import org.openconcerto.sql.ui.light.LightEditFrame;
import org.openconcerto.sql.ui.light.LightUIPanelFiller;
import org.openconcerto.sql.ui.light.SavableCustomEditorProvider;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.sql.view.list.SQLTableModelSourceOffline;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.light.ComboValueConvertor;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.IntValueConvertor;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.StringValueConvertor;
import org.openconcerto.utils.CollectionMap2Itf.SetMapItf;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.LinkedListMap;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Value;
import org.openconcerto.utils.cache.CacheResult;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.Transformer;
import org.openconcerto.utils.change.ListChangeIndex;
import org.openconcerto.utils.change.ListChangeRecorder;
import org.openconcerto.utils.i18n.Grammar;
import org.openconcerto.utils.i18n.Grammar_fr;
import org.openconcerto.utils.i18n.NounClass;
import org.openconcerto.utils.i18n.Phrase;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;

/**
 * Décrit comment manipuler un élément de la BD (pas forcément une seule table, voir
 * privateForeignField).
 * 
 * @author ilm
 */
public abstract class SQLElement {

    private static Phrase createPhrase(String singular, String plural) {
        final NounClass nounClass;
        final String base;
        if (singular.startsWith("une ")) {
            nounClass = NounClass.FEMININE;
            base = singular.substring(4);
        } else if (singular.startsWith("un ")) {
            nounClass = NounClass.MASCULINE;
            base = singular.substring(3);
        } else {
            nounClass = null;
            base = singular;
        }
        final Phrase res = new Phrase(Grammar_fr.getInstance(), base, nounClass);
        if (nounClass != null)
            res.putVariant(Grammar.INDEFINITE_ARTICLE_SINGULAR, singular);
        res.putVariant(Grammar.PLURAL, plural);
        return res;
    }

    // from the most loss of information to the least.
    public static enum ReferenceAction {
        /** If a referenced row is archived, empty the foreign field */
        SET_EMPTY,
        /** If a referenced row is archived, archive this row too */
        CASCADE,
        /** If a referenced row is to be archived, abort the operation */
        RESTRICT
    }

    static final public String DEFAULT_COMP_ID = "default component code";
    /**
     * If this value is passed to the constructor, {@link #createCode()} will only be called the
     * first time {@link #getCode()} is. This allow the method to use objects passed to the
     * constructor of a subclass.
     */
    static final public String DEFERRED_CODE = new String("deferred code");

    @GuardedBy("this")
    private SQLElementDirectory directory;
    private String l18nPkgName;
    private Class<?> l18nClass;
    private Phrase name;
    private final SQLTable primaryTable;
    // used as a key in SQLElementDirectory so it should be immutable
    private String code;
    private ComboSQLRequest combo;
    private ListSQLRequest list;
    private SQLTableModelSourceOnline tableSrc;
    private final ListChangeRecorder<IListeAction> rowActions;
    private final LinkedListMap<String, ITransformer<Tuple2<SQLElement, String>, SQLComponent>> components;
    // links
    private SQLElementLinks ownedLinks;
    private SQLElementLinks otherLinks;
    // keep it for now as joins are disallowed (see initFF())
    private String parentFF;

    // lazy creation
    private SQLCache<SQLRowAccessor, Object> modelCache;

    private final Map<String, JComponent> additionalFields;
    private final List<SQLTableModelColumn> additionalListCols;
    @GuardedBy("this")
    private List<String> mdPath;

    private Group defaultGroup;
    private Group groupForCreation;
    private Group groupForModification;

    @Deprecated
    public SQLElement(String singular, String plural, SQLTable primaryTable) {
        this(primaryTable, createPhrase(singular, plural));
    }

    public SQLElement(SQLTable primaryTable) {
        this(primaryTable, null);
    }

    public SQLElement(final SQLTable primaryTable, final Phrase name) {
        this(primaryTable, name, null);
    }

    public SQLElement(final SQLTable primaryTable, final Phrase name, final String code) {
        super();
        if (primaryTable == null) {
            throw new DBStructureItemNotFound("table is null for " + this.getClass());
        }
        this.primaryTable = primaryTable;
        this.setL18nPackageName(null);
        this.setDefaultName(name);
        this.code = code == null ? createCode() : code;
        this.combo = null;
        this.list = null;
        this.rowActions = new ListChangeRecorder<IListeAction>(new ArrayList<IListeAction>());
        this.resetRelationships();

        this.components = new LinkedListMap<String, ITransformer<Tuple2<SQLElement, String>, SQLComponent>>();

        this.modelCache = null;

        // the components should always be in the same order
        this.additionalFields = new LinkedHashMap<String, JComponent>();
        this.additionalListCols = new ArrayList<SQLTableModelColumn>();
        this.mdPath = Collections.emptyList();
    }

    /**
     * Should return the code for this element. This method is only called if the <code>code</code>
     * parameter of the constructor is <code>null</code>.
     * 
     * @return the default code for this element.
     */
    protected String createCode() {
        return getClass().getName() + "-" + getTable().getName();
    }

    public Group getGroupForCreation() {
        if (this.groupForCreation != null) {
            return this.groupForCreation;
        }
        return getDefaultGroup();
    }

    public Group getGroupForModification() {
        if (this.groupForModification != null) {
            return this.groupForModification;
        }
        return getDefaultGroup();
    }

    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    public void setDefaultGroup(Group defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    /**
     * Get the group based on the edit mode
     * 
     * @param editMode
     * @return
     */
    public Group getEditGroup(final EditMode editMode) {
        if (editMode.equals(EditMode.CREATION)) {
            return this.getGroupForCreation();
        } else {
            return this.getGroupForModification();
        }
    }

    /**
     * Override this function in an element to show default values in edit frame
     * 
     * @param token - The security token of session
     * 
     * @return a default SQLRowValues
     */
    public SQLRowValues createDefaultRowValues(final String token) {
        return new SQLRowValues(getTable());
    }

    /**
     * Create the edition frame for this SQLElement
     * 
     * @param configuration current configuration
     * @param parentFrame parent frame of the edit frame
     * @param editMode edition mode (CREATION, MODIFICATION, READONLY)
     * @param sqlRow SQLRowAccessor use for fill the edition frame
     * @param sessionSecurityToken String, use for find session with an instance of LightServer
     * @return the edition frame of this SQLElement
     */
    public LightEditFrame createEditFrame(final PropsConfiguration configuration, final LightUIFrame parentFrame, final EditMode editMode, final SQLRowAccessor sqlRow,
            final String sessionSecurityToken) {
        final Group editGroup = this.getEditGroup(editMode);
        if (editGroup == null) {
            Log.get().severe("The edit group is null for this element : " + this);
            return null;
        }

        final GroupToLightUIConvertor convertor = this.getGroupToLightUIConvertor(configuration, editMode, sqlRow, sessionSecurityToken);
        final LightEditFrame editFrame = convertor.convert(editGroup, sqlRow, parentFrame, editMode);

        if (editMode.equals(EditMode.CREATION)) {
            editFrame.createTitlePanel(this.getCreationFrameTitle());
        } else if (editMode.equals(EditMode.MODIFICATION)) {
            editFrame.createTitlePanel(this.getModificationFrameTitle(sqlRow));
            new LightUIPanelFiller(editFrame.getFirstChild(LightUIPanel.class)).fillFromRow(configuration, sqlRow);
        } else if (editMode.equals(EditMode.READONLY)) {
            editFrame.createTitlePanel(this.getReadOnlyFrameTitle(sqlRow));
            new LightUIPanelFiller(editFrame.getFirstChild(LightUIPanel.class)).fillFromRow(configuration, sqlRow);
        }

        this.setEditFrameModifiers(editFrame, sessionSecurityToken);

        return editFrame;
    }

    /**
     * Get title for read only mode
     * 
     * @param sqlRow - SQLRowValues use for fill the edition frame
     * @return The title for read only mode
     */
    protected String getReadOnlyFrameTitle(final SQLRowAccessor sqlRow) {
        return EditFrame.getReadOnlyMessage(this);
    }

    /**
     * Get title for modification mode
     * 
     * @param sqlRow - SQLRowValues use for fill the edition frame
     * @return The title for read only mode
     */
    protected String getModificationFrameTitle(final SQLRowAccessor sqlRow) {
        return EditFrame.getModifyMessage(this);
    }

    /**
     * Get title for creation mode
     * 
     * @param sqlRow - SQLRowValues use for fill the edition frame
     * @return The title for read only mode
     */
    protected String getCreationFrameTitle() {
        return EditFrame.getCreateMessage(this);
    }

    /**
     * 
     * @param configuration - The user SQL configuration
     * @param editMode - Edit mode of the frame
     * @param sqlRow - The row to update
     * @param token - The session security token
     * 
     * @return An initialized GroupToLightUIConvertor
     */
    public GroupToLightUIConvertor getGroupToLightUIConvertor(final PropsConfiguration configuration, final EditMode editMode, final SQLRowAccessor sqlRow, final String token) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        if (editMode.equals(EditMode.CREATION)) {
            convertor.putAllCustomEditorProvider(this.getCustomEditorProviderForCreation(configuration, token));
        } else {
            convertor.putAllCustomEditorProvider(this.getCustomEditorProviderForModification(configuration, sqlRow, token));
        }
        return convertor;
    }

    /**
     * Override this function in an element and put new value in map for use ComboValueConvertor.
     * This one allow you to change value store in database by an other one.
     * 
     * @return Map which contains all ComboValueConvertors use for this SQLElement edition. Key: ID
     *         of group item / Value: ComboValueConvertor
     */
    // TODO: use renderer instead of ValueConvertor
    public Map<String, ComboValueConvertor<?>> getComboConvertors() {
        return new HashMap<String, ComboValueConvertor<?>>();
    }

    /**
     * Override this function in an element and put new value in map for use ConvertorModifier. This
     * one allow you to apply some change on LightUIElement before send it to client
     * 
     */
    // TODO: implement with IClosure
    public void setEditFrameModifiers(final LightEditFrame frame, final String sessionToken) {
    }

    public final Map<String, CustomEditorProvider> getCustomEditorProviderForCreation(final Configuration configuration, final String sessionToken) {
        final Map<String, CustomEditorProvider> map = this.getDefaultCustomEditorProvider(configuration, null, sessionToken);
        map.putAll(this._getCustomEditorProviderForCreation(configuration, sessionToken));
        return map;
    }

    public final Map<String, CustomEditorProvider> getCustomEditorProviderForModification(final Configuration configuration, final SQLRowAccessor sqlRow, final String sessionToken) {
        final Map<String, CustomEditorProvider> map = this.getDefaultCustomEditorProvider(configuration, sqlRow, sessionToken);
        map.putAll(this._getCustomEditorProviderForModification(configuration, sqlRow, sessionToken));
        return map;
    }

    protected Map<String, CustomEditorProvider> _getCustomEditorProviderForCreation(final Configuration configuration, final String sessionToken) {
        return new HashMap<String, CustomEditorProvider>();
    }

    protected Map<String, CustomEditorProvider> _getCustomEditorProviderForModification(final Configuration configuration, final SQLRowAccessor sqlRow, final String sessionToken) {
        return new HashMap<String, CustomEditorProvider>();
    }

    protected Map<String, CustomEditorProvider> _getDefaultCustomEditorProvider(final Configuration configuration, final SQLRowAccessor sqlRow, final String sessionToken) {
        return new HashMap<String, CustomEditorProvider>();
    }

    private final Map<String, CustomEditorProvider> getDefaultCustomEditorProvider(final Configuration configuration, final SQLRowAccessor sqlRow, final String sessionToken) {
        final Map<String, ComboValueConvertor<?>> comboConvertors = this.getComboConvertors();
        final Map<String, CustomEditorProvider> result = new HashMap<String, CustomEditorProvider>();
        for (final Entry<String, ComboValueConvertor<?>> entry : comboConvertors.entrySet()) {
            result.put(entry.getKey(), new SavableCustomEditorProvider() {
                final ComboValueConvertor<?> convertor = entry.getValue();

                @Override
                public LightUIElement createUIElement(final String elementId) {
                    final LightUIComboBox uiCombo = new LightUIComboBox(elementId);

                    if (sqlRow == null) {
                        this.convertor.fillCombo(uiCombo, null);
                    } else {
                        final SQLField field = configuration.getFieldMapper().getSQLFieldForItem(elementId);
                        if (this.convertor instanceof StringValueConvertor) {
                            ((StringValueConvertor) this.convertor).fillCombo(uiCombo, sqlRow.getString(field.getFieldName()));
                        } else if (this.convertor instanceof IntValueConvertor) {
                            if (sqlRow.getObject(field.getFieldName()) == null) {
                                this.convertor.fillCombo(uiCombo, null);
                            } else {
                                ((IntValueConvertor) this.convertor).fillCombo(uiCombo, sqlRow.getInt(field.getFieldName()));
                            }
                        }
                    }
                    return uiCombo;
                }

                @Override
                protected void _save(final SQLRowValues sqlRow, final SQLField sqlField, final LightUIElement uiElement) {
                    final LightUIComboBox combo = (LightUIComboBox) uiElement;
                    if (combo.hasSelectedValue()) {
                        if (this.convertor instanceof StringValueConvertor) {
                            sqlRow.put(sqlField.getName(), ((StringValueConvertor) this.convertor).getIdFromIndex(combo.getSelectedValue().getId()));
                        } else if (this.convertor instanceof IntValueConvertor) {
                            sqlRow.put(sqlField.getName(), combo.getSelectedValue().getId());
                        } else {
                            throw new IllegalArgumentException("the save is not implemented for the class: " + this.convertor.getClass().getName() + " - ui element id: " + uiElement.getId());
                        }
                    } else {
                        sqlRow.put(sqlField.getName(), null);
                    }
                }
            });
        }
        result.putAll(this._getDefaultCustomEditorProvider(configuration, sqlRow, sessionToken));
        return result;
    }

    /**
     * Override this function in an element to execute some code just after inserted new row in
     * database
     *
     * @param editFrame - The edit frame of this SQLRow
     * @param sqlRow - The row which was just inserted
     * @param sessionToken Security token of session which allow to find session in LightServer
     *        instance
     * 
     * @throws Exception
     */
    public void doAfterLightInsert(final LightEditFrame editFrame, final SQLRow sqlRow, final String sessionToken) throws Exception {

    }

    /**
     * Override this function in an element to execute some code just after deleted a row in
     * database
     *
     * @param frame - The current frame
     * @param sqlRow - The row which was deleted
     * @param sessionToken Security token of session which allow to find session in LightServer
     *        instance
     * 
     * @throws Exception
     */
    public void doAfterLightDelete(final LightUIFrame frame, final SQLRowValues sqlRow, final String sessionToken) throws Exception {

    }

    /**
     * Override this function in an element to execute some code before inserted new row in database
     *
     * @param frame - The current frame
     * @param sqlRow - The row which will be deleted
     * @param sessionToken - Security token of session which allow to find session in LightServer
     *        instance
     * 
     * @throws Exception
     */
    public void doBeforeLightDelete(final LightUIFrame frame, final SQLRowValues sqlRow, final String sessionToken) throws Exception {

    }

    /**
     * Override this function in an element to execute some code before inserted new row in database
     * 
     * @param editFrame - The edit frame of this SQLRowValues
     * @param sqlRow - The row which was just inserted
     * @param sessionToken - Security token of session which allow to find session in LightServer
     *        instance
     * 
     * @throws Exception
     */
    public void doBeforeLightInsert(final LightEditFrame editFrame, final SQLRowValues sqlRow, final String sessionToken) throws Exception {

    }

    /**
     * Get ShowAs values of this SQLElement
     * 
     * @param id - The id which you want to expand
     * 
     * @return A SQLRowValues with data
     */
    public SQLRowValues getValuesOfShowAs(final Number id) {
        final SQLRowValues tmp = new SQLRowValues(this.getTable());
        final ListMap<String, String> showAs = this.getShowAs();

        for (final List<String> listStr : showAs.values()) {
            tmp.putNulls(listStr);
        }
        this.getDirectory().getShowAs().expand(tmp);

        final SQLRowValues fetched = SQLRowValuesListFetcher.create(tmp).fetchOne(id);
        if (fetched == null) {
            throw new IllegalArgumentException("Impossible to find Row in database - table: " + this.getTable().getName() + ", id: " + id);
        }

        return fetched;
    }

    /**
     * Must be called if foreign/referent keys are added or removed.
     */
    public synchronized void resetRelationships() {
        if (this.areRelationshipsInited()) {
            // if we remove links, notify owned elements
            for (final SQLElementLink l : this.ownedLinks.getByPath().values()) {
                l.getOwned().resetRelationshipsOf(this);
            }
        }
        this.ownedLinks = this instanceof JoinSQLElement ? new SQLElementLinks(SetMap.<LinkType, SQLElementLink> empty()) : null;
        this.otherLinks = this instanceof JoinSQLElement ? new SQLElementLinks(SetMap.<LinkType, SQLElementLink> empty()) : null;
        this.parentFF = null;
    }

    private synchronized void resetRelationshipsOf(final SQLElement changed) {
        // MAYBE optimize and only remove links for the passed argument
        this.otherLinks = null;
    }

    protected synchronized final boolean areRelationshipsInited() {
        return this.ownedLinks != null;
    }

    // return Path from owner to owned
    private final Set<Path> createPaths(final boolean wantedOwned) {
        assert !(this instanceof JoinSQLElement) : "joins cannot have SQLElementLink : " + this;
        final SQLTable thisTable = this.getTable();
        final Set<Link> allLinks = thisTable.getDBSystemRoot().getGraph().getAllLinks(getTable());
        final Set<Path> res = new HashSet<Path>();
        for (final Link l : allLinks) {
            final boolean owned;
            final Path pathFromOwner;
            final SQLElement sourceElem = this.getElementLenient(l.getSource());
            if (sourceElem instanceof JoinSQLElement) {
                final JoinSQLElement joinElem = (JoinSQLElement) sourceElem;
                pathFromOwner = joinElem.getPathFromOwner();
                // ATTN when source == target, the same path will both owned and not
                owned = joinElem.getLinkToOwner().equals(l);
            } else if (l.getSource() == l.getTarget()) {
                owned = wantedOwned;
                pathFromOwner = new PathBuilder(l.getSource()).add(l, Direction.FOREIGN).build();
            } else {
                owned = l.getSource() == thisTable;
                pathFromOwner = new PathBuilder(l.getSource()).add(l).build();
            }
            if (owned == wantedOwned)
                res.add(pathFromOwner);
        }
        return res;
    }

    // this implementation uses getParentFFName() and assumes that links to privates are COMPOSITION
    final SetMap<LinkType, Path> getDefaultLinkTypes() {
        final Set<Path> ownedPaths = createPaths(true);
        final SetMap<LinkType, Path> res = new SetMap<LinkType, Path>();
        final String parentFFName = getParentFFName();
        if (parentFFName != null) {
            final Path pathToParent = new PathBuilder(getTable()).addForeignField(parentFFName).build();
            if (!ownedPaths.remove(pathToParent))
                throw new IllegalStateException("getParentFFName() " + pathToParent + " isn't in " + ownedPaths);
            res.add(LinkType.PARENT, pathToParent);
        }
        final List<String> privateFields = this.getPrivateFields();
        if (!privateFields.isEmpty())
            Log.get().warning("getPrivateFields() is deprecated use setupLinks(), " + this + " : " + privateFields);
        // links to private are COMPOSITION by default (normal links to privates are few)
        final Iterator<Path> iter = ownedPaths.iterator();
        while (iter.hasNext()) {
            final Path ownedPath = iter.next();
            if (getElement(ownedPath.getLast()).isPrivate()) {
                iter.remove();
                res.add(LinkType.COMPOSITION, ownedPath);
            } else if (ownedPath.length() == 1 && ownedPath.isSingleField() && privateFields.contains(ownedPath.getSingleField(0).getName())) {
                throw new IllegalStateException("getPrivateFields() contains " + ownedPath + " which points to an element which isn't private");
            }
        }
        res.addAll(LinkType.ASSOCIATION, ownedPaths);
        return res;
    }

    final List<ReferenceAction> getPossibleActions(final LinkType lt, final SQLElement targetElem) {
        // MAYBE move required fields to SQLElement and use RESTRICT

        final List<ReferenceAction> res;
        if (lt == LinkType.PARENT) {
            // SET_EMPTY would create an orphan
            res = Arrays.asList(ReferenceAction.CASCADE, ReferenceAction.RESTRICT);
        } else if (lt == LinkType.COMPOSITION) {
            res = Arrays.asList(ReferenceAction.SET_EMPTY, ReferenceAction.RESTRICT);
        } else {
            assert lt == LinkType.ASSOCIATION;
            if (targetElem.isShared()) {
                res = Arrays.asList(ReferenceAction.RESTRICT, ReferenceAction.SET_EMPTY);
            } else {
                res = Arrays.asList(ReferenceAction.values());
            }
        }
        return res;
    }

    private synchronized void initFF() {
        if (areRelationshipsInited())
            return;

        final SQLElementLinksSetup paths = new SQLElementLinksSetup(this);
        setupLinks(paths);
        this.ownedLinks = new SQLElementLinks(paths.getResult());

        // try to fill old attributes
        final SQLElementLink parentLink = this.getParentLink();
        if (parentLink != null) {
            if (parentLink.getSingleField() != null)
                this.parentFF = parentLink.getSingleField().getName();
            else
                throw new UnsupportedOperationException("Parent field name not supported : " + parentLink);
        } else {
            this.parentFF = null;
        }
        assert assertPrivateDefaultValues();

        // if we added links, let the owned know
        final Set<SQLElement> toReset = new HashSet<SQLElement>();
        for (final SQLElementLink l : this.ownedLinks.getByPath().values()) {
            toReset.add(l.getOwned());
        }
        for (final SQLElement e : toReset) {
            e.resetRelationshipsOf(this);
        }

        this.ffInited();
    }

    // since by definition private cannot be shared, the default value must be empty
    private final boolean assertPrivateDefaultValues() {
        final Set<SQLElementLink> privates = this.getOwnedLinks().getByType(LinkType.COMPOSITION);
        for (final SQLElementLink e : privates) {
            if (!e.isJoin()) {
                final SQLField singleField = e.getSingleField();
                final Number privateDefault = (Number) singleField.getParsedDefaultValue().getValue();
                final Number foreignUndef = e.getPath().getLast().getUndefinedIDNumber();
                assert NumberUtils.areNumericallyEqual(privateDefault, foreignUndef) : singleField + " not empty : " + privateDefault;
            }
        }
        return true;
    }

    public boolean isPrivate() {
        return false;
    }

    /**
     * Set {@link LinkType type} and other information for each owned link of this element.
     * 
     * @param links the setup object.
     */
    protected void setupLinks(SQLElementLinksSetup links) {
    }

    /**
     * Was used to set the action of an {@link SQLElementLink}.
     * 
     * @deprecated use {@link SQLElementLinkSetup#setType(LinkType, ReferenceAction)}
     */
    protected void ffInited() {
        // MAYBE use DELETE_RULE of Link
    }

    private final Set<SQLField> getSingleFields(final SQLElementLinks links, final LinkType type) {
        final Set<SQLField> res = new HashSet<SQLField>();
        for (final SQLElementLink l : links.getByType(type)) {
            final SQLField singleField = l.getSingleField();
            if (singleField == null)
                throw new IllegalStateException("Not single field : " + l);
            res.add(singleField);
        }
        return res;
    }

    private synchronized void initRF() {
        if (this.otherLinks != null)
            return;
        final Set<Path> otherPaths = this.createPaths(false);
        final SetMap<LinkType, SQLElementLink> tmp = new SetMap<LinkType, SQLElementLink>();
        for (final Path p : otherPaths) {
            final SQLElement refElem = this.getElementLenient(p.getFirst());
            final SQLElementLink elementLink;
            if (refElem == null) {
                // RESTRICT : play it safe
                elementLink = new SQLElementLink(null, p, this, LinkType.ASSOCIATION, null, ReferenceAction.RESTRICT);
            } else {
                elementLink = refElem.getOwnedLinks().getByPath(p);
                assert elementLink.getOwned() == this;
            }
            tmp.add(elementLink.getLinkType(), elementLink);
        }
        this.otherLinks = new SQLElementLinks(tmp);
    }

    final void setDirectory(final SQLElementDirectory directory) {
        // since this method should only be called at the end of SQLElementDirectory.addSQLElement()
        assert directory == null || directory.getElement(this.getTable()) == this;
        synchronized (this) {
            if (this.directory != directory) {
                if (this.areRelationshipsInited())
                    this.resetRelationships();
                this.directory = directory;
            }
        }
    }

    public synchronized final SQLElementDirectory getDirectory() {
        return this.directory;
    }

    final SQLElement getElement(SQLTable table) {
        final SQLElement res = getElementLenient(table);
        if (res == null)
            throw new IllegalStateException("no element for " + table.getSQLName());
        return res;
    }

    final SQLElement getElementLenient(SQLTable table) {
        synchronized (this) {
            return this.getDirectory().getElement(table);
        }
    }

    public final SQLElement getForeignElement(String foreignField) {
        try {
            return this.getElement(this.getForeignTable(foreignField));
        } catch (RuntimeException e) {
            throw new IllegalStateException("no element for " + foreignField + " in " + this, e);
        }
    }

    private final SQLTable getForeignTable(String foreignField) {
        return this.getTable().getBase().getGraph().getForeignTable(this.getTable().getField(foreignField));
    }

    public final synchronized String getL18nPackageName() {
        return this.l18nPkgName;
    }

    public final synchronized Class<?> getL18nClass() {
        return this.l18nClass;
    }

    public final void setL18nLocation(Class<?> clazz) {
        this.setL18nLocation(clazz.getPackage().getName(), clazz);
    }

    public final void setL18nPackageName(String name) {
        this.setL18nLocation(name, null);
    }

    /**
     * Set the location for the localized name.
     * 
     * @param name a package name, can be <code>null</code> :
     *        {@link SQLElementDirectory#getL18nPackageName()} will be used.
     * @param ctxt the class loader to load the resource, <code>null</code> meaning this class.
     * @see SQLElementDirectory#getName(SQLElement)
     */
    public final synchronized void setL18nLocation(final String name, final Class<?> ctxt) {
        this.l18nPkgName = name;
        this.l18nClass = ctxt == null ? this.getClass() : ctxt;
    }

    /**
     * Set the default name, used if no translations could be found.
     * 
     * @param name the default name, if <code>null</code> the {@link #getTable() table} name will be
     *        used.
     */
    public final synchronized void setDefaultName(Phrase name) {
        this.name = name != null ? name : Phrase.getInvariant(getTable().getName());
    }

    /**
     * The default name.
     * 
     * @return the default name, never <code>null</code>.
     */
    public final synchronized Phrase getDefaultName() {
        return this.name;
    }

    /**
     * The name of this element in the current locale.
     * 
     * @return the name of this, {@link #getDefaultName()} if there's no {@link #getDirectory()
     *         directory} or if it hasn't a name for this.
     * @see SQLElementDirectory#getName(SQLElement)
     */
    public final Phrase getName() {
        final SQLElementDirectory dir = this.getDirectory();
        final Phrase res = dir == null ? null : dir.getName(this);
        return res == null ? this.getDefaultName() : res;
    }

    public String getPluralName() {
        return this.getName().getVariant(Grammar.PLURAL);
    }

    public String getSingularName() {
        return this.getName().getVariant(Grammar.INDEFINITE_ARTICLE_SINGULAR);
    }

    public ListMap<String, String> getShowAs() {
        // nothing by default
        return null;
    }

    /**
     * Fields that can neither be inserted nor updated.
     * 
     * @return fields that cannot be modified.
     */
    public Set<String> getReadOnlyFields() {
        return Collections.emptySet();
    }

    /**
     * Fields that can only be set on insertion.
     * 
     * @return fields that cannot be modified.
     */
    public Set<String> getInsertOnlyFields() {
        return Collections.emptySet();
    }

    private final SQLCache<SQLRowAccessor, Object> getModelCache() {
        if (this.modelCache == null)
            this.modelCache = new SQLCache<SQLRowAccessor, Object>(60, -1, "modelObjects of " + this.getCode());
        return this.modelCache;
    }

    // *** update

    /**
     * Compute the necessary steps to transform <code>from</code> into <code>to</code>.
     * 
     * @param from the row currently in the db.
     * @param to the new values.
     * @return the script transforming <code>from</code> into <code>to</code>.
     */
    public final UpdateScript update(SQLRowValues from, SQLRowValues to) {
        return this.update(from, to, false);
    }

    public final UpdateScript update(final SQLRowValues from, final SQLRowValues to, final boolean allowedToChangeTo) {
        return this.update(from, to, allowedToChangeTo, Transformer.<SQLRowValues> nopTransformer());
    }

    private final UpdateScript update(final SQLRowValues from, SQLRowValues to, boolean allowedToChangeTo, ITransformer<SQLRowValues, SQLRowValues> copy2originalRows) {
        check(from);
        check(to);

        for (final SQLRowValues v : from.getGraph().getItems()) {
            if (!v.hasID())
                throw new IllegalArgumentException("missing id in " + v + " : " + from.printGraph());
        }
        if (!to.hasID()) {
            if (!allowedToChangeTo) {
                final Map<SQLRowValues, SQLRowValues> copied = to.getGraph().deepCopy(false);
                to = copied.get(to);
                allowedToChangeTo = true;
                copy2originalRows = Transformer.fromMap(CollectionUtils.invertMap(new IdentityHashMap<SQLRowValues, SQLRowValues>(), copied));
            }
            // from already exists in the DB, so if we're re-using it for another row, all
            // non-provided fields must be reset
            to.fillWith(SQLRowValues.SQL_DEFAULT, false);
            to.setPrimaryKey(from);
        }
        if (from.getID() != to.getID())
            throw new IllegalArgumentException("not the same row: " + from + " != " + to);

        final UpdateScript res = new UpdateScript(this, from, copy2originalRows.transformChecked(to));
        // local values and foreign links
        for (final FieldGroup group : to.getFieldGroups()) {
            if (group.getKeyType() != Type.FOREIGN_KEY) {
                // i.e. primary key or normal field
                res.getUpdateRow().putAll(to.getAbsolutelyAll(), group.getFields());
            } else {
                final SQLKey k = group.getKey();
                if (k.getFields().size() > 1)
                    throw new IllegalStateException("Multi-field not supported : " + k);
                final String field = group.getSingleField();
                assert field != null;

                final Path p = new PathBuilder(getTable()).add(k.getForeignLink()).build();
                final SQLElementLink elemLink = this.getOwnedLinks().getByPath(p);
                if (elemLink.getLinkType() == LinkType.COMPOSITION) {
                    final SQLElement privateElem = elemLink.getOwned();
                    final Object fromPrivate = from.getObject(field);
                    final Object toPrivate = to.getObject(field);
                    assert !from.isDefault(field) : "A row in the DB cannot have DEFAULT";
                    final boolean fromIsEmpty = from.isForeignEmpty(field);
                    // as checked in initFF() the default for a private is empty
                    final boolean toIsEmpty = to.isDefault(field) || to.isForeignEmpty(field);
                    if (fromIsEmpty && toIsEmpty) {
                        // nothing to do, don't add to v
                    } else if (fromIsEmpty) {
                        final SQLRowValues toPR = (SQLRowValues) toPrivate;
                        // insert, eg CPI.ID_OBS=1 -> CPI.ID_OBS={DES="rouillé"}
                        // clear referents otherwise we will merge the updateRow with the to
                        // graph (toPR being a private is pointed to by its owner, which itself
                        // points to others, but we just want the private)
                        assert CollectionUtils.getSole(toPR.getReferentRows(elemLink.getSingleField())) == to : "Shared private " + toPR.printGraph();
                        final SQLRowValues copy = toPR.deepCopy().removeReferents(elemLink.getSingleField());
                        res.getUpdateRow().put(field, copy);
                        res.mapRow(copy2originalRows.transformChecked(toPR), copy);
                    } else if (toIsEmpty) {
                        // cut and archive
                        res.getUpdateRow().putEmptyLink(field);
                        res.addToArchive(privateElem, from.getForeign(field));
                    } else {
                        // neither is empty
                        final Number fromForeignID = from.getForeignIDNumber(field);
                        if (fromForeignID == null)
                            throw new IllegalArgumentException("Non-empty private in old row, but null ID for " + elemLink);
                        if (toPrivate == null)
                            throw new IllegalArgumentException("Non-empty private in new row, but null value for " + elemLink);
                        assert toPrivate instanceof Number || toPrivate instanceof SQLRowValues;
                        // with the above check, toForeignID is null if and only if toPrivate is an
                        // SQLRowValues without ID
                        final Number toForeignID = to.getForeignIDNumber(field);

                        if (toForeignID != null && !NumberUtils.areNumericallyEqual(fromForeignID, toForeignID))
                            throw new IllegalArgumentException("private have changed for " + field + " : " + fromPrivate + " != " + toPrivate);
                        if (toPrivate instanceof SQLRowValues) {
                            if (!(fromPrivate instanceof SQLRowValues))
                                throw new IllegalArgumentException("Asymetric graph, old row doesn't contain a row for " + elemLink + " : " + fromPrivate);
                            final SQLRowValues fromPR = (SQLRowValues) fromPrivate;
                            final SQLRowValues toPR = (SQLRowValues) toPrivate;
                            // must have same ID
                            res.put(field, privateElem.update(fromPR, toPR, allowedToChangeTo, copy2originalRows));
                        } else {
                            // if toPrivate is just an ID and the same as fromPrivate, nothing to do
                            assert toPrivate instanceof Number && toForeignID != null;
                        }
                    }
                } else if (to.isDefault(field)) {
                    res.getUpdateRow().putDefault(field);
                } else {
                    res.getUpdateRow().put(field, to.getForeignIDNumber(field));
                }
            }
        }
        // now private referents
        for (final SQLElementLink elemLink : this.getOwnedLinks().getByPath().values()) {
            if (elemLink.isJoin()) {
                if (elemLink.getLinkType() == LinkType.COMPOSITION) {
                    final Tuple2<List<SQLRowValues>, Map<Number, SQLRowValues>> fromPrivatesTuple = indexRows(from.followPath(elemLink.getPath(), CreateMode.CREATE_NONE, false));
                    // already checked at the start of the method
                    assert fromPrivatesTuple.get0().isEmpty() : "Existing rows without ID : " + fromPrivatesTuple.get0();
                    final Map<Number, SQLRowValues> fromPrivates = fromPrivatesTuple.get1();

                    final Tuple2<List<SQLRowValues>, Map<Number, SQLRowValues>> toPrivatesTuple = indexRows(to.followPath(elemLink.getPath(), CreateMode.CREATE_NONE, false));
                    final Map<Number, SQLRowValues> toPrivates = toPrivatesTuple.get1();

                    final List<Number> onlyInFrom = new ArrayList<Number>(fromPrivates.keySet());
                    onlyInFrom.removeAll(toPrivates.keySet());
                    final Set<Number> onlyInTo = new HashSet<Number>(toPrivates.keySet());
                    onlyInTo.removeAll(fromPrivates.keySet());
                    final Set<Number> inFromAndTo = new HashSet<Number>(toPrivates.keySet());
                    inFromAndTo.retainAll(fromPrivates.keySet());

                    if (!onlyInTo.isEmpty())
                        throw new IllegalStateException("Unknown IDs : " + onlyInTo + " for " + elemLink + " from IDs : " + fromPrivates);

                    // pair of rows (old row then new row) with the same ID or with the new row
                    // lacking and ID
                    final List<SQLRowValues> matchedPrivates = new ArrayList<SQLRowValues>();
                    for (final Number inBoth : inFromAndTo) {
                        matchedPrivates.add(fromPrivates.get(inBoth));
                        matchedPrivates.add(toPrivates.get(inBoth));
                    }

                    final SQLField toMainField = elemLink.getPath().getStep(0).getSingleField();
                    final SQLField toPrivateField = elemLink.getPath().getStep(-1).getSingleField();
                    for (final SQLRowValues privateSansID : toPrivatesTuple.get0()) {
                        if (!onlyInFrom.isEmpty()) {
                            matchedPrivates.add(fromPrivates.get(onlyInFrom.remove(0)));
                            matchedPrivates.add(privateSansID);
                        } else {
                            // insert new, always creating the join row
                            final SQLRowValues copy = privateSansID.deepCopy().removeReferents(toPrivateField);
                            res.getUpdateRow().put(elemLink.getPath(), true, copy);
                            res.mapRow(copy2originalRows.transformChecked(privateSansID), copy);
                        }
                    }

                    final SQLElement privateElem = elemLink.getOwned();
                    final Iterator<SQLRowValues> iter = matchedPrivates.iterator();
                    while (iter.hasNext()) {
                        final SQLRowValues fromPrivate = iter.next();
                        final SQLRowValues toPrivate = iter.next();

                        final SQLRowValues fromJoin = CollectionUtils.getSole(fromPrivate.getReferentRows(toPrivateField));
                        if (fromJoin == null)
                            throw new IllegalStateException("Shared private " + fromPrivate.printGraph());
                        final UpdateScript updateScript = privateElem.update(fromPrivate, toPrivate, allowedToChangeTo, copy2originalRows);

                        final SQLRowValues joinCopy = new SQLRowValues(fromJoin, ForeignCopyMode.NO_COPY);
                        assert joinCopy.getGraphSize() == 1;
                        joinCopy.put(toMainField.getName(), res.getUpdateRow());
                        joinCopy.put(toPrivateField.getName(), updateScript.getUpdateRow());
                        res.add(updateScript);
                    }

                    for (final Number id : onlyInFrom) {
                        // this will also cut the link from the main row
                        res.addToArchive(privateElem, fromPrivates.get(id));
                    }
                } else {
                    final Path pathToFK = elemLink.getPath().minusLast();
                    final Step fkStep = elemLink.getPath().getStep(-1);
                    final String fkField = fkStep.getSingleField().getName();
                    final ListMap<Number, SQLRowValues> fromFKs = indexByFK(from.followPath(pathToFK, CreateMode.CREATE_NONE, false), fkField);
                    final ListMap<Number, SQLRowValues> toFKs = indexByFK(to.followPath(pathToFK, CreateMode.CREATE_NONE, false), fkField);

                    // find foreignIDs that haven't changed
                    for (final Entry<Number, List<SQLRowValues>> e : toFKs.entrySet()) {
                        final Number foreignID = e.getKey();
                        final int count = e.getValue().size();
                        for (int i = 0; i < count; i++) {
                            if (fromFKs.containsKey(foreignID)) {
                                fromFKs.get(foreignID).remove(0);
                                fromFKs.removeIfEmpty(foreignID);
                                e.getValue().remove(0);
                            }
                        }
                    }
                    assert fromFKs.removeAllEmptyCollections().isEmpty() : "Should have been done along the way";
                    // do it after to avoid ConcurrentModificationException
                    toFKs.removeAllEmptyCollections();

                    // if there's more foreignIDs, re-use old join rows until we need to create new
                    // ones.
                    for (final Entry<Number, List<SQLRowValues>> e : toFKs.entrySet()) {
                        final Number foreignID = e.getKey();
                        final int count = e.getValue().size();
                        for (int i = 0; i < count; i++) {
                            assert !fromFKs.containsKey(foreignID) : "Should have been used above";
                            final SQLRowValues toUse;
                            if (fromFKs.isEmpty()) {
                                toUse = res.getUpdateRow().putRowValues(pathToFK, true);
                            } else {
                                // take first available join
                                final Entry<Number, List<SQLRowValues>> fromEntry = fromFKs.entrySet().iterator().next();
                                final SQLRowValues fromJoin = fromEntry.getValue().remove(0);
                                fromFKs.removeIfEmpty(fromEntry.getKey());
                                // copy existing join ID to avoid inserting a new join in the DB
                                toUse = new SQLRowValues(fromJoin.getTable()).setID(fromJoin.getIDNumber());
                                res.getUpdateRow().put(elemLink.getPath().getStep(0), toUse);
                            }
                            toUse.put(fkField, foreignID);
                        }
                    }

                    // lastly, delete remaining join rows (don't just archive otherwise if the main
                    // row is unarchived it will get back all links from every modification)
                    for (final SQLRowValues rowWithFK : fromFKs.allValues()) {
                        res.addToDelete(rowWithFK);
                    }
                }
            } // else foreign link already handled above
        }

        return res;
    }

    // first rows without IDs, then those with IDs
    static private Tuple2<List<SQLRowValues>, Map<Number, SQLRowValues>> indexRows(final Collection<SQLRowValues> rows) {
        final List<SQLRowValues> sansID = new ArrayList<SQLRowValues>();
        final Map<Number, SQLRowValues> map = new HashMap<Number, SQLRowValues>();
        for (final SQLRowValues r : rows) {
            if (r.hasID()) {
                final SQLRowValues previous = map.put(r.getIDNumber(), r);
                if (previous != null)
                    throw new IllegalStateException("Duplicate " + r.asRow());
            } else {
                sansID.add(r);
            }
        }
        return Tuple2.create(sansID, map);
    }

    static private ListMap<Number, SQLRowValues> indexByFK(final Collection<SQLRowValues> rows, final String fieldName) {
        final ListMap<Number, SQLRowValues> res = new ListMap<Number, SQLRowValues>();
        for (final SQLRowValues rowWithFK : rows) {
            if (rowWithFK.isForeignEmpty(fieldName))
                throw new IllegalArgumentException("Missing foreign key for " + fieldName);
            else
                res.add(rowWithFK.getForeignIDNumber(fieldName), rowWithFK);
        }
        return res;
    }

    public final void unarchiveNonRec(int id) throws SQLException {
        this.unarchive(this.getTable().getRow(id), false);
    }

    public final void unarchive(int id) throws SQLException {
        this.unarchive(this.getTable().getRow(id));
    }

    public final void unarchive(final SQLRow row) throws SQLException {
        this.unarchive(row, true);
    }

    public void unarchive(final SQLRow row, final boolean desc) throws SQLException {
        checkUndefined(row);
        // don't test row.isArchived() (it is done by getTree())
        // to allow an unarchived parent to unarchive all its descendants.

        // make sure that all fields are loaded
        final SQLRow upToDate = row.getTable().getRow(row.getID());
        // nos descendants
        final SQLRowValues descsAndMe = desc ? this.getTree(upToDate, true) : upToDate.asRowValues();
        final SQLRowValues connectedRows = new ArchivedGraph(this.getDirectory(), descsAndMe).expand();
        SQLUtils.executeAtomic(this.getTable().getBase().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                setArchive(Collections.singletonList(connectedRows.getGraph()), false);
                return null;
            }
        });
    }

    public final void archive(int id) throws SQLException {
        this.archiveIDs(Collections.singleton(id));
    }

    public final void archiveIDs(final Collection<? extends Number> ids) throws SQLException {
        this.archive(TreesOfSQLRows.createFromIDs(this, ids), true);
    }

    public final void archive(final Collection<? extends SQLRowAccessor> rows) throws SQLException {
        // rows checked by TreesOfSQLRows
        this.archive(new TreesOfSQLRows(this, rows), true);
    }

    public final void archive(SQLRow row) throws SQLException {
        this.archive(row, true);
    }

    /**
     * Archive la ligne demandée et tous ses descendants mais ne cherche pas à couper les références
     * pointant sur ceux-ci. ATTN peut donc laisser la base dans un état inconsistent, à n'utiliser
     * que si aucun lien ne pointe sur ceux ci. En revanche, accélère grandement (par exemple pour
     * OBSERVATION) car pas besoin de chercher toutes les références.
     * 
     * @param id la ligne voulue.
     * @throws SQLException if pb while archiving.
     */
    public final void archiveNoCut(int id) throws SQLException {
        this.archive(this.getTable().getRow(id), false);
    }

    protected void archive(final SQLRow row, final boolean cutLinks) throws SQLException {
        this.archive(new TreesOfSQLRows(this, row), cutLinks);
    }

    protected void archive(final TreesOfSQLRows trees, final boolean cutLinks) throws SQLException {
        if (trees.getElem() != this)
            throw new IllegalArgumentException(this + " != " + trees.getElem());
        if ((trees.isFetched() ? trees.getTrees().keySet() : trees.getRows()).isEmpty())
            return;
        for (final SQLRow row : trees.getRows())
            checkUndefined(row);

        SQLUtils.executeAtomic(this.getTable().getBase().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                if (!trees.isFetched())
                    trees.fetch(LockStrength.UPDATE);
                // reference
                // d'abord couper les liens qui pointent sur les futurs archivés
                if (cutLinks) {
                    // TODO prend bcp de temps
                    // FIXME update tableau pour chaque observation, ecrase les changements
                    // faire : 'La base à changée voulez vous recharger ou garder vos modifs ?'
                    final Map<SQLElementLink, ? extends Collection<SQLRowValues>> externReferences = trees.getExternReferences().getMap();
                    // avoid toString() which might make requests to display rows (eg archived)
                    if (Log.get().isLoggable(Level.FINEST))
                        Log.get().finest("will cut : " + externReferences);
                    for (final Entry<SQLElementLink, ? extends Collection<SQLRowValues>> e : externReferences.entrySet()) {
                        if (e.getKey().isJoin()) {
                            final Path joinPath = e.getKey().getPath();
                            final Path toJoinTable = joinPath.minusLast();
                            final SQLTable joinTable = toJoinTable.getLast();
                            assert getElement(joinTable) instanceof JoinSQLElement;
                            final Set<Number> ids = new HashSet<Number>();
                            for (final SQLRowValues joinRow : e.getValue()) {
                                assert joinRow.getTable() == joinTable;
                                ids.add(joinRow.getIDNumber());
                            }
                            // MAYBE instead of losing the information (as with simple foreign key),
                            // archive it
                            final String query = "DELETE FROM " + joinTable.getSQLName() + " WHERE " + new Where(joinTable.getKey(), ids);
                            getTable().getDBSystemRoot().getDataSource().execute(query);
                            for (final Number id : ids)
                                joinTable.fireRowDeleted(id.intValue());
                        } else {
                            final Link refKey = e.getKey().getSingleLink();
                            for (final SQLRowAccessor ref : e.getValue()) {
                                ref.createEmptyUpdateRow().putEmptyLink(refKey.getSingleField().getName()).update();
                            }
                        }
                    }
                    Log.get().finest("done cutting links");
                }

                // on archive tous nos descendants
                setArchive(trees.getClusters(), true);

                return null;
            }
        });
    }

    static private final SQLRowValues setArchive(SQLRowValues r, final boolean archive) throws SQLException {
        final SQLField archiveField = r.getTable().getArchiveField();
        final Object newVal;
        if (Boolean.class.equals(archiveField.getType().getJavaType()))
            newVal = archive;
        else
            newVal = archive ? 1 : 0;
        r.put(archiveField.getName(), newVal);
        return r;
    }

    // all rows will be either archived or unarchived (handling cycles)
    static private void setArchive(final Collection<SQLRowValuesCluster> clustersToArchive, final boolean archive) throws SQLException {
        final Set<SQLRowValues> toArchive = Collections.newSetFromMap(new IdentityHashMap<SQLRowValues, Boolean>());
        for (final SQLRowValuesCluster c : clustersToArchive)
            toArchive.addAll(c.getItems());

        final Map<SQLRow, SQLRowValues> linksCut = new HashMap<SQLRow, SQLRowValues>();
        while (!toArchive.isEmpty()) {
            // archive the maximum without referents
            // or unarchive the maximum without foreigns
            int archivedCount = -1;
            while (archivedCount != 0) {
                archivedCount = 0;
                final Iterator<SQLRowValues> iter = toArchive.iterator();
                while (iter.hasNext()) {
                    final SQLRowValues desc = iter.next();
                    final boolean correct;
                    if (desc.isArchived() == archive) {
                        // all already correct rows should be removed in the first loop, so they
                        // cannot be in linksCut
                        assert !linksCut.containsKey(desc.asRow());
                        correct = true;
                    } else if (archive && !desc.hasReferents() || !archive && !desc.hasForeigns()) {
                        SQLRowValues updateVals = linksCut.remove(desc.asRow());
                        if (updateVals == null)
                            updateVals = new SQLRowValues(desc.getTable());
                        // ne pas faire les fire après sinon qd on efface plusieurs éléments
                        // de la même table :
                        // on fire pour le 1er => updateSearchList => IListe.select(userID)
                        // hors si userID a aussi été archivé (mais il n'y a pas eu son fire
                        // correspondant), le component va lancer un RowNotFound
                        setArchive(updateVals, archive).setID(desc.getIDNumber());
                        // don't check validity since table events might have not already be
                        // fired
                        assert updateVals.getGraphSize() == 1 : "Archiving a graph : " + updateVals.printGraph();
                        updateVals.getGraph().store(StoreMode.COMMIT, false);
                        correct = true;
                    } else {
                        correct = false;
                    }
                    if (correct) {
                        // remove from graph
                        desc.clear();
                        desc.clearReferents();
                        assert desc.getGraphSize() == 1 : "Next loop won't progress : " + desc.printGraph();
                        archivedCount++;
                        iter.remove();
                    }
                }
            }

            // if not empty there's at least one cycle
            if (!toArchive.isEmpty()) {
                // Identify one cycle, ATTN first might not be itself part of the cycle, like the
                // BATIMENT and the LOCALs :
                /**
                 * <pre>
                 * BATIMENT
                 * |      \
                 * LOCAL1  LOCAL2
                 * |        \
                 * CPI ---> SOURCE
                 *     <--/
                 * </pre>
                 */
                final SQLRowValues first = toArchive.iterator().next();
                // Among the rows in the cycle, archive one by cutting links (choose
                // one with the least of them)
                final AtomicReference<SQLRowValues> cutLinksRef = new AtomicReference<SQLRowValues>(null);
                first.getGraph().walk(first, null, new ITransformer<State<Object>, Object>() {
                    @Override
                    public Object transformChecked(State<Object> input) {
                        final SQLRowValues last = input.getCurrent();
                        boolean cycleFound = false;
                        int minLinksCount = -1;
                        SQLRowValues leastLinks = null;
                        final Iterator<SQLRowValues> iter = input.getValsPath().iterator();
                        while (iter.hasNext()) {
                            final SQLRowValues v = iter.next();
                            if (!cycleFound) {
                                // start of cycle found
                                cycleFound = iter.hasNext() && v == last;
                            }
                            if (cycleFound) {
                                // don't use getReferentRows() as it's not the row count but
                                // the link count that's important
                                final int linksCount = archive ? v.getReferentsMap().allValues().size() : v.getForeigns().size();
                                // otherwise should have been removed above
                                assert linksCount > 0;
                                if (leastLinks == null || linksCount < minLinksCount) {
                                    leastLinks = v;
                                    minLinksCount = linksCount;
                                }
                            }
                        }
                        if (cycleFound) {
                            cutLinksRef.set(leastLinks);
                            throw new StopRecurseException();
                        }

                        return null;
                    }
                }, new WalkOptions(Direction.REFERENT).setRecursionType(RecursionType.BREADTH_FIRST).setStartIncluded(false).setCycleAllowed(true));
                final SQLRowValues cutLinks = cutLinksRef.get();

                // if there were no cycles rows would have been removed above
                assert cutLinks != null;

                // cut links, and store them to be restored
                if (archive) {
                    for (final Entry<SQLField, Set<SQLRowValues>> e : new SetMap<SQLField, SQLRowValues>(cutLinks.getReferentsMap()).entrySet()) {
                        final String fieldName = e.getKey().getName();
                        for (final SQLRowValues v : e.getValue()) {
                            // store before cutting
                            SQLRowValues cutVals = linksCut.get(v.asRow());
                            if (cutVals == null) {
                                cutVals = new SQLRowValues(v.getTable());
                                linksCut.put(v.asRow(), cutVals);
                            }
                            assert !cutVals.getFields().contains(fieldName) : fieldName + " already cut for " + v;
                            assert !v.isForeignEmpty(fieldName) : "Nothing to cut";
                            cutVals.put(fieldName, v.getForeignIDNumber(fieldName));
                            // cut graph
                            v.putEmptyLink(fieldName);
                            // cut DB
                            new SQLRowValues(v.getTable()).putEmptyLink(fieldName).update(v.getID());
                        }
                    }
                } else {
                    // store before cutting
                    final Set<String> foreigns = new HashSet<String>(cutLinks.getForeigns().keySet());
                    final SQLRowValues oldVal = linksCut.put(cutLinks.asRow(), new SQLRowValues(cutLinks, ForeignCopyMode.COPY_ID_OR_RM));
                    // can't pass twice, as the first time we clear all foreigns, so the next loop
                    // must unarchive it.
                    assert oldVal == null : "Already cut";
                    // cut graph
                    cutLinks.removeAll(foreigns);
                    // cut DB
                    final SQLRowValues updateVals = new SQLRowValues(cutLinks.getTable());
                    for (final String fieldName : foreigns) {
                        updateVals.putEmptyLink(fieldName);
                    }
                    updateVals.update(cutLinks.getID());
                }
                // ready to begin another loop
                assert archive && !cutLinks.hasReferents() || !archive && !cutLinks.hasForeigns();
            }
        }
        // for unarchive we need to update again the already treated (unarchived) row
        assert !archive || linksCut.isEmpty() : "Some links weren't restored : " + linksCut;
        if (!archive) {
            for (final Entry<SQLRow, SQLRowValues> e : linksCut.entrySet()) {
                e.getValue().update(e.getKey().getID());
            }
        }
    }

    public void delete(SQLRowAccessor r) throws SQLException {
        this.check(r);
        if (true)
            throw new UnsupportedOperationException("not yet implemented.");
    }

    public final SQLTable getTable() {
        return this.primaryTable;
    }

    /**
     * A code identifying a specific meaning for the table and fields. I.e. it is used by
     * {@link #getName() names} and {@link SQLFieldTranslator item metadata}. E.g. if two
     * applications use the same table for different purposes (at different times, of course), their
     * elements should not share a code. On the contrary, if one application merely adds a field to
     * an existing table, the new element should keep the same code so that existing name and
     * documentation remain.
     * 
     * @return a code for the table and its meaning.
     */
    public synchronized final String getCode() {
        if (this.code == DEFERRED_CODE) {
            final String createCode = this.createCode();
            if (createCode == DEFERRED_CODE)
                throw new IllegalStateException("createCode() returned DEFERRED_CODE");
            this.code = createCode;
        }
        return this.code;
    }

    /**
     * Is the rows of this element shared, ie rows are unique and must not be copied.
     * 
     * @return <code>true</code> if this element is shared.
     */
    public boolean isShared() {
        return false;
    }

    /**
     * Must the rows of this element be copied when traversing a hierarchy.
     * 
     * @return <code>true</code> if the element must not be copied.
     */
    public boolean dontDeepCopy() {
        return false;
    }

    // *** rf

    public final synchronized SQLElementLinks getLinksOwnedByOthers() {
        this.initRF();
        return this.otherLinks;
    }

    private final Set<SQLField> getReferentFields(final LinkType type) {
        return getSingleFields(this.getLinksOwnedByOthers(), type);
    }

    // not deprecated since joins to parents are unsupported (and unecessary since an SQLElement can
    // only have one parent)
    public final Set<SQLField> getChildrenReferentFields() {
        return this.getReferentFields(LinkType.PARENT);
    }

    // *** ff

    public synchronized final SQLElementLinks getOwnedLinks() {
        this.initFF();
        return this.ownedLinks;
    }

    public final SQLElementLink getOwnedLink(final String fieldName) {
        return this.getOwnedLink(fieldName, null);
    }

    /**
     * Return the {@link #getOwnedLinks() owned link} that crosses the passed field.
     * 
     * @param fieldName any field of {@link #getTable()}.
     * @param type the type of the wanted link, <code>null</code> meaning any type.
     * @return the link matching the parameter.
     */
    public final SQLElementLink getOwnedLink(final String fieldName, final LinkType type) {
        final Link foreignLink = this.getTable().getDBSystemRoot().getGraph().getForeignLink(this.getTable().getField(fieldName));
        if (foreignLink == null)
            return null;
        return this.getOwnedLinks().getByPath(new PathBuilder(getTable()).add(foreignLink, Direction.FOREIGN).build(), type);
    }

    public final boolean hasOwnedLinks(final LinkType type) {
        return !this.getOwnedLinks().getByType(type).isEmpty();
    }

    public final SQLField getParentForeignField() {
        return getOptionalField(this.getParentForeignFieldName());
    }

    public final synchronized String getParentForeignFieldName() {
        this.initFF();
        return this.parentFF;
    }

    public final SQLElementLink getParentLink() {
        return CollectionUtils.getSole(this.getOwnedLinks().getByType(LinkType.PARENT));
    }

    public final Set<SQLElementLink> getChildrenLinks() {
        return this.getLinksOwnedByOthers().getByType(LinkType.PARENT);
    }

    public final SQLElement getChildElement(final String tableName) {
        final Set<SQLElementLink> links = new HashSet<SQLElementLink>();
        for (final SQLElementLink childLink : this.getChildrenLinks()) {
            if (childLink.getOwner().getTable().getName().equals(tableName))
                links.add(childLink);
        }
        if (links.size() != 1)
            throw new IllegalStateException("no exactly one child table named " + tableName + " : " + links);
        else
            return links.iterator().next().getOwner();
    }

    // optional but if specified it must exist
    private final SQLField getOptionalField(final String name) {
        return name == null ? null : this.getTable().getField(name);
    }

    // Previously there was another method which listed children but this method is preferred since
    // it avoids writing IFs to account for customer differences and there's no ambiguity (you
    // return a field of this table instead of a table name that must be searched in roots and then
    // a foreign key must be found).
    /**
     * Should be overloaded to specify our parent.
     * 
     * @return <code>null</code> for this implementation.
     */
    protected String getParentFFName() {
        return null;
    }

    public final SQLElement getParentElement() {
        if (this.getParentForeignFieldName() == null)
            return null;
        else
            return this.getForeignElement(this.getParentForeignFieldName());
    }

    public final SQLElement getPrivateElement(String foreignField) {
        final SQLElementLink privateLink = this.getOwnedLink(foreignField, LinkType.COMPOSITION);
        return privateLink == null ? null : privateLink.getOwned();
    }

    /**
     * The graph of this table and its privates.
     * 
     * @return an SQLRowValues of this element's table filled with
     *         {@link SQLRowValues#setAllToNull() <code>null</code>s} except for private foreign
     *         fields containing SQLRowValues.
     */
    public final SQLRowValues getPrivateGraph() {
        return this.getPrivateGraph(null);
    }

    /**
     * The graph of this table and its privates.
     * 
     * @param fields which fields should be included in the graph, <code>null</code> meaning all.
     * @return an SQLRowValues of this element's table filled with <code>null</code>s according to
     *         the <code>fields</code> parameter except for private foreign fields containing
     *         SQLRowValues.
     */
    public final SQLRowValues getPrivateGraph(final VirtualFields fields) {
        return this.getPrivateGraph(fields, false, true);
    }

    public final SQLRowValues getPrivateGraph(final VirtualFields fields, final boolean ignoreNotDeepCopied, final boolean includeJoins) {
        final SQLRowValues res = includeJoins ? this.getOwnedJoinsGraph(fields) : new SQLRowValues(this.getTable());
        if (fields == null) {
            res.setAllToNull();
        } else {
            res.putNulls(this.getTable().getFieldsNames(fields));
        }
        for (final SQLElementLink link : this.getOwnedLinks().getByType(LinkType.COMPOSITION)) {
            final SQLElement owned = link.getOwned();
            if (ignoreNotDeepCopied && owned.dontDeepCopy()) {
                res.remove(link.getPath().getStep(0));
            } else {
                res.put(link.getPath(), false, owned.getPrivateGraph(fields, ignoreNotDeepCopied, includeJoins));
            }
        }
        return res;
    }

    public final SQLRowValues getOwnedJoinsGraph(final VirtualFields fields) {
        final SQLRowValues res = new SQLRowValues(this.getTable());
        for (final SQLElementLink link : this.getOwnedLinks().getByPath().values()) {
            if (link.isJoin()) {
                final SQLRowValues joinVals = res.putRowValues(link.getPath().getStep(0));
                joinVals.fill(joinVals.getTable().getFieldsNames(fields == null ? VirtualFields.ALL : fields), null, false, true);
            }
        }
        return res;
    }

    /**
     * Renvoie les champs qui sont 'privé' càd que les ligne pointées par ce champ ne sont
     * référencées que par une et une seule ligne de cette table. Cette implementation renvoie une
     * liste vide. This method is intented for subclasses, call {@link #getPrivateForeignFields()}
     * which does some checks.
     * 
     * @return la List des noms des champs privés, eg ["ID_OBSERVATION_2"].
     * @deprecated use {@link #setupLinks(SQLElementLinksSetup)}
     */
    protected List<String> getPrivateFields() {
        return Collections.emptyList();
    }

    public final void clearPrivateFields(SQLRowValues rowVals) {
        for (SQLElementLink l : this.getOwnedLinks().getByType(LinkType.COMPOSITION)) {
            rowVals.remove(l.getPath().getStep(0));
        }
    }

    /**
     * Specify an action for a normal foreign field.
     * 
     * @param ff the foreign field name.
     * @param action what to do if a referenced row must be archived.
     * @throws IllegalArgumentException if <code>ff</code> is not a normal foreign field.
     */
    public final void setAction(final String ff, ReferenceAction action) throws IllegalArgumentException {
        final Path p = new PathBuilder(getTable()).addForeignField(ff).build();
        this.getOwnedLinks().getByPath(p).setAction(action);
    }

    // *** rf and ff

    /**
     * The links towards the parents (either {@link LinkType#PARENT} or {@link LinkType#COMPOSITION}
     * ) of this element.
     * 
     * @return the links towards the parents of this element.
     */
    public final SQLElementLinks getContainerLinks() {
        return getContainerLinks(true, true);
    }

    public final SQLElementLinks getContainerLinks(final boolean privateParent, final boolean parent) {
        final SetMapItf<LinkType, SQLElementLink> byType = new SetMap<LinkType, SQLElementLink>();
        if (parent)
            byType.addAll(LinkType.PARENT, this.getOwnedLinks().getByType(LinkType.PARENT));
        if (privateParent)
            byType.addAll(LinkType.COMPOSITION, this.getLinksOwnedByOthers().getByType(LinkType.COMPOSITION));
        final SQLElementLinks res = new SQLElementLinks(byType);
        assert res.getByType().size() <= 1 : "Child and private at the same time";
        return res;
    }

    // *** request

    public final ComboSQLRequest getComboRequest() {
        return getComboRequest(false);
    }

    /**
     * Return a combo request for this element.
     * 
     * @param create <code>true</code> if a new instance should be returned, <code>false</code> to
     *        return a shared instance.
     * @return a combo request for this.
     */
    public final ComboSQLRequest getComboRequest(final boolean create) {
        if (!create) {
            if (this.combo == null) {
                this.combo = this.createComboRequest();
            }
            return this.combo;
        } else {
            return this.createComboRequest();
        }
    }

    public final ComboSQLRequest createComboRequest() {
        return this.createComboRequest(null, null);
    }

    public final ComboSQLRequest createComboRequest(final List<String> fields, final Where w) {
        final ComboSQLRequest res = new ComboSQLRequest(this.getTable(), fields == null ? this.getComboFields() : fields, w, this.getDirectory());
        this._initComboRequest(res);
        return res;
    }

    protected void _initComboRequest(final ComboSQLRequest req) {
    }

    // not all elements need to be displayed in combos so don't make this method abstract
    protected List<String> getComboFields() {
        return this.getListFields();
    }

    public final synchronized ListSQLRequest getListRequest() {
        if (this.list == null) {
            this.list = createListRequest();
        }
        return this.list;
    }

    /**
     * Return the field expander to pass to {@link ListSQLRequest}.
     * 
     * @return the {@link FieldExpander} to pass to {@link ListSQLRequest}.
     * @see #createListRequest(List, Where, FieldExpander)
     */
    protected FieldExpander getListExpander() {
        return getDirectory().getShowAs();
    }

    public final ListSQLRequest createListRequest() {
        return this.createListRequest(null);
    }

    public final ListSQLRequest createListRequest(final List<String> fields) {
        return this.createListRequest(fields, null, null);
    }

    /**
     * Create and initialise a new list request with the passed arguments. Pass <code>null</code>
     * for default arguments.
     * 
     * @param fields the list fields, <code>null</code> meaning {@link #getListFields()}.
     * @param w the where, can be <code>null</code>.
     * @param expander the field expander, <code>null</code> meaning {@link #getListExpander()}.
     * @return a new ready-to-use list request.
     */
    public final ListSQLRequest createListRequest(final List<String> fields, final Where w, final FieldExpander expander) {
        final ListSQLRequest res = instantiateListRequest(fields == null ? this.getListFields() : fields, w, expander == null ? this.getListExpander() : expander);
        this._initListRequest(res);
        return res;
    }

    /**
     * Must just create a new instance without altering parameters. The parameters are passed by
     * {@link #createListRequest(List, Where, FieldExpander)}, if you need to change default values
     * overload the needed method. This method should only be used if one needs a subclass of
     * {@link ListSQLRequest}.
     * 
     * @param fields the list fields.
     * @param w the where.
     * @param expander the field expander.
     * @return a new uninitialised list request.
     */
    protected ListSQLRequest instantiateListRequest(final List<String> fields, final Where w, final FieldExpander expander) {
        return new ListSQLRequest(this.getTable(), fields, w, expander);
    }

    /**
     * Initialise a new instance. E.g. one can {@link ListSQLRequest#addToGraphToFetch(String...)
     * add fields} to the fetcher.
     * 
     * @param req the instance to initialise.
     */
    protected void _initListRequest(final ListSQLRequest req) {
    }

    public final SQLTableModelSourceOnline getTableSource() {
        return this.getTableSource(!cacheTableSource());
    }

    /**
     * Return a table source for this element.
     * 
     * @param create <code>true</code> if a new instance should be returned, <code>false</code> to
     *        return a shared instance.
     * @return a table source for this.
     */
    public final synchronized SQLTableModelSourceOnline getTableSource(final boolean create) {
        if (!create) {
            if (this.tableSrc == null) {
                this.tableSrc = createTableSource();
            }
            return this.tableSrc;
        } else
            return this.createTableSource();
    }

    public final SQLTableModelSourceOnline createTableSource() {
        return createTableSource((Where) null);
    }

    public final SQLTableModelSourceOnline createTableSource(final List<String> fields) {
        return createTableSourceOnline(createListRequest(fields));
    }

    public final SQLTableModelSourceOnline createTableSource(final Where w) {
        return createTableSourceOnline(createListRequest(null, w, null));
    }

    public final SQLTableModelSourceOnline createTableSourceOnline(final ListSQLRequest req) {
        return initTableSource(instantiateTableSourceOnline(req));
    }

    protected SQLTableModelSourceOnline instantiateTableSourceOnline(final ListSQLRequest req) {
        return new SQLTableModelSourceOnline(req, this);
    }

    protected synchronized void _initTableSource(final SQLTableModelSource res) {
        if (!this.additionalListCols.isEmpty())
            res.getColumns().addAll(this.additionalListCols);
    }

    public final <S extends SQLTableModelSource> S initTableSource(final S res) {
        return this.initTableSource(res, false);
    }

    public final synchronized <S extends SQLTableModelSource> S initTableSource(final S res, final boolean minimal) {
        // do init first since it can modify the columns
        if (!minimal)
            this._initTableSource(res);
        // setEditable(false) on read only fields
        // MAYBE setReadOnlyFields() on SQLTableModelSource, so that SQLTableModelLinesSource can
        // check in commit()
        final Set<String> dontModif = CollectionUtils.union(this.getReadOnlyFields(), this.getInsertOnlyFields());
        for (final String f : dontModif)
            for (final SQLTableModelColumn col : res.getColumns(getTable().getField(f)))
                if (col instanceof SQLTableModelColumnPath)
                    ((SQLTableModelColumnPath) col).setEditable(false);
        return res;
    }

    public final SQLTableModelSourceOffline createTableSourceOffline() {
        return createTableSourceOfflineWithWhere(null);
    }

    public final SQLTableModelSourceOffline createTableSourceOfflineWithWhere(final Where w) {
        return createTableSourceOffline(createListRequest(null, w, null));
    }

    public final SQLTableModelSourceOffline createTableSourceOffline(final ListSQLRequest req) {
        return initTableSource(instantiateTableSourceOffline(req));
    }

    protected SQLTableModelSourceOffline instantiateTableSourceOffline(final ListSQLRequest req) {
        return new SQLTableModelSourceOffline(req, this);
    }

    /**
     * Whether to cache our tableSource.
     * 
     * @return <code>true</code> to call {@link #createTableSource()} only once, or
     *         <code>false</code> to call it each time {@link #getTableSource()} is.
     */
    protected boolean cacheTableSource() {
        return true;
    }

    abstract protected List<String> getListFields();

    public final void addListFields(final List<String> fields) {
        for (final String f : fields)
            this.addListColumn(new SQLTableModelColumnPath(getTable().getField(f)));
    }

    public final void addListColumn(SQLTableModelColumn col) {
        this.additionalListCols.add(col);
    }

    public final Collection<IListeAction> getRowActions() {
        return this.rowActions;
    }

    public final void addRowActionsListener(final IClosure<ListChangeIndex<IListeAction>> listener) {
        this.rowActions.getRecipe().addListener(listener);
    }

    public final void removeRowActionsListener(final IClosure<ListChangeIndex<IListeAction>> listener) {
        this.rowActions.getRecipe().rmListener(listener);
    }

    public String getDescription(SQLRow fromRow) {
        return fromRow.toString();
    }

    // *** iterators

    static interface ChildProcessor<R extends SQLRowAccessor> {
        public void process(R parent, SQLField joint, R child) throws SQLException;
    }

    /**
     * Execute <code>c</code> for each children of <code>row</code>. NOTE: <code>c</code> will be
     * called with <code>row</code> as its first parameter, and with its child of the same type
     * (SQLRow or SQLRowValues) for the third parameter.
     * 
     * @param <R> type of SQLRowAccessor to use.
     * @param row the parent row.
     * @param c what to do for each children.
     * @param deep <code>true</code> to ignore {@link #dontDeepCopy()}.
     * @param archived <code>true</code> to iterate over archived children.
     * @throws SQLException if <code>c</code> raises an exn.
     */
    private <R extends SQLRowAccessor> void forChildrenDo(R row, ChildProcessor<? super R> c, boolean deep, boolean archived) throws SQLException {
        for (final SQLElementLink childLink : this.getChildrenLinks()) {
            if (deep || !childLink.getChild().dontDeepCopy()) {
                final SQLField childField = childLink.getSingleField();
                final List<SQLRow> children = row.asRow().getReferentRows(childField, archived ? SQLSelect.ARCHIVED : SQLSelect.UNARCHIVED);
                // eg BATIMENT[516]
                for (final SQLRow child : children) {
                    c.process(row, childField, convert(child, row));
                }
            }
        }
    }

    // convert toConv to same type as row
    @SuppressWarnings("unchecked")
    private <R extends SQLRowAccessor> R convert(final SQLRow toConv, R row) {
        final R ch;
        if (row instanceof SQLRow)
            ch = (R) toConv;
        else if (row instanceof SQLRowValues)
            ch = (R) toConv.createUpdateRow();
        else
            throw new IllegalStateException("SQLRowAccessor is neither SQLRow nor SQLRowValues: " + toConv);
        return ch;
    }

    // first the leaves
    private void forDescendantsDo(final SQLRow row, final ChildProcessor<SQLRow> c, final boolean deep) throws SQLException {
        this.forDescendantsDo(row, c, deep, true, false);
    }

    <R extends SQLRowAccessor> void forDescendantsDo(final R row, final ChildProcessor<R> c, final boolean deep, final boolean leavesFirst, final boolean archived) throws SQLException {
        this.check(row);
        this.forChildrenDo(row, new ChildProcessor<R>() {
            public void process(R parent, SQLField joint, R child) throws SQLException {
                if (!leavesFirst)
                    c.process(parent, joint, child);
                getElement(child.getTable()).forDescendantsDo(child, c, deep, leavesFirst, archived);
                if (leavesFirst)
                    c.process(parent, joint, child);
            }
        }, deep, archived);
    }

    void check(SQLRowAccessor row) {
        if (!row.getTable().equals(this.getTable()))
            throw new IllegalArgumentException("row must of table " + this.getTable() + " : " + row);
    }

    private void checkUndefined(SQLRow row) {
        this.check(row);
        if (row.isUndefined())
            throw new IllegalArgumentException("row is undefined: " + row);
    }

    // *** copy

    public final SQLRow copyRecursive(int id) throws SQLException {
        return this.copyRecursive(this.getTable().getRow(id));
    }

    public final SQLRow copyRecursive(SQLRow row) throws SQLException {
        return this.copyRecursive(row, null);
    }

    public SQLRow copyRecursive(final SQLRow row, final SQLRow parent) throws SQLException {
        return this.copyRecursive(row, parent, null);
    }

    /**
     * Copy <code>row</code> and its children into <code>parent</code>.
     * 
     * @param row which row to clone.
     * @param parent which parent the clone will have, <code>null</code> meaning the same than
     *        <code>row</code>.
     * @param c allow one to modify the copied rows before they are inserted, can be
     *        <code>null</code>.
     * @return the new copy.
     * @throws SQLException if an error occurs.
     */
    public SQLRow copyRecursive(final SQLRow row, final SQLRow parent, final IClosure<SQLRowValues> c) throws SQLException {
        return copyRecursive(row, false, parent, c);
    }

    /**
     * Copy <code>row</code> and its children into <code>parent</code>.
     * 
     * @param row which row to clone.
     * @param full <code>true</code> if {@link #dontDeepCopy()} should be ignored, i.e. an exact
     *        copy will be made.
     * @param parent which parent the clone will have, <code>null</code> meaning the same than
     *        <code>row</code>.
     * @param c allow one to modify the copied rows before they are inserted, can be
     *        <code>null</code>.
     * @return the new copy.
     * @throws SQLException if an error occurs.
     */
    public SQLRow copyRecursive(final SQLRow row, final boolean full, final SQLRow parent, final IClosure<SQLRowValues> c) throws SQLException {
        check(row);
        if (row.isUndefined())
            return row;

        // current => new copy
        // contains private and join rows otherwise we can't fix ASSOCIATION
        final Map<SQLRow, SQLRowValues> copies = new HashMap<SQLRow, SQLRowValues>();

        return SQLUtils.executeAtomic(this.getTable().getBase().getDataSource(), new SQLFactory<SQLRow>() {
            @Override
            public SQLRow create() throws SQLException {

                // eg SITE[128]
                final SQLRowValues copy = createTransformedCopy(row, full, parent, copies, c);

                forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                    public void process(SQLRow parent, SQLField joint, SQLRow desc) throws SQLException {
                        final SQLRowValues parentCopy = copies.get(parent);
                        if (parentCopy == null)
                            throw new IllegalStateException("null copy of " + parent);
                        final SQLRowValues descCopy = createTransformedCopy(desc, full, null, copies, c);
                        descCopy.put(joint.getName(), parentCopy);
                    }
                }, full, false, false);
                // ne pas descendre en deep

                // private and parent relationships are already handled, now fix ASSOCIATION : the
                // associations in the source hierarchy either point outside or inside the
                // hierarchy, for the former the copy is correct. But for the latter, the copy still
                // point to the source hierarchy when it should point to copy hierarchy.
                forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                    public void process(SQLRow parent, SQLField joint, SQLRow desc) throws SQLException {
                        for (final SQLElementLink link : getElement(desc.getTable()).getOwnedLinks().getByType(LinkType.ASSOCIATION)) {
                            final Path toRowWFK = link.getPath().minusLast();
                            final Step lastStep = link.getPath().getStep(-1);
                            for (final SQLRow rowWithFK : desc.getDistantRows(toRowWFK)) {
                                final SQLRow ref = rowWithFK.getForeignRow(lastStep.getSingleLink(), SQLRowMode.NO_CHECK);
                                // eg copy of SOURCE[12] is SOURCE[354]
                                final SQLRowValues refCopy = copies.get(ref);
                                if (refCopy != null) {
                                    // CPI[1203]
                                    final SQLRowValues rowWithFKCopy = copies.get(rowWithFK);
                                    rowWithFKCopy.put(lastStep, refCopy);
                                }
                            }
                        }
                    }
                }, full);

                // we used to remove foreign links pointing outside the copy, but this was almost
                // never right, e.g. : copy a batiment, its locals loose ID_FAMILLE ; copy a local,
                // if a source in it points to an item in another local, its copy won't.

                return copy.insert();
            }
        });
    }

    private final SQLRowValues createTransformedCopy(SQLRow desc, final boolean full, SQLRow parent, final Map<SQLRow, SQLRowValues> map, final IClosure<SQLRowValues> c) throws SQLException {
        final SQLRowValues copiedVals = getElement(desc.getTable()).createCopy(desc, full, parent, null, map);
        assert copiedVals != null : "failed to copy " + desc;
        if (c != null)
            c.executeChecked(copiedVals);
        return copiedVals;
    }

    public final SQLRow copy(int id) throws SQLException {
        return this.copy(this.getTable().getRow(id));
    }

    public final SQLRow copy(SQLRow row) throws SQLException {
        return this.copy(row, null);
    }

    public final SQLRow copy(SQLRow row, SQLRow parent) throws SQLException {
        final SQLRowValues copy = this.createCopy(row, parent);
        return copy == null ? row : copy.insert();
    }

    public final SQLRowValues createCopy(int id) {
        final SQLRow row = this.getTable().getRow(id);
        return this.createCopy(row, null);
    }

    /**
     * Copies the passed row into an SQLRowValues. NOTE: this method will only access the DB if
     * necessary : when <code>row</code> is not an {@link SQLRowValues} and this element has
     * {@link LinkType#COMPOSITION privates} or {@link SQLElementLink#isJoin() joins}. Otherwise the
     * copy won't be a copy of the current values in DB, but of the current values of the passed
     * instance.
     * 
     * @param row the row to copy, can be <code>null</code>.
     * @param parent the parent the copy will be in, <code>null</code> meaning the same as
     *        <code>row</code>. If it's an {@link SQLRowValues} it will be used directly, otherwise
     *        {@link SQLRowAccessor#getIDNumber()} will be used (i.e. if the copy isn't to be linked
     *        to its parent, pass a {@link SQLRowAccessor#asRow() row}).
     * @return a copy ready to be inserted, or <code>null</code> if <code>row</code> cannot be
     *         copied.
     */
    public SQLRowValues createCopy(SQLRowAccessor row, SQLRowAccessor parent) {
        return createCopy(row, false, parent);
    }

    public SQLRowValues createCopy(SQLRowAccessor row, final boolean full, SQLRowAccessor parent) {
        return this.createCopy(row, full, parent, null, null);
    }

    public SQLRowValues createCopy(SQLRowAccessor row, final boolean full, SQLRowAccessor parent, final IdentityHashMap<SQLRowValues, SQLRowValues> valsMap, final Map<SQLRow, SQLRowValues> rowMap) {
        // do NOT copy the undefined
        if (row == null || row.isUndefined())
            return null;
        this.check(row);

        final Set<SQLElementLink> privates = this.getOwnedLinks().getByType(LinkType.COMPOSITION);
        final SQLRowValues privateGraph = this.getPrivateGraph(VirtualFields.ALL, !full, true);
        // Don't make one request per private, just fetch the whole graph at once
        // further with joined privates an SQLRow cannot contain privates nor carry the lack of them
        // (without joins a row lacking privates was passed with just an SQLRow with undefined
        // foreign keys).
        final SQLRowValues rowVals;
        if (row instanceof SQLRowValues) {
            rowVals = (SQLRowValues) row;
        } else if (privateGraph.getGraphSize() == 1) {
            rowVals = null;
        } else {
            final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(privateGraph);
            fetcher.setSelID(row.getIDNumber());
            rowVals = CollectionUtils.getSole(fetcher.fetch());
            if (rowVals == null)
                throw new IllegalStateException("Not exactly one row for " + row);
        }
        // Use just fetched values so that data is coherent.
        final SQLRowAccessor upToDateRow = rowVals != null ? rowVals : row;

        final SQLRowValues copy = new SQLRowValues(this.getTable());
        this.loadAllSafe(copy, upToDateRow);
        if (valsMap != null) {
            if (rowVals == null)
                throw new IllegalArgumentException("Cannot fill map since no SQLRowValues were provided");
            valsMap.put(rowVals, copy);
        }
        if (rowMap != null) {
            if (!upToDateRow.hasID())
                throw new IllegalArgumentException("Cannot fill map since no SQLRow were provided");
            rowMap.put(upToDateRow.asRow(), copy);
        }

        for (final SQLElementLink privateLink : privates) {
            final SQLElement privateElement = privateLink.getOwned();
            final boolean deepCopy = full || !privateElement.dontDeepCopy();
            if (!privateLink.isJoin()) {
                final String privateName = privateLink.getSingleField().getName();
                if (deepCopy && !rowVals.isForeignEmpty(privateName)) {
                    final SQLRowValues foreign = checkPrivateLoaded(privateLink, rowVals.getForeign(privateName));
                    final SQLRowValues child = privateElement.createCopy(foreign, full, null, valsMap, rowMap);
                    copy.put(privateName, child);
                    // use upToDateRow instead of rowVals since the latter might be null if
                    // !full
                } else if (upToDateRow.getFields().contains(privateName)) {
                    copy.putEmptyLink(privateName);
                }
            } else {
                // join
                assert privateLink.getPath().getStep(0).getDirection() == Direction.REFERENT;
                if (deepCopy) {
                    copyJoin(rowVals, full, valsMap, rowMap, copy, privateLink);
                } // else nothing to do since there's no fields in copy
            }
        }

        for (final SQLElementLink association : this.getOwnedLinks().getByType(LinkType.ASSOCIATION)) {
            if (association.isJoin()) {
                copyJoin(rowVals, full, valsMap, rowMap, copy, association);
            } // else fields already in copy
        }

        // si on a spécifié un parent, eg BATIMENT[23]
        if (parent != null) {
            final SQLTable foreignTable = this.getParentForeignField().getForeignTable();
            if (!parent.getTable().equals(foreignTable))
                throw new IllegalArgumentException(parent + " is not a parent of " + row);
            copy.put(this.getParentForeignFieldName(), parent instanceof SQLRowValues ? parent : parent.getIDNumber());
        }

        return copy;
    }

    private SQLRowValues checkPrivateLoaded(final SQLElementLink privateLink, final SQLRowAccessor foreign) {
        assert privateLink.getLinkType() == LinkType.COMPOSITION && privateLink.getOwned().getTable() == foreign.getTable();
        // otherwise the recursive call will fetch the missing data, which could be
        // incoherent with rowVals
        if (!(foreign instanceof SQLRowValues))
            throw new IllegalStateException("Graph missing non-empty private for " + privateLink);
        return (SQLRowValues) foreign;
    }

    private final void copyJoin(final SQLRowValues rowVals, final boolean full, final IdentityHashMap<SQLRowValues, SQLRowValues> valsMap, final Map<SQLRow, SQLRowValues> rowMap,
            final SQLRowValues copy, final SQLElementLink link) {
        assert link.isJoin();
        final Step firstStep = link.getPath().getStep(0);
        final SQLElement joinElem = getElement(firstStep.getTo());
        final Step lastStep = link.getPath().getStep(-1);
        for (final SQLRowValues joinToCopy : rowVals.followPath(link.getPath().minusLast(), CreateMode.CREATE_NONE, false)) {
            final SQLRowValues joinCopy = new SQLRowValues(joinElem.getTable());
            joinElem.loadAllSafe(joinCopy, joinToCopy, link.getLinkType() == LinkType.COMPOSITION);
            copy.put(firstStep, joinCopy);
            if (valsMap != null)
                valsMap.put(joinToCopy, joinCopy);
            if (rowMap != null)
                rowMap.put(joinToCopy.asRow(), joinCopy);
            // copy private
            if (link.getLinkType() == LinkType.COMPOSITION) {
                final SQLElement privateElement = link.getOwned();
                final SQLRowAccessor privateRow = joinToCopy.getForeign(lastStep.getSingleLink());
                if (privateRow.isUndefined())
                    throw new IllegalStateException("Joined to undefined " + link);
                checkPrivateLoaded(link, privateRow);
                final SQLRowValues privateCopy = privateElement.createCopy(privateRow, full, null, valsMap, rowMap);
                joinCopy.put(lastStep, privateCopy);
            }
            assert !joinCopy.hasID() && joinCopy.getFields().containsAll(lastStep.getSingleLink().getCols());
        }
    }

    static private final VirtualFields JOIN_SAFE_FIELDS = VirtualFields.ALL.difference(VirtualFields.PRIMARY_KEY, VirtualFields.ORDER);
    static private final VirtualFields SAFE_FIELDS = JOIN_SAFE_FIELDS.difference(VirtualFields.FOREIGN_KEYS);

    /**
     * Load all values that can be safely copied (shared by multiple rows). This means all values
     * except private, primary, and order.
     * 
     * @param vals the row to modify.
     * @param row the row to be loaded.
     */
    public final void loadAllSafe(final SQLRowValues vals, final SQLRowAccessor row) {
        this.loadAllSafe(vals, row, null);
    }

    private final void loadAllSafe(final SQLRowValues vals, final SQLRowAccessor row, final Boolean isPrivateJoinElement) {
        check(vals);
        check(row);
        // JoinSQLElement has no links but we still want to copy metadata
        if (this instanceof JoinSQLElement) {
            if (isPrivateJoinElement == null)
                throw new IllegalStateException("joins are not public");
            assert this.getOwnedLinks().getByPath().size() == 0;
            vals.setAll(row.getValues(JOIN_SAFE_FIELDS));
            // remove links to owned if private join
            final Path pathFromOwner = ((JoinSQLElement) this).getPathFromOwner();
            assert pathFromOwner.length() == 2;
            if (isPrivateJoinElement)
                vals.remove(pathFromOwner.getStep(1));
        } else {
            if (isPrivateJoinElement != null)
                throw new IllegalStateException("should a join : " + this);
            // Don't copy foreign keys then remove privates (i.e. JOIN_SAFE_FIELDS), as this will
            // copy ignored paths (see SQLElementLinkSetup.ignore()) and they might be privates
            vals.setAll(row.getValues(SAFE_FIELDS));
            for (final SQLElementLink l : this.getOwnedLinks().getByPath().values()) {
                if (l.getLinkType() != LinkType.COMPOSITION && !l.isJoin()) {
                    vals.putAll(row.getValues(l.getSingleLink().getCols()));
                }
            }
        }
    }

    // *** getRows

    /**
     * Returns the descendant rows : the children of this element, recursively. ATTN does not carry
     * the hierarchy.
     * 
     * @param row a SQLRow.
     * @return the descendant rows by SQLTable.
     */
    public final ListMap<SQLTable, SQLRow> getDescendants(SQLRow row) {
        check(row);
        final ListMap<SQLTable, SQLRow> mm = new ListMap<SQLTable, SQLRow>();
        try {
            this.forDescendantsDo(row, new ChildProcessor<SQLRow>() {
                public void process(SQLRow parent, SQLField joint, SQLRow child) throws SQLException {
                    mm.add(joint.getTable(), child);
                }
            }, true);
        } catch (SQLException e) {
            // never happen
            e.printStackTrace();
        }
        return mm;
    }

    /**
     * Returns the tree beneath the passed row.
     * 
     * @param row the root of the desired tree.
     * @param archived <code>true</code> if the returned rows should be archived.
     * @return the asked tree.
     */
    private SQLRowValues getTree(SQLRow row, boolean archived) {
        check(row);
        final SQLRowValues res = row.asRowValues();
        try {
            this.forDescendantsDo(res, new ChildProcessor<SQLRowValues>() {
                public void process(SQLRowValues parent, SQLField joint, SQLRowValues desc) throws SQLException {
                    desc.put(joint.getName(), parent);
                }
            }, true, false, archived);
        } catch (SQLException e) {
            // never happen cause process don't throw it
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Returns the children of the passed row.
     * 
     * @param row a SQLRow.
     * @return the children rows by SQLTable.
     */
    public ListMap<SQLTable, SQLRow> getChildrenRows(SQLRow row) {
        check(row);
        // List to retain order
        final ListMap<SQLTable, SQLRow> mm = new ListMap<SQLTable, SQLRow>();
        try {
            this.forChildrenDo(row, new ChildProcessor<SQLRow>() {
                public void process(SQLRow parent, SQLField joint, SQLRow child) throws SQLException {
                    mm.add(child.getTable(), child);
                }
            }, true, false);
        } catch (SQLException e) {
            // never happen
            e.printStackTrace();
        }
        // TODO return Map of SQLElement instead of SQLTable (this avoids the caller a call to
        // getDirectory())
        return mm;
    }

    public SQLRowValues getContainer(final SQLRowValues row) {
        return this.getContainer(row, true, true);
    }

    public final SQLRowValues getContainer(final SQLRowValues row, final boolean privateParent, final boolean parent) {
        check(row);
        if (row.isUndefined() || !privateParent && !parent)
            return null;

        final List<SQLRowValues> parents = new ArrayList<SQLRowValues>();
        for (final SQLElementLink l : this.getContainerLinks(privateParent, parent).getByPath().values()) {
            parents.addAll(row.followPath(l.getPathToParent(), CreateMode.CREATE_NONE, true));
        }
        if (parents.size() > 1)
            throw new IllegalStateException("More than one parent for " + row + " : " + parents);
        return parents.size() == 0 ? null : parents.get(0);
    }

    @Deprecated
    public SQLRow getForeignParent(SQLRow row) {
        return this.getForeignParent(row, SQLRowMode.VALID);
    }

    // ATTN cannot replace with getParent(SQLRowAccessor) since some callers assume the result to be
    // a foreign row (which isn't the case for private)
    @Deprecated
    private SQLRow getForeignParent(SQLRow row, final SQLRowMode mode) {
        check(row);
        return this.getParentForeignFieldName() == null ? null : row.getForeignRow(this.getParentForeignFieldName(), mode);
    }

    public final SQLRowValues fetchPrivateParent(final SQLRowAccessor row, final boolean modifyParameter) {
        return this.fetchPrivateParent(row, modifyParameter, ArchiveMode.UNARCHIVED);
    }

    /**
     * Return the parent if any of the passed row. This method will access the DB.
     * 
     * @param row the row.
     * @param modifyParameter <code>true</code> if <code>row</code> can be linked to the result,
     *        <code>false</code> to link a new {@link SQLRowValues}.
     * @param archiveMode the parent must match this mode.
     * @return the matching parent linked to its child, <code>null</code> if <code>row</code>
     *         {@link SQLRowAccessor#isUndefined()}, if this isn't a private or if no parent exist.
     * @throws IllegalStateException if <code>row</code> has more than one parent matching.
     */
    public final SQLRowValues fetchPrivateParent(final SQLRowAccessor row, final boolean modifyParameter, final ArchiveMode archiveMode) {
        return this.fetchContainer(row, modifyParameter, archiveMode, true, false);
    }

    public final SQLRowValues fetchContainer(final SQLRowAccessor row) {
        return fetchContainer(row, ArchiveMode.UNARCHIVED);
    }

    public final SQLRowValues fetchContainer(final SQLRowAccessor row, final ArchiveMode archiveMode) {
        return this.fetchContainer(row, false, archiveMode, true, true);
    }

    static private SQLField getToID(final Step s) {
        return s.isForeign() ? s.getSingleField() : s.getTo().getKey();
    }

    public final SQLRowValues fetchContainer(final SQLRowAccessor row, final boolean modifyParameter, final ArchiveMode archiveMode, final boolean privateParent, final boolean parent) {
        check(row);
        if (row.isUndefined() || !privateParent && !parent)
            return null;
        final SQLSyntax syntax = SQLSyntax.get(getTable());
        final List<SQLElementLink> parentLinks = new ArrayList<SQLElementLink>(this.getContainerLinks(privateParent, parent).getByPath().values());
        if (parentLinks.isEmpty())
            return null;
        final ListIterator<SQLElementLink> listIter = parentLinks.listIterator();
        final List<String> selects = new ArrayList<String>(parentLinks.size());
        while (listIter.hasNext()) {
            final SQLElementLink parentLink = listIter.next();

            final SQLSelect sel = new SQLSelect(true);
            sel.addSelect(getToID(parentLink.getStepToParent()), null, "parentID");
            final SQLField joinPK = parentLink.getPath().getTable(1).getKey();
            if (parentLink.isJoin()) {
                sel.addSelect(joinPK, null, "joinID");
            } else {
                sel.addRawSelect(syntax.cast("NULL", joinPK.getType()), "joinID");
            }
            sel.addRawSelect(String.valueOf(listIter.previousIndex()), "fieldIndex");
            sel.setArchivedPolicy(archiveMode);
            sel.setWhere(new Where(getToID(parentLink.getStepToChild()), "=", row.getIDNumber()));

            assert sel.getTableRefs().size() == 1 : "Non optimal query";
            selects.add(sel.asString());
        }
        final List<?> parentIDs = getTable().getDBSystemRoot().getDataSource().executeA(CollectionUtils.join(selects, "\nUNION ALL "));
        if (parentIDs.size() > 1)
            throw new IllegalStateException("More than one parent for " + row + " : " + parentIDs);
        else if (parentIDs.size() == 0)
            // e.g. no UNARCHIVED parent of an ARCHIVED private
            return null;

        final Object[] idAndIndex = (Object[]) parentIDs.get(0);
        final Number mainID = (Number) idAndIndex[0];
        final Number joinID = (Number) idAndIndex[1];
        final SQLElementLink parentLink = parentLinks.get(((Number) idAndIndex[2]).intValue());
        final Path toChildPath = parentLink.getPathToChild();
        final SQLRowValues res = new SQLRowValues(toChildPath.getTable(0)).setID(mainID);
        final SQLRowValues rowWithFK;
        if (parentLink.isJoin()) {
            if (joinID == null)
                throw new IllegalStateException("Missing join ID for " + parentLink);
            final Step parentToJoin = toChildPath.getStep(0);
            rowWithFK = res.putRowValues(parentToJoin).setID(joinID);
        } else {
            rowWithFK = res;
        }
        assert rowWithFK.hasID();
        // first convert to SQLRow to avoid modifying the (graph of our) method parameter
        rowWithFK.put(toChildPath.getStep(-1), (modifyParameter ? row : row.asRow()).asRowValues());
        return res;
    }

    /**
     * Return the main row if any of the passed row. This method will access the DB.
     * 
     * @param row the row, if it's a {@link SQLRowValues} it will be linked to the result.
     * @param archiveMode the parent must match this mode.
     * @return the matching parent linked to its child, <code>null</code> if <code>row</code>
     *         {@link SQLRowAccessor#isUndefined()}, if this isn't a private or if no parent exist.
     * @see #fetchPrivateParent(SQLRowAccessor, boolean, ArchiveMode)
     */
    public final SQLRowValues fetchPrivateRoot(SQLRowAccessor row, final ArchiveMode archiveMode) {
        SQLRowValues prev = null;
        SQLRowValues res = fetchPrivateParent(row, true, archiveMode);
        while (res != null) {
            prev = res;
            res = getElement(res.getTable()).fetchPrivateParent(res, true, archiveMode);
        }
        return prev;
    }

    Map<SQLField, List<SQLRow>> getNonChildrenReferents(SQLRow row) {
        check(row);
        final Map<SQLField, List<SQLRow>> mm = new HashMap<SQLField, List<SQLRow>>();
        final Set<SQLField> nonChildren = new HashSet<SQLField>(row.getTable().getDBSystemRoot().getGraph().getReferentKeys(row.getTable()));
        nonChildren.removeAll(this.getChildrenReferentFields());
        for (final SQLField refField : nonChildren) {
            // eg CONTACT.ID_SITE => [CONTACT[12], CONTACT[13]]
            mm.put(refField, row.getReferentRows(refField));
        }
        return mm;
    }

    /**
     * Returns a java object modeling the passed row.
     * 
     * @param row the row to model.
     * @return an instance modeling the passed row or <code>null</code> if there's no class to model
     *         this table.
     * @see SQLRowAccessor#getModelObject()
     */
    public Object getModelObject(SQLRowAccessor row) {
        check(row);
        if (this.getModelClass() == null)
            return null;

        final Object res;
        // seuls les SQLRow peuvent être cachées
        if (row instanceof SQLRow) {
            // MAYBE make the modelObject change
            final CacheResult<Object> cached = this.getModelCache().check(row, Collections.singleton(row));
            if (cached.getState() == CacheResult.State.INTERRUPTED)
                throw new RTInterruptedException("interrupted while waiting for the cache");
            else if (cached.getState() == CacheResult.State.VALID)
                return cached.getRes();

            try {
                res = this.createModelObject(row);
                this.getModelCache().put(cached, res);
            } catch (RuntimeException exn) {
                this.getModelCache().removeRunning(cached);
                throw exn;
            }
        } else
            res = this.createModelObject(row);

        return res;
    }

    private final Object createModelObject(SQLRowAccessor row) {
        if (!RowBacked.class.isAssignableFrom(this.getModelClass()))
            throw new IllegalStateException("modelClass must inherit from RowBacked: " + this.getModelClass());
        final Constructor<? extends RowBacked> ctor;
        try {
            ctor = this.getModelClass().getConstructor(new Class[] { SQLRowAccessor.class });
        } catch (Exception e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "no SQLRowAccessor constructor", e);
        }
        try {
            return ctor.newInstance(new Object[] { row });
        } catch (Exception e) {
            throw ExceptionUtils.createExn(RuntimeException.class, "pb creating instance", e);
        }
    }

    protected Class<? extends RowBacked> getModelClass() {
        return null;
    }

    // *** equals

    public static final class EqualOptionBuilder {

        private boolean ignoreNotDeepCopied, testNonShared, testParent, testMetadata;

        public EqualOptionBuilder() {
            this.ignoreNotDeepCopied = false;
            this.testNonShared = false;
            this.testParent = false;
            this.testMetadata = false;
        }

        public boolean isIgnoreNotDeepCopied() {
            return this.ignoreNotDeepCopied;
        }

        public EqualOptionBuilder setIgnoreNotDeepCopied(boolean ignoreNotDeepCopied) {
            this.ignoreNotDeepCopied = ignoreNotDeepCopied;
            return this;
        }

        public boolean isNonSharedTested() {
            return this.testNonShared;
        }

        public EqualOptionBuilder setNonSharedTested(boolean testNonShared) {
            this.testNonShared = testNonShared;
            return this;
        }

        public boolean isParentTested() {
            return this.testParent;
        }

        public EqualOptionBuilder setParentTested(boolean testParent) {
            this.testParent = testParent;
            return this;
        }

        public boolean isMetadataTested() {
            return this.testMetadata;
        }

        public EqualOptionBuilder setMetadataTested(boolean testMetadata) {
            this.testMetadata = testMetadata;
            return this;
        }

        public EqualOption build() {
            return new EqualOption(this.ignoreNotDeepCopied, this.testNonShared, this.testParent, this.testMetadata);
        }
    }

    @Immutable
    public static final class EqualOption {

        static private final VirtualFields EQUALS_FIELDS = VirtualFields.CONTENT.union(VirtualFields.ARCHIVE);
        static private final VirtualFields EQUALS_WITH_MD_FIELDS = EQUALS_FIELDS.union(VirtualFields.METADATA);

        public static final EqualOption ALL = new EqualOption(false, true, true, true);
        public static final EqualOption ALL_BUT_IGNORE_NOT_DEEP_COPIED = ALL.createBuilder().setIgnoreNotDeepCopied(true).build();

        public static final EqualOption IGNORE_NOT_DEEP_COPIED = new EqualOptionBuilder().setIgnoreNotDeepCopied(true).build();
        public static final EqualOption TEST_NOT_DEEP_COPIED = new EqualOptionBuilder().setIgnoreNotDeepCopied(false).build();

        static final EqualOption fromIgnoreNotDeepCopied(final boolean ignoreNotDeepCopied) {
            return ignoreNotDeepCopied ? IGNORE_NOT_DEEP_COPIED : TEST_NOT_DEEP_COPIED;
        }

        private final boolean ignoreNotDeepCopied, testNonShared, testParent;
        private final VirtualFields fields;

        protected EqualOption(final boolean ignoreNotDeepCopied, final boolean testNonShared, final boolean testParent, final boolean testMetadata) {
            this.ignoreNotDeepCopied = ignoreNotDeepCopied;
            this.testNonShared = testNonShared;
            this.testParent = testParent;
            this.fields = testMetadata ? EQUALS_WITH_MD_FIELDS : EQUALS_FIELDS;
        }

        public boolean isIgnoreNotDeepCopied() {
            return this.ignoreNotDeepCopied;
        }

        public boolean isNonSharedTested() {
            return this.testNonShared;
        }

        public boolean isParentTested() {
            return this.testParent;
        }

        public EqualOptionBuilder createBuilder() {
            return new EqualOptionBuilder().setIgnoreNotDeepCopied(isIgnoreNotDeepCopied()).setNonSharedTested(isNonSharedTested()).setParentTested(this.isParentTested())
                    .setMetadataTested(this.fields == EQUALS_WITH_MD_FIELDS);
        }
    }

    public boolean equals(SQLRow row, SQLRow row2) {
        return this.equals(row, row2, false);
    }

    /**
     * Compare local values (excluding order and obviously primary key). This method doesn't cross
     * links except for privates but it does compare the value of shared normal links. This method
     * always uses the DB.
     * 
     * @param row the first row.
     * @param row2 the second row.
     * @param ignoreNotDeepCopied if <code>true</code> ignores the rows that are
     *        {@link #dontDeepCopy() not to be copied}. See also the <code>full</code> parameter of
     *        {@link #createCopy(SQLRowAccessor, boolean, SQLRowAccessor)}.
     * @return <code>true</code> if the two rows are equal.
     * @see #equals(SQLRowValues, SQLRowValues, boolean)
     */
    public boolean equals(SQLRow row, SQLRow row2, boolean ignoreNotDeepCopied) {
        return this.equals(row, row2, EqualOption.fromIgnoreNotDeepCopied(ignoreNotDeepCopied));
    }

    public boolean equals(SQLRow row, SQLRow row2, final EqualOption option) {
        return this.diff(row, row2, option).get0();
    }

    private static final Tuple2<Boolean, DiffResult> TRUE_NULL = new Tuple2<Boolean, DiffResult>(true, null);
    private static final Tuple2<Boolean, DiffResult> FALSE_NULL = new Tuple2<Boolean, DiffResult>(false, null);

    // Boolean is never null, DiffResult is null if difference is trivial
    Tuple2<Boolean, DiffResult> diff(SQLRow row, SQLRow row2, final EqualOption option) {
        check(row);
        if (!row2.getTable().equals(this.getTable()))
            return FALSE_NULL;
        if (row.equals(row2))
            return TRUE_NULL;
        // the same table but not the same id

        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(getPrivateGraphForEquals(option));
        final List<SQLRowValues> fetched = fetcher.fetch(new Where(this.getTable().getKey(), Arrays.asList(row.getIDNumber(), row2.getIDNumber())));
        if (fetched.size() > 2)
            throw new IllegalStateException("More than 2 rows for " + row + " and " + row2);
        else if (fetched.size() < 2)
            // at least one is inexistent or archived
            return FALSE_NULL;

        final DiffResult res = equalsPruned(fetched.get(0), fetched.get(1));
        return Tuple2.create(res.isEqual(), res);
    }

    /**
     * Compare local values (excluding order and obviously primary key). This method doesn't cross
     * links except for privates but it does compare the value of shared normal links. This method
     * never uses the DB but does {@link SQLRowValuesCluster#prune(SQLRowValues, SQLRowValues)
     * prune} the parameters before comparing them.
     * 
     * @param row the first row.
     * @param row2 the second row.
     * @param ignoreNotDeepCopied if <code>true</code> ignores the rows that are
     *        {@link #dontDeepCopy() not to be copied}. See also the <code>full</code> parameter of
     *        {@link #createCopy(SQLRowAccessor, boolean, SQLRowAccessor)}.
     * @return <code>true</code> if the two rows are equal.
     * @see #equals(SQLRow, SQLRow, boolean)
     */
    public boolean equals(SQLRowValues row, SQLRowValues row2, boolean ignoreNotDeepCopied) {
        return this.equals(row, row2, EqualOption.fromIgnoreNotDeepCopied(ignoreNotDeepCopied));
    }

    public boolean equals(SQLRowValues row, SQLRowValues row2, final EqualOption option) {
        check(row);
        if (row == row2)
            return true;
        if (!row2.getTable().equals(this.getTable()))
            return false;

        final SQLRowValues privateGraphForEquals = getPrivateGraphForEquals(option);
        return equalsPruned(row.prune(privateGraphForEquals), row2.prune(privateGraphForEquals)).isEqual();
    }

    private final SQLRowValues getPrivateGraphForEquals(final EqualOption option) {
        // don't include joins as we only add those required by "option"
        final SQLRowValues res = this.getPrivateGraph(option.fields, option.isIgnoreNotDeepCopied(), false);
        for (final SQLRowValues item : new HashSet<SQLRowValues>(res.getGraph().getItems())) {
            final SQLElement elem = getElement(item.getTable());
            // remove parent
            final SQLElementLink parentLink = elem.getParentLink();
            setLink(item, parentLink, option.isParentTested());
            // remove non shared normal links
            // add shared normal links (if join)
            for (final SQLElementLink normalLink : elem.getOwnedLinks().getByType(LinkType.ASSOCIATION)) {
                setLink(item, normalLink, option.isNonSharedTested() || normalLink.getOwned().isShared());
            }
        }
        return res;
    }

    private final void setLink(final SQLRowValues item, final SQLElementLink link, final boolean shouldBeTested) {
        if (link == null)
            return;
        if (shouldBeTested) {
            if (link.isJoin()) {
                assert link.getPath().getStep(0).getDirection() == Direction.REFERENT;
                item.assurePath(link.getPath().minusLast()).fillWith(null, false);
            }
        } else {
            if (!link.isJoin()) {
                item.removeForeignKey(link.getSingleLink());
            }
        }
    }

    static private DiffResult equalsPruned(SQLRowValues row, SQLRowValues row2) {
        // neither use order nor PK (don't just remove PK since we need them for
        // DiffResult.fillRowMap())
        return row.getGraph().getFirstDifference(row, row2, false, false, false);
    }

    public boolean equalsRecursive(SQLRow row, SQLRow row2) throws SQLException {
        return this.equalsRecursive(row, row2, EqualOption.ALL);
    }

    /**
     * Test those rows and all their descendants.
     * 
     * @param row first row.
     * @param row2 second row.
     * @param option how to compare each descendant, note that #{@link EqualOption#isParentTested()}
     *        is only meaningful for the passed (root) rows, since descendants are found through
     *        their parents (i.e. they always have equal parents).
     * @return true if both trees are equal according to <code>option</code>.
     * @throws SQLException if an error occurs.
     */
    public boolean equalsRecursive(SQLRow row, SQLRow row2, EqualOption option) throws SQLException {
        // if (!equals(row, row2))
        // return false;
        return new SQLElementRowR(this, row).equals(new SQLElementRowR(this, row2), option);
    }

    // no need for equals()/hashCode() since there's only one SQLElement per table and directory

    @Override
    public String toString() {
        return this.getClass().getName() + " " + this.getTable().getSQLName();
    }

    // *** gui

    public final void addComponentFactory(final String id, final ITransformer<Tuple2<SQLElement, String>, SQLComponent> t) {
        if (t == null)
            throw new NullPointerException();
        this.components.add(id, t);
    }

    public final void removeComponentFactory(final String id, final ITransformer<Tuple2<SQLElement, String>, SQLComponent> t) {
        if (t == null)
            throw new NullPointerException();
        this.components.remove(id, t);
    }

    private final SQLComponent createComponentFromFactory(final String id, final boolean defaultItem) {
        final String actualID = defaultItem ? DEFAULT_COMP_ID : id;
        final Tuple2<SQLElement, String> t = Tuple2.create(this, id);
        // start from the most recently added factory
        final Iterator<ITransformer<Tuple2<SQLElement, String>, SQLComponent>> iter = this.components.getNonNull(actualID).descendingIterator();
        while (iter.hasNext()) {
            final SQLComponent res = iter.next().transformChecked(t);
            if (res != null)
                return res;
        }
        return null;
    }

    public final SQLComponent createDefaultComponent() {
        return this.createComponent(DEFAULT_COMP_ID);
    }

    /**
     * Create the component for the passed ID. First factories for the passed ID are executed, after
     * that if ID is the {@link #DEFAULT_COMP_ID default} then {@link #createComponent()} is called
     * else factories for {@link #DEFAULT_COMP_ID} are executed.
     * 
     * @param id the requested ID.
     * @return the component, never <code>null</code>.
     * @throws IllegalStateException if no component is found.
     */
    public final SQLComponent createComponent(final String id) throws IllegalStateException {
        return this.createComponent(id, true);
    }

    /**
     * Create the component for the passed ID. First factories for the passed ID are executed, after
     * that if ID is the {@link #DEFAULT_COMP_ID default} then {@link #createComponent()} is called
     * else factories for {@link #DEFAULT_COMP_ID} are executed.
     * 
     * @param id the requested ID.
     * @param required <code>true</code> if the result cannot be <code>null</code>.
     * @return the component or <code>null</code> if all factories return <code>null</code> and
     *         <code>required</code> is <code>false</code>.
     * @throws IllegalStateException if <code>required</code> and no component is found.
     */
    public final SQLComponent createComponent(final String id, final boolean required) throws IllegalStateException {
        SQLComponent res = this.createComponentFromFactory(id, false);
        if (res == null) {
            if (CompareUtils.equals(id, DEFAULT_COMP_ID)) {
                // since we don't pass id to this method, only call it for DEFAULT_ID
                res = this.createComponent();
            } else {
                res = this.createComponentFromFactory(id, true);
            }
        }
        if (res != null)
            res.setCode(id);
        else if (required)
            throw new IllegalStateException("No component for " + id);
        return res;
    }

    /**
     * Retourne l'interface graphique de saisie.
     * 
     * @return l'interface graphique de saisie.
     */
    protected abstract SQLComponent createComponent();

    public final void addToMDPath(final String mdVariant) {
        if (mdVariant == null)
            throw new NullPointerException();
        synchronized (this) {
            final LinkedList<String> newL = new LinkedList<String>(this.mdPath);
            newL.addFirst(mdVariant);
            this.mdPath = Collections.unmodifiableList(newL);
        }
    }

    public synchronized final void removeFromMDPath(final String mdVariant) {
        final LinkedList<String> newL = new LinkedList<String>(this.mdPath);
        if (newL.remove(mdVariant))
            this.mdPath = Collections.unmodifiableList(newL);
    }

    /**
     * The variants searched to find item metadata by
     * {@link SQLFieldTranslator#getDescFor(SQLTable, String, String)}. This allow to configure this
     * element to choose between the simultaneously loaded metadata.
     * 
     * @return the variants path.
     */
    public synchronized final List<String> getMDPath() {
        return this.mdPath;
    }

    /**
     * Allows a module to add a view for a field to this element.
     * 
     * @param field the field of the component.
     * @return <code>true</code> if no view existed.
     */
    public final boolean putAdditionalField(final String field) {
        return this.putAdditionalField(field, (JComponent) null);
    }

    public final boolean putAdditionalField(final String field, final JTextComponent comp) {
        return this.putAdditionalField(field, (JComponent) comp);
    }

    public final boolean putAdditionalField(final String field, final SQLTextCombo comp) {
        return this.putAdditionalField(field, (JComponent) comp);
    }

    // private as only a few JComponent are OK
    private final boolean putAdditionalField(final String field, final JComponent comp) {
        if (this.additionalFields.containsKey(field)) {
            return false;
        } else {
            this.additionalFields.put(field, comp);
            return true;
        }
    }

    public final Map<String, JComponent> getAdditionalFields() {
        return Collections.unmodifiableMap(this.additionalFields);
    }

    public final void removeAdditionalField(final String field) {
        this.additionalFields.remove(field);
    }

    public final boolean askArchive(final Component comp, final Number ids) {
        return Value.hasValue(this.askArchive(comp, Collections.singleton(ids)));
    }

    /**
     * Ask to the user before archiving.
     * 
     * @param comp the parent component.
     * @param ids which rows to archive.
     * @return <code>null</code> if there was an error (already presented to the user),
     *         {@link Value#hasValue() a value} if the user agreed, none if the user refused.
     * @deprecated this methods mixes DB and UI access.
     */
    public final Value<TreesOfSQLRows> askArchive(final Component comp, final Collection<? extends Number> ids) {
        final TreesOfSQLRows trees = TreesOfSQLRows.createFromIDs(this, ids);
        try {
            trees.fetch(LockStrength.NONE);
            final Boolean agreed = this.ask(comp, trees);
            if (agreed == null) {
                return null;
            } else if (agreed) {
                this.archive(trees, true);
                return Value.getSome(trees);
            } else {
                return Value.getNone();
            }
        } catch (SQLException e) {
            ExceptionHandler.handle(comp, TM.tr("sqlElement.archiveError", this, ids), e);
            return null;
        }
    }

    /**
     * Ask the user about rows to archive.
     * 
     * @param comp the parent component.
     * @param trees which rows to archive.
     * @return <code>null</code> if there was an error (already presented to the user),
     *         <code>true</code> if the user agreed, <code>false</code> if the user refused.
     */
    public Boolean ask(final Component comp, final TreesOfSQLRows trees) {
        boolean shouldArchive = false;
        if (!trees.isFetched())
            throw new IllegalStateException("Trees not yet fetched");
        try {
            final int rowCount = trees.getTrees().size();
            if (rowCount == 0)
                return true;
            // only check rights if there's actually some rows to delete
            if (!UserRightsManager.getCurrentUserRights().canDelete(getTable()))
                throw new SQLException("forbidden");
            // only display main rows since the user might not be aware of the private ones (the UI
            // might hide the fact that one panel is in fact multiple rows)
            final Map<SQLTable, List<SQLRowAccessor>> descs = trees.getDescendantsByTable();
            final SortedMap<LinkToCut, Integer> externRefs = trees.getExternReferences().countByLink();
            final String confirmDelete = getTM().trA("sqlElement.confirmDelete");
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put("rowCount", rowCount);
            final int descsSize = descs.size();
            final int externsSize = externRefs.size();
            if (descsSize + externsSize > 0) {
                final String descsS = descsSize > 0 ? toString(descs) : null;
                final String externsS = externsSize > 0 ? toStringExtern(externRefs) : null;
                map.put("descsSize", descsSize);
                map.put("descs", descsS);
                map.put("externsSize", externsSize);
                map.put("externs", externsS);
                map.put("times", "once");
                int i = askSerious(comp, getTM().trM("sqlElement.deleteRef.details", map) + getTM().trM("sqlElement.deleteRef", map), confirmDelete);
                if (i == JOptionPane.YES_OPTION) {
                    map.put("times", "twice");
                    final String msg = externsSize > 0 ? getTM().trM("sqlElement.deleteRef.details2", map) : "";
                    i = askSerious(comp, msg + getTM().trM("sqlElement.deleteRef", map), confirmDelete);
                    if (i == JOptionPane.YES_OPTION) {
                        shouldArchive = true;
                    } else {
                        JOptionPane.showMessageDialog(comp, getTM().trA("sqlElement.noLinesDeleted"), getTM().trA("sqlElement.noLinesDeletedTitle"), JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } else {
                int i = askSerious(comp, getTM().trM("sqlElement.deleteNoRef", map), confirmDelete);
                if (i == JOptionPane.YES_OPTION) {
                    shouldArchive = true;
                }
            }
            return shouldArchive;
        } catch (Exception e) {
            ExceptionHandler.handle(comp, TM.tr("sqlElement.rowsToArchiveError", this), e);
            return null;
        }
    }

    private final String toString(Map<SQLTable, List<SQLRowAccessor>> descs) {
        final List<String> l = new ArrayList<String>(descs.size());
        for (final Entry<SQLTable, List<SQLRowAccessor>> e : descs.entrySet()) {
            final SQLTable t = e.getKey();
            final SQLElement elem = getElement(t);
            l.add(elemToString(e.getValue().size(), elem));
        }
        return CollectionUtils.join(l, "\n");
    }

    private static final String elemToString(int count, SQLElement elem) {
        return "- " + elem.getName().getNumeralVariant(count, Grammar.INDEFINITE_NUMERAL);
    }

    // traduire TRANSFO.ID_ELEMENT_TABLEAU_PRI -> {TRANSFO[5], TRANSFO[12]}
    // en 2 transformateurs vont perdre leurs champs 'Circuit primaire'
    private final String toStringExtern(SortedMap<LinkToCut, Integer> externRefs) {
        final List<String> l = new ArrayList<String>();
        final Map<String, Object> map = new HashMap<String, Object>(4);
        for (final Entry<LinkToCut, Integer> entry : externRefs.entrySet()) {
            final LinkToCut foreignKey = entry.getKey();
            final int count = entry.getValue();
            final String label = foreignKey.getLabel();
            final SQLElement elem = getElement(foreignKey.getTable());
            map.put("elementName", elem.getName());
            map.put("count", count);
            map.put("linkName", label);
            l.add(getTM().trM("sqlElement.linksWillBeCut", map));
        }
        return CollectionUtils.join(l, "\n");
    }

    private final int askSerious(Component comp, String msg, String title) {
        return JOptionPane.showConfirmDialog(comp, msg, title + " (" + this.getPluralName() + ")", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    }

}
