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
        log("NEIGHBOUR added workerId=%d host=%s port=%d neighbours=%s",
                worker.getId(),
                worker.getHost(),
                worker.getPort(),
                neighbours);
    }

    @Override
    public ElectionResult startElection() throws java.rmi.RemoteException {
        String electionId = workerId + "-" + UUID.randomUUID();
        log("ELECTION start electionId=%s workerId=%d JAC=%d neighbours=%s",
                electionId,
                workerId,
                jobAllocationCounter.get(),
                neighbours);
        ElectionResult result = receiveElection(electionId, selfInfo);
        log("ELECTION selected coordinatorId=%d candidates=%s",
                result.getCoordinatorId(),
                result.getReachableCandidates());
        announceCoordinator(electionId, result.getCoordinatorId());
        return result;
    }

    @Override
    public ElectionResult receiveElection(String electionId, WorkerInfo sender) {
        if (electionId == null || electionId.isBlank()) {
            throw new IllegalArgumentException("Election id must not be blank");
        }

        if (!processedElectionIds.add(electionId)) {
            log("ELECTION duplicate ignored electionId=%s sender=%s",
                    electionId,
                    formatWorker(sender));
            return new ElectionResult(electionId, -1, List.of());
        }

        log("ELECTION received electionId=%s sender=%s workerId=%d JAC=%d",
                electionId,
                formatWorker(sender),
                workerId,
                jobAllocationCounter.get());

        Set<ElectionCandidate> reachableCandidates = new HashSet<>();
        reachableCandidates.add(new ElectionCandidate(selfInfo, jobAllocationCounter.get()));

        for (WorkerInfo neighbourInfo : neighbours) {
            if (sender != null && neighbourInfo.getId() == sender.getId()) {
                log("ELECTION skip sender workerId=%d electionId=%s",
                        neighbourInfo.getId(),
                        electionId);
                continue;
            }

            try {
                log("ELECTION forward electionId=%s to workerId=%d",
                        electionId,
                        neighbourInfo.getId());
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

        log("ELECTION local result electionId=%s selectedCoordinator=%d candidates=%s",
                electionId,
                coordinatorId,
                reachableCandidates);
        return new ElectionResult(electionId, coordinatorId, new ArrayList<>(reachableCandidates));
    }

    @Override
    public void announceCoordinator(String electionId, int coordinatorId) {
        if (electionId == null || electionId.isBlank()) {
            throw new IllegalArgumentException("Election id must not be blank");
        }

        currentCoordinatorId = coordinatorId;
        processedCoordinatorAnnouncementIds.add(electionId);
        log("COORDINATOR selected electionId=%s coordinatorId=%d", electionId, coordinatorId);

        for (WorkerInfo neighbourInfo : neighbours) {
            try {
                log("COORDINATOR send electionId=%s coordinatorId=%d to workerId=%d",
                        electionId,
                        coordinatorId,
                        neighbourInfo.getId());
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
        log("COORDINATOR unavailable currentCoordinatorId=-1");
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
            log("COORDINATOR duplicate ignored electionId=%s sender=%s",
                    electionId,
                    formatWorker(sender));
            return;
        }

        currentCoordinatorId = coordinatorId;
        log("COORDINATOR received electionId=%s coordinatorId=%d sender=%s",
                electionId,
                coordinatorId,
                formatWorker(sender));

        for (WorkerInfo neighbourInfo : neighbours) {
            if (sender != null && neighbourInfo.getId() == sender.getId()) {
                log("COORDINATOR skip sender workerId=%d electionId=%s",
                        neighbourInfo.getId(),
                        electionId);
                continue;
            }

            try {
                log("COORDINATOR forward electionId=%s coordinatorId=%d to workerId=%d",
                        electionId,
                        coordinatorId,
                        neighbourInfo.getId());
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

    private void log(String message, Object... args) {
        System.out.printf("[WORKER %d] %s%n", workerId, String.format(message, args));
    }

    private static String formatWorker(WorkerInfo workerInfo) {
        if (workerInfo == null) {
            return "none";
        }

        return String.format("workerId=%d host=%s port=%d",
                workerInfo.getId(),
                workerInfo.getHost(),
                workerInfo.getPort());
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
        workerNode.log("START workerId=%d rmiUrl=rmi://localhost:%d/%s JAC=%d coordinatorId=%d leaderman=%s",
                workerId,
                workerPort,
                workerBindingName,
                workerNode.getJobAllocationCounter(),
                workerNode.getCurrentCoordinatorId(),
                workerNode.getLeaderman());

        Registry bootstrapRegistry = LocateRegistry.getRegistry(bootstrapHost, bootstrapPort);
        BootstrapService bootstrap = (BootstrapService) bootstrapRegistry.lookup(bootstrapBindingName);
        workerNode.log("BOOTSTRAP connected rmi://%s:%d/%s",
                bootstrapHost,
                bootstrapPort,
                bootstrapBindingName);

        WorkerInfo workerInfo = new WorkerInfo(workerId, "localhost", workerPort);
        WorkerInfo neighbourInfo = bootstrap.getRandomActiveWorker();
        if (neighbourInfo != null && neighbourInfo.getId() != workerId) {
            workerNode.log("NEIGHBOUR bootstrap selected %s", formatWorker(neighbourInfo));
            workerNode.addNeighbour(neighbourInfo);

            WorkerService neighbour = lookupWorker(neighbourInfo);
            workerNode.log("NEIGHBOUR notifying workerId=%d to add workerId=%d",
                    neighbourInfo.getId(),
                    workerId);
            neighbour.addNeighbour(workerInfo);
        } else {
            workerNode.log("NEIGHBOUR none available from bootstrap");
        }

        workerNode.log("REGISTER sending to bootstrap workerId=%d host=%s port=%d",
                workerInfo.getId(),
                workerInfo.getHost(),
                workerInfo.getPort());
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
