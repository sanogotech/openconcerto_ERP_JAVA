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
 
 package org.openconcerto.sql.utils;

import static org.openconcerto.utils.FileUtils.addSuffix;
import static java.util.Collections.singleton;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.ChangeTable.NameTransformer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.LogUtils;
import org.openconcerto.utils.PropertiesUtils;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2.List2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

/**
 * To dump or restore a database. For each root there's a folder with its name, and inside a CSV
 * file for each table and SQL files for each system.
 * 
 * <pre>
 * dumpDir/dump.properties
 *         rootName/
 *                  t1.txt
 *                  t2.txt
 *                  psql/
 *                  h2/     pre.sql
 *                          post.sql (optional)
 *                          tables-create/t1.sql
 *                                        t2.sql
 *                          tables-alter/t1.sql
 *                                       t2.sql
 * 
 * </pre>
 * 
 * @author Sylvain
 */
public class Copy {

    // TODO dry run
    // TODO lock : see ClientConf.acquireClientLock() and FileUtils.doWithLock()
    // MAYBE add storeLog.csv (dumped from in-memory H2 DB) with one row for each update to the dump
    // DATE,SYSTEM NAME/IP,WHO,FULL PATH of DIR,SYSROOT,noStruct, noData, roots, tables

    private static final String STRUCT_EXT = ".sql";
    // no names => same string for any Locale, no invalid FS characters
    private static final DateFormat DATE_FMT = new SimpleDateFormat("yyyy MM dd HH.mm.ss");

    private synchronized static final String format(final Date d) {
        return DATE_FMT.format(d);
    }

    static private boolean isEmpty(final String name) {
        return StringUtils.isEmpty(name, true);
    }

    private static <T extends Enum<T>> T getEnum(final String propName, final Class<T> enumType, final T defaultVal) {
        assert enumType == defaultVal.getClass();
        final String propValue = System.getProperty(propName);
        if (isEmpty(propValue))
            return defaultVal;
        return Enum.valueOf(enumType, propValue.toUpperCase());
    }

    static public enum FileExistsMode {
        /**
         * Fail if the dump directory already exists.
         */
        FAIL,
        /**
         * Rename the existing dump directory.
         */
        RENAME,
        /**
         * Remove the existing dump directory.
         */
        DELETE,
        /**
         * Just the new write files, overwriting if necessary. Useful for dumping specific tables in
         * specific roots in multiple times.
         */
        OVERWRITE;
    }

    static public enum CreationMode {
        /**
         * The item must not already exist.
         */
        REQUIRED,
        /**
         * The item may already exist.
         */
        ALLOWED,
        /**
         * The item must already exist.
         */
        FORBIDDEN
    }

    static private Set<String> getFiles(final File dir, final String ext) {
        final File[] files = dir.listFiles(FileUtils.createEndFileFilter(ext));
        if (files == null)
            return null;
        final Set<String> res = new HashSet<String>();
        for (final File f : files)
            res.add(f.getName().substring(0, f.getName().length() - ext.length()));
        return res;
    }

    public static final String ROOTS_TO_MAP = "rootsToMap";
    private static final String NO_STRUCT = "noStruct";
    private static final String NO_DATA = "noData";
    public static final String DELETE_TABLE = "deleteTable";
    public static final String NAME_TO_STORE = "nameToStore";
    private static final String ROOT_CREATION_MODE = "rootCreationMode";
    private static final String FILE_MODE = "fileMode";

    private static final int CURRENT_VERSION = 2;

    private static final String PROPS_NAME = "dump.properties";
    private static final String VERSION_KEY = "version";
    private static final String CREATED_KEY = "created";
    private static final String MODIFIED_KEY = "modified";

