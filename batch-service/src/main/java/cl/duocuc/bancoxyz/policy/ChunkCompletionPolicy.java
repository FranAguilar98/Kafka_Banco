package cl.duocuc.bancoxyz.policy;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;

public class ChunkCompletionPolicy extends SimpleCompletionPolicy {

    private final long maxDurationMillis;
    private long chunkStartTime;

    public ChunkCompletionPolicy(int chunkSize, long maxDurationMillis) {
        super(chunkSize);
        this.maxDurationMillis = maxDurationMillis;
    }

    @Override
    public RepeatContext start(RepeatContext parent) {
        this.chunkStartTime = System.currentTimeMillis();
        return super.start(parent);
    }

    @Override
    public boolean isComplete(RepeatContext context) {
        boolean tiempoExcedido = (System.currentTimeMillis() - chunkStartTime) >= maxDurationMillis;
        return super.isComplete(context) || tiempoExcedido;
    }
}