package cs324.bootstrap;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class BootstrapServer {
    public static final String DEFAULT_BINDING_NAME = "BootstrapService";
    public static final int DEFAULT_RMI_PORT = 1099;

    private BootstrapServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RMI_PORT;
        String bindingName = args.length >= 2 ? args[1] : DEFAULT_BINDING_NAME;

        Registry registry = LocateRegistry.createRegistry(port);
        BootstrapService bootstrapNode = new BootstrapNode();
        registry.rebind(bindingName, bootstrapNode);

        System.out.printf("Bootstrap Node running at rmi://localhost:%d/%s%n", port, bindingName);
        System.out.println("Press Ctrl+C to stop.");

        Thread.currentThread().join();
    }
}
