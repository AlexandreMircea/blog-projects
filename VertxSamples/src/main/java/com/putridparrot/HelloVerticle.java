package com.putridparrot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.impl.DiscoveryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloVerticle.class);
    private static final String ROOT = "/hello";

    private final Router router;
    private ServiceDiscovery discovery;
    private Record publishedRecord;

    public HelloVerticle(Router router) {
        this.router = router;
    }

    @Override
    public void start(Future<Void> future) {

        SharedVerticle
            .configuration(vertx)
            .setHandler(ar ->
        {
             if(ar.succeeded()) {
                JsonObject o  = ar.result();
                int port = o.getInteger("hello.port", 8080);

                router.route(ROOT).handler(ctx -> {

                    LOGGER.debug("Hello called");

                    vertx.eventBus()
                            .publish(SharedVerticle.SVC_BUS, "Hello Service");

                    ctx.response()
                            .putHeader("content-type", "text/plain")
                            .end("Hello " + ctx.queryParam("name"));
                });

                vertx.createHttpServer()
                        .requestHandler(router::accept)
                        .listen(port, l ->
                        {
                            if(l.succeeded()) {
                                LOGGER.info("HTTP \"Hello\" server started on port " + port);

                                discovery = SharedVerticle.createServiceDiscovery(vertx);

                                SharedVerticle.publish(discovery,
                                        "hello-service",
                                        "localhost",
                                        port,
                                        ROOT)
                                        .setHandler(r ->
                                        {
                                            if(r.succeeded()) {
                                                publishedRecord = r.result();
                                            }
                                        });

                                future.succeeded();
                            }
                            else {
                                future.fail(l.cause());
                            }
                        });
            }
        });
    }

    @Override
    public void stop() {
        SharedVerticle.unpublish(discovery, publishedRecord);
    }
}
