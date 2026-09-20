package cs324.worker;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class WorkerTestClient {
    private WorkerTestClient() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 5001;
        int workerId = args.length >= 3 ? Integer.parseInt(args[2]) : 1;
        String bindingName = "Worker-" + workerId;

        Registry registry = LocateRegistry.getRegistry(host, port);
        WorkerService worker = (WorkerService) registry.lookup(bindingName);

        System.out.println("Worker ID: " + worker.getWorkerId());
        System.out.println("JAC: " + worker.getJobAllocationCounter());
        System.out.println("Neighbours: " + worker.getNeighbours());
        System.out.println("Current coordinator ID: " + worker.getCurrentCoordinatorId());
        System.out.println("leaderman: " + worker.getLeaderman());
    }
}
