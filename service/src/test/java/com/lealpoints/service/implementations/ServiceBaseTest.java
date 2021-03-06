package com.lealpoints.service.implementations;

import xyz.greatapp.libs.service.context.ThreadContext;
import xyz.greatapp.libs.service.context.ThreadContextService;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class ServiceBaseTest {

    protected static ThreadContextService createThreadContextService(ThreadContext threadContext, int times) {
        ThreadContextService threadContextService = createMock(ThreadContextService.class);
        expect(threadContextService.getThreadContext()).andReturn(threadContext).times(times);
        replay(threadContextService);
        return threadContextService;
    }

    protected static ThreadContextService createThreadContextService(ThreadContext threadContext) {
        return createThreadContextService(threadContext, 1);
    }
}
