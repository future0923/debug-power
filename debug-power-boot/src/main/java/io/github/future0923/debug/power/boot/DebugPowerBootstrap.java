package io.github.future0923.debug.power.boot;

import io.github.future0923.debug.power.base.constants.ProjectConstants;
import io.github.future0923.debug.power.base.logging.AnsiLog;
import io.github.future0923.debug.power.base.utils.DebugPowerExecUtils;
import io.github.future0923.debug.power.base.utils.DebugPowerFileUtils;
import io.github.future0923.debug.power.base.utils.DebugPowerIOUtils;
import io.github.future0923.debug.power.base.utils.DebugPowerJavaVersionUtils;
import io.github.future0923.debug.power.base.utils.DebugPowerLibUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

/**
 * @author future0923
 */
public class DebugPowerBootstrap {

    public static void main(String[] args) {
        DefaultParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("lp", "listen-port", true, "target application server listen port");
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("debug-power", options);
            return;
        }
        Package bootstrapPackage = DebugPowerBootstrap.class.getPackage();
        if (bootstrapPackage != null) {
            String debugPowerBootVersion = bootstrapPackage.getImplementationVersion();
            if (debugPowerBootVersion != null) {
                AnsiLog.info("debug-power-boot version: " + debugPowerBootVersion);
            }
        }
        File debugPowerHomeDir = DebugPowerLibUtils.getDebugPowerLibDir();
        File coreJarFile = new File(debugPowerHomeDir, "debug-power-core.jar");
        if (ProjectConstants.DEBUG || !coreJarFile.exists()) {
            coreJarFile = DebugPowerFileUtils.getLibResourceJar(DebugPowerBootstrap.class.getClassLoader(), "debug-power-core");
        }
        File agentJarFile = new File(debugPowerHomeDir, "debug-power-agent.jar");
        if (ProjectConstants.DEBUG || !agentJarFile.exists()) {
            agentJarFile = DebugPowerFileUtils.getLibResourceJar(DebugPowerBootstrap.class.getClassLoader(), "debug-power-agent");
        }

        long pid = -1;
        try {
            pid = DebugPowerExecUtils.select();
        } catch (InputMismatchException e) {
            AnsiLog.error("Please input an integer to select pid.");
            System.exit(1);
        }
        if (pid < 0) {
            AnsiLog.error("Please select an available pid.");
            System.exit(1);
        }
        AnsiLog.info("Try to attach process " + pid);
        String javaHome = DebugPowerExecUtils.findJavaHome();
        File javaPath = DebugPowerExecUtils.findJava(javaHome);
        if (javaPath == null) {
            throw new IllegalArgumentException(
                    "Can not find java/java.exe executable file under java home: " + javaHome);
        }
        File toolsJar = DebugPowerExecUtils.findToolsJar(javaHome);
        if (DebugPowerJavaVersionUtils.isLessThanJava9()) {
            if (toolsJar == null || !toolsJar.exists()) {
                throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome);
            }
        }
        List<String> command = new ArrayList<>();
        command.add(javaPath.getAbsolutePath());

        if (toolsJar != null && toolsJar.exists()) {
            command.add("-Xbootclasspath/a:" + toolsJar.getAbsolutePath());
        }

        command.add("-jar");
        command.add(coreJarFile.getAbsolutePath());
        command.add("--pid");
        command.add(String.valueOf(pid));
        command.add("--agent");
        command.add(agentJarFile.getAbsolutePath());
        String listenPort = cmd.getOptionValue("listen-port");
        if (listenPort != null) {
            command.add("--listen-port");
            command.add(listenPort);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            final Process proc = pb.start();
            Thread redirectStdout = new Thread(() -> {
                InputStream inputStream = proc.getInputStream();
                try {
                    DebugPowerIOUtils.copy(inputStream, System.out);
                } catch (IOException e) {
                    DebugPowerIOUtils.close(inputStream);
                }
            });

            Thread redirectStderr = new Thread(() -> {
                InputStream inputStream = proc.getErrorStream();
                try {
                    DebugPowerIOUtils.copy(inputStream, System.err);
                } catch (IOException e) {
                    DebugPowerIOUtils.close(inputStream);
                }

            });
            redirectStdout.start();
            redirectStderr.start();
            redirectStdout.join();
            redirectStderr.join();

            int exitValue = proc.exitValue();
            if (exitValue != 0) {
                AnsiLog.error("attach fail, targetPid: " + pid);
                System.exit(1);
            }
        } catch (Throwable e) {
            // ignore
        }
        AnsiLog.info("Attach process {} success.", pid);
    }

}
