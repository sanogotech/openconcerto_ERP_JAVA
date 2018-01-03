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
 
 package org.openconcerto.utils;

import java.io.File;
import java.io.IOException;

/**
 * A set of base directories.
 * 
 * @author Sylvain CUAZ
 */
public abstract class BaseDirs {

    static private final String DATA = "Data";
    static private final String PREFERENCES = "Preferences";
    static private final String CACHES = "Caches";

    // https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html
    static public final class XDG extends BaseDirs {
        protected XDG(final ProductInfo info, final String subdir) {
            super(info, subdir);
        }

        @Override
        protected File _getAppDataFolder() {
            /*
             * $XDG_DATA_HOME defines the base directory relative to which user specific data files
             * should be stored. If $XDG_DATA_HOME is either not set or empty, a default equal to
             * $HOME/.local/share should be used.
             */
            return new File(StringUtils.coalesce(System.getenv("XDG_DATA_HOME"), System.getenv("HOME") + "/.local/share"), this.getAppID());
        }

        @Override
        protected File _getPreferencesFolder() {
            /*
             * $XDG_CONFIG_HOME defines the base directory relative to which user specific
             * configuration files should be stored. If $XDG_CONFIG_HOME is either not set or empty,
             * a default equal to $HOME/.config should be used.
             */
            return new File(StringUtils.coalesce(System.getenv("XDG_CONFIG_HOME"), System.getenv("HOME") + "/.config"), this.getAppID());
        }

        @Override
        protected File _getCacheFolder() {
            /*
             * $XDG_CACHE_HOME defines the base directory relative to which user specific
             * non-essential data files should be stored. If $XDG_CACHE_HOME is either not set or
             * empty, a default equal to $HOME/.cache should be used.
             */
            return new File(StringUtils.coalesce(System.getenv("XDG_CACHE_HOME"), System.getenv("HOME") + "/.cache"), this.getAppID());
        }
    }

    static public final class Unknown extends BaseDirs {

        protected Unknown(final ProductInfo info, final String subdir) {
            super(info, subdir);
        }

    }

    static public final class Windows extends BaseDirs {
        private final String path;

        protected Windows(final ProductInfo info, final String subdir) {
            super(info, subdir);
            final String orgID = info.getOrganizationName() == null ? null : FileUtils.sanitize(info.getOrganizationName());
            final String appID = this.getAppName();
            // handle missing org and avoid OpenConcerto/OpenConcerto
            this.path = orgID == null || orgID.equals(appID) ? appID : orgID + File.separatorChar + appID;
            // ProductInfo test emptiness
            assert this.path.charAt(0) != File.separatorChar && this.path.charAt(this.path.length() - 1) != File.separatorChar : "Separator not in between : " + this.path;
        }

        protected final String getPath() {
            return this.path;
        }

        @Override
        protected File _getAppDataFolder() {
            // do not use LOCALAPPDATA as the user needs its data synchronised
            return new File(System.getenv("APPDATA"), this.getPath() + File.separatorChar + DATA);
        }

        @Override
        protected File _getPreferencesFolder() {
            // do not use LOCALAPPDATA as configuration should be small enough to be synchronised on
            // the network
            return new File(System.getenv("APPDATA"), this.getPath() + File.separatorChar + PREFERENCES);
        }

        @Override
        protected File _getCacheFolder() {
            // use LOCALAPPDATA as caches can be quite big and don't need to be synchronised
            return new File(System.getenv("LOCALAPPDATA"), this.getPath() + File.separatorChar + CACHES);
        }
    }

    // https://developer.apple.com/library/mac/qa/qa1170/_index.html
    static public final class Mac extends BaseDirs {

        protected Mac(final ProductInfo info, final String subdir) {
            super(info, subdir);
        }

        @Override
        protected File _getAppDataFolder() {
            // NOTE : "Application Support" directory is reserved for non-essential application
            // resources
            return new File(System.getProperty("user.home") + "/Library/" + this.getAppName());
        }

        @Override
        protected File _getPreferencesFolder() {
            return new File(System.getProperty("user.home") + "/Library/Preferences/" + this.getAppFullID());
        }

        @Override
        protected File _getCacheFolder() {
            return new File(System.getProperty("user.home") + "/Library/Caches/" + this.getAppFullID());
        }
    }

    static public final class Portable extends BaseDirs {