    private static void usage() {
        System.out.println("Usage: " + Copy.class.getName() + " [ -store | -load ] directory url [root,... [table,...]]");
        System.out.println("Dump or restore roots or tables from/to url to/from files.");
        System.out.println("The roots and tables are specified as comma separated lists." + "If there's only one it can be specified in the URL."
                + " If no roots are specified then use all available ones, i.e. store every visible (limited by " + ROOTS_TO_MAP + ") root in the database and load every root directory of the dump"
                + " If no tables are specified then use all available ones, i.e. store every existing tables of each dumped root and load every table file in each root directory of the dump");
        System.out.println("System properties:");
        System.out.println("\t" + ROOTS_TO_MAP + " = comma separated list of roots to map in addition to the ones dumped/restored."
                + " Only needed when dumping/restoring roots that reference roots not being dumped/restored.");
        System.out.println("\t" + NO_STRUCT + " = true to avoid dumping/restoring the structure");
        System.out.println("\t" + NO_DATA + " = true to avoid dumping/restoring the data");
        System.out.println("\t" + DELETE_TABLE + " = (only for loading) true to empty tables before loading data");
        System.out.println("\t" + ROOT_CREATION_MODE + " = (only for loading) existing root check : " + Arrays.asList(CreationMode.values()));
        System.out.println("\t" + FILE_MODE + " = (only for storing) existing dump handling : " + Arrays.asList(FileExistsMode.values()));
        System.out.println("\t" + NAME_TO_STORE + " = (only for storing one root) root name to use when storing, e.g. allow to copy one root to another");
    }

    // args : URL then optionally roots and tables
    static Tuple3<SQL_URL, Map<String, String>, Set<String>> parseNames(final String[] args, final int offset) throws SQLException, IOException, URISyntaxException {
        // find out if roots and tables are part of the URL or separate arguments
        final int urlIndex = offset;
        final SQL_URL url = SQL_URL.create(args[urlIndex]);
        final int rootsIndex, tablesIndex;
        if (url.getTableName() != null) {
            assert url.getRootName() != null;
            rootsIndex = urlIndex;
            tablesIndex = urlIndex;
        } else if (url.getRootName() != null) {
            rootsIndex = urlIndex;
            tablesIndex = urlIndex + 1;
        } else {
            rootsIndex = urlIndex + 1;
            tablesIndex = rootsIndex + 1;
        }
        if (args.length > tablesIndex + 1)
            throw new IllegalArgumentException("Too many parameters");

        final List<String> roots;
        if (url.getRootName() != null) {
            roots = Collections.singletonList(url.getRootName());
        } else if (rootsIndex < args.length) {
            roots = SQLRow.toList(args[rootsIndex]);
        } else {
            roots = null;
        }
        final Map<String, String> rootNames;
        if (roots == null) {
            rootNames = null;
        } else if (roots.size() == 1) {
            rootNames = Collections.singletonMap(roots.get(0), System.getProperty(NAME_TO_STORE));
        } else {
            rootNames = CollectionUtils.createMap(roots);
        }

        final Set<String> tables;
        if (url.getTableName() != null) {
            tables = Collections.singleton(url.getTableName());
        } else if (tablesIndex < args.length) {
            tables = new HashSet<String>(SQLRow.toList(args[tablesIndex]));
        } else {
            tables = null;
        }

        return Tuple3.create(url, rootNames, tables);
    }

    public static void main(String[] args) throws SQLException, IOException, URISyntaxException {
        if (args.length < 3) {
            usage();
            System.exit(1);
        }

        final boolean store;
        if (args[0].equals("-store"))
            store = true;
        else if (args[0].equals("-load"))
            store = false;
        else
            throw new IllegalArgumentException("-store or -load");
        final File dir = new File(args[1]);

        final Tuple3<SQL_URL, Map<String, String>, Set<String>> names = parseNames(args, 2);

        final CreationMode rootCreationMode = getEnum(ROOT_CREATION_MODE, CreationMode.class, CreationMode.REQUIRED);
        final FileExistsMode existingDumpMode = getEnum(FILE_MODE, FileExistsMode.class, FileExistsMode.RENAME);

        LogUtils.rmRootHandlers();
        LogUtils.setUpConsoleHandler();
        Log.get().setLevel(Level.INFO);

        System.setProperty(SQLBase.ALLOW_OBJECT_REMOVAL, "true");
        // we're backup/restore tool: don't touch the data at all
        System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "true");

