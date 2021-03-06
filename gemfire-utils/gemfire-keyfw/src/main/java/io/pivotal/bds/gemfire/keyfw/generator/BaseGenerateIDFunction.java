package io.pivotal.bds.gemfire.keyfw.generator;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.execute.ResultSender;

public abstract class BaseGenerateIDFunction<T> implements Function {

    private String id;
    protected final Logger log;

    private static final long serialVersionUID = 1L;

    public BaseGenerateIDFunction(String id) {
        this.id = id;
        this.log = LoggerFactory.getLogger(getClass());
    }

    public BaseGenerateIDFunction() {
        this(null);
    }

    @Override
    public void execute(FunctionContext context) {
        ResultSender<T> sender = context.getResultSender();

        try {
            Object args = context.getArguments();
            log.debug("execute: args={}", args);

            if (args == null) {
                throw new NullPointerException("Missing arguments");
            }

            Class<?> c = args.getClass();
            String domain = null;

            if (c.isArray()) {
                String[] s = (String[]) args;
                domain = s[0];
            } else if (c == String.class) {
                domain = (String) args;
            } else {
                throw new IllegalArgumentException("Invalid type for args: " + c);
            }

            log.debug("execute: domain={}", domain);

            if (context instanceof RegionFunctionContext) {
                // generate multiple keys, filter is not important, the number of objects in filter determines the number of keys
                RegionFunctionContext rctx = (RegionFunctionContext) context;
                Set<?> filter = rctx.getFilter();

                if (filter.isEmpty()) {
                    throw new IllegalArgumentException("Filter is empty");
                }

                Iterator<?> iter = filter.iterator();

                while (iter.hasNext()) {
                    iter.next();
                    log.debug("execute: domain={}", domain);

                    T id = getGenerator().generate(domain);

                    log.debug("execute: id={}", id);

                    if (iter.hasNext()) {
                        sender.sendResult(id);
                    } else {
                        sender.lastResult(id);
                    }
                }

            } else {
                T id = getGenerator().generate(domain);

                log.debug("execute: id={}", id);

                sender.lastResult(id);
            }

        } catch (Exception x) {
            log.error(x.toString(), x);
            throw new FunctionException(x.toString(), x);
        }
    }

    @Override
    public String getId() {
        return id == null ? getClass().getSimpleName() : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public boolean isHA() {
        return true;
    }

    @Override
    public boolean optimizeForWrite() {
        return true;
    }

    protected abstract IDGenerator<T> getGenerator();
}
