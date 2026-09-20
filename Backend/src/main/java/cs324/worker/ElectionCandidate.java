package cs324.worker;

import cs324.bootstrap.WorkerInfo;

import java.io.Serializable;
import java.util.Objects;

public final class ElectionCandidate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final WorkerInfo workerInfo;
    private final int jobAllocationCounter;

    public ElectionCandidate(WorkerInfo workerInfo, int jobAllocationCounter) {
        if (workerInfo == null) {
            throw new IllegalArgumentException("Worker info must not be null");
        }

        this.workerInfo = workerInfo;
        this.jobAllocationCounter = jobAllocationCounter;
    }

    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public int getWorkerId() {
        return workerInfo.getId();
    }

    public int getJobAllocationCounter() {
        return jobAllocationCounter;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ElectionCandidate)) {
            return false;
        }
        ElectionCandidate that = (ElectionCandidate) other;
        return workerInfo.getId() == that.workerInfo.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerInfo.getId());
    }

    @Override
    public String toString() {
        return "ElectionCandidate{"
                + "workerInfo=" + workerInfo
                + ", JAC=" + jobAllocationCounter
                + '}';
    }
}
