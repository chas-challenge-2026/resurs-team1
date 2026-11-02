package se.comerit.resurs;

import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class CacheResetListener implements TestExecutionListener {
    @Override
    public void beforeTestMethod(TestContext testContext) {
        testContext.getApplicationContext()
                .getBeanProvider(CacheManager.class)
                .ifAvailable(cacheManager -> cacheManager.getCacheNames()
                        .forEach(name -> cacheManager.getCache(name).clear()));
    }
}
