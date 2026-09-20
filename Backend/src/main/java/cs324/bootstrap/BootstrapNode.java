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
    }

    @Override
    public boolean unregisterWorker(int workerId) throws RemoteException {
        return activeWorkers.remove(workerId) != null;
    }

    @Override
    public List<WorkerInfo> getActiveWorkers() throws RemoteException {
        return List.copyOf(activeWorkers.values());
    }

    @Override
    public WorkerInfo getRandomActiveWorker() throws RemoteException {
        List<WorkerInfo> snapshot = new ArrayList<>(activeWorkers.values());
        if (snapshot.isEmpty()) {
            return null;
        }

        return snapshot.get(ThreadLocalRandom.current().nextInt(snapshot.size()));
    }
}
