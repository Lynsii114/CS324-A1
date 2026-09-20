package cs324.worker;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public final class MaxJobClient {
    private MaxJobClient() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java -cp Backend/out cs324.worker.MaxJobClient <coordinatorHost> <coordinatorPort> <coordinatorId> <number1> [number2 ...]");
            System.exit(1);
        }

        String coordinatorHost = args[0];
        int coordinatorPort = Integer.parseInt(args[1]);
        int coordinatorId = Integer.parseInt(args[2]);
        List<Integer> numbers = new ArrayList<>();
        for (int index = 3; index < args.length; index++) {
            numbers.add(Integer.parseInt(args[index]));
        }

        Registry registry = LocateRegistry.getRegistry(coordinatorHost, coordinatorPort);
        WorkerService coordinator = (WorkerService) registry.lookup("Worker-" + coordinatorId);

        int result = coordinator.submitMaxJob(numbers);
        System.out.println("MAX result: " + result);
    }
}
