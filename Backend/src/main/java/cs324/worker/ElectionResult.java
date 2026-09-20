package cs324.worker;

import cs324.bootstrap.WorkerInfo;

import java.io.Serializable;
import java.util.List;

public final class ElectionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String electionId;
    private final int coordinatorId;
    private final List<WorkerInfo> reachableWorkers;

    public ElectionResult(String electionId, int coordinatorId, List<WorkerInfo> reachableWorkers) {
        this.electionId = electionId;
        this.coordinatorId = coordinatorId;
        this.reachableWorkers = List.copyOf(reachableWorkers);
    }

    public String getElectionId() {
        return electionId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public List<WorkerInfo> getReachableWorkers() {
        return reachableWorkers;
    }

    @Override
    public String toString() {
        return "ElectionResult{"
                + "electionId='" + electionId + '\''
                + ", coordinatorId=" + coordinatorId
                + ", reachableWorkers=" + reachableWorkers
                + '}';
    }
}