        private final File rootDir;

        protected Portable(final File rootDir, final ProductInfo info, final String subdir) {
            super(info, subdir);
            this.rootDir = rootDir;
        }

        public final File getRootDir() {
            return this.rootDir;
        }

        @Override
        protected File _getAppDataFolder() {
            return new File(this.getRootDir(), DATA);
        }

        @Override
        protected File _getPreferencesFolder() {
            return new File(this.getRootDir(), PREFERENCES);
        }

        @Override
        protected File _getCacheFolder() {
            return new File(this.getRootDir(), CACHES);
        }
    }

    public static final BaseDirs createPortable(final File rootDir, final ProductInfo info, final String subdir) {
        return new Portable(rootDir, info, subdir);
    }

    public static final BaseDirs create(final ProductInfo info) {
        return create(info, null);
    }

    public static final BaseDirs create(final ProductInfo info, final String subdir) {
        final OSFamily os = OSFamily.getInstance();
        if (os == OSFamily.Windows)
            return new Windows(info, subdir);
        else if (os == OSFamily.Mac)
            return new Mac(info, subdir);
        else if (os instanceof OSFamily.Unix)
            return new XDG(info, subdir);
        else
            return new Unknown(info, subdir);
    }

    private final ProductInfo info;
    private final String subdir;

    protected BaseDirs(final ProductInfo info, final String subdir) {
        this.info = info;
        this.subdir = subdir == null ? null : FileUtils.sanitize(subdir);
    }

    // should use other methods to avoid invalid characters
    private final ProductInfo getInfo() {
        return this.info;
    }

    protected final String getAppName() {
        return FileUtils.sanitize(this.getInfo().getName());
    }

    protected final String getAppID() {
        return this.getInfo().getID();
    }

    protected final String getAppFullID() {
        final String res = this.getInfo().getFullID();
        return res != null ? res : this.getAppID();
    }

    protected File getFolderToWrite(final File dir) throws IOException {
        if (dir.isDirectory() && dir.canWrite())
            return dir;
        if (dir.exists())
            throw new IOException((dir.isDirectory() ? "Not writable: " : "Not a directory: ") + dir);
        // TODO create with 0700 mode in Java 7 (from § Referencing this specification)
        FileUtils.mkdir_p(dir);
        return dir;
    }

    protected final File getSubDir(final File dir) {
        return this.subdir == null ? dir : new File(dir, this.subdir);
    }

    protected File _getAppDataFolder() {
        return new File(System.getProperty("user.home"), "." + this.getAppFullID());
    }

    // where to write user-hidden data files (e.g. mbox files, DB files)
    public final File getAppDataFolder() {
        return getSubDir(_getAppDataFolder());
    }

    public final File getAppDataFolderToWrite() throws IOException {
        return getFolderToWrite(this.getAppDataFolder());
    }

    protected File _getPreferencesFolder() {
        return this.getAppDataFolder();
    }

    // where to write configuration
    public final File getPreferencesFolder() {
        return getSubDir(_getPreferencesFolder());
    }

    public final File getPreferencesFolderToWrite() throws IOException {
        return getFolderToWrite(this.getPreferencesFolder());
    }

    protected File _getCacheFolder() {
        return new File(System.getProperty("java.io.tmpdir"), this.getAppFullID());
    }

    // where to write data that can be re-created
    public final File getCacheFolder() {
        return getSubDir(_getCacheFolder());
    }

    public final File getCacheFolderToWrite() throws IOException {
        return getFolderToWrite(this.getCacheFolder());
    }

    @Override
    public String toString() {
        return BaseDirs.class.getSimpleName() + " " + this.getClass().getSimpleName();
    }

    public static void main(String[] args) {
        final String appName = args.length > 0 ? args[0] : "fooApp";
        final String companyName = args.length > 1 ? args[1] : "acme";
        final String subdir = System.getProperty("subdir");
        final BaseDirs instance = create(new ProductInfo(CollectionUtils.createMap(ProductInfo.ORGANIZATION_NAME, companyName, ProductInfo.NAME, appName)), subdir);
        System.out.println(instance);
        System.out.println("app data : " + instance.getAppDataFolder());
        System.out.println("preferences : " + instance.getPreferencesFolder());
        System.out.println("cache : " + instance.getCacheFolder());
    }
}
