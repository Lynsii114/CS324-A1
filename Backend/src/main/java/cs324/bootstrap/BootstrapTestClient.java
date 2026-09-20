package cs324.bootstrap;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class BootstrapTestClient {
    private BootstrapTestClient() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : BootstrapServer.DEFAULT_RMI_PORT;
        String bindingName = args.length >= 3 ? args[2] : BootstrapServer.DEFAULT_BINDING_NAME;

        Registry registry = LocateRegistry.getRegistry(host, port);
        BootstrapService bootstrap = (BootstrapService) registry.lookup(bindingName);

        bootstrap.registerWorker(new WorkerInfo(9001, "localhost", 5901));
        bootstrap.registerWorker(new WorkerInfo(9002, "localhost", 5902));

        System.out.println("Active workers: " + bootstrap.getActiveWorkers());
        System.out.println("Random worker: " + bootstrap.getRandomActiveWorker());
        System.out.println("Unregistered test worker 9001: " + bootstrap.unregisterWorker(9001));
        System.out.println("Active workers after unregister: " + bootstrap.getActiveWorkers());
    }
}
