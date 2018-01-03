package org.openconcerto.tools;

import java.io.File;
import java.io.IOException;

import org.openconcerto.erp.modules.ModulePackager;

public class AllModulesPackager {
	/**
	 * Tool to package all the modules
	 */
	private File dir;

	public AllModulesPackager(File dir) {
		this.dir = dir;
	}

	public static void main(String[] args) throws IOException {
		AllModulesPackager p = new AllModulesPackager(new File("../"));
		p.packageTo(new File("Modules"));
	}

	private void packageTo(File outputDir) throws IOException {
		outputDir.mkdirs();
		File[] dirs = this.dir.listFiles();
		for (int i = 0; i < dirs.length; i++) {
			File projectDir = dirs[i];
			String name = projectDir.getName();
			if (!projectDir.isDirectory() || name.equals("OpenConcerto") || name.equals("Tools")
					|| name.startsWith(".")) {
				continue;
			}

			File propsFile = new File(projectDir, "module.properties");
			if (propsFile.exists()) {
				System.out.println("AllModulesPackager.packageTo() packaging module : " + name);
				try {
					final ModulePackager modulePackager = new ModulePackager(propsFile, new File(projectDir, "bin"));
					modulePackager.setSkipDuplicateFiles(true);
					File libDir = new File(projectDir, "lib");
					if (libDir.exists())
						modulePackager.addJarsFromDir(libDir);
					modulePackager.writeToDir(outputDir);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("AllModulesPackager.packageTo() packaging module : " + name + " failed");
				}
			} else {
				System.out.println("AllModulesPackager.packageTo() skipping " + name + " no module.properties");
			}
		}
	}

}
