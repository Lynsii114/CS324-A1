package cs324.worker;

import cs324.bootstrap.BootstrapServer;
import cs324.bootstrap.BootstrapService;
import cs324.bootstrap.WorkerInfo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerNode extends UnicastRemoteObject implements WorkerService {
    private static final long serialVersionUID = 1L;

    private final int workerId;
    private final AtomicInteger jobAllocationCounter = new AtomicInteger(0);
    private final CopyOnWriteArrayList<WorkerInfo> neighbours = new CopyOnWriteArrayList<>();
    private volatile int currentCoordinatorId;
    private final String leaderman = "cs324";

    public WorkerNode(int workerId) throws java.rmi.RemoteException {
        super();
        if (workerId < 0) {
            throw new IllegalArgumentException("Worker id must not be negative");
        }

        this.workerId = workerId;
        this.currentCoordinatorId = workerId;
    }

    @Override
    public int getWorkerId() {
        return workerId;
    }

    @Override
    public int getJobAllocationCounter() {
        return jobAllocationCounter.get();
    }

    @Override
    public List<WorkerInfo> getNeighbours() {
        return List.copyOf(neighbours);
    }

    @Override
    public void addNeighbour(WorkerInfo worker) {
        if (worker == null || worker.getId() == workerId || neighbours.contains(worker)) {
            return;
        }

        neighbours.add(worker);
    }

    @Override
    public int getCurrentCoordinatorId() {
        return currentCoordinatorId;
    }

    @Override
    public String getLeaderman() {
        return leaderman;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java -cp Backend/out cs324.worker.WorkerNode <workerId> <workerRmiPort> [bootstrapHost] [bootstrapPort] [bootstrapBindingName]");
            System.exit(1);
        }

        int workerId = Integer.parseInt(args[0]);
        int workerPort = Integer.parseInt(args[1]);
        String bootstrapHost = args.length >= 3 ? args[2] : "localhost";
        int bootstrapPort = args.length >= 4 ? Integer.parseInt(args[3]) : BootstrapServer.DEFAULT_RMI_PORT;
        String bootstrapBindingName = args.length >= 5 ? args[4] : BootstrapServer.DEFAULT_BINDING_NAME;
        String workerBindingName = "Worker-" + workerId;

        WorkerNode workerNode = new WorkerNode(workerId);
        Registry workerRegistry = LocateRegistry.createRegistry(workerPort);
        workerRegistry.rebind(workerBindingName, workerNode);

        Registry bootstrapRegistry = LocateRegistry.getRegistry(bootstrapHost, bootstrapPort);
        BootstrapService bootstrap = (BootstrapService) bootstrapRegistry.lookup(bootstrapBindingName);

        WorkerInfo workerInfo = new WorkerInfo(workerId, "localhost", workerPort);
        WorkerInfo neighbourInfo = bootstrap.getRandomActiveWorker();
        if (neighbourInfo != null && neighbourInfo.getId() != workerId) {
            workerNode.addNeighbour(neighbourInfo);

            Registry neighbourRegistry = LocateRegistry.getRegistry(neighbourInfo.getHost(), neighbourInfo.getPort());
            WorkerService neighbour = (WorkerService) neighbourRegistry.lookup("Worker-" + neighbourInfo.getId());
            neighbour.addNeighbour(workerInfo);
        }

        bootstrap.registerWorker(workerInfo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                bootstrap.unregisterWorker(workerId);
            } catch (Exception ignored) {
                // The bootstrap process may already be stopped.
            }
        }));

        System.out.printf("Worker %d running at rmi://localhost:%d/%s%n", workerId, workerPort, workerBindingName);
        System.out.printf("Registered with Bootstrap Node at rmi://%s:%d/%s%n", bootstrapHost, bootstrapPort, bootstrapBindingName);
        System.out.printf("JAC=%d, coordinatorId=%d, leaderman=%s%n",
                workerNode.getJobAllocationCounter(),
                workerNode.getCurrentCoordinatorId(),
                workerNode.getLeaderman());
        System.out.println("Neighbours=" + workerNode.getNeighbours());
        System.out.println("Press Ctrl+C to stop.");

        Thread.currentThread().join();
    }
}
