package cs324.worker;

import cs324.bootstrap.WorkerInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WorkerService extends Remote {
    int getWorkerId() throws RemoteException;

    int getJobAllocationCounter() throws RemoteException;

    List<WorkerInfo> getNeighbours() throws RemoteException;

    void addNeighbour(WorkerInfo worker) throws RemoteException;

    ElectionResult startElection() throws RemoteException;

    ElectionResult receiveElection(String electionId, WorkerInfo sender) throws RemoteException;

    void announceCoordinator(String electionId, int coordinatorId) throws RemoteException;

    void receiveCoordinatorAnnouncement(String electionId, int coordinatorId, WorkerInfo sender) throws RemoteException;

    void markCoordinatorUnavailable() throws RemoteException;

    List<String> getProcessedElectionIds() throws RemoteException;

    int getCurrentCoordinatorId() throws RemoteException;

    String getLeaderman() throws RemoteException;
}
