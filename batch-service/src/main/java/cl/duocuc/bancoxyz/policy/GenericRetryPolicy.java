package cl.duocuc.bancoxyz.policy;
 
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;
 
import java.sql.SQLTransientException;
 

public class GenericRetryPolicy implements RetryPolicy {
 
    private final int maxIntentos;
 
    public GenericRetryPolicy(int maxIntentos) {
        this.maxIntentos = maxIntentos;
    }
 
    @Override
    public boolean canRetry(RetryContext context) {
        return context.getRetryCount() < maxIntentos
                && context.getLastThrowable() instanceof SQLTransientException;
    }
 
    @Override
    public RetryContext open(RetryContext parent) {
        return new RetryContextSupport(parent);
    }
 
    @Override
    public void close(RetryContext context) {
    }
 
    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        ((RetryContextSupport) context).registerThrowable(throwable);
    }
}
 