package cs324.bootstrap;

import java.io.Serializable;
import java.util.Objects;

public final class WorkerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String host;
    private final int port;

    public WorkerInfo(int id, String host, int port) {
        if (id < 0) {
            throw new IllegalArgumentException("Worker id must not be negative");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Worker host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Worker port must be between 1 and 65535");
        }

        this.id = id;
        this.host = host;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkerInfo)) {
            return false;
        }
        WorkerInfo that = (WorkerInfo) other;
        return port == that.port
                && id == that.id
                && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port);
    }

    @Override
    public String toString() {
        return "WorkerInfo{"
                + "id=" + id
                + ", host='" + host + '\''
                + ", port=" + port
                + '}';
    }
}
