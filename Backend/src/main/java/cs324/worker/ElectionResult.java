package cs324.worker;

import cs324.bootstrap.WorkerInfo;

import java.io.Serializable;
import java.util.List;

public final class ElectionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String electionId;
    private final int coordinatorId;
    private final List<ElectionCandidate> reachableCandidates;

    public ElectionResult(String electionId, int coordinatorId, List<ElectionCandidate> reachableCandidates) {
        this.electionId = electionId;
        this.coordinatorId = coordinatorId;
        this.reachableCandidates = List.copyOf(reachableCandidates);
    }

    public String getElectionId() {
        return electionId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public List<ElectionCandidate> getReachableCandidates() {
        return reachableCandidates;
    }

    public List<WorkerInfo> getReachableWorkers() {
        return reachableCandidates.stream()
                .map(ElectionCandidate::getWorkerInfo)
                .toList();
    }

    @Override
    public String toString() {
        return "ElectionResult{"
                + "electionId='" + electionId + '\''
                + ", coordinatorId=" + coordinatorId
                + ", reachableCandidates=" + reachableCandidates
                + '}';
    }
}
