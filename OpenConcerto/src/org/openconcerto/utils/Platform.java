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

import static org.openconcerto.utils.DesktopEnvironment.cmdSubstitution;
import org.openconcerto.utils.cc.ITransformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * To abstract differences between platform for non java tasks.
 * 
 * @author Sylvain
 * @see DesktopEnvironment
 */
public abstract class Platform {

    private static final int PING_TIMEOUT = 250;

    public static final Platform getInstance() {
        final OSFamily os = OSFamily.getInstance();
        if (os == OSFamily.Windows) {
            return CYGWIN;
        } else if (os == OSFamily.FreeBSD || os == OSFamily.Mac) {
            return FREEBSD;
        } else {
            return LINUX;
        }
    }

    public abstract boolean supportsPID();

    public abstract boolean isRunning(final int pid) throws IOException;

    public abstract String getPath(final File f);

    public final String getPID() throws IOException {
        final Process p = this.eval("echo -n $PPID");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public final String tail(final File f, final int n) throws IOException {
        final Process p = Runtime.getRuntime().exec(new String[] { "tail", "-n" + n, this.getPath(f) });
        return cmdSubstitution(p);
    }

    protected abstract String getBash();

    /**
     * Create a symbolic link from f2 to f1. NOTE: the path from f2 to f1 is made relative (eg
     * lastLog -> ./allLogs/tuesdayLog).
     * 
     * @param f1 the destination of the link, eg "/dir/allLogs/tuesdayLog".
     * @param f2 the name of the link, eg "/dir/lastLog".
     * @throws IOException if an error occurs.
     */
    public final void ln_s(File f1, File f2) throws IOException {
        FileUtils.ln(f1, f2);
    }

    public boolean isSymLink(File f) throws IOException {
        return exitStatus(Runtime.getRuntime().exec(new String[] { "test", "-L", f.getAbsolutePath() })) == 0;
    }

    // see cygwin
    public File getNativeSymlinkFile(File dir) {
        return dir;
    }

    public abstract String readLink(File f) throws IOException;

    public final boolean exists(File f) throws IOException {
        return exitStatus(Runtime.getRuntime().exec(new String[] { "test", "-e", f.getAbsolutePath() })) == 0;
    }

    public final void append(File f1, File f2) throws IOException {
        final String c = "cat '" + f1.getAbsolutePath() + "' >> '" + f2.getAbsolutePath() + "'";
        try {
            this.eval(c).waitFor();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public Process cp_l(File src, File dest) throws IOException {
        return Runtime.getRuntime().exec(new String[] { "cp", "-prl", src.getAbsolutePath(), dest.getAbsolutePath() });
    }

    public final boolean ping(InetAddress host) throws IOException {
        return this.ping(host, PING_TIMEOUT);
    }

    /**
     * Test whether that address is reachable.
     * 
     * @param host the host to reach.
     * @param timeout the time, in milliseconds, before the call aborts.
     * @return <code>true</code> if the address is reachable.
     * @throws IOException if a network error occurs.
     */
    public abstract boolean ping(InetAddress host, final int timeout) throws IOException;

    public final PingBuilder createPingBuilder() {
        return new PingBuilder(this);
    }

    protected abstract PingResult ping(InetAddress host, final PingBuilder pingBuilder, final int routingTableIndex) throws IOException;

    protected abstract BigDecimal parsePingAverageRT(String statsLine);

    protected final PingResult ping(final String command, final int totalCount, int requiredCount) throws IOException {
        if (requiredCount <= 0)
            requiredCount = totalCount;
        final List<String> countAndLastLine = StringUtils.splitIntoLines(cmdSubstitution(eval(command)));
        if (countAndLastLine.size() != 2)
            throw new IllegalStateException("Not 2 lines in " + countAndLastLine);
        final int replied = Integer.parseInt(countAndLastLine.get(0));
        assert replied <= totalCount;
        final PingResult res = new PingResult(totalCount, replied, requiredCount, replied == 0 ? null : parsePingAverageRT(countAndLastLine.get(1).trim()));
        return res;
    }

    /**
     * Eval the passed string with bash.
     * 
     * @param s a bash script.
     * @return the created process.
     * @throws IOException If an I/O error occurs.
     */
    public final Process eval(String s) throws IOException {
        return Runtime.getRuntime().exec(new String[] { this.getBash(), "-c", s });
    }

    public final int exitStatus(Process p) {
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public abstract boolean isAdmin() throws IOException;

    private static abstract class UnixPlatform extends Platform {

        @Override
        public boolean supportsPID() {
            return true;
        }

        public final boolean isRunning(final int pid) throws IOException {
            // --pid only works on Linux, -p also on Nexenta
            final Process p = Runtime.getRuntime().exec(new String[] { "ps", "-p", String.valueOf(pid) });
            return this.exitStatus(p) == 0;
        }

        public String getPath(final File f) {
            return f.getPath();
        }

        @Override
        protected String getBash() {
            return "bash";
        }

        @Override
        public String readLink(File f) throws IOException {
            // --no-newline long form not supported on FreeBSD
            final Process p = Runtime.getRuntime().exec(new String[] { "readlink", "-n", f.getAbsolutePath() });
            return cmdSubstitution(p);
        }

        @Override
        public boolean ping(InetAddress host, final int timeout) throws IOException {
            try {
                return host.isReachable(timeout);
            } catch (SocketException e) {
                // On FreeBSD at least, if the destination is blocked by the firewall :
                // java.net.SocketException: Operation not permitted
                // at java.net.Inet4AddressImpl.isReachable0(Native Method)
                // at java.net.Inet4AddressImpl.isReachable(Inet4AddressImpl.java:70)
                Log.get().log(Level.FINER, "Swallow exception", e);
                return false;
            }
        }

        // Linux : rtt min/avg/max/mdev = 12.552/13.399/14.247/0.855 ms
        // FreeBSD : round-trip min/avg/max/stddev = 0.301/0.371/0.442/0.071 ms
        private static final Pattern PING_STATS_PATTERN = Pattern
                .compile("^\\p{Blank}*(?:rtt|round-trip)\\p{Blank}+min/avg/max/(?:mdev|stddev)\\p{Blank}+=\\p{Blank}+([0-9\\.]+)/([0-9\\.]+)/([0-9\\.]+)/([0-9\\.]+)\\p{Blank}+ms$");

        @Override
        protected BigDecimal parsePingAverageRT(String statsLine) {
            final Matcher m = PING_STATS_PATTERN.matcher(statsLine);
            if (!m.matches())
                throw new IllegalArgumentException("Not matching " + PING_STATS_PATTERN + " : " + statsLine);
            return new BigDecimal(m.group(2));
        }

        @Override
        public boolean isAdmin() throws IOException {
            // root is uid 0
            return cmdSubstitution(this.eval("id -u")).trim().equals("0");
        }
    }

    private static final Platform LINUX = new UnixPlatform() {
        @Override
        public PingResult ping(final InetAddress host, final PingBuilder pingBuilder, final int routingTableIndex) throws IOException {
            if (routingTableIndex > 0)
                throw new UnsupportedOperationException("On Linux, choosing a different routing table requires changing the system policy");
            final List<String> command = new ArrayList<String>(16);
            command.add("ping");
            final int totalCount = pingBuilder.getTotalCount();
            command.add("-c");
            command.add(String.valueOf(totalCount));

            if (pingBuilder.getWaitTime() > 0) {
                command.add("-W");
                final int timeInSeconds = pingBuilder.getWaitTime() / 1000;
                command.add(String.valueOf(Math.max(timeInSeconds, 1)));
            }

            command.add("-M");
            command.add(pingBuilder.isDontFragment() ? "do" : "dont");

            if (pingBuilder.getLength() > 0) {
                command.add("-s");
                command.add(String.valueOf(pingBuilder.getLength()));
            }
            if (pingBuilder.getTTL() > 0) {
                command.add("-t");
                command.add(String.valueOf(pingBuilder.getTTL()));
            }
            if (pingBuilder.getSourceAddress() != null) {
                command.add("-I");
                command.add(pingBuilder.getSourceAddress());
            }

            command.add(host.getHostAddress());

            return ping("out=$(" + CollectionUtils.join(command, " ") + ") ; grep -c ttl= <<< \"$out\" ; tail -n1 <<< \"$out\"", totalCount, pingBuilder.getRequiredReplies());
        }
    };

    private static final Platform FREEBSD = new UnixPlatform() {
        @Override
        public PingResult ping(final InetAddress host, final PingBuilder pingBuilder, final int routingTableIndex) throws IOException {
            final List<String> command = new ArrayList<String>(16);
            command.add("setfib");
            command.add(String.valueOf(routingTableIndex));
            command.add("ping");
            final int totalCount = pingBuilder.getTotalCount();
            command.add("-c");
            command.add(String.valueOf(totalCount));

            if (pingBuilder.getWaitTime() > 0) {
                command.add("-W");
                command.add(String.valueOf(pingBuilder.getWaitTime()));
            }

            if (pingBuilder.isDontFragment()) {
                command.add("-D");
            }
            if (pingBuilder.getLength() > 0) {
                command.add("-s");
                command.add(String.valueOf(pingBuilder.getLength()));
            }
            if (pingBuilder.getTTL() > 0) {
                command.add("-m");
                command.add(String.valueOf(pingBuilder.getTTL()));
            }
            if (pingBuilder.getSourceAddress() != null) {
                command.add("-S");
                command.add(pingBuilder.getSourceAddress());
            }

            command.add(host.getHostAddress());

            return ping("out=$(" + CollectionUtils.join(command, " ") + ") ; grep -c ttl= <<< \"$out\" ; tail -n1 <<< \"$out\"", totalCount, pingBuilder.getRequiredReplies());
        }
    };

    private static final class CygwinPlatform extends Platform {

        @Override
        public boolean supportsPID() {
            return false;
        }

        @Override
        public boolean isRunning(int pid) throws IOException {
            // PID TTY STIME COMMAND
            // 864 ? 09:27:57 \??\C:\WINDOWS\system32\winlogon.exe
            // so if we count lines, we should get 2
            final Process p = this.eval("export PATH=$PATH:/usr/bin ; test $(ps -sW -p " + pid + " | wc -l) -eq 2 ");
            return this.exitStatus(p) == 0;
        }

        @Override
        protected String getBash() {
            // We used to specify the full path here, but this needed to be configurable (e.g.
            // cygwin 32 or 64 bit), plus all other programs are required to be on the PATH
            return "bash.exe";
        }

        @Override
        public String readLink(File f) throws IOException {
            // windows format to be able to use File
            final Process p = Runtime.getRuntime().exec(new String[] { "readshortcut.exe", "-w", f.getAbsolutePath() });
            return cmdSubstitution(p).trim();
        }

        @Override
        public String getPath(File f) {
            return toCygwinPath(f);
        }

        @Override
        public boolean ping(InetAddress host, final int timeout) throws IOException {
            // windows implem of isReachable() is buggy
            // see http://bordet.blogspot.com/2006/07/icmp-and-inetaddressisreachable.html
            try {
                final int exit = Runtime.getRuntime().exec("ping -n 1 -w " + timeout + " " + host.getHostAddress()).waitFor();
                return exit == 0;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public PingResult ping(final InetAddress host, final PingBuilder pingBuilder, final int routingTableIndex) throws IOException {
            if (routingTableIndex > 0)
                throw new UnsupportedOperationException("Only one routing table on Windows");
            final List<String> command = new ArrayList<String>(16);
            command.add("ping");
            final int totalCount = pingBuilder.getTotalCount();
            command.add("-n");
            command.add(String.valueOf(totalCount));

            if (pingBuilder.getWaitTime() > 0) {
                command.add("-w");
                command.add(String.valueOf(pingBuilder.getWaitTime()));
            }

            if (pingBuilder.isDontFragment()) {
                command.add("-f");
            }
            if (pingBuilder.getLength() > 0) {
                command.add("-l");
                command.add(String.valueOf(pingBuilder.getLength()));
            }
            if (pingBuilder.getTTL() > 0) {
                command.add("-i");
                command.add(String.valueOf(pingBuilder.getTTL()));
            }
            if (pingBuilder.getSourceAddress() != null) {
                command.add("-S");
                command.add(pingBuilder.getSourceAddress());
            }

            command.add(host.getHostAddress());

            // can't use local variable : never could work out newlines problem
            // see https://cygwin.com/ml/cygwin-announce/2011-02/msg00027.html
            return ping("tmpF=$(mktemp) && " + CollectionUtils.join(command, " ") + " > $tmpF ; grep -c \"TTL=\" < $tmpF ; tail -n1 $tmpF; rm \"$tmpF\"", totalCount, pingBuilder.getRequiredReplies());
        }

        // Minimum = 35ms, Maximum = 36ms, Moyenne = 35ms
        private static final Pattern PING_STATS_PATTERN = Pattern.compile("^\\p{Blank}*\\p{Alpha}+ = ([0-9\\.]+)ms, \\p{Alpha}+ = ([0-9\\.]+)ms, \\p{Alpha}+ = ([0-9\\.]+)ms$");

        @Override
        protected BigDecimal parsePingAverageRT(String statsLine) {
            final Matcher m = PING_STATS_PATTERN.matcher(statsLine);
            if (!m.matches())
                throw new IllegalArgumentException("Not matching " + PING_STATS_PATTERN + " : " + statsLine);
            return new BigDecimal(m.group(3));
        }

        @Override
        public boolean isAdmin() throws IOException {
            // SID administrators S-1-5-32-544 or S-1-5-114
            return this.exitStatus(this.eval("id -G | egrep '\\b(544|114)\\b'")) == 0;
        }

        @Override
        public boolean isSymLink(File f) throws IOException {
            // links created with "ln" can loose their "test -L" status over a copy (eg between two
            // Eclipse workspaces), so check with filename
            return getNativeSymlinkFile(f).exists() || super.isSymLink(f);
        }

        // when cygwin does "ln -s f link" it actually creates "link.lnk"
        @Override
        public File getNativeSymlinkFile(File dir) {
            return FileUtils.addSuffix(dir, ".lnk");
        }
    };

    private static final Platform CYGWIN = new CygwinPlatform();

    public static String toCygwinPath(File dir) {
        final List<File> ancestors = getAncestors(dir);

        final String root = ancestors.get(0).getPath();
        final List<File> rest = ancestors.subList(1, ancestors.size());
        return "/cygdrive/" + root.charAt(0) + "/" + CollectionUtils.join(rest, "/", new ITransformer<File, String>() {
            @Override
            public String transformChecked(File f) {
                return f.getName();
            }
        });
    }

    public static List<File> getAncestors(File f) {
        final File abs = f.getAbsoluteFile();
        File current = abs;
        final List<File> res = new ArrayList<File>();

        while (current != null) {
            res.add(0, current);
            current = current.getParentFile();
        }
        return res;
    }

    public static final class PingResult {

        private final int wantedCount, repliesCount, requiredReplies;
        private final BigDecimal averageRTT;

        private PingResult(final int wantedCount, final int repliesCount, final int requiredReplies, final BigDecimal averageRTT) {
            this.wantedCount = wantedCount;
            this.repliesCount = repliesCount;
            this.requiredReplies = requiredReplies;
            this.averageRTT = averageRTT;
        }

        public final int getRepliesCount() {
            return this.repliesCount;
        }

        public final int getRequiredReplies() {
            return this.requiredReplies;
        }

        public final boolean hasRequiredReplies() {
            return this.hasEnoughReplies(this.getRequiredReplies());
        }

        public final boolean hasEnoughReplies(final int min) {
            return this.getRepliesCount() >= min;
        }

        public final BigDecimal getAverageRTT() {
            return this.averageRTT;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " " + this.getRepliesCount() + "/" + this.wantedCount + " reply(ies), average RTT : " + this.getAverageRTT() + " ms";
        }
    }

    public static final class PingBuilder {

        private final Platform platform;

        // in milliseconds
        private int waitTime = 4000;
        private boolean dontFragment = false;
        private int length = -1;
        private String srcAddr = null;
        private int ttl = -1;
        private int totalCount = 4;
        private int requiredReplies = -1;

        PingBuilder(final Platform p) {
            this.platform = p;
        }

        public final PingBuilder setWaitTime(final int waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public final int getWaitTime() {
            return this.waitTime;
        }

        public final boolean isDontFragment() {
            return this.dontFragment;
        }

        public final PingBuilder setDontFragment(boolean dontFragment) {
            this.dontFragment = dontFragment;
            return this;
        }

        public final int getLength() {
            return this.length;
        }

        public final PingBuilder setLength(int length) {
            this.length = length;
            return this;
        }

        public String getSourceAddress() {
            return this.srcAddr;
        }

        public PingBuilder setSourceAddress(String srcAddr) {
            this.srcAddr = srcAddr;
            return this;
        }

        public final int getTTL() {
            return this.ttl;
        }

        public final PingBuilder setTTL(int ttl) {
            this.ttl = ttl;
            return this;
        }

        public final int getTotalCount() {
            return this.totalCount;
        }

        public final PingBuilder setTotalCount(int totalCount) {
            if (totalCount <= 0)
                throw new IllegalArgumentException("Negative count : " + totalCount);
            this.totalCount = totalCount;
            return this;
        }

        public final int getRequiredReplies() {
            return this.requiredReplies;
        }

        public final PingBuilder setRequiredReplies(int requiredReplies) {
            this.requiredReplies = requiredReplies;
            return this;
        }

        public final boolean isAlive(final InetAddress host) throws IOException {
            return this.isAlive(host, 0);
        }

        public final boolean isAlive(final InetAddress host, final int routingTableIndex) throws IOException {
            return execute(host, routingTableIndex).hasRequiredReplies();
        }

        public PingResult execute(final InetAddress host) throws IOException {
            return execute(host, 0);
        }

        public PingResult execute(final InetAddress host, final int routingTableIndex) throws IOException {
            return this.platform.ping(host, this, routingTableIndex);
        }
    }

    public static final class Ping {
        public static void main(String[] args) throws IOException {
            final int totalCount = Integer.parseInt(System.getProperty("count", "4"));
            final String srcAddr = System.getProperty("srcAddr");
            System.out.println(Platform.getInstance().createPingBuilder().setTotalCount(totalCount).setSourceAddress(srcAddr).execute(InetAddress.getByName(args[0])));
        }
    }
}
