package io.github.future0923.debug.power.core;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.github.future0923.debug.power.base.config.AgentArgs;
import io.github.future0923.debug.power.base.logging.AnsiLog;
import io.github.future0923.debug.power.base.utils.DebugPowerJavaVersionUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.Properties;

/**
 * @author future0923
 */
public class DebugPower {

    public static void main(String[] args) throws Exception {
        DefaultParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("p", "pid", true, "java pid");
        options.addOption("a", "agent", true, "java agent path");
        options.addOption("lp", "listen-port", true, "target application server listen port");
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("debug-power", options);
            return;
        }
        attachAgent(cmd);
    }

    private static void attachAgent(CommandLine cmd) throws Exception {
        String javaPid = cmd.getOptionValue("pid");
        String debugPowerAgentPath = cmd.getOptionValue("agent");
        VirtualMachineDescriptor virtualMachineDescriptor = null;
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(javaPid)) {
                virtualMachineDescriptor = descriptor;
                break;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) {
                virtualMachine = VirtualMachine.attach(javaPid);
            } else {
                virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            }
            Properties targetSystemProperties = virtualMachine.getSystemProperties();
            String targetJavaVersion = DebugPowerJavaVersionUtils.javaVersionStr(targetSystemProperties);
            String currentJavaVersion = DebugPowerJavaVersionUtils.javaVersionStr();
            if (targetJavaVersion != null && currentJavaVersion != null) {
                if (!targetJavaVersion.equals(currentJavaVersion)) {
                    AnsiLog.warn("Current VM java version: {} do not match target VM java version: {}, attach may fail.",
                            currentJavaVersion, targetJavaVersion);
                    AnsiLog.warn("Target VM JAVA_HOME is {}, debug-power-boot JAVA_HOME is {}, try to set the same JAVA_HOME.",
                            targetSystemProperties.getProperty("java.home"), System.getProperty("java.home"));
                }
            }
            try {
                AgentArgs agentArgs = new AgentArgs();
                if (cmd.hasOption("listen-port")) {
                    agentArgs.setListenPort(cmd.getOptionValue("listen-port"));
                }
                virtualMachine.loadAgent(debugPowerAgentPath, agentArgs.format());
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Non-numeric value found")) {
                    AnsiLog.warn(e);
                    AnsiLog.warn("It seems to use the lower version of JDK to attach the higher version of JDK.");
                    AnsiLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw e;
                }
            } catch (com.sun.tools.attach.AgentLoadException ex) {
                if ("0".equals(ex.getMessage())) {
                    // https://stackoverflow.com/a/54454418
                    AnsiLog.warn(ex);
                    AnsiLog.warn("It seems to use the higher version of JDK to attach the lower version of JDK.");
                    AnsiLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw ex;
                }
            }
        } finally {
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
        }
    }
}