        final DBSystemRoot sysRoot = SQLServer.create(names.get0(), SQLRow.toList(System.getProperty(ROOTS_TO_MAP, "")), true, new IClosure<SQLDataSource>() {
            @Override
            public void executeChecked(SQLDataSource input) {
                input.addConnectionProperty("allowMultiQueries", "true");
            }
        });
        try {
            new Copy(store, dir, existingDumpMode, sysRoot, Boolean.getBoolean(NO_STRUCT), Boolean.getBoolean(NO_DATA)).applyTo(rootCreationMode, names.get1(), names.get2());
        } finally {
            sysRoot.getServer().destroy();
        }
    }

    private final boolean store;
    private final boolean noStruct;
    private final boolean noData;
    private final File dir;
    private final FileExistsMode mode;
    private final DBSystemRoot sysRoot;

    public Copy(final boolean store, final File dir, final DBSystemRoot base, boolean noStruct, boolean noData) throws SQLException, IOException {
        this(store, dir, FileExistsMode.RENAME, base, noStruct, noData);
    }

    public Copy(final boolean store, final File dir, final FileExistsMode mode, final DBSystemRoot base, boolean noStruct, boolean noData) throws SQLException, IOException {
        this.store = store;
        this.noStruct = noStruct;
        this.noData = noData;
        this.dir = dir;
        this.mode = mode;

        this.sysRoot = base;
    }

    /**
     * Apply the copy operation to the passed root or table (and only it).
     * 
     * @param rootName the name of the root to copy, cannot be <code>null</code>.
     * @param tableName the name of a table in <code>rootName</code>, <code>null</code> meaning all.
     * @throws SQLException if the database couldn't be accessed.
     * @throws IOException if the files couldn't be accessed.
     */
    public final void applyTo(final String rootName, final String tableName) throws SQLException, IOException {
        this.applyTo(rootName, rootName, tableName);
    }

    public final File applyTo(final String rootName, final String newRootName, final String tableName) throws SQLException, IOException {
        if (rootName == null)
            throw new NullPointerException("Null root name");
        return this.applyTo(CreationMode.ALLOWED, Collections.singletonMap(rootName, newRootName), tableName == null ? null : Collections.singleton(tableName));
    }

    public final File applyTo(final CreationMode rootCreationMode, final Map<String, String> rootNames, final Set<String> tableNames) throws SQLException, IOException {
        File previousDump = null;
        if (this.store && this.dir.exists()) {
            if (this.mode == FileExistsMode.FAIL) {
                throw new IllegalStateException("Dump exists at " + this.dir);
            } else if (this.mode == FileExistsMode.RENAME) {
                previousDump = addSuffix(this.dir, '_' + format(new Date()));
                if (!this.dir.renameTo(previousDump))
                    throw new IOException("could not rename " + this.dir + " to " + previousDump);
            } else if (this.mode == FileExistsMode.DELETE) {
                FileUtils.rm_R(this.dir);
            } else {
                // nothing to do
                assert this.mode == FileExistsMode.OVERWRITE;
                previousDump = this.dir;
            }
        }

        // setup roots to map
        if (!this.sysRoot.isMappingAllRoots()) {
            if (rootNames == null && this.store) {
                this.sysRoot.mapAllRoots();
                this.sysRoot.reload();
            } else {
                final Collection<String> roots;
                if (rootNames == null) {
                    assert !this.store;
                    roots = this.getRoots();
                } else {
                    roots = rootNames.keySet();
                }
                this.sysRoot.reload(this.sysRoot.addRootsToMap(roots));
            }
        }
        final Set<String> resolvedRootNames;
        if (this.store) {
            resolvedRootNames = resolveNames(this.sysRoot.getName(), rootNames == null ? null : rootNames.keySet(), this.sysRoot.getChildrenNames());
        } else {
            resolvedRootNames = resolveNames(this.dir.getName(), rootNames == null ? null : rootNames.keySet(), this.getRoots());
        }
        final Map<String, String> resolvedRoots;
        if (rootNames == null) {
            resolvedRoots = CollectionUtils.createMap(resolvedRootNames);
        } else {
            resolvedRoots = new LinkedHashMap<String, String>(rootNames);
            resolvedRoots.keySet().retainAll(resolvedRootNames);
        }

        SQLUtils.executeAtomic(this.sysRoot.getDataSource(), new ConnectionHandlerNoSetup<Object, IOException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException, IOException {
                applyToP(rootCreationMode, resolvedRoots, tableNames);
                return null;
            }
        });

        return previousDump;
    }

    private void applyToP(final CreationMode rootCreationMode, final Map<String, String> rootNames, final Set<String> tableNames) throws IOException, SQLException {
        final boolean dumpExists = this.dir.exists();
        final File propsFile = new File(this.dir, PROPS_NAME);
        final Properties dumpProps = propsFile.exists() ? PropertiesUtils.createFromFile(propsFile) : new Properties();
        final Integer storedVersion = dumpExists ? Integer.parseInt(dumpProps.getProperty(VERSION_KEY, "1")) : null;

        final SQLSyntax syntax = this.sysRoot.getSyntax();
        if (this.store) {
            if (dumpExists) {
                assert storedVersion != null;
                if (storedVersion != CURRENT_VERSION)
                    throw new IllegalStateException("Cannot modify dump version " + storedVersion + " with current version " + CURRENT_VERSION);
            } else {
                dumpProps.setProperty(VERSION_KEY, String.valueOf(CURRENT_VERSION));
            }
            final Date now = new Date();
            if (dumpProps.getProperty(CREATED_KEY) == null) {
                if (dumpExists)
                    System.err.println("Missing created key on existing dump");
                dumpProps.setProperty(CREATED_KEY, format(now));
            }
            dumpProps.setProperty(MODIFIED_KEY, format(now));

            FileUtils.mkdir_p(this.dir);
            final FileOutputStream out = new FileOutputStream(propsFile);
            try {
                dumpProps.store(out, "");
            } finally {
                out.close();
            }

            final NameTransformer nameTransformer = new NameTransformer() {
                @Override
                public SQLName transformTableName(SQLName tableName) {
                    if (tableName.getItemCount() != 2)
                        throw new IllegalArgumentException("Not 2 items : " + tableName);
                    final String translatedName = rootNames.get(tableName.getItem(0));
                    if (isEmpty(translatedName)) {
                        // i.e. not to be translated or points outside of the stored roots
                        return tableName;
                    } else {
                        return new SQLName(translatedName, tableName.getItem(1));
                    }
                }
            };

            for (final Entry<String, String> e : rootNames.entrySet()) {
                final String dbName = e.getKey();
                final String newRootName = isEmpty(e.getValue()) ? dbName : e.getValue();
                if (!this.sysRoot.contains(dbName))
                    throw new IllegalArgumentException(dbName + " does not exist in " + this.sysRoot);
                final DBRoot r = this.sysRoot.getRoot(dbName);

                final Set<String> resolvedTables = this.resolveNames(r.getName(), tableNames, r.getChildrenNames());
                final File rootDir = this.getDir(newRootName);

                if (!this.noStruct) {
                    final String newName = newRootName.equals(dbName) ? "" : " -> " + newRootName;
                    System.err.print("Structure of " + dbName + newName + "." + resolvedTables + " ... ");
                    for (final SQLSystem sys : SQLSystem.values()) {
                        final SQLSyntax targetSyntax = sys.getSyntax();
                        if (targetSyntax != null) {
                            // write how to create the root
                            final File rootFile = getRootFile(newRootName, sys, true);
                            FileUtils.mkParentDirs(rootFile);
                            FileUtils.write(r.getDefinitionSQL(targetSyntax, false).asString(newRootName, false, true), rootFile);

                            // write how to create the tables in 2 directories
                            // 1. the creation of the table itself
                            final File createDir = getTableDir(newRootName, sys, true);
                            // 2. the modification to add constraints (and indexes)
                            final File alterDir = getTableDir(newRootName, sys, false);
                            FileUtils.mkdir_p(createDir);
                            FileUtils.mkdir_p(alterDir);

                            for (final String tableName : resolvedTables) {
                                final SQLTable t = r.getTable(tableName);
                                final List2<String> createTable = t.getCreateTable(targetSyntax).getCreateAndAlter(nameTransformer);
                                if (isEmpty(createTable.get0()))
                                    throw new IllegalStateException("Empty CREATE for " + t.getSQLName());
                                FileUtils.write(createTable.get0(), getTableFile(createDir, tableName));
                                // table with no constraints
                                if (!isEmpty(createTable.get1()))
                                    FileUtils.write(createTable.get1(), getTableFile(alterDir, tableName));
                            }
                        }
                    }
                    System.err.println("done");
                }
                if (!this.noData) {
                    System.err.println("Data of " + resolvedTables + " ... ");
                    syntax.storeData(r, resolvedTables, rootDir);
                    System.err.println("Data done");
                }
            }
        } else if (storedVersion == 1) {
            for (final String rootName : rootNames.keySet()) {
                if (tableNames == null) {
                    this.loadV1(rootName, null);
                } else {
                    for (final String t : tableNames)
                        this.loadV1(rootName, t);
                }
            }
        } else {
            // load

            // don't support deletion of roots since it can drop constraints from other roots that
            // won't get loaded

            final Set<String> existingRoots = this.sysRoot.getChildrenNames();
            if (rootCreationMode == CreationMode.FORBIDDEN) {
                if (!existingRoots.containsAll(rootNames.keySet()))
                    throw new IllegalStateException("Some roots are missing");
            } else if (rootCreationMode == CreationMode.REQUIRED) {
                if (!Collections.disjoint(existingRoots, rootNames.keySet()))
                    throw new IllegalStateException("Some roots are already created");
            }

            final SQLSystem system = this.sysRoot.getServer().getSQLSystem();
            final SetMap<String, String> resolvedTablesByRoot = new SetMap<String, String>();
            for (final String rootName : rootNames.keySet()) {
                final Set<String> availableData = this.noData ? null : this.getTablesData(rootName);
                final Set<String> availableStruct = this.noStruct ? null : this.getTablesStruct(rootName, system);
                final Set<String> availableTables;
                if (availableData == null)
                    availableTables = availableStruct;
                else if (availableStruct == null)
                    availableTables = availableData;
                else
                    availableTables = CollectionUtils.union(availableData, availableStruct);

                final Set<String> resolvedTables = resolveNames(rootName, tableNames, availableTables);
                // otherwise no union was done
                if (availableData != null && availableStruct != null) {
                    if (!availableStruct.containsAll(resolvedTables))
                        throw new IllegalStateException("Structure missing for " + CollectionUtils.substract(resolvedTables, availableStruct));
                    if (!availableData.containsAll(resolvedTables))
                        throw new IllegalStateException("Data missing for " + CollectionUtils.substract(resolvedTables, availableData));
                }
                resolvedTablesByRoot.put(rootName, resolvedTables);

                DBRoot r = existingRoots.contains(rootName) ? this.sysRoot.getRoot(rootName) : null;

                if (!this.noStruct) {
                    if (r == null) {
                        System.err.print("Creation of " + rootName + " ... ");
                        final String sql = FileUtils.readUTF8(getRootFile(rootName, system, true));
                        // 'CREATE SCHEMA' doit être la première instruction d'un traitement de
                        // requêtes.
                        if (system == SQLSystem.MSSQL)
                            SQLUtils.executeScript(sql, this.sysRoot);
                        else
                            this.sysRoot.getDataSource().execute(sql);
                        System.err.println("done");
                    }

                    System.err.print("Creation of " + resolvedTables + " ... ");
                    final File createDir = getTableDir(rootName, system, true);
                    for (final String tableName : resolvedTables) {
                        final String tableSQL = FileUtils.readUTF8(getTableFile(createDir, tableName));
                        this.sysRoot.getDataSource().execute(tableSQL);
                    }
                    System.err.println("done");

                    this.sysRoot.refetch(Collections.singleton(rootName));
                    r = this.sysRoot.getRoot(rootName);
                }
                if (!this.noData) {
                    System.err.println("Data of " + rootName + " ... ");
                    // only need to disable foreign checks if the structure wasn't just created
                    // above otherwise the constraints will only be created afterwards
                    syntax.loadData(this.getDir(rootName), r, resolvedTables, Boolean.getBoolean(DELETE_TABLE), this.noStruct);
                    System.err.println("Data done");
                }

            }
            // out of above loop for inter-root cycles
            if (!this.noStruct) {
                for (final String rootName : rootNames.keySet()) {
                    final Set<String> resolvedTables = resolvedTablesByRoot.get(rootName);

                    System.err.println("Constraints of " + rootName + "." + resolvedTables + " ... ");

                    final File alterDir = getTableDir(rootName, system, false);
                    for (final String tableName : resolvedTables) {
                        final File alterFile = getTableFile(alterDir, tableName);
                        if (alterFile.exists()) {
                            this.sysRoot.getDataSource().execute(FileUtils.readUTF8(alterFile));
                        }
                    }

                    System.err.println("Constraints done");

                    final File rootFile = getRootFile(rootName, system, false);
                    if (rootFile.exists())
                        this.sysRoot.getDataSource().execute(FileUtils.readUTF8(rootFile));
                }
                this.sysRoot.refetch(rootNames.keySet());
            }
        }
    }

    private void loadV1(final String rootName, final String tableName) throws IOException, SQLException {
        DBRoot r = this.sysRoot.contains(rootName) ? this.sysRoot.getRoot(rootName) : null;

        if (!this.noStruct) {
            System.err.print("Structure of " + rootName + " ... ");
            {
                final SQLSystem system = this.sysRoot.getServer().getSQLSystem();
                String sql = FileUtils.readUTF8(getSQLFile(rootName, tableName, system));
                // for tables export there's no CREATE SCHEMA generated
                if (r == null && tableName != null) {
                    sql = new SQLCreateRoot(SQLSyntax.get(this.sysRoot), rootName).asString() + ";\n" + sql;
                }
                // 'CREATE SCHEMA' doit être la première instruction d'un traitement de requêtes.
                if (system == SQLSystem.MSSQL)
                    SQLUtils.executeScript(sql, this.sysRoot);
                else
                    this.sysRoot.getDataSource().execute(sql);
                this.sysRoot.refetch(Collections.singleton(rootName));
                r = this.sysRoot.getRoot(rootName);
            }
            System.err.println("done");
        }

        if (!this.noData) {
            System.err.println("Data of " + rootName + " ... ");
            final SQLSyntax syntax = this.sysRoot.getServer().getSQLSystem().getSyntax();
            final Set<String> tableNames = tableName == null ? null : singleton(tableName);
            // TODO support table with non-ASCII chars
            // eg : if on win with MySQL SET character_set_filesystem = latin1
            // may be just zip all data
            syntax.loadData(this.getDir(rootName), r, tableNames, Boolean.getBoolean(DELETE_TABLE));
            System.err.println("Data done");
        }
    }

    private Set<String> resolveNames(final String parentName, final Set<String> wantedNames, final Set<String> availableNames) {
        return this.resolveNames(parentName, wantedNames, availableNames, this.store);
    }

    private Set<String> resolveNames(final String parentName, final Set<String> wantedNames, final Set<String> availableNames, final boolean throwExn) {
        final Set<String> res;
        if (wantedNames != null) {
            final Set<String> missing = new HashSet<String>(wantedNames);
            missing.removeAll(availableNames);
            if (missing.size() > 0) {
                if (throwExn)
                    throw new IllegalStateException("Not found in " + parentName + " : " + missing);
                else
                    System.err.println("In " + parentName + " ignoring " + missing);
            }
            res = new HashSet<String>(availableNames);
            res.retainAll(wantedNames);
        } else {
            res = availableNames;
        }
        return res;
    }

    private Set<String> getRoots() {
        final File[] dirs = this.dir.listFiles(FileUtils.DIR_FILTER);
        if (dirs == null)
            throw new IllegalStateException("No directory at : " + this.dir);
        final Set<String> res = new HashSet<String>();
        for (final File dir : dirs)
            res.add(FileUtils.FILENAME_ESCAPER.unescape(dir.getName()));
        return res;
    }

    private File getDir(final String rootName) {
        return new File(this.dir, FileUtils.FILENAME_ESCAPER.escape(rootName));
    }

    private Set<String> getTablesData(final String rootName) {
        final Set<String> tables = getFiles(this.getDir(rootName), SQLSyntax.DATA_EXT);
        if (tables == null)
            throw new IllegalStateException("No table data files in " + rootName);
        return tables;
    }

    private Set<String> getTablesStruct(final String rootName, final SQLSystem system) {
        // use create.sql as there can be no constraint and thus no ALTER file
        final Set<String> tables = getFiles(this.getTableDir(rootName, system, true), STRUCT_EXT);
        if (tables == null)
            throw new IllegalStateException("No table structure files in " + rootName);
        return tables;
    }

    private File getSQLFile(final String rootName, final String tableName, final SQLSystem system) {
        final String t = tableName == null ? "" : tableName + "-";
        return new File(this.getDir(rootName), t + system.name().toLowerCase() + STRUCT_EXT);
    }

    private File getRootDir(final String rootName, final SQLSystem system) {
        return new File(this.getDir(rootName), system.name().toLowerCase());
    }

    private File getRootFile(final String rootName, final SQLSystem system, final boolean pre) {
        return new File(getRootDir(rootName, system), pre ? "pre.sql" : "post.sql");
    }

    private File getTableDir(final String rootName, final SQLSystem system, final boolean create) {
        return new File(getRootDir(rootName, system), create ? "tables-create" : "tables-alter");
    }

    private File getTableFile(final File tableDir, final String tableName) {
        return new File(tableDir, FileUtils.FILENAME_ESCAPER.escape(tableName) + STRUCT_EXT);
    }
}
