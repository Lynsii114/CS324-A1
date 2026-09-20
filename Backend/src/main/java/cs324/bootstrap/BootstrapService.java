package cs324.bootstrap;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BootstrapService extends Remote {
    void registerWorker(WorkerInfo worker) throws RemoteException;

    boolean unregisterWorker(int workerId) throws RemoteException;

    List<WorkerInfo> getActiveWorkers() throws RemoteException;

    WorkerInfo getRandomActiveWorker() throws RemoteException;
}
