package cs324.worker;

import cs324.bootstrap.BootstrapServer;
import cs324.bootstrap.BootstrapService;
import cs324.bootstrap.WorkerInfo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

public class WorkerNode extends UnicastRemoteObject implements WorkerService {
    private static final long serialVersionUID = 1L;

    private final int workerId;
    private final WorkerInfo selfInfo;
    private final AtomicInteger jobAllocationCounter = new AtomicInteger(0);
    private final CopyOnWriteArrayList<WorkerInfo> neighbours = new CopyOnWriteArrayList<>();
    private final Set<String> processedElectionIds = ConcurrentHashMap.newKeySet();
    private final Set<String> processedCoordinatorAnnouncementIds = ConcurrentHashMap.newKeySet();
    private volatile int currentCoordinatorId;
    private final String leaderman = "cs324";

    public WorkerNode(int workerId, String host, int port) throws java.rmi.RemoteException {
        super();
        if (workerId < 0) {
            throw new IllegalArgumentException("Worker id must not be negative");
        }

        this.workerId = workerId;
        this.selfInfo = new WorkerInfo(workerId, host, port);
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
    public ElectionResult startElection() throws java.rmi.RemoteException {
        String electionId = workerId + "-" + UUID.randomUUID();
        ElectionResult result = receiveElection(electionId, selfInfo);
        announceCoordinator(electionId, result.getCoordinatorId());
        return result;
    }

    @Override
    public ElectionResult receiveElection(String electionId, WorkerInfo sender) {
        if (electionId == null || electionId.isBlank()) {
            throw new IllegalArgumentException("Election id must not be blank");
        }

        if (!processedElectionIds.add(electionId)) {
            return new ElectionResult(electionId, -1, List.of());
        }

        Set<ElectionCandidate> reachableCandidates = new HashSet<>();
        reachableCandidates.add(new ElectionCandidate(selfInfo, jobAllocationCounter.get()));

        for (WorkerInfo neighbourInfo : neighbours) {
            if (sender != null && neighbourInfo.getId() == sender.getId()) {
                continue;
            }

            try {
                WorkerService neighbour = lookupWorker(neighbourInfo);
                ElectionResult neighbourResult = neighbour.receiveElection(electionId, selfInfo);
                reachableCandidates.addAll(neighbourResult.getReachableCandidates());
            } catch (Exception exception) {
                System.err.printf("Worker %d could not forward election %s to worker %d: %s%n",
                        workerId,
                        electionId,
                        neighbourInfo.getId(),
                        exception.getMessage());
            }
        }

        int coordinatorId = reachableCandidates.stream()
                .min(Comparator.comparingInt(ElectionCandidate::getJobAllocationCounter)
                        .thenComparing(Comparator.comparingInt(ElectionCandidate::getWorkerId).reversed()))
                .map(ElectionCandidate::getWorkerId)
                .orElse(workerId);

        return new ElectionResult(electionId, coordinatorId, new ArrayList<>(reachableCandidates));
    }

    @Override
    public void announceCoordinator(String electionId, int coordinatorId) {
        if (electionId == null || electionId.isBlank()) {
            throw new IllegalArgumentException("Election id must not be blank");
        }

        currentCoordinatorId = coordinatorId;
        processedCoordinatorAnnouncementIds.add(electionId);

        for (WorkerInfo neighbourInfo : neighbours) {
            try {
                WorkerService neighbour = lookupWorker(neighbourInfo);
                neighbour.receiveCoordinatorAnnouncement(electionId, coordinatorId, selfInfo);
            } catch (Exception exception) {
                System.err.printf("Worker %d could not announce coordinator %d to worker %d: %s%n",
                        workerId,
                        coordinatorId,
                        neighbourInfo.getId(),
                        exception.getMessage());
            }
        }
    }

    @Override
    public void markCoordinatorUnavailable() {
        currentCoordinatorId = -1;
    }

    @Override
    public List<String> getProcessedElectionIds() {
        return List.copyOf(processedElectionIds);
    }

    @Override
    public int getCurrentCoordinatorId() {
        return currentCoordinatorId;
    }

    @Override
    public String getLeaderman() {
        return leaderman;
    }

    @Override
    public void receiveCoordinatorAnnouncement(String electionId, int coordinatorId, WorkerInfo sender) {
        if (!processedCoordinatorAnnouncementIds.add(electionId)) {
            return;
        }

        currentCoordinatorId = coordinatorId;

        for (WorkerInfo neighbourInfo : neighbours) {
            if (sender != null && neighbourInfo.getId() == sender.getId()) {
                continue;
            }

            try {
                WorkerService neighbour = lookupWorker(neighbourInfo);
                neighbour.receiveCoordinatorAnnouncement(electionId, coordinatorId, selfInfo);
            } catch (Exception exception) {
                System.err.printf("Worker %d could not forward coordinator announcement to worker %d: %s%n",
                        workerId,
                        neighbourInfo.getId(),
                        exception.getMessage());
            }
        }
    }

    private static WorkerService lookupWorker(WorkerInfo workerInfo) throws Exception {
        Registry registry = LocateRegistry.getRegistry(workerInfo.getHost(), workerInfo.getPort());
        return (WorkerService) registry.lookup("Worker-" + workerInfo.getId());
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

        WorkerNode workerNode = new WorkerNode(workerId, "localhost", workerPort);
        Registry workerRegistry = LocateRegistry.createRegistry(workerPort);
        workerRegistry.rebind(workerBindingName, workerNode);

        Registry bootstrapRegistry = LocateRegistry.getRegistry(bootstrapHost, bootstrapPort);
        BootstrapService bootstrap = (BootstrapService) bootstrapRegistry.lookup(bootstrapBindingName);

        WorkerInfo workerInfo = new WorkerInfo(workerId, "localhost", workerPort);
        WorkerInfo neighbourInfo = bootstrap.getRandomActiveWorker();
        if (neighbourInfo != null && neighbourInfo.getId() != workerId) {
            workerNode.addNeighbour(neighbourInfo);

            WorkerService neighbour = lookupWorker(neighbourInfo);
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
