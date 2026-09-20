package cs324.bootstrap;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

public class BootstrapNode extends UnicastRemoteObject implements BootstrapService {
    private static final long serialVersionUID = 1L;

    private final ConcurrentMap<Integer, WorkerInfo> activeWorkers = new ConcurrentHashMap<>();

    public BootstrapNode() throws RemoteException {
        super();
    }

    @Override
    public void registerWorker(WorkerInfo worker) throws RemoteException {
        if (worker == null) {
            throw new IllegalArgumentException("Worker must not be null");
        }

        activeWorkers.put(worker.getId(), worker);
        System.out.printf("[BOOTSTRAP] REGISTER workerId=%d host=%s port=%d activeWorkers=%s%n",
                worker.getId(),
                worker.getHost(),
                worker.getPort(),
                getActiveWorkers());
    }

    @Override
    public boolean unregisterWorker(int workerId) throws RemoteException {
        boolean removed = activeWorkers.remove(workerId) != null;
        System.out.printf("[BOOTSTRAP] UNREGISTER workerId=%d removed=%s activeWorkers=%s%n",
                workerId,
                removed,
                getActiveWorkers());
        return removed;
    }

    @Override
    public List<WorkerInfo> getActiveWorkers() throws RemoteException {
        return List.copyOf(activeWorkers.values());
    }

    @Override
    public WorkerInfo getRandomActiveWorker() throws RemoteException {
        List<WorkerInfo> snapshot = new ArrayList<>(activeWorkers.values());
        if (snapshot.isEmpty()) {
            System.out.println("[BOOTSTRAP] RANDOM request -> no active workers");
            return null;
        }

        WorkerInfo selected = snapshot.get(ThreadLocalRandom.current().nextInt(snapshot.size()));
        System.out.printf("[BOOTSTRAP] RANDOM request -> workerId=%d from activeWorkers=%s%n",
                selected.getId(),
                snapshot);
        return selected;
    }
}
