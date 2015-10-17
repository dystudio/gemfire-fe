package io.pivotal.bds.gemfire.drools.server.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.Globals;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.RegionFunctionContext;
import com.gemstone.gemfire.cache.execute.ResultSender;

import io.pivotal.bds.gemfire.drools.server.data.RuleExecutionContext;
import io.pivotal.bds.gemfire.drools.server.data.RuleExecutionResult;
import io.pivotal.bds.gemfire.drools.server.util.ApplicationContextGlobals;
import io.pivotal.bds.metrics.timer.Timer;

public class StatefulRuleExecutionFunction implements Function, ApplicationContextAware {

    private ApplicationContext context;
    private KieServices services;
    private Timer timer = new Timer("StatefulRuleExecutionFunction");

    private static final Logger LOG = LoggerFactory.getLogger(StatefulRuleExecutionFunction.class);

    private static final long serialVersionUID = 1L;

    public StatefulRuleExecutionFunction(KieServices services) {
        this.services = services;
    }

    @Override
    public void execute(FunctionContext context) {
        LOG.info("execute");

        try {
            ResultSender<RuleExecutionResult> sender = context.getResultSender();
            RuleExecutionContext rec = (RuleExecutionContext) context.getArguments();
            RegionFunctionContext rctx = (RegionFunctionContext) context;

            ReleaseId relId = rec.getReleaseId();
            Object args = rec.getArguments();
            LOG.info("execute: args={}", args);

            Set<?> filter = rctx.getFilter();

            timer.start();
            for (Object o : filter) {
                LOG.info("execute: o={}, args={}", o, args);

                KieContainer container = services.newKieContainer(relId);
                KieSession session = container.newKieSession();

                try {
                    Globals globals = session.getGlobals();
                    globals.set("log", LOG);
                    globals.setDelegate(new ApplicationContextGlobals(this.context));

                    session.insert(o);

                    if (args != null) {
                        session.insert(args);
                    }

                    RuleExecutionResult result = new RuleExecutionResult(new ArrayList<>(), new HashMap<>());
                    session.insert(result);

                    int cnt = session.fireAllRules();
                    LOG.info("execute: o={}, args={}, cnt={}, result={}", o, args, cnt, result);

                    sender.lastResult(result);
                } finally {
                    session.dispose();
                }
            }
            timer.end();
        } catch (FunctionException x) {
            LOG.error("execute: x={}", x.toString(), x);
            throw x;
        } catch (Exception x) {
            LOG.error("execute: x={}", x.toString(), x);
            throw new FunctionException(x.toString(), x);
        }
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
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

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        LOG.info("setApplicationContext");
        this.context = context;
    }

}
